package com.alpaca.core.interceptors.pagination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }),
		@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = { Statement.class }) })
public class PageHelper implements Interceptor {

	private static final Log logger = LogFactory.getLog(PageHelper.class);
	public static final ThreadLocal<Page> localPage = new ThreadLocal<Page>();
	private static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	private static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	private String dialect = "mysql";

	/**
	 * 开始分页
	 * 
	 * @param currentPage
	 * @param pageSize
	 */
	public static void startPage(int currentPage, int pageSize) {
		localPage.set(new Page(currentPage, pageSize));
	}

	/**
	 * 结束分页并返回结果，该方法必须被调用，否则localPage会一直保存下去，直到下一次startPage
	 * 
	 * @return
	 */
	public static Page endPage() {
		Page page = localPage.get();
		localPage.remove();
		return page;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (localPage.get() == null) { // 没有throadlocal 不拦截
			return invocation.proceed();
		}
		if (invocation.getTarget() instanceof StatementHandler) {
			StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
			MetaObject metaStatementHandler = MetaObject.forObject(statementHandler, DEFAULT_OBJECT_FACTORY,
					DEFAULT_OBJECT_WRAPPER_FACTORY);
			// 分离代理对象链(由于目标类可能被多个拦截器拦截，从而形成多次代理，通过下面的两次循环
			// 可以分离出最原始的的目标类)
			while (metaStatementHandler.hasGetter("h")) {
				Object object = metaStatementHandler.getValue("h");
				metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
						DEFAULT_OBJECT_WRAPPER_FACTORY);
			}
			// 分离最后一个代理对象的目标类
			while (metaStatementHandler.hasGetter("target")) {
				Object object = metaStatementHandler.getValue("target");
				metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
						DEFAULT_OBJECT_WRAPPER_FACTORY);
			}
			MappedStatement mappedStatement = (MappedStatement) metaStatementHandler
					.getValue("delegate.mappedStatement");
			// 分页信息
			Page page = localPage.get();
			BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
			// 分页参数作为参数对象parameterObject的一个属性
			String sql = boundSql.getSql();
			// 重写sql
			String pageSql = buildPageSql(sql, page);
			// 重写分页sql
			metaStatementHandler.setValue("delegate.boundSql.sql", pageSql);
			Connection connection = (Connection) invocation.getArgs()[0];
			// 重设分页参数里的总页数等
			setPageParameter(sql, connection, mappedStatement, boundSql, page);
			// 将执行权交给下一个拦截器
			return invocation.proceed();
		} else if (invocation.getTarget() instanceof ResultSetHandler) { // 处理查询结果集
			Object result = invocation.proceed();
			Page page = localPage.get();
			page.setResult((List<?>) result);
		    return result; 
		}
		return null;
	}

	private String buildPageSql(String sql, Page page) {
		if (page != null) {
			StringBuilder pageSql = new StringBuilder();
			if ("mysql".equals(dialect)) {
				pageSql = buildPageSqlForMysql(sql, page);
			} else if ("oracle".equals(dialect)) {
				pageSql = buildPageSqlForOracle(sql, page);
			} else {
				return sql;
			}
			return pageSql.toString();
		} else {
			return sql;
		}
	}

	public StringBuilder buildPageSqlForMysql(String sql, Page page) {
		StringBuilder pageSql = new StringBuilder(100);
		String beginrow = String.valueOf((page.getCurrentPage() - 1) * page.getPageSize());
		pageSql.append(sql);
		pageSql.append(" limit " + beginrow + "," + page.getPageSize());
		return pageSql;
	}

	public StringBuilder buildPageSqlForOracle(String sql, Page page) {
		StringBuilder pageSql = new StringBuilder(100);
		String beginrow = String.valueOf((page.getCurrentPage() - 1) * page.getPageSize());
		String endrow = String.valueOf(page.getCurrentPage() * page.getPageSize());
		pageSql.append("select * from ( select temp.*, rownum row_id from ( ");
		pageSql.append(sql);
		pageSql.append(" ) temp where rownum <= ").append(endrow);
		pageSql.append(") where row_id > ").append(beginrow);
		return pageSql;
	}

	private void setPageParameter(String sql, Connection connection, MappedStatement mappedStatement,
			BoundSql boundSql, Page page) {
		String countSql = "select count(0) from (" + sql + ") as total";
		PreparedStatement countStmt = null;
		ResultSet rs = null;
		try {
			countStmt = connection.prepareStatement(countSql);
			BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql,
					boundSql.getParameterMappings(), boundSql.getParameterObject());
			setParameters(countStmt, mappedStatement, countBS, boundSql.getParameterObject());
			rs = countStmt.executeQuery();
			int totalCount = 0;
			if (rs.next()) {
				totalCount = rs.getInt(1);
			}
			page.setTotalCount(totalCount);
			int totalPage = totalCount / page.getPageSize() + ((totalCount % page.getPageSize() == 0) ? 0 : 1);
			page.setTotalPage(totalPage);
		} catch (SQLException e) {
			logger.error("Ignore this exception", e);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("Ignore this exception", e);
			}
			try {
				countStmt.close();
			} catch (SQLException e) {
				logger.error("Ignore this exception", e);
			}
		}
	}

	private void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql,
			Object parameterObject) throws SQLException {
		ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
		parameterHandler.setParameters(ps);
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler || target instanceof ResultSetHandler) {
			return Plugin.wrap(target, this);
		} else {
			return target;
		}
	}

	@Override
	public void setProperties(Properties properties) {
	}
}

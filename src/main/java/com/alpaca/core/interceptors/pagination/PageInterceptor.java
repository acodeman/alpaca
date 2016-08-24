package com.alpaca.core.interceptors.pagination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.ibatis.executor.parameter.ParameterHandler;
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
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;

@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }) })
public class PageInterceptor implements Interceptor {

	private static final Log logger = LogFactory.getLog(PageInterceptor.class);
	private static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	private static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	private static final String DEFAULT_PAGE_SQL_ID = ".*Page$"; // 需要拦截的ID(正则匹配)
	private static final String DEFAULT_DIALECT = "mysql";
	private String dialect;

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
		MetaObject metaStatementHandler = MetaObject.forObject(statementHandler, DEFAULT_OBJECT_FACTORY,
				DEFAULT_OBJECT_WRAPPER_FACTORY);
		// 分离代理对象链(由于目标类可能被多个拦截器拦截，从而形成多次代理，通过下面的两次循环
		// 可以分离出最原始的的目标类)
		while (metaStatementHandler.hasGetter("h")) {
			Object object = metaStatementHandler.getValue("h");
			metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
		}
		// 分离最后一个代理对象的目标类
		while (metaStatementHandler.hasGetter("target")) {
			Object object = metaStatementHandler.getValue("target");
			metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
		}
		Configuration configuration = (Configuration) metaStatementHandler.getValue("delegate.configuration");
		dialect = configuration.getVariables().getProperty("dialect");
		if (null == dialect || "".equals(dialect)) {
			logger.warn("Property dialect is not setted,use default 'mysql' ");
			dialect = DEFAULT_DIALECT;
		}
		String pageSqlId = configuration.getVariables().getProperty("pageSqlId");
		if (null == pageSqlId || "".equals(pageSqlId)) {
			logger.warn("Property pageSqlId is not setted,use default '.*Page$' ");
			pageSqlId = DEFAULT_PAGE_SQL_ID;
		}
		MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
		if (mappedStatement.getId().matches(pageSqlId)) {
			BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
			Object parameterObject = boundSql.getParameterObject();
			if (parameterObject == null) {
				throw new NullPointerException("parameterObject is null!");
			} else {
				PageInfo page = (PageInfo) metaStatementHandler
						.getValue("delegate.boundSql.parameterObject.pageInfo");
				String sql = boundSql.getSql();
				String pageSql = buildPageSql(sql, page);
				metaStatementHandler.setValue("delegate.boundSql.sql", pageSql);
				metaStatementHandler.setValue("delegate.rowBounds.offset", RowBounds.NO_ROW_OFFSET);
				metaStatementHandler.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
				Connection connection = (Connection) invocation.getArgs()[0];
				setPageParameter(sql, connection, mappedStatement, boundSql, page);
			}
		}
		return invocation.proceed();
	}

	private String buildPageSql(String sql, PageInfo page) {
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

	public StringBuilder buildPageSqlForMysql(String sql, PageInfo page) {
		StringBuilder pageSql = new StringBuilder(100);
		String beginrow = String.valueOf((page.getCurrentPage() - 1) * page.getPageSize());
		pageSql.append(sql);
		pageSql.append(" limit " + beginrow + "," + page.getPageSize());
		return pageSql;
	}

	public StringBuilder buildPageSqlForOracle(String sql, PageInfo page) {
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
			BoundSql boundSql, PageInfo page) {
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
		if (target instanceof StatementHandler) {
			return Plugin.wrap(target, this);
		} else {
			return target;
		}
	}

	@Override
	public void setProperties(Properties properties) {}

}

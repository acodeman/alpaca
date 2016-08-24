package com.alpaca.core.interceptors.pagination;

import java.util.List;

/**
 * Description: 分页 Author: liuzh Update: liuzh(2014-04-16 10:56)
 */
public class Page {
	private int currentPage;
	private int pageSize;
	private int startRow;
	private int endRow;
	private long totalCount;
	private long totalPage;
	private int pages;
	private List<?> result;

	public Page(int currentPage, int pageSize) {
		this.currentPage = currentPage;
		this.pageSize = pageSize;
		this.startRow = currentPage > 0 ? (currentPage - 1) * pageSize : 0;
		this.endRow = currentPage * pageSize;
	}

	public List<?> getResult() {
		return result;
	}

	public void setResult(List<?> result) {
		this.result = result;
	}

	public int getPages() {
		return pages;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}

	public int getEndRow() {
		return endRow;
	}

	public void setEndRow(int endRow) {
		this.endRow = endRow;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getStartRow() {
		return startRow;
	}

	public void setStartRow(int startRow) {
		this.startRow = startRow;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	public long getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(long totalPage) {
		this.totalPage = totalPage;
	}

}

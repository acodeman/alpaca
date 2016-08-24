package com.alpaca.test;

import com.alpaca.core.interceptors.pagination.Page;

public interface TestUserMapper {

	public Page findUsersPage(String username , Page page);
}

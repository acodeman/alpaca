package com.alpaca.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alpaca.core.interceptors.pagination.Page;

/**
 * 
 * @author hjy
 * 
 */
@Service
public class TestUserService {

	@Autowired
	private TestUserMapper userMapper;

	public Page findUsersPage(String username, Page page) {
		return userMapper.findUsersPage("cjf",page);
	}
}

package com.alpaca.core.login;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author hjy
 * 
 */
@Service
public class UserService {
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserMapper userMapper;

	public User findByUsername(String username) {
		List<User> users = userMapper.findByUserName(username);
		if (users.size() > 1) {
			logger.warn("username '{}' is repeat", username);
			return users.get(0);
		} else if (users.size() == 1) {
			return users.get(0);
		} else {
			return null;
		}
	}
}

package com.alpaca.core.login;

import java.util.List;

public interface UserMapper {

	public List<User> findByUserName(String username);
}

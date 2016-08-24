package com.alpaca.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alpaca.core.interceptors.pagination.Page;

@Controller
@RequestMapping("/test")
public class TestController {
	@Autowired
	private TestUserService testUserService;
	
	/**
	 * 传入分页对象时,表示我要进行分页
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/users")
	public  Page users() {
		String username = null;
		return  testUserService.findUsersPage(username,new Page(2,1));
	}
	
	@ResponseBody
	@RequestMapping("/noPage")
	public  Page noPage() {
		String username = null;
//		PageHelper.startPage(2, 1);
		return  testUserService.findUsersPage(username,null);
//		return PageHelper.endPage();
	}
}

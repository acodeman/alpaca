package com.alpaca.core.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.alpaca.core.session.SessionFactory;

@Controller
public class LoginController {

	private final String LOGIN_PAGE = "login";
	
	private final String INDEX_PAGE = "index";
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SessionFactory sessionFactory; 
	
	@RequestMapping("/login")
	public ModelAndView login(HttpServletRequest req, HttpServletResponse res ,@RequestParam("username") String usernam1) {
		System.out.println("U"+ usernam1);
		String username = req.getParameter("username");
		String password = req.getParameter("password");
		System.out.println(username);
		if(!StringUtils.hasText(username) || !StringUtils.hasText(password)){
			return new ModelAndView(LOGIN_PAGE,"msg","请输入用户名和密码!");
		}
		User user = userService.findByUsername(username);
		if (user == null) {
			return new ModelAndView(LOGIN_PAGE,"msg","该用户不存在!");
		} else {
			if (user.getPassword().equals(password)) {
				sessionFactory.setSession(req.getSession());
				return new ModelAndView(INDEX_PAGE,"msg","登录成功!");
			} else {
				return new ModelAndView(LOGIN_PAGE,"msg","用户名或密码错误!");
			}
		}
	}
}

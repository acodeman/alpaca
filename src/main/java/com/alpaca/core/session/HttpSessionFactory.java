package com.alpaca.core.session;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

@Component
public class HttpSessionFactory extends SessionFactory{


	@Override
	public void getSession() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSession(HttpSession httpSession) {
		httpSession.setAttribute("data", "¹Â°Á²ÔÀÇ");
		String sessionId = httpSession.getId();
		System.out.println(sessionId);
	}

}

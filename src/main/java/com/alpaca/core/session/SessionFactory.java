package com.alpaca.core.session;

import javax.servlet.http.HttpSession;

abstract public class SessionFactory {
	
	public abstract void setSession(HttpSession httpSession);

	public abstract void getSession();
	
}

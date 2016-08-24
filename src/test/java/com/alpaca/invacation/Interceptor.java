package com.alpaca.invacation;


public interface Interceptor {
	
	public Object intercept(Invacation invacation) throws Throwable;
	
	public Object register(Object target);
}

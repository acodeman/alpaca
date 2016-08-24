package com.alpaca.invacation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Invacation {
	private Object target;
	private Method method;
	private Object[] args;


	public Invacation(Object target, Method method, Object[] args) {
		super();
		this.target = target;
		this.method = method;
		this.args = args;
	}

	public Object process() throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		return method.invoke(target, args);
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

}

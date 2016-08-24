package com.alpaca.invacation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.util.ObjectUtils;

public class TargetProxy implements InvocationHandler {

	private Object target;

	private Interceptor interceptor;
	
	public TargetProxy(Object target, Interceptor interceptor) {
		super();
		this.target = target;
		this.interceptor = interceptor;
	}

	public static Object bind(Object target,Interceptor interceptor) {
		return Proxy.newProxyInstance(target.getClass().getClassLoader(),
				target.getClass().getInterfaces(), new TargetProxy(target,interceptor));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		MethodName methodName = interceptor.getClass().getAnnotation(MethodName.class);
		if(methodName == null){
			return interceptor.intercept(new Invacation(target,method,args));
		}
		String name = methodName.value();
		System.out.println("name : " + name);
		if(name.equals(method.getName())){
			return interceptor.intercept(new Invacation(target,method,args));
		}else{
			return method.invoke(target, args);
		}
	}
}
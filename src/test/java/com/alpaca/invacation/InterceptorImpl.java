package com.alpaca.invacation;

@MethodName(value = "execute", name = "")
public class InterceptorImpl implements Interceptor{

	@Override
	public Object intercept(Invacation invacation) throws Throwable {
		System.out.println("狗日的");
		return invacation.process();
	}

	@Override
	public Object register(Object target) {
		return TargetProxy.bind(target, this);
	}

}

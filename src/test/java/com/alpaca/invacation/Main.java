package com.alpaca.invacation;


public class Main {

	public static void main(String[] args) {
		TargetObject target = new TargetImpl();
		InterceptorImpl interceptor = new InterceptorImpl();
		TargetObject proxy = (TargetObject) interceptor.register(target);
		proxy.execute();
		proxy.execute2();

	}

}

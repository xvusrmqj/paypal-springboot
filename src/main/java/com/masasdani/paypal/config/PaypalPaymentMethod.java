package com.masasdani.paypal.config;

public enum PaypalPaymentMethod {

	/**
	 * paypal的直接信用卡支付有限制，只支持英国。所以这种方式不能用。
	 */
	@Deprecated
	credit_card,
	paypal
	
}

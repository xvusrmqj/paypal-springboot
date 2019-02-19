package com.masasdani.paypal.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.masasdani.paypal.config.PaypalPaymentIntent;
import com.masasdani.paypal.config.PaypalPaymentMethod;
import com.masasdani.paypal.service.PaypalService;
import com.masasdani.paypal.util.URLUtils;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

/**
 * https://developer.paypal.com/docs/api/quickstart/payments/#
 */
@Controller
@RequestMapping("/")
public class PaymentController {
	
	public static final String PAYPAL_CONFIRM_URL = "pay/confirm";
	public static final String PAYPAL_CANCEL_URL = "pay/cancel";
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private PaypalService paypalService;
	
	@RequestMapping(method = RequestMethod.GET)
	public String index(){
		return "index";
	}

	/**
	 * paypal 交付（payment）步骤如下：
	 * 1. 创建支付对象 -> 返回paypal的URL
	 * 2. 用户到paypal的URL中进行交付操作
	 * 	a） 用户确定支付信息 TODO 在paypal页面没有显示支付金额？
	 * 	b） 用户确定支付方式（信用卡、paypal账户）进行支付
	 * 3. 执行
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "pay")
	public String pay(HttpServletRequest request){
		String cancelUrl = URLUtils.getBaseURl(request) + "/" + PAYPAL_CANCEL_URL;
		String successUrl = URLUtils.getBaseURl(request) + "/" + PAYPAL_CONFIRM_URL;
		try {
			Payment payment = paypalService.createPayment(
					4.00, 
					"USD", 
					PaypalPaymentMethod.paypal, 
					PaypalPaymentIntent.sale,
					"payment description", 
					cancelUrl, 
					successUrl);
			for(Links links : payment.getLinks()){
				if(links.getRel().equals("approval_url")){ //重定向到paypal网站进行支付
					return "redirect:" + links.getHref();
				}
			}
		} catch (PayPalRESTException e) {
			log.error(e.getMessage());
		}
		return "redirect:/";
	}

	/**
	 * 当用户取消支付信息后调用
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = PAYPAL_CANCEL_URL)
	public String cancelPay(){
		return "cancel";
	}

	/**
	 * 当用户确认支付信息后调用
	 * @param paymentId
	 * @param payerId
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = PAYPAL_CONFIRM_URL)
	public String confirmPay(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId){
		try {
			// 执行支付
			Payment payment = paypalService.executePayment(paymentId, payerId);
			// 支付成功
			if(payment.getState().equals("approved")){ // 这个相当于前端回调
				return "success";
			}
		} catch (PayPalRESTException e) {
			log.error(e.getMessage());
		}
		return "redirect:/";
	}
	
}

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
 * paypal 交付（payment）步骤如下：
 * 1. 请求调用paypal进行支付 -> 返回paypal的URL {@link #pay(HttpServletRequest)}
 * 2. 用户到paypal的URL中进行交付操作
 * 	a） 用户确定支付信息 TODO 在paypal页面没有显示支付金额？
 * 	b） 用户确定支付方式（信用卡、paypal账户）进行支付
 * 3. 执行支付
 * 	a) 用户确认调用 {@link #confirmPay(String, String)}
 * 	b) 用户取消调用 {@link #cancelPay()}
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
	 * 请求调用paypal进行支付
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "pay")
	public String pay(HttpServletRequest request){
		String cancelUrl = URLUtils.getBaseURl(request) + "/" + PAYPAL_CANCEL_URL;
		String confirmUrl = URLUtils.getBaseURl(request) + "/" + PAYPAL_CONFIRM_URL;
		try {
			Payment payment = paypalService.createPayment(
					4.00, 
					"USD", 
//					PaypalPaymentMethod.credit_card, // paypal rest api 不支持直接信用卡付款。必须有paypal账户。
					PaypalPaymentMethod.paypal,
					PaypalPaymentIntent.sale,
					"payment description", 
					cancelUrl, 
					confirmUrl);
			System.out.println("payment = "+ payment);
			for(Links links : payment.getLinks()){
				//重定向到paypal网站进行支付
				// 如果是支付方式为paypal，会有这个approval_url
				if(links.getRel().equals("approval_url")){
					System.out.println("approval_url"+links.getRel());
					return "redirect:" + links.getHref();
				}
				// 如果是支付方式为credit_card，没有这个approval_url
				System.out.println("links = "+ links);
			}
		} catch (PayPalRESTException e) {
			log.error(e.getMessage());
		}
		System.out.println("---------redirect:/");
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
			// 支付成功, 这个相当于trade系统的前端回调
			if(payment.getState().equals("approved")){
				return "success";
			}
		} catch (PayPalRESTException e) {
			log.error(e.getMessage());
		}
		return "redirect:/";
	}
	
}

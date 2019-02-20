package com.masasdani.paypal.service;

import com.masasdani.paypal.config.PaypalPaymentIntent;
import com.masasdani.paypal.config.PaypalPaymentMethod;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaypalService {
    @Autowired
    private Environment env;

    /**
     * 创建支付对象
     * 可参见：
     * https://github.com/paypal/PayPal-Java-SDK/wiki/Making-First-Call
     * https://developer.paypal.com/docs/api/quickstart/payments/#define-payment
     *
     * @param total       总价
     * @param currency    货币：美元/人民币/...
     * @param method      支付方式：信用卡、paypal
     * @param intent      支付意图：sale, authorize, order
     * @param description 描述：
     * @param cancelUrl   取消回调
     * @param successUrl  成功回调
     * @return 返回支付对象
     * @throws PayPalRESTException
     */
    public Payment createPayment(
            Double total,
            String currency,
            PaypalPaymentMethod method,
            PaypalPaymentIntent intent,
            String description,
            String cancelUrl,
            String successUrl) throws PayPalRESTException {
        // 设置支付数额
        Amount amount = new Amount();
        amount.setCurrency(currency);
        total = new BigDecimal(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
        amount.setTotal(String.format("%.2f", total)); // Total must be equal to sum of shipping, tax and subtotal.

        // 设置交易信息
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        // 设置支付者的信息
        Payer payer = new Payer();
        payer.setPaymentMethod(method.toString());

        // 设置重定向URL
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);

        // 添加支付信息
        Payment payment = new Payment();
        payment.setIntent(intent.toString());
        payment.setPayer(payer); // 交付者
        payment.setTransactions(transactions); // 交易信息
        payment.setRedirectUrls(redirectUrls); // 重定向URL

        return payment.create(
                new APIContext(
                        env.getProperty("paypal.client.app"),
                        env.getProperty("paypal.client.secret"),
                        env.getProperty("paypal.mode")));
    }
    /**
     *  执行支付
     */
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);
        return payment.execute(new APIContext(
                env.getProperty("paypal.client.app"),
                env.getProperty("paypal.client.secret"),
                env.getProperty("paypal.mode")), paymentExecute);
    }

    /**
     * subscribe webhook to events.
     * 此功能可以在Paypal DashBoard中配置，不用编程的方式。
     */
    public void addWebHook(){
        //1. Define webhook events
        List eventTypes = new ArrayList();
        eventTypes.add(new EventType("PAYMENT.CAPTURE.COMPLETED"));
        eventTypes.add(new EventType("PAYMENT.CAPTURE.DENIED"));
        eventTypes.add(new EventType("PAYMENT.ORDER.CANCELLED"));
        eventTypes.add(new EventType("PAYMENT.ORDER.CREATED"));
        eventTypes.add(new EventType("PAYMENT.SALE.COMPLETED"));
        eventTypes.add(new EventType("PAYMENT.SALE.DENIED"));
        eventTypes.add(new EventType("VAULT.CREDIT-CARD.CREATED"));
        eventTypes.add(new EventType("VAULT.CREDIT-CARD.DELETED"));

        Webhook webhook = new Webhook();
        webhook.setUrl("https://db30b88d.ngrok.io/webhook");
        webhook.setEventTypes(eventTypes);
        //2. Create webhook
        try{
            Webhook createdWebhook = webhook.create(new APIContext(
                    env.getProperty("paypal.client.app"),
                    env.getProperty("paypal.client.secret"),
                    env.getProperty("paypal.mode")), webhook);
            System.out.println("Webhook successfully created with ID " + createdWebhook.getId());
        } catch (PayPalRESTException e) {
            System.err.println(e.getDetails());
        }
    }
}

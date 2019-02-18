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
     * 可参见：https://github.com/paypal/PayPal-Java-SDK/wiki/Making-First-Call
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
        Amount amount = new Amount();
        amount.setCurrency(currency);
        total = new BigDecimal(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
        amount.setTotal(String.format("%.2f", total));

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod(method.toString());

        Payment payment = new Payment();
        payment.setIntent(intent.toString());
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        return payment.create(
                new APIContext(
                        env.getProperty("paypal.client.app"),
                        env.getProperty("paypal.client.secret"),
                        env.getProperty("paypal.mode")));
    }

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
}

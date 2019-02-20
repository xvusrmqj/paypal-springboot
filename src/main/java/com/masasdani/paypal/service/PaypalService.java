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
//        /* REST API不支持直接支付 */
//        if(PaypalPaymentMethod.credit_card.equals(method)){ // 如果是信用卡方式，需要提供
//            List<FundingInstrument> fundingInstrumentList = getFundingInstrumentList();
//            payer.setFundingInstruments(fundingInstrumentList);
//        }

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
     * 不要使用此方法，因为REST API不支持直接信用卡支付。
     * 测试REST API是否可以直接使用信用卡，而无需登录Paypal。
     * 设置一些List<FundingInstrument>信息，只用于PaypalPaymentMethod为信用卡的方式。
     * @return
     */
    @Deprecated
    private List<FundingInstrument> getFundingInstrumentList(){
        // ###Address
        // Base Address object used as shipping or billing
        // address in a payment. [Optional]
        Address billingAddress = new Address();
        billingAddress.setCity("Johnstown");
        billingAddress.setCountryCode("US");
        billingAddress.setLine1("52 N Main ST");
        billingAddress.setPostalCode("43210");
        billingAddress.setState("OH");
        // ###CreditCard
        // A resource representing a credit card that can be
        // used to fund a payment.
        CreditCard creditCard = new CreditCard();
        creditCard.setBillingAddress(billingAddress);
        creditCard.setCvv2(874);
        creditCard.setExpireMonth(11);
        creditCard.setExpireYear(2028);
        creditCard.setFirstName("Joe");
        creditCard.setLastName("Shopper");
        creditCard.setNumber("4417119669820331");
        creditCard.setType("visa");

        // ###FundingInstrument
        // A resource representing a Payeer's funding instrument.
        // Use a Payer ID (A unique identifier of the payer generated
        // and provided by the facilitator. This is required when
        // creating or using a tokenized funding instrument)
        // and the `CreditCardDetails`
        FundingInstrument fundingInstrument = new FundingInstrument();
        fundingInstrument.setCreditCard(creditCard);

        // The Payment creation API requires a list of
        // FundingInstrument; add the created `FundingInstrument`
        // to a List
        List<FundingInstrument> fundingInstruments = new ArrayList<FundingInstrument>();
        fundingInstruments.add(fundingInstrument);
        return fundingInstruments;
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

package com.masasdani.paypal.service;

import com.masasdani.paypal.config.PaypalPaymentIntent;
import com.masasdani.paypal.config.PaypalPaymentMethod;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PaypalServiceTest {
    @Autowired
    private PaypalService paypalService;
    public static final String SUCCESSURL = "http://localhost:8080/pay/success";
    public static final String CANCELURL = "http://localhost:8080/pay/cancel";
    @Test
    public void createPayment() throws PayPalRESTException {
        Payment payment = paypalService.createPayment(
                4.00,
                "USD",
                PaypalPaymentMethod.paypal,
                PaypalPaymentIntent.sale,
                "payment description",
                CANCELURL,
                SUCCESSURL);
        System.out.println(payment);
    }
}
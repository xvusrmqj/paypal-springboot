package com.masasdani.paypal.config;

import com.paypal.base.rest.APIContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaypalConfig {
    @Bean
    public APIContext apiContext(@Value("${paypal.client.app}") String clientId,
                                 @Value("${paypal.client.secret}") String clientSecret,
                                 @Value("${paypal.mode}") String mode){
        return new APIContext(clientId, clientSecret, mode);
    }
}

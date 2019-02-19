package com.masasdani.paypal.controller;

import com.masasdani.paypal.service.PaypalService;
import com.paypal.api.payments.Event;
import com.paypal.base.Constants;
import com.paypal.base.rest.APIContext;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class WebhookController {
    @Autowired
    private Environment env;

    /**
     * webhook listener
     *
     * @return
     */
    @RequestMapping("webhook")
    @ResponseBody // 如果没有返回值需要加入这个注解，否则thymeleaf会进行解析报错。
    public void webhooks(HttpServletRequest request) throws Exception {
        System.out.println("---------webhook--------");
        /**
         * 这里用到的WebhookId是{@link PaypalService#addWebHook()}产生的.
         * 这个WebhookId可以在产生时打印出来。
         * 也可以在DashBoard中查看到，路径为My Apps & Credentials/REST API apps/{App Name}/SANDBOX WEBHOOKS
         */
        String WebhookId = "87K08989S8398921G";
        APIContext apiContext = new APIContext(
                env.getProperty("paypal.client.app"),
                env.getProperty("paypal.client.secret"),
                env.getProperty("paypal.mode"));
        // Set the webhookId that you received when you created this webhook.
        apiContext.addConfiguration(Constants.PAYPAL_WEBHOOK_ID, WebhookId);

        String body = getBody(request);
        Map<String, String> headersInfo = getHeadersInfo(request);
        System.out.println("body = " + body);
        System.out.println("headersInfo = " + headersInfo);

        if(Event.validateReceivedEvent(apiContext, headersInfo, body) &&
                "PAYMENT.SALE.COMPLETED".equals(new JSONObject(body).get("event_type"))){
            System.out.println("支付完成！");
        }
    }

    // Simple helper method to help you extract the headers from HttpServletRequest object.
    private static Map<String, String> getHeadersInfo(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        @SuppressWarnings("rawtypes")
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }

    // Simple helper method to fetch request data as a string from HttpServletRequest object.
    private static String getBody(HttpServletRequest request) throws IOException {
        String body;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }

}

package com.xuecheng.orders;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.xuecheng.orders.config.AlipayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author yepianer
 * @date 2023/11/4 10:10
 * @project_name yepianerxuecheng
 * @description 请求支付宝查询订单状态
 */

@SpringBootTest
public class AliPayTest {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Test
    public void queryPayResult() throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL,APP_ID,APP_PRIVATE_KEY,
                AlipayConfig.FORMAT,AlipayConfig.CHARSET,ALIPAY_PUBLIC_KEY,AlipayConfig.SIGNTYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", "1721906086235459584");
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            System.out.println(response.getBody().toString());
        } else {
            System.out.println("调用失败");
        }
    }

}

package com.xuecheng.orders.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yepianer
 * @date 2023/11/5 12:02
 * @project_name yepianerxuecheng
 * @description 订单相关接口
 */
@Api(value = "订单支付接口",tags = "订单支付接口")
@Slf4j
@Controller
public class OrderController {

    @Resource
    OrderService orderService;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @ApiOperation("生成支付二维码")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto) {

        SecurityUtil.XcUser user = SecurityUtil.getUser();
        String id = user.getId();

        //调用service，完成插入订单信息，插入支付记录，生成支付二维码
        PayRecordDto payRecordDto = orderService.createOrder(id, addOrderDto);
        return payRecordDto;
    }

    @ApiOperation("扫码下单接口")
    @GetMapping("/requestpay")
    public void requestpay(String payNo, HttpServletResponse httpResponse) throws IOException {

        //传入支付记录号，判断支付记录号是否存在
        XcPayRecord payRecordByPayno = orderService.getPayRecordByPayno(payNo);
        if (payRecordByPayno == null){
            XueChengPlusException.cast("支付记录不存在");
        }
        //支付结果
        String status = payRecordByPayno.getStatus();
        if ("601002".equals(status)){
            XueChengPlusException.cast("已支付，无需重复支付");
        }
        //请求支付宝下单
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY,
                AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        //
//        alipayRequest.setNotifyUrl("http://yepianer.natapp1.cc/orders/paynotify");//在公共参数中设置回跳和通知地址
        alipayRequest.setBizContent("{" +
                "    \"out_trade_no\":\""+ payNo + "\"," +
                "    \"total_amount\":"+ payRecordByPayno.getTotalPrice() +"," +
                "    \"subject\":\""+ payRecordByPayno.getOrderName() +"\"," +
                "    \"product_code\":\"QUICK_WAP_WAY\"" +
                "  }");//填充业务参数
        String form = null; //调用SDK生成表单
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
    }

    @ApiOperation("查询支付结果")
    @GetMapping("/payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) throws IOException {

        //查询支付结果
        PayRecordDto payRecordDto = orderService.queryPayResult(payNo);

        return null;

    }
}

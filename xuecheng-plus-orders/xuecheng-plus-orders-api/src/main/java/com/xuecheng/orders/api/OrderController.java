package com.xuecheng.orders.api;

import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

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
}
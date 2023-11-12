package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author yepianer
 * @date 2023/11/10 9:53
 * @project_name yepianerxuecheng
 * @description 接收消息通知处理列
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Resource
    MyCourseTablesService myCourseTablesService;

    @Transactional
    //监听消息队列接收支付结果通知
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message, Channel channel){

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] body = message.getBody();
        String jsonString = new String(body);
        //转成对象
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);

        //解析消息的内容
        //选课id
        String chooseCourseId = mqMessage.getBusinessKey1();
        //订单类型
        String  orderType = mqMessage.getBusinessKey2();

        //学习中心服务主要购买课程类的支付订单结果
        if (orderType.equals("60201")){

            //根据消息内容，更新选课记录，向我的课程表插入记录
            boolean b = myCourseTablesService.saveChooseCourseStauts(chooseCourseId);
            if (!b){
                XueChengPlusException.cast("保存选课状态失败");
            }
        }



    }
}

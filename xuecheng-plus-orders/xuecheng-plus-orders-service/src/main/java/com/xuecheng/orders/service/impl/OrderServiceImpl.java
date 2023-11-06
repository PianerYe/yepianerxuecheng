package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/11/5 12:07
 * @project_name yepianerxuecheng
 * @description 订单相关的接口
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    XcOrdersMapper ordersMapper;
    @Resource
    XcOrdersGoodsMapper ordersGoodsMapper;
    @Resource
    XcPayRecordMapper payRecordMapper;
    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        //幂等性判断，同一个选课记录，只能有一个订单
        //插入订单表,订单主表，订单明细表
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        //插入支付记录
        XcPayRecord payRecord = createPayRecord(xcOrders);
        Long payNo = payRecord.getPayNo();
        //生成二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        //支付二维码的url地址
        String url = String.format(qrcodeurl, payNo);
        //二维码图片
        String qrCode = null;
        try {
            qrCode = qrCodeUtil.createQRCode(url,200,200);
        }catch (Exception e){
           XueChengPlusException.cast("生成二维码失败");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto){
        //幂等性判断，同一个选课记录，只能有一个订单
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (xcOrders != null){
            return xcOrders;
        }
        //插入订单表,订单主表，订单明细表
        //订单主表
        xcOrders = new XcOrders();
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());//使用雪花算法生成订单号
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");//未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");//订单类型
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());//如果是选课这里记录选课表的组件id

        int insert = ordersMapper.insert(xcOrders);
        if (insert<=0){
            XueChengPlusException.cast("添加订单失败");
        }
        //订单ID
        Long orderId = xcOrders.getId();
        //订单明细表
        //将前端传入的明细的json转成List
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //遍历，插入订单明细
        xcOrdersGoods.forEach(goods->{
            goods.setGoodsId(String.valueOf(orderId));
            //插入订单明细
            int insert1 = ordersGoodsMapper.insert(goods);
            if (insert1<=0){
                XueChengPlusException.cast("插入订单明细失败");
            }

        });
        return null;
    }

    /**
     * @author yepianer
     * @date 2023/11/5 12:07
     * @project_name yepianerxuecheng
     * @description 订单相关的接口
     */
    public XcPayRecord createPayRecord(XcOrders orders){
        //订单id
        Long ordersId = orders.getId();
        XcOrders xcOrders = ordersMapper.selectById(ordersId);
        //如果此订单不存在不能添加支付记录
        if (xcOrders == null){
            XueChengPlusException.cast("订单不存在");
        }
        //订单状态
        String status = xcOrders.getStatus();
        //如果此订单支付结果成功，不再添加支付记录，避免重复支付
        if ("601002".equals(status)){
            XueChengPlusException.cast("此订单已支付");
        }
        //添加支付记录
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());//支付记录号
        xcPayRecord.setOrderId(ordersId);
        xcPayRecord.setOrderName(xcOrders.getOrderName());
        xcPayRecord.setTotalPrice(xcOrders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");//未支付
        xcPayRecord.setUserId(xcOrders.getUserId());
        int insert = payRecordMapper.insert(xcPayRecord);
        if (insert <= 0){
            XueChengPlusException.cast("插入支付记录失败");
        }
        return xcPayRecord;
    }

    //根据业务id查询订单,业务id就是选课记录表的主键
    public XcOrders getOrderByBusinessId(String businessId){
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }
}

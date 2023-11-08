package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
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
import java.util.Map;

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
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
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

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //调用支付宝的接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        System.out.println(payStatusDto);
        //拿到支付结果更新支付记录表和订单表的支付状态
        saveAliPayStatus(payStatusDto);

        return null;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo){
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL,APP_ID,APP_PRIVATE_KEY,
                AlipayConfig.FORMAT,AlipayConfig.CHARSET,ALIPAY_PUBLIC_KEY,AlipayConfig.SIGNTYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        //支付宝返回的信息
        String body = null;
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()){
                //交易不成功
                XueChengPlusException.cast("请求支付查询支付结果失败");
            }
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XueChengPlusException.cast("请求支付查询支付结果异常");
        }
        Map resultMap = JSON.parseObject(body, Map.class);
        Map bodyMap = (Map) resultMap.get("alipay_trade_query_response");

        //解析支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_no((String)bodyMap.get("trade_no"));//支付宝的交易号
        payStatusDto.setTrade_status((String) bodyMap.get("trade_status"));
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTotal_amount((String) bodyMap.get("total_amount"));//总金额

        return payStatusDto;
    }



    /**
     * @description 保存支付宝支付结果
     * @param payStatusDto  支付结果信息
     * @return void
     * @author Mr.M
     * @date 2022/10/4 16:52
     */
    public void saveAliPayStatus(PayStatusDto payStatusDto){

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
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //遍历，插入订单明细
        xcOrdersGoodsList.forEach(goods->{
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods,xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId);
            //插入订单明细
            int insert1 = ordersGoodsMapper.insert(xcOrdersGoods);
            if (insert1<=0){
                XueChengPlusException.cast("插入订单明细失败");
            }

        });
        return xcOrders;
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

package com.xuecheng.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xuecheng.auth.pojo.WxMaUserInfo;
import com.xuecheng.auth.pojo.WxScanDto;
import com.xuecheng.auth.pojo.WxparamDto;
import com.xuecheng.ucenter.model.po.XcUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

import static com.alibaba.fastjson.JSONPatch.OperationType.test;

/**
 * @author yepianer
 * @date 2023/10/9 22:15
 * @project_name yepianerxuecheng
 * @description
 */
@Slf4j
//@Controller
@RestController
public class WxLoginController {

    @Resource
    RedisTemplate redisTemplate;
//    @RequestMapping("/wxLogin")
//    public String wxLogin(String code,String state) throws IOException{
//        log.debug("微信扫码回调,code:{},state:{}",code,state);
//        //todo:远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
//
//
//        XcUser xcUser = new XcUser();
//        //暂时硬编写，目的是调试环境
//        xcUser.setUsername("t1");
//        if (xcUser==null){
//            return "redirect:http://www.51xuecheng.cn/error.html";
//        }
//        String username = xcUser.getUsername();
//        return "redirect:http://www.51xuecheng.cn/sign.html?username=\"+username+" + "&authType=wx";
//    }
    @PostMapping ("/wxLogin")
//    public WxScanDto wxLoginTwo(@RequestBody String wxparamDto) throws IOException{
    public WxScanDto wxLoginTwo(@RequestBody WxparamDto wxparamDto) throws IOException{
        WxScanDto wxScanDto = new WxScanDto();
        if (wxparamDto == null){
            //传入对象数值为空
            log.error("传入的二维码错误，获取的用户信息为空");
            throw new RuntimeException("传入的二维码错误，获取的用户信息为空");
        }
        wxScanDto.setTempUserId(wxparamDto.getTempUserId());
//        if (wxparamDto.getTempUserId() == null){
//            log.error("传入的二维码用户信息错误，获取的用户信息ID为空");
//            throw new RuntimeException("传入的二维码用户信息错误，获取的用户信息ID为空");
//        }
//        wxScanDto.setTempUserId(wxparamDto.getTempUserId());
        //todo:远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        XcUser xcUser = new XcUser();
        //暂时硬编写，目的是调试环境
        xcUser.setUsername("t1");
        if (xcUser==null){
//            return "redirect:http://www.51xuecheng.cn/error.html";
            wxScanDto.setScanSuccess(false);
            wxScanDto.setCode("2");
            wxScanDto.setMsg("用户登录失败");
            //将一些数据信息放入redis，方便轮询的时候查询
            redisTemplate.opsForValue().set(wxScanDto.getTempUserId() + wxScanDto.getCode(),"WrongCode");
            return wxScanDto;
        }
        String username = xcUser.getUsername();
//      return "redirect:http://www.51xuecheng.cn/sign.html?username=\"+username+" + "&authType=wx";
        wxScanDto.setScanSuccess(true);
        wxScanDto.setCode("0");
        wxScanDto.setMsg("用户登录成功！");
        //将一些数据信息放入redis，方便轮询的时候查询
        System.out.println(wxScanDto.getTempUserId() + wxScanDto.getCode());
        redisTemplate.opsForValue().set(wxScanDto.getTempUserId() + wxScanDto.getCode(),xcUser.getUsername());

        return wxScanDto;
    }

    @CrossOrigin(origins = "http://www.51xuecheng.cn") // 允许指定源跨域请求
    @GetMapping("/getCodeItem/{tempUserId}")
    public JSONObject getCodeItem(@PathVariable("tempUserId") String tempUserId) {
        if (tempUserId == null){
            return null;
        }
        String tempUserIdCode = tempUserId + "0";
        String tempUserIdCodeRedis = (String) redisTemplate.opsForValue().get(tempUserIdCode);
        if (tempUserIdCodeRedis != null){
//            return tempUserIdCodeRedis;
            JSONObject jsonObject = new JSONObject();
            String redirect  = "http://www.51xuecheng.cn/sign.html?username=" + tempUserIdCodeRedis + "&authType=wx";
            jsonObject.put("redirect",redirect);
            return jsonObject;
        }
        tempUserIdCode = tempUserId + "2";
        tempUserIdCodeRedis = (String) redisTemplate.opsForValue().get(tempUserIdCode);
        if (tempUserIdCodeRedis != null){
            JSONObject jsonObject = new JSONObject();
            String redirect = "http://www.51xuecheng.cn/error.html";
            jsonObject.put("redirect",redirect);
            return jsonObject;
        }
        return null;

    }







}

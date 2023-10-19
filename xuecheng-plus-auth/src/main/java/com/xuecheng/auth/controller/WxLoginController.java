package com.xuecheng.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.auth.pojo.WxMaUserInfo;
import com.xuecheng.auth.pojo.WxScanDto;
import com.xuecheng.auth.pojo.WxparamDto;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    @Resource
    XcUserMapper xcUserMapper;
    @Resource
    PasswordEncoder passwordEncoder;
    @Resource
    XcUserRoleMapper xcUserRoleMapper;
    @Resource
    WxLoginController currentPoxy;
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
    public WxScanDto wxLoginTwo(@RequestBody WxparamDto wxparamDto) throws IOException{
        WxScanDto wxScanDto = new WxScanDto();
        if (wxparamDto == null){
            //传入对象数值为空
            log.error("传入的二维码错误，获取的用户信息为空");
            throw new RuntimeException("传入的二维码错误，获取的用户信息为空");
        }
        wxScanDto.setTempUserId(wxparamDto.getTempUserId());
        //todo:远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        XcUser xcUser = currentPoxy.addXcUser(wxparamDto);

        if (xcUser==null){
//            return "redirect:http://www.51xuecheng.cn/error.html";
            wxScanDto.setScanSuccess(false);
            wxScanDto.setCode("2");
            wxScanDto.setMsg("用户登录失败");
            //将一些数据信息放入redis，方便轮询的时候查询
            redisTemplate.opsForValue().set(wxScanDto.getTempUserId() + wxScanDto.getCode(),"WrongCode",60, TimeUnit.SECONDS);
            return wxScanDto;
        }
        String username = xcUser.getUsername();
//      return "redirect:http://www.51xuecheng.cn/sign.html?username=\"+username+" + "&authType=wx";
        wxScanDto.setScanSuccess(true);
        wxScanDto.setCode("0");
        wxScanDto.setMsg("用户登录成功！");
        //将一些数据信息放入redis，方便轮询的时候查询
        System.out.println(wxScanDto.getTempUserId() + wxScanDto.getCode());
        redisTemplate.opsForValue().set(wxScanDto.getTempUserId() + wxScanDto.getCode(),xcUser.getUsername(),60,TimeUnit.SECONDS);
        return wxScanDto;
    }

    @Transactional
    public XcUser addXcUser(WxparamDto wxparamDto){
        //查询用户信息
        XcUser xcUser = new XcUser();
        //todo:远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        //需要查询用户username
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getWxUnionid, wxparamDto.getWxMaUserInfo().getUnionId());
        XcUser xcUserData = xcUserMapper.selectOne(queryWrapper);
        if (xcUserData == null){
            //数据插入到数据库
            String xcUserId = UUID.randomUUID().toString();
            xcUser.setId(xcUserId);//主键
            xcUser.setUsername(wxparamDto.getWxMaUserInfo().getOpenId());
            xcUser.setName(wxparamDto.getWxMaUserInfo().getNickName());
            xcUser.setNickname(wxparamDto.getWxMaUserInfo().getNickName());
            xcUser.setWxUnionid(wxparamDto.getWxMaUserInfo().getUnionId());
            xcUser.setSex(wxparamDto.getWxMaUserInfo().getGender());
            xcUser.setCreateTime(LocalDateTime.now());
            xcUser.setUtype("101001");
            xcUser.setStatus("1");
            //创建密码
            String passwordEncode = passwordEncoder.encode("123456");
            xcUser.setPassword(passwordEncode);
            int insert = xcUserMapper.insert(xcUser);
            if (insert <= 0){
                throw new RuntimeException("插入数据失败，请查看具体情况");
            }
            //向用户角色关系表新增记录
            XcUserRole xcUserRole = new XcUserRole();
            String xcUserRoleId = UUID.randomUUID().toString();
            xcUserRole.setId(xcUserRoleId);
            xcUserRole.setUserId(xcUserId);
            xcUserRole.setRoleId("17"); //17代表学生
            xcUserRole.setCreateTime(LocalDateTime.now());
            int insert1 = xcUserRoleMapper.insert(xcUserRole);
            if (insert1 <=0){
                throw new RuntimeException("插入数据失败，请查看具体情况");
            }
        }else {
            BeanUtils.copyProperties(xcUserData,xcUser);
        }
        return xcUser;
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

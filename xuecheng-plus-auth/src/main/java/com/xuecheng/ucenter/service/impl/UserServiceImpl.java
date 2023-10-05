package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author yepianer
 * @date 2023/10/3 22:42
 * @project_name yepianerxuecheng
 * @description
 */
@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {

    @Resource
    XcUserMapper xcUserMapper;

    @Resource
    ApplicationContext applicationContext;

    //传人的认证请求参数就是AuthParamsDto
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //将传入的json转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;

        try{
            authParamsDto = JSON.parseObject(s,AuthParamsDto.class);
        }catch (Exception e){
            throw new RuntimeException("请求认证的参数不符合要求");
        }

        //认证类型,有password,wx...
        String authType = authParamsDto.getAuthType();

        //根据认证类型从spring容器中取出指定的Bean
        String beanName = authType + "_authservice";
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        //调用统一execute方法完成认证
        XcUserExt execute = authService.execute(authParamsDto);

        //账号
        String username = authParamsDto.getUsername();
        //根据username账号查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        //查询到用户不存在，要返回null即可，spring security框架提示抛出异常用户不存在
        if (xcUser == null){
            return null;
        }
        //如果查到了用户拿到了正确的密码，最终封装成一个UserDetails对象给spring security框架返回，由框架进行密码比对
        String password = xcUser.getPassword();
        //权限
        String[] authorities = {"test"};
        xcUser.setPassword(null);
        //将用户的信息转成json
        String userJson = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();

        return userDetails;
    }
}

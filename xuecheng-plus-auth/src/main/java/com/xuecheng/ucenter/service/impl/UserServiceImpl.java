package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import lombok.extern.slf4j.Slf4j;
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

    //
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //账号
        String username = s;
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

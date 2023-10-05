package com.xuecheng.auth.config;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author yepianer
 * @date 2023/10/5 23:39
 * @project_name yepianerxuecheng
 * @description 重写了原来的DaoAuthenticationProvider的校验密码的方法,因为我们统一认证的入口,因为有些校验方式不需要密码
 */
@Component
public class DaoAuthenticationProviderCustom extends DaoAuthenticationProvider {

    @Resource
    public void setUserDetailsService(UserDetailsService userDetailsService){
        super.setUserDetailsService(userDetailsService);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {

    }
}

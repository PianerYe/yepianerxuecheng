package com.xuecheng.auth.pojo;

import lombok.Data;

/**
 * @author yepianer
 * @date 2023/10/13 10:15
 * @project_name yepianerxuecheng
 * @description
 */
@Data
public class WxMaUserInfo {
    String openId;
    String nickName;
    String gender; //0是女性1是男性
    String language;
    String city;
    String province;
    String country;
    String avatarUrl;
    String unionId;

}

package com.xuecheng.auth.pojo;

import lombok.Data;

/**
 * @author yepianer
 * @date 2023/10/13 10:32
 * @project_name yepianerxuecheng
 * @description
 */
@Data
public class WxScanDto {
    Boolean scanSuccess; //true or false
    String code; //0登录成功 其他，失败
    String msg;
    String tempUserId;
}

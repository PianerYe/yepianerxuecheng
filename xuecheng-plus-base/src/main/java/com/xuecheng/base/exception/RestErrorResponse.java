package com.xuecheng.base.exception;

import java.io.Serializable;

/**
 * Date:2023/7/5
 * Author: Yepianer
 * Description: 和前端约定返回的异常信息
 */
public class RestErrorResponse implements Serializable {
    private String errMessage;

    public RestErrorResponse(String errMessage){
        this.errMessage= errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}

package com.xuecheng.content.feignclient;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author yepianer
 * @date 2023/9/17 15:06
 * @project_name yepianerxuecheng
 * @description
 */
@Component
public class MediaServiceClientFallback implements MediaServiceClient{
    @Override
    public String upload(MultipartFile filedata, String objectName) throws IOException {

        return null;
    }
}

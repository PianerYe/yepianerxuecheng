package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * @author yepianer
 * @date 2023/9/17 15:09
 * @project_name yepianerxuecheng
 * @description
 */
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    //拿到了熔断的异常信息
    @Override
    public MediaServiceClient create(Throwable cause) {

        return new MediaServiceClient() {
            //发生熔断上传服用调用此方法执行降级逻辑
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException {
                log.debug("远程调用上传文件的接口发生熔断：{}",cause.toString(),cause);
                return null;
            }
        };

    }
}

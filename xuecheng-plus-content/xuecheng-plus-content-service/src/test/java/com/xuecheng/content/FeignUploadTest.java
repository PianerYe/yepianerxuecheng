package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * @author yepianer
 * @date 2023/9/15 21:21
 * @project_name yepianerxuecheng
 * @description 测试远程调用媒资服务
 */
@SpringBootTest
public class FeignUploadTest {

    @Resource
    MediaServiceClient mediaServiceClient;

    @Test
    public void test() throws IOException {

        //将file转成MultipartFile
        File file = new File("D:\\java\\yepianerxuecheng\\xuecheng-plus-content\\xuecheng-plus-content-service\\src\\test\\resources\\templates\\122.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        //远程调用得到返回值
        String upload = mediaServiceClient.upload(multipartFile, "course/122.html");
        if (upload == null){
            System.out.println("走了降级逻辑");
        }
    }
}

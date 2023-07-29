package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author yepianer
 * @date 2023/7/25 23:58
 * @project_name yepianerxuecheng
 * @description 测试minio的sdk
 */
public class MinioTest {

    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.255.100:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() throws Exception {

        //通过扩展名得到媒体资源类型 mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".xlsx");
//        System.out.println(extensionMatch);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }

        //上传文件的参数信息
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")//确定桶
                .filename("C:\\Users\\yepianer\\Desktop\\微杀记事本\\156菜谱难度归类.xlsx")//指定本地文件路径
              //  .object("1.xlsx")//对象名 在桶下存储该文件
                .object("test/01/1.xlsx")//对象名放在子目录下
                .contentType(mimeType)//设置媒体文件类型
                .build();

        //上传文件
        minioClient.uploadObject(uploadObjectArgs);

    }

    //删除文件
    @Test
    public void test_delete() throws Exception {

        //RemoveObjectArgs
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("testbucket")
                .object("1.xlsx")
                .build();

        //删除文件
        minioClient.removeObject(removeObjectArgs);

    }

    //查询文件 从minio中下载
    @Test
    public void test_getFile() throws Exception {

        //GetObjectArgs
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("test/01/1.xlsx")
                .build();

        //查询文件 远程服务器获取的流对象
        GetObjectResponse inputStream = minioClient.getObject(getObjectArgs);

        //指定输出流
        FileOutputStream outputStream = new FileOutputStream(
                new File("C:\\Users\\yepianer\\Desktop\\miniotest.xlsx"));

        IOUtils.copy(inputStream,outputStream);

        //校验文件的完整性对文件的内容进行md5
        FileInputStream fileInputStream = new FileInputStream(
                "C:\\Users\\yepianer\\Desktop\\微杀记事本\\156菜谱难度归类.xlsx");
//        String source_md5 = DigestUtils.md5Hex(inputStream);
        String source_md5 = DigestUtils.md5Hex(fileInputStream);
        String  local_md5 = DigestUtils.md5Hex(new FileInputStream(
                "C:\\Users\\yepianer\\Desktop\\miniotest.xlsx"));
        if (source_md5.equals(local_md5)){
            System.out.println("下载成功");
        }
    }
}

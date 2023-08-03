package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/29 19:13
 * @project_name yepianerxuecheng
 * @description 测试大文件上传方法
 */
public class BigFileTest {

    //分块测试
    @Test
    public void testChunk() throws IOException {
        //源文件
        File sourFile = new File("D:\\斗鱼视频\\【天涯明月刀】默默123444的精彩时刻 20211129 00点场.mp4");
        //分块文件存储路径
        String chunkFilePath = "E:\\video\\chunk\\";
        //分块文件的大小
        int chunkSize = 1024* 1024 * 50;
        //分块文件个数
        int chunkNum = (int) Math.ceil(sourFile.length() * 1.0 / chunkSize);
        //使用流从源文件读数据，向分块文件中写数据
        RandomAccessFile raf_r = new RandomAccessFile(sourFile, "r");
        //缓存区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            //分块文件的写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_r.read(bytes))!= -1){
                raf_rw.write(bytes,0,len);
                if (chunkFile.length()>= chunkSize){
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();

    }

    //将分块进行合并
    @Test
    public void testMerge() throws IOException {
        //块文件路径
        File chunkFolder = new File("E:\\video\\chunk\\");
        //源文件
        File sourFile = new File("D:\\斗鱼视频\\【天涯明月刀】默默123444的精彩时刻 20211129 00点场.mp4");
        //合并后的文件
        File mergeFile = new File("E:\\video\\merge\\1.mp4");

        //取出刚才所有的分块文件
        File[] files = chunkFolder.listFiles();
        //将数组转成List
        List<File> filesList = Arrays.asList(files);
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        //向合并文件写的流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        //缓存区
        byte[] bytes = new byte[1024];
        //遍历分块文件，向合并文件写
        for (File file:filesList) {
            //读分块的流
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len=raf_r.read(bytes))!= -1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }
        raf_rw.close();
        //合并文件完成后对合并的文件进行校验
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourFile);
        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        if (md5_merge.equals(md5_source)){
            System.out.println("文件合并完成");
        }

    }
}

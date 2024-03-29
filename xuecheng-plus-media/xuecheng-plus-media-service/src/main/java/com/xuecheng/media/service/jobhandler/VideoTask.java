package com.xuecheng.media.service.jobhandler;

import com.alibaba.nacos.common.utils.IoUtils;
import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
/**
 * 任务处理类
 * */
@Slf4j
@Component
public class VideoTask {

    @Resource
    MediaFileProcessService mediaFileProcessService;
    @Resource
    MediaFileService mediaFileService;

    //ffmpeg的路径
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;
    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数

        //确定cpu的核心数
        int processors = Runtime.getRuntime().availableProcessors();

        //查询待处理的任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);

        //任务数量
        int size = mediaProcessList.size();
        log.debug("取到视频任务处理数:"+size);
        if (size<=0){
            return;
        }
        //创建一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        //使用计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            //将任务加入线程池
            executorService.execute(()->{
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //文件id就是md5
                    String fileId = mediaProcess.getFileId();
                    //开启任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占任务失败,任务id：{}", taskId);
                        return;
                    }

                    //桶
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();

                    //下载Minio视频到本地

                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    FileOutputStream outputStream = null;
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("minio",".avi");
                        outputStream = new FileOutputStream(tempFile);
                        IoUtils.copy(new FileInputStream(file),outputStream);
                    }catch (IOException e){
                        log.debug("原文件转换成avi出错,file:{}",file);
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinsihStatus(taskId, "3", fileId, null, "原文件转换成avi出错");
                        return;
                    }
                    //tempFile = "minioXXXXXXXX.avi"
                    if (file == null) {
                        log.debug("下载视频出错,任务id:{},bucket:{},objectName:{}", taskId, bucket, objectName);
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinsihStatus(taskId, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }
                    //源avi视频的路径
//                    String video_path = file.getAbsolutePath();
                    String video_path = tempFile.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    //先创建一个临时文件，作为转换后的文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常:{}", e.getMessage());
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinsihStatus(taskId, "3", fileId, null, "创建临时文件异常");
                        return;
                    }
                    String mp4_path = mp4File.getAbsolutePath();
                    mp4_path = mp4_path.substring(0,mp4_path.lastIndexOf(File.separator)) + File.separator;
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4_name, mp4_path);
                    //开始视频转换，成功将返回success,成功返回success,失败返回失败原因
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {

                        log.debug("视频转码失败,bucket:{},objectName:{},原因:{}", bucket, objectName, result);
                        mediaFileProcessService.saveProcessFinsihStatus(taskId, "3", fileId, null, result);
                        return;
                    }
                    String localChunkFilePath = mp4_path + mp4_name;
                    mp4_name = mp4_name.substring(0,1)
                            + "/" + mp4_name.substring(1,2)
                            + "/" + mp4_name.substring(0,mp4_name.lastIndexOf("."))
                            + "/" + mp4_name;

                    //上传到minio
                    boolean b1 = mediaFileService.addMediaFilesToMinIO(localChunkFilePath, "video/mp4", bucket, mp4_name);
//                    boolean b1 = mediaFileService.addMediaFilesToMinIO(mp4_path, "video/mp4", bucket, objectName);
                    if (!b1) {
                        log.debug("上传mp4到minio失败,taskId:{}", taskId);
                        mediaFileProcessService.saveProcessFinsihStatus(taskId, "3", fileId, null, "上传mp4到minio失败");
                        return;
                    }
                    //mp4文件的url
                    String url = getFilePathByMd5(fileId, ".mp4");

                    //保存任务的状态为成功
                    mediaFileProcessService.saveProcessFinsihStatus(taskId, "2", fileId, url, null);
                    //保存任务的处理结果
                }finally {
                    //计数器减去1
                    countDownLatch.countDown();
                }
            });
        });

        //阻塞,最大限度的等待时间,阻塞最多等待一定的时间后，解除阻塞
        countDownLatch.await( 30, TimeUnit.MINUTES);

    }

    private String getFilePathByMd5(String fileMd5,String fileExt){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2)
                + "/" + fileMd5 + "/" + fileMd5  + fileExt;
    }

}

package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClinet;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.lang.reflect.Field;

/**
 * @author yepianer
 * @date 2023/9/15 0:30
 * @project_name yepianerxuecheng
 * @description
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Resource
    CoursePublishService coursePublishService;
    @Resource
    SearchServiceClinet searchServiceClinet;
    @Resource
    CoursePublishMapper coursePublishMapper;
    //coursePublishJobHandler
    //任务调度入口
    @XxlJob("coursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception{
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数
        //调用抽象类的方法执行任务
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

    //执行课程发布任务的逻辑,如果此方法抛出异常说明任务执行失败
    @Override
    public boolean execute(MqMessage mqMessage) {
        //从mqMessage拿到
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        //课程静态化上传到minio
        generateCourseHtml(mqMessage,courseId);
        //向elasticsearch写索引数据
        saveCourseIndex(mqMessage,courseId);
        //向redis写缓存
        saveCourseCache(mqMessage,courseId);
        //返回true表示任务完成
        return true;
    }

    //生成课程静态化并上传至文件系统
    private void generateCourseHtml(MqMessage mqMessage,Long courseId){
        //任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        //做任务幂等性处理
        //查询数据库,取出该阶段的执行状态
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne>0){
            log.debug("课程静态化任务完成，无需处理...");
            return;
        }
        //开始课程静态化 生成html页面
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null){
            XueChengPlusException.cast("生成的静态页面为空");
        }
        // 将html上传到Minio
        coursePublishService.uploadCourseHtml(courseId,file);

        //。。任务处理完成写任务状态为完成
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息，第二个阶段任务
    private void saveCourseIndex(MqMessage mqMessage,Long courseId){
        //任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        //取出第二个阶段状态
        int stageTwo = mqMessageService.getStageTwo(taskId);
        //做任务幂等性处理
        if (stageTwo>0){
            log.debug("课程索引信息已写入，无需执行...");
            return;
        }
        //查询课程信息，调用搜索服务添加索引接口
        //从课程发布表查询课程信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);

        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        //远程调用
        Boolean add = searchServiceClinet.add(courseIndex);
        if (!add){
            XueChengPlusException.cast("远程调用搜索服务，添加索引失败");
        }
        //。。任务处理完成写任务状态为完成
        mqMessageService.completedStageTwo(taskId);
    }

    private void saveCourseCache(MqMessage mqMessage,Long courseId){
        //任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        //取出第二个阶段状态
        int stageThree = mqMessageService.getStageThree(taskId);
        //做任务幂等性处理
        if (stageThree>0){
            log.debug("将课程信息缓存至redis,课程id:{}",courseId);
            return;
        }
        //将课程信息缓存至redis...

        //。。任务处理完成写任务状态为完成
        mqMessageService.completedStageThree(taskId);
    }
}

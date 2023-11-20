package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author yepianer
 * @date 2023/11/16 14:45
 * @project_name yepianerxuecheng
 * @description
 */
@Service
@Slf4j
public class LearningServiceImpl implements LearningService {

    @Resource
    MyCourseTablesService myCourseTablesService;
    @Resource
    ContentServiceClient contentServiceClient;
    @Resource
    MediaServiceClient mediaServiceClient;
    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {

        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //判断coursepublish为null不再播放
        if (coursepublish == null){
            return RestResponse.validfail("课程不存在");
        }

        //远程调用内容管理服务，根据课程计划id（teachplanId），判断该课程是否支持试学,如果is_preview的值
        //为1表示支持试学.
        //也可以从coursepublish对象中解析出课程计划信息去判断是否支持试学
        //todo:如果支持试学，直接调用媒资服务查询视频的播放地址，返回
        String teachplanJson = coursepublish.getTeachplan();
        //[{"courseId":2,"grade":1,
        // "id":266,"isPreview":"0",
        // "orderby":1,"parentid":0,"pname":"第1章",
        // "teachPlanTreeNodes":[{"courseId":2,"grade":2,"id":267,"isPreview":"0","orderby":1,
        // "parentid":266,"pname":"第1节",
        // "teachplanMedia":{"courseId":2,"teachplanId":267}}]}]
        Teachplan teachplan = JSON.parseObject(teachplanJson, Teachplan.class);
        if ("1".equals(teachplan.getIsPreview())){
            RestResponse<String> playUrlByMediaId =
                    mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrlByMediaId;
        }


        if (StringUtil.isNotEmpty(userId)){
            //获取学习资格
            XcCourseTablesDto learningStatus =
                    myCourseTablesService.getLearningStatus(userId, courseId);
            /**学习资格状态
             *[{"code":"702001","desc":"正常学习"},
             * {"code":"702002","desc":"没有选课或选课后没有支付"},
             * {"code":"702003","desc":"已过期需要申请续期或重新支付"}]
             * */
            String learnStatus = learningStatus.getLearnStatus();
            if ("702002".equals(learnStatus)){
                return RestResponse.validfail("无法学习，没有选课或选课后没有支付");
            }else if ("702003".equals(learnStatus)){
                return RestResponse.validfail("无法学习，您的选课已过期需要申请续期或重新支付");
            }else {
                //有资格学习,要返回视频的播放地址
                //todo：远程调用媒资获取视频播放地址
                RestResponse<String> playUrlByMediaId =
                        mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }
        }
        //如果用户没有登录
        //取出课程的收费规则
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)){
            //有资格学习,要返回视频的播放地址
            //todo：远程调用媒资获取视频播放地址
            RestResponse<String> playUrlByMediaId =
                    mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrlByMediaId;
        }
        //远程调用feign获取视频的地址
        return RestResponse.validfail("课程需要购买");
    }
}

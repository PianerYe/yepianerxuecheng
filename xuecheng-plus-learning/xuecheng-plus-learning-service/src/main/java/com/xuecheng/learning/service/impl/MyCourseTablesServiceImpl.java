package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/10/20 14:50
 * @project_name yepianerxuecheng
 * @description 选课相关接口实现
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Resource
    ContentServiceClient contentServiceClient;
    @Resource
    XcCourseTablesMapper courseTablesMapper;
    @Resource
    XcChooseCourseMapper chooseCourseMapper;
    @Resource
    MyCourseTablesService currentPoxy;
    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null){
            XueChengPlusException.cast("课程信息不存在");
        }
        //课程收费标准
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)){
            //添加免费课程
            chooseCourse = currentPoxy.addFreeCoruse(userId, coursepublish);//向选课记录表写
            //添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTabls(chooseCourse);

        }else {
            //如果是收费课程
            chooseCourse = addChargeCoruse(userId, coursepublish);
        }
        //构造返回值
        XcChooseCourseDto xcCourseTablesDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcCourseTablesDto);
        //查询学生的学习资格
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        //设置学习资格状态
        xcCourseTablesDto.setLearnStatus(learningStatus.getLearnStatus());

        return xcCourseTablesDto;
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    @Transactional
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        //如果存在免费的选课记录且选课成功下单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getCourseId,coursepublish.getId())
                .eq(XcChooseCourse::getOrderType,"700001") //免费课程
                .eq(XcChooseCourse::getStatus,"701001"); //选课成功
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() >0){
            return xcChooseCourses.get(0);
        }
        //添加选课信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();

        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setStatus("701001");//选课成功
        xcChooseCourse.setCreateDate(LocalDateTime.now());

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));

        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0){
            XueChengPlusException.cast("选课记录表添加失败");
        }

        return xcChooseCourse;
    }

    /**
     * @description 判断学习资格
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @author Mr.M
     * @date 2022/10/3 7:37
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //查询我的课程表，如果没查到则说明没有选课
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null){
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        //如果查到了，判断是否过期，如果过期不能继续学习，没有过期可以继续学习
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires){
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        }else {
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }

    @Transactional
    @Override
    public boolean saveChooseCourseStauts(String chooseCourseId) {

        //根据选课id查询选课表
        XcChooseCourse chooseCourse = chooseCourseMapper.selectById(chooseCourseId);
        if (chooseCourse == null){
            log.debug("接收到购买课程的消息，根据选课id从数据库找不到选课记录:选课id:{}",chooseCourse);
            return false;
        }
        //选课状态
        String status = chooseCourse.getStatus();
        //只有当未支付时才更新为已支付
        if ("701002".equals(status)){
            //更新选课记录的状态为支付成功
            chooseCourse.setStatus("701001");
            int i = chooseCourseMapper.updateById(chooseCourse);
            if ( i <= 0){
                log.debug("添加选课记录失败:{}",chooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
            }
            //向我的课程表插入记录
            XcCourseTables xcCourseTables = addCourseTabls(chooseCourse);
            return true;
        }
        return false;
    }

    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId,CoursePublish coursepublish){
        //如果存在收费的选课且选课状态为只支付，则直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getCourseId,coursepublish.getId())
                .eq(XcChooseCourse::getOrderType,"700002") //收费课程
                .eq(XcChooseCourse::getStatus,"701002"); //待支付
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() >0){
            return xcChooseCourses.get(0);
        }
        //添加选课信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();

        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setStatus("701002");//待支付成功
        xcChooseCourse.setCreateDate(LocalDateTime.now());

        xcChooseCourse.setValidDays(365);//收费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));

        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0){
            XueChengPlusException.cast("选课记录表添加失败");
        }
        return xcChooseCourse;
    }
    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse){
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        XcCourseTables xcCourseTables =
                getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null){
            return xcCourseTables;
        }

        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        //记录选课表的组件
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());//选课类型
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = courseTablesMapper.insert(xcCourseTables);
        if (insert<=0){
            XueChengPlusException.cast("添加我的课程表失败");
        }

        return xcCourseTables;
    }

    /**
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables =
                courseTablesMapper.selectOne(
                        new LambdaQueryWrapper<XcCourseTables>()
                                .eq(XcCourseTables::getUserId, userId)
                                .eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }
}

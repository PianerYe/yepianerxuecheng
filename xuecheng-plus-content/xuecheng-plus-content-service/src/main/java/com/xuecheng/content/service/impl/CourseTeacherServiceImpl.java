package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/15 0:16
 * @project_name yepianerxuecheng
 * @description
 */
@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Resource
    CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> getTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> list = courseTeacherMapper.selectList(queryWrapper);
        return list;
    }
}

package com.xuecheng.learning.service;

import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.po.XcChooseCourse;

/**
 * @author yepianer
 * @date 2023/10/20 14:38
 * @project_name yepianerxuecheng
 * @description 选课相关的接口
 */
public interface MyCourseTablesService {

    /**
     * @description 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
     * @author Mr.M
     * @date 2022/10/24 17:33
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish);
}

package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * 课程信息管理接口
 * */
public interface CourseBaseInfoService {

    // 课程分页查询
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto);

    /**
     * 新增课程
     * @param companyId 机构id
     * @param addCourseDto 课程信息
     * @return 课程详细信息
     * */
    public CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto addCourseDto);

    /**
     * 根据课程的id查询课程的信息
     * @param courseId 课程id
     * @return 课程详细信息
     * */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    /**
     * 修改课程
     * @param companyId 机构id
     * @param editCourseDto 修改课程的信息
     * @return 课程详细信息
     * */
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto);
}

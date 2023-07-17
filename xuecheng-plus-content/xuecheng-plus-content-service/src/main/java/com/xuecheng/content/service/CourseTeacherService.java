package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/15 0:16
 * @project_name yepianerxuecheng
 * @description
 */
public interface CourseTeacherService {

    public List<CourseTeacher> getTeacherList(Long courseId);

    public void addTeacher(Long companyId, CourseTeacher courseTeacher);

    public void deleteTeacher(Long companyId, Long courseId, Long id);
}

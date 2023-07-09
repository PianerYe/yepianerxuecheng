package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/9 17:02
 * @project_name yepianerxuecheng
 * @description 课程计划管理相关的接口
 */
public interface TeachplanService {
    /**
     * 根据课程id查询课程计划
     * @param courseId 课程id
     * */
    public List<TeachplanDto> findTeachplanTree(Long courseId);
}

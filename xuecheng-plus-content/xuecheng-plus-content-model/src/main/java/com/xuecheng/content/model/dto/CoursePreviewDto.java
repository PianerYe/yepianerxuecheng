package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import lombok.Data;

import java.util.List;

/**
 * @author yepianer
 * @date 2023/9/10 22:06
 * @project_name yepianerxuecheng
 * @description 课程预览的模型类
 */
@Data
public class CoursePreviewDto {

    //课程基本信息,营销信息
    private CourseBaseInfoDto courseBase;
    //课程计划信息
    private List<TeachplanDto> teachplans;
    //课程师资信息...

}

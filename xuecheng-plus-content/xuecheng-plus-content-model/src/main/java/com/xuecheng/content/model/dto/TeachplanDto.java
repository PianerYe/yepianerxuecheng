package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/9 15:40
 * @project_name yepianerxuecheng
 * @description 课程计划信息的模式类
 */
@Data
@ToString
public class TeachplanDto extends Teachplan {
    //与媒资关联的信息
    private TeachplanMedia teachplanMedia;
    //小章节list
    private List<TeachplanDto> teachPlanTreeNodes;
}

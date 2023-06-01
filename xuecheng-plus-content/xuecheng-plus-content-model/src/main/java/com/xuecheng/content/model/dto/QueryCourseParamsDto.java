package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @descripion 课程查询条件模型类
 * @author yepianer
 * @date 2023/05/31
 * @version 1.0
 * */
@Data
@ToString
public class QueryCourseParamsDto implements Serializable {
    //审核状态
    @ApiModelProperty(value = "审核状态")
    private String auditStatus;
    //课程名称
    @ApiModelProperty(value = "课程名称")
    private String courseName;
    //发布状态
    @ApiModelProperty(value = "发布状态")
    private String publishStatus;
}

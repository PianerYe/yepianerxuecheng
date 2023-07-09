package com.xuecheng.content.model.dto;

import com.xuecheng.base.exception.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author yepianer
 * @date 2023/7/7 20:26
 * @project_name yepianerxuecheng
 * @description
 */
@Data
@ApiModel(value = "EditCourseDto",description = "修改课程基本信息")
public class EditCourseDto extends AddCourseDto{

//    @NotEmpty(message = "课程id不能为空")
//    @NotEmpty(message = "课程id不能为空",groups = {ValidationGroups.Update.class})
    @ApiModelProperty(value = "课程id",required = true)
    private long Id;
}

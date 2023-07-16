package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/14 23:31
 * @project_name yepianerxuecheng
 * @description
 */

@Api(value = "师资管理接口",tags = "师资管理接口")
@RestController
public class CourseTeacherController {

    @Resource
    CourseTeacherService courseTeacherService;

    @ApiOperation("查询师资列表")
    @GetMapping ("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> getTeacherList(@PathVariable Long courseId){
        List<CourseTeacher> list = courseTeacherService.getTeacherList(courseId);
        return list;
    }

    @ApiOperation("添加教师/修改教师")
    @PostMapping ("/courseTeacher")
    public void addTeach(@RequestBody CourseTeacher courseTeacher){
        //只允许向机构自己的课程中添加老师、删除老师。
        Long companyId = 1232141425L;
        courseTeacherService.addTeacher(companyId,courseTeacher);
    }

    @ApiOperation("删除教师")
    @DeleteMapping ("/courseTeacher")
    public void deleteTeach(){
        return;
    }
}

package com.xuecheng.content.api;


import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.utils.SecurityUtil;
import com.xuecheng.content.model.WxparamDto;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;

@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController()
public class CourseBaseInforController {

    @Resource
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBaseDto> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        //subsectionNum  任务数   <div>{{scope.row.charge | chargeText}}</div> 是否付费

        PageResult<CourseBaseDto> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);

        return courseBasePageResult;

    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated({ValidationGroups.Inster.class,}) AddCourseDto addCourseDto){

        //获取到用户所属机构的id
        Long companyId = 1232141425L;
//        int i = 1/0 ;
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(companyId, addCourseDto);

        return courseBase;
    }

    @ApiOperation("根据课程id查询接口")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId) {
        //获取当前用户的身份
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        System.out.println(principal);
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //当user为null,如果打印输出，会报错
//        System.out.println(user.getUsername());
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @ApiOperation("修改课程id查询接口")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){
        //获取到用户所属机构的id
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
        return courseBaseInfoDto;
    }

//    @ApiOperation("审核完成后,发布课程")
//    @PostMapping("/coursepublish/{id}")
//    public void setCoursepublish(@PathVariable Long id){
//        //获取到用户所属机构的id
//        Long companyId = 1232141425L;
//        courseBaseInfoService.setCoursepublish(companyId,id);
//    }

    @ApiOperation("审核完成后,下架课程")
    @GetMapping ("/courseoffline/{id}")
    public void setCourseoffline(@PathVariable Long id){
        //获取到用户所属机构的id
        Long companyId = 1232141425L;
        courseBaseInfoService.setCourseoffline(companyId,id);
    }

    @ApiOperation("未提交状态的课程，进行删除")
    @DeleteMapping("/course/{id}")
    public void deleteCourse(@PathVariable Long id){
        //获取到用户所属机构的id
        Long companyId = 1232141425L;
        courseBaseInfoService.deleteCourse(companyId,id);

    }

    @PostMapping ("/wxLogin")
    public String wxLoginTwo(@RequestBody WxparamDto wxparamrDto) {
        //tempUserId : 3471A8391863D80028274FE5B52CCC3E1697035065495
        //todo:远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
//        XcUser xcUser = new XcUser();
//        暂时硬编写，目的是调试环境
//        xcUser.setUsername("t1");
//        if (xcUser==null){
//            return "redirect:http://www.51xuecheng.cn/error.html";
//        }
//        String username = xcUser.getUsername();
        return "redirect:http://www.51xuecheng.cn/sign.html?username=\"+username+" + "&authType=wx";
    }
}

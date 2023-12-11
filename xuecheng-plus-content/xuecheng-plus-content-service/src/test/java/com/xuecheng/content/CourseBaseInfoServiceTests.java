package com.xuecheng.content;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.CourseBaseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class CourseBaseInfoServiceTests {
    @Resource
    CourseBaseInfoService courseBaseInfoService;

    @Test
    void testCourseBaseInfoService(){

        QueryCourseParamsDto courseParamsDto = new QueryCourseParamsDto();
        courseParamsDto.setCourseName("java");
        courseParamsDto.setAuditStatus("202004");
//        courseParamsDto.setPublishStatus("203001");

        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(3L);

        Long companyId = 1232141425L;
//        PageResult<CourseBaseDto> courseBasePageResult =
//                courseBaseInfoService.queryCourseBaseList(pageParams, courseParamsDto);

        PageResult<CourseBaseDto> courseBasePageResult =
                courseBaseInfoService.queryCourseBaseList(companyId,pageParams, courseParamsDto);
        System.out.println(courseBasePageResult);
    }
}

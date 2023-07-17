package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/15 0:16
 * @project_name yepianerxuecheng
 * @description
 */
@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Resource
    CourseTeacherMapper courseTeacherMapper;
    @Resource
    CourseBaseMapper courseBaseMapper;

    @Override
    public List<CourseTeacher> getTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> list = courseTeacherMapper.selectList(queryWrapper);
        return list;
    }

    private Boolean isEqualCompany(Long companyId, CourseTeacher courseTeacher){
        //先判断companyId是否是该公司的机构
        Long courseId = courseTeacher.getCourseId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        Long companyId1 = courseBase.getCompanyId();
        if (companyId1 == null){
            XueChengPlusException.cast("公司id不能为空");
            return false;
        }
        if(!companyId1.equals(companyId)){
            XueChengPlusException.cast("不是该公司的机构，无法添加老师");
            return false;
        }
        return true;
    }

    /**
     * 添加教师
     * */
    @Transactional
    @Override
    public void addTeacher(Long companyId, CourseTeacher courseTeacher) {
        Boolean equalCompany = isEqualCompany(companyId, courseTeacher);
        if (equalCompany){
            //是该公司的机构，可以进行添加操作
            CourseTeacher courseTeacher1 = courseTeacherMapper.selectById(courseTeacher.getId());
            if (courseTeacher1 ==null){
                courseTeacher.setCreateDate(LocalDateTime.now());
                int insert = courseTeacherMapper.insert(courseTeacher);
                    if (insert <= 0){
                        XueChengPlusException.cast("添加课程失败");
                    }else {
                        System.out.println("添加成功");
                    }
            } else {
                LambdaUpdateWrapper updateWrapper = new LambdaUpdateWrapper<CourseTeacher>();
                CourseTeacher courseTeacherNew  = new CourseTeacher();
                BeanUtils.copyProperties(courseTeacher,courseTeacherNew);
                courseTeacherNew.setCreateDate(courseTeacher1.getCreateDate());
                int i = courseTeacherMapper.updateById(courseTeacherNew);
                if (i <= 0){
                    XueChengPlusException.cast("修改课程失败");
                }else {
                    System.out.println("修改课程成功");
                }
            }

        }
    }

    @Transactional
    @Override
    public void deleteTeacher(Long companyId, Long courseId, Long id) {
        CourseTeacher courseTeacher = courseTeacherMapper.selectById(id);
        Boolean equalCompany = isEqualCompany(companyId,courseTeacher);
        if (equalCompany){
            int i = courseTeacherMapper.deleteById(id);
            if ( i <= 0){
                XueChengPlusException.cast("删除课程失败");
            }
            System.out.println("删除课程成功");
        }
    }
}

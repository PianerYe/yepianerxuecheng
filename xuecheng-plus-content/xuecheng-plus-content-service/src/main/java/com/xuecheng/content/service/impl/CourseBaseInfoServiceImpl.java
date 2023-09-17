package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Resource
    CourseBaseMapper courseBaseMapper;
    @Resource
    CourseMarketMapper courseMarketMapper;
    @Resource
    CourseCategoryMapper courseCategoryMapper;
    @Resource
    CourseTeacherMapper courseTeacherMapper;
    @Resource
    TeachplanMapper teachplanMapper;

    @Override
    public PageResult<CourseBaseDto> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        // 测试查询接口
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //拼接查询条件
        //根据课程名称模糊查询  name like '%名称%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName,queryCourseParamsDto.getCourseName());
        //根据课程审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //分页参数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        Page<CourseBaseDto> dtoPage = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //分页查询E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page,queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(page,dtoPage,"records");
        //数据
        List<CourseBase> items = pageResult.getRecords();
        List<CourseBaseDto>list =items.stream().map((item)->{
            CourseBaseDto courseBaseDto = new CourseBaseDto();
            BeanUtils.copyProperties(item,courseBaseDto);
            Long courseBaseDtoId = courseBaseDto.getId();
            LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Teachplan::getCourseId,courseBaseDtoId)
                    .eq(Teachplan::getGrade,"1");
            //SELECT COUNT(1) FROM teachplan WHERE course_id = 25 AND grade = "1"
            Integer integer = teachplanMapper.selectCount(wrapper);
            courseBaseDto.setSubsectionNum(integer);
            //查询是否收费免费
            //select charge from course_market where id = courseBaseDtoId;
            LambdaQueryWrapper<CourseMarket> wrapper2 = new LambdaQueryWrapper<>();
            wrapper2.eq(CourseMarket::getId,courseBaseDtoId);
            CourseMarket courseMarket = courseMarketMapper.selectOne(wrapper2);
            String charge ;
            if (courseMarket == null){
                charge = null;
            }else {
                charge = courseMarket.getCharge();
            }
            courseBaseDto.setCharge(charge);

            return courseBaseDto;
        }).collect(Collectors.toList());
        //总记录数
        long total = pageResult.getTotal();

        //准备返回数据 List<T> items, long counts, long page, long pageSize
        PageResult<CourseBaseDto> courseBasePageResult = new PageResult<>(list, total,pageParams.getPageNo(), pageParams.getPageSize());

        return  courseBasePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //参数合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
////            throw new RuntimeException("课程名称为空");
//            XueChengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
//        }

        //向课程基本信息表course_base写入数据
        CourseBase courseBaseNew = new CourseBase();
        //将传入的页面的参数放到courseBaseNew对象
//        courseBaseNew.setName(dto.getName());
//        courseBaseNew.setDescription(dto.getDescription());
        //上边从原始对象中get拿数据向新对象set，比较复杂
        BeanUtils.copyProperties(dto,courseBaseNew);//主要属性名称一致就可以拷贝
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //审核状态默认为未提交
        courseBaseNew.setAuditStatus("202002");
        //发布状态默认为未发布
        courseBaseNew.setStatus("203001");
        //插入数据库
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0){
            throw new RuntimeException("添加课程失败");
        }
        //向课程营销表course_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        //将页面输入的数据拷贝到courseMarketNew
        BeanUtils.copyProperties(dto,courseMarketNew);
        //课程id
        Long id = courseBaseNew.getId();
        courseMarketNew.setId(id);
        //保存营销信息
        saveCourseMarket(courseMarketNew);
        //从数据库查出课程的详细信息,包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(id);
        return courseBaseInfo;
    }

    //查询课程信息
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase==null){
            return null;
        }
        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if (courseMarket!=null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }else {
            CourseMarket courseMarket1 = new CourseMarket();
            courseMarket1.setId(courseId);
            courseMarket1.setCharge("201000");
            BeanUtils.copyProperties(courseMarket1,courseBaseInfoDto);
        }
        //通过courseCategoryMapper查询分类信息，将分类名称放在courseBaseInfoDto中
        //todo: 课程分类的名称设置到对象中
        String mt = courseBaseInfoDto.getMt();
        CourseCategory courseCategoryMt = courseCategoryMapper.selectById(mt);
        String mTName = courseCategoryMt.getName();
        courseBaseInfoDto.setMtName(mTName);

        String st = courseBaseInfoDto.getSt();
        CourseCategory courseCategorySt = courseCategoryMapper.selectById(st);
        String sTName = courseCategorySt.getName();
        courseBaseInfoDto.setStName(sTName);

        return courseBaseInfoDto;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        //拿到课程id
        Long courseId = editCourseDto.getId();
        //查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null){
            XueChengPlusException.cast("课程不存在");
        }
        //数据合法性校验
        //根据具体的业务逻辑去校验
        //本机构只能修改本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        //封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        //修改时间
        courseBase.setChangeDate(LocalDateTime.now());
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        //更新数据库
        int i = courseBaseMapper.updateById(courseBase);
        if (i <= 0){
            XueChengPlusException.cast("修改课程失败");
        }
        //更新营销信息
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto,courseMarketNew);
        saveCourseMarket(courseMarketNew);
        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    /**
     * 审核完成课程，修改状态改为发布
     * */
    @Override
    public void setCoursepublish(Long companyId, Long id) {
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        //update status = "203002"  course_base where id = #{id}
        int i = courseBaseMapper.updateStatusCoursepublish(id);
        if (i <= 0){
            XueChengPlusException.cast("发布状态有误");
        }
    }

    /**
     * 审核完成课程，修改状态改为下架
     * */
    @Override
    public void setCourseoffline(Long companyId, Long id) {
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        //update status = "203002"  course_base where id = #{id}
        int i = courseBaseMapper.updateStatusCourseoffline(id);
        if (i <= 0){
            XueChengPlusException.cast("发布状态有误");
        }
    }

    @Transactional
    @Override
    public void deleteCourse(Long companyId,Long id) {
        //首先判断是否是同一机构下的
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        //首先，判断课程是未审核状态的
        String auditStatus = courseBase.getAuditStatus();
        // 判断是否是未审核状态
        if (!"202002".equals(auditStatus)){
            XueChengPlusException.cast("该课程已经审核通过，无法直接删除，请联系管理员");
        }
        //表明是未审核状态的，可以删除执行操作
        //其次，判断其他关联的数据库关联信息，然后进行删除
        //courseBase表 course_teacher表  course_plan表  teachplan_media表  teachplan_work表
        //不删除teachplan_media表  teachplan_course表  暂时
        //判断course_teacher表
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(CourseTeacher::getCourseId,id);
        //删除course_teacher表数据
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        if (courseTeachers != null){
            for (CourseTeacher courseTeacher : courseTeachers) {
                Long teacherId = courseTeacher.getId();
                courseTeacherMapper.deleteById(teacherId);
            }
        }
        //判断 course_plan表
        LambdaQueryWrapper<Teachplan> queryWrapper2 = new LambdaQueryWrapper();
        queryWrapper2.eq(Teachplan::getCourseId,id);
        //删除course_plan表数据
        List<Teachplan> teachplans = teachplanMapper.selectList(queryWrapper2);
        if (teachplans != null){
            for (Teachplan teachplan : teachplans) {
                Long teachplanId = teachplan.getId();
                teachplanMapper.deleteById(teachplanId);
            }
        }
        //删除courseBase表
        courseBaseMapper.deleteById(id);
    }


    //单独写一个方法保存营销信息，逻辑：存在则更新，不存在则添加
    private int saveCourseMarket(CourseMarket courseMarketNew){
        //参数的合法性校验
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isEmpty(charge)){
            throw new RuntimeException("收费规则为空");
        }
        //如果课程收费，价格没有填写也需要抛异常
        if (charge.equals("201001")){
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice() <= 0){
//                throw new RuntimeException("课程的价格不能为空并且必须大于0");
                XueChengPlusException.cast("课程的价格不能为空并且必须大于0");
            }
        }
        //从数据库查询营销信息,存在则更新，不存在则添加
        Long id = courseMarketNew.getId();//主键
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseMarket == null){
            //插入数据库
            int insert = courseMarketMapper.insert(courseMarketNew);
            return insert;
        }else {
            //将courseMarketNew拷贝到courseMarket
            BeanUtils.copyProperties(courseMarketNew,courseMarket);
            courseMarket.setId(courseMarketNew.getId());
            int i = courseMarketMapper.updateById(courseMarket);
            return i;
        }
    }
}

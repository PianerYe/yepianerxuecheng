package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/9 17:04
 * @project_name yepianerxuecheng
 * @description
 */
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Resource
    TeachplanMapper teachplanMapper;
    @Resource
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    private int getTeachplanCount(Long courseId,Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Teachplan> eq = queryWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count+1;
    }
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断是新增还是修改
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId == null){
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //确定排序字段,找到同级节点个数，排序字段就是个数加1 SELECT COUNT(1) FROM teachplan WHERE course_id = 117 AND parentid = 0
            Long parentId = teachplan.getParentid();
            Long courseId = teachplan.getCourseId();
            int teachplanCount = getTeachplanCount(courseId, parentId);
            teachplan.setOrderby(teachplanCount);

            teachplanMapper.insert(teachplan);
        }else {
            //修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //将参数复制到teachplan
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    @Override
    public void deleteTeachplan(Long id) {
        //首先查询id为269的课程信息关联的子课程信息是否存在，如果存在无法删除
//        Teachplan teachplan = teachplanMapper.selectById(courseId);
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Teachplan> eq = queryWrapper.eq(Teachplan::getParentid, id);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        if (count == 0){
            //判断章节id是否和课程计划和媒体关联表关联，如果关联则删除关联表信息
            LambdaQueryWrapper<TeachplanMedia> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(TeachplanMedia::getTeachplanId,id);
            Integer count1 = teachplanMediaMapper.selectCount(queryWrapper1);
            if (count1 != 0){
                //删除关联表信息
               teachplanMediaMapper.delete(queryWrapper1);
            }
            //删除子级信息
            int i = teachplanMapper.deleteById(id);
        }else {
            XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
        }
    }


    private void getUpdateOrderBy(Teachplan teachplan,int changeOrderby){
        LambdaUpdateWrapper<Teachplan> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Teachplan::getOrderby, changeOrderby);
        teachplanMapper.update(teachplan,updateWrapper);
    }
    @Transactional
    @Override
    public void moveupTeachplan(Long id) {
        //首先查询这是大章节的排序还是小章节的总排序（层级结构）
        //可以直接查询父类的id，通过这个sql语句直接排序即可
        //SELECT orderby FROM teachplan WHERE id = 117
            //查询parentid
        Teachplan teachplan = teachplanMapper.selectById(id);
        Integer orderby = teachplan.getOrderby();
        if (orderby == 1){
            XueChengPlusException.cast("已经在最顶端了，无法上移了");
        }else if (orderby >= 2){
            //被迫下移的大/小章节 -1
            //SELECT * FROM teachplan WHERE course_id = 117 AND parentid = 291 AND orderby = 1;
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId,teachplan.getCourseId())
                    .eq(Teachplan::getParentid,teachplan.getParentid())
                    .eq(Teachplan::getOrderby,(teachplan.getOrderby()-1));
            Teachplan teachplan1 = teachplanMapper.selectOne(queryWrapper);//备注1
            getUpdateOrderBy(teachplan1, orderby + 1);
            //上移的大/小章节 +1
            getUpdateOrderBy(teachplan, orderby - 1);

        }else {
            XueChengPlusException.cast("出现未知的负值异常");
        }


    }
}

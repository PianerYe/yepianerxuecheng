package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
}

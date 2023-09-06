package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/9 17:02
 * @project_name yepianerxuecheng
 * @description 课程计划管理相关的接口
 */
public interface TeachplanService {
    /**
     * 根据课程id查询课程计划
     * @param courseId 课程id
     * */
    public List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增/修改/保存课程计划
     * */
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    public void deleteTeachplan(Long id);

    public void moveupTeachplan(Long id);

    public void movedownTeachplan(Long id);

    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    public void deleteWithAssociationMedia(String teachPlanId, String mediaId);
}

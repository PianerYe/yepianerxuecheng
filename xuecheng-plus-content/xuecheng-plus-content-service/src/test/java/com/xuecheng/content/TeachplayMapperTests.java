package com.xuecheng.content;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yepianer
 * @date 2023/7/9 16:29
 * @project_name yepianerxuecheng
 * @description 课程计划mapper测试
 */
@SpringBootTest
public class TeachplayMapperTests {

    @Resource
    TeachplanMapper teachplanMapper;

    @Test
    public void testSelectTreeNodes(){
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(117L);
        System.out.println(teachplanDtos);
    }
}

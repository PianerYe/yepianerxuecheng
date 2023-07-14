package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Resource
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点的子节点，封装成List<CourseCategoryTreeDto>
        //先将list转成map,key就是节点的id，value就是CourseCategoryTreeDto对象,目的为了方便从map获取节点y
        //.filter(item->!id.equals(item.getId()))把根节点排除
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos
                .stream()
                .filter(item->!id.equals(item.getId()))
                .collect(Collectors
                        .toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        //定义一个list作为最终返回的List
        List<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();
        //从头遍历List<CourseCategoryTreeDto>，一边遍历一边找节点的子节点放在父节点的childTreeNodes
        courseCategoryTreeDtos
                .stream()
                .filter(item->!id.equals(item.getId()))
                .forEach(item->{
            //向list写入元素
            if(item.getParentid().equals(id)){
                courseCategoryList.add(item);
            }
                //找到节点的父节点
                CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
                if (courseCategoryTreeDto != null) {
                    if (courseCategoryTreeDto.getChildrenTreeNodes() == null){
                        //如果该父节点的ChildrenTreeNodes属性为空要new一个集合，因为要向该集合中放它的子节点
                        courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                    }
                    //找到每个子节点的父节点放在父节点的childTreeNodes属性中
                    courseCategoryTreeDto.getChildrenTreeNodes().add(item);
                }
        });
        return courseCategoryList;
    }
}

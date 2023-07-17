package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseBase;
import lombok.Data;

@Data
public class CourseBaseDto extends CourseBase {
    //任务数，一共含有几个大章节，就是所谓的任务数
    private Integer subsectionNum;
    //是否收费，对应词典。201000 免费  201001 收费
    private String charge;
}

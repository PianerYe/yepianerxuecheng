package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class CourseBaseInfoDto extends CourseBase {

    //收费规则，对应数据字典
    private String charge;
    //价格
    private Float price;
    //原价
    private Float originalPrice;
    //咨询qq
    private String qq;
    //微信
    private String wechat;
    //电话
    private String phone;
    //有效天数
    private Integer validDays;
    //大分类名称
    private String mtName;
    //小分类名称
    private String stName;

}

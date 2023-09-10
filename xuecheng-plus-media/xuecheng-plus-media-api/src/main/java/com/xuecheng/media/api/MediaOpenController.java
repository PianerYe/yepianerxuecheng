package com.xuecheng.media.api;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.media.model.dto.RestResponse;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author yepianer
 * @date 2023/9/10 23:05
 * @project_name yepianerxuecheng
 * @description
 */
@Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
@RestController
@RequestMapping("/open")
public class MediaOpenController {

    @Resource
    MediaFileService mediaFileService;

    @ApiOperation("预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable String mediaId){
        //查询文件信息
        MediaFiles mediaFiles = mediaFileService.getFileById(mediaId);
        if (mediaFiles == null){
            return RestResponse.validfail("找不到视频");
        }
        //取出视频播放地址
        String url = mediaFiles.getUrl();
        if (StringUtils.isEmpty(url)){
            return RestResponse.validfail("该视频正在处理中");
        }
        return RestResponse.success(mediaFiles.getUrl());

    }


}

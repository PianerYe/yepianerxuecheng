package com.xuecheng.media.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * @author yepianer
 * @date 2023/7/26 23:54
 * @project_name yepianerxuecheng
 * @description
 */
@Data
@ToString
public class UploadFileParamsDto {
    /**
     * 文件名称
     */
    private String filename;


    /**
     * 文件类型（文档，音频，视频）
     */
    private String fileType;
    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 标签
     */
    private String tags;

    /**
     * 上传人
     */
    private String username;

    /**
     * 备注
     */
    private String remark;
}

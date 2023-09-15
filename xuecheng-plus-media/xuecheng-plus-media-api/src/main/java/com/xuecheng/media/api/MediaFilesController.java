package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * @description 媒资文件管理接口
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
@Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


  @Resource
  MediaFileService mediaFileService;


 @ApiOperation("媒资列表查询接口")
 @PostMapping("/files")
 public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
  Long companyId = 1232141425L;
  return mediaFileService.queryMediaFiels(companyId,pageParams,queryMediaParamsDto);

 }

 @ApiOperation("上传图片")
 @RequestMapping(value = "/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public UploadFileResultDto upload(@RequestPart("filedata")MultipartFile filedata,
                                   @RequestParam(value = "objectName",required = false) String objectName) throws IOException {

     //准备上传文件的信息
     UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
     uploadFileParamsDto.setFilename(filedata.getOriginalFilename());//原始文件名称
     uploadFileParamsDto.setFileSize(filedata.getSize());//文件大小
     uploadFileParamsDto.setFileType("001001");//文件类型
     //接收到文件了
     //创建临时文件
     File tempFile = File.createTempFile("minio", ".temp");
     filedata.transferTo(tempFile);

     Long companyId = 1232141425L;
     //文件路径
     String localFilePath = tempFile.getAbsolutePath();

     //调用service上传图片
     UploadFileResultDto uploadFileResultDto = mediaFileService.uploadFile(companyId, uploadFileParamsDto, localFilePath,objectName);

     return uploadFileResultDto;
 }

}

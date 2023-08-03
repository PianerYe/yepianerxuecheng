package com.xuecheng.media.api;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author yepianer
 * @date 2023/7/30 4:20
 * @project_name yepianerxuecheng
 * @description 上传视频
 */
@Api(value = "大文件上传接口",tags = "大文件上传接口")
@RestController
public class BigFilesController {
    @Resource
    MediaFileService mediaFileService;

    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkfile(@RequestParam("fileMd5") String fileMd5){
        RestResponse<Boolean> booleanRestResponse = mediaFileService.checkFile(fileMd5);
        return booleanRestResponse;
    }
    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk){
        RestResponse<Boolean> booleanRestResponse = mediaFileService.checkChunk(fileMd5,chunk);
        return booleanRestResponse;
    }
    @ApiOperation(value = "上传分块文件")
    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadchunk(@RequestParam("file")MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk") int chunk){
        String localFilePath = "";
        try {
            //创建一个临时文件
            File tempFile = File.createTempFile("minio", ".temp");
            file.transferTo(tempFile);
//            Long companyId = 1232141425L;
            //文件路径
            localFilePath = tempFile.getAbsolutePath();
        }catch (Exception e){
            XueChengPlusException.cast("创建临时文件失败");
        }

        RestResponse restResponse = mediaFileService.uploadChunk(fileMd5, chunk, localFilePath);
        return restResponse;
    }
    @ApiOperation(value = "合并文件")
    @PostMapping("/upload/mergechunks")
    public RestResponse mergechunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal){
        return null;
    }
}

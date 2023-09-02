package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */

@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

  @Resource
  MediaFilesMapper mediaFilesMapper;
  @Autowired
  MinioClient minioClient;
  @Resource
  MediaProcessMapper mediaProcessMapper;

  @Resource
  MediaFileService currentProxy;

  //存储普通文件
  @Value("${minio.bucket.files}")
  private String bucket_mediafiles;
  //存储视频
  @Value(("${minio.bucket.videofiles}"))
  private String bucket_video;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  queryWrapper.like(StringUtils.isNotEmpty(queryMediaParamsDto.getFilename()),MediaFiles::getFilename,queryMediaParamsDto.getFilename())
          .eq(StringUtils.isNotEmpty(queryMediaParamsDto.getFileType()),MediaFiles::getFileType,queryMediaParamsDto.getFileType());
  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }


 //根据扩展名获取mimeType
 private String getMimeType(String extension){
   if (extension == null){
     extension = "";
   }
   //通过扩展名得到媒体资源类型 mimeType
   ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
   String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
   if (extensionMatch!=null){
    mimeType = extensionMatch.getMimeType();
   }
   return mimeType;
 }

 // 将文件上传到minio
 /**
  * @param objectName 对象名
  * @param localFilePath 本地路径
  * @param bucket 桶
  * @param mimeType 媒体类型
  * */
 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,
                              String bucket, String objectName){
  //上传文件的参数信息
  try {
   UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
           .bucket(bucket)//确定桶
           .filename(localFilePath)//指定本地文件路径
           .object(objectName)//对象名放在子目录下
           .contentType(mimeType)//设置媒体文件类型
           .build();
    //上传文件
    minioClient.uploadObject(uploadObjectArgs);
    log.debug("上传文件到minio成功bucket:{},objectName:{}",bucket,objectName);
    return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}",bucket,objectName,e.getMessage());
  }
  return false;
 }

 //获取文件默认存储目录路径 年月日
 private String getDefaultFolderPath(){
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")  + "/";
  // 2023/02/17/
  return folder;
 }
 // 获取文件的md5
 private String getFileMd5(File file){
  try(FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  }catch (Exception e){
    e.printStackTrace();
    return null;
  }

 }

 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

  //文件名
  String filename = uploadFileParamsDto.getFilename();
  //先得到扩展名
  String extension = filename.substring(filename.lastIndexOf("."));
  //得到mimeType
  String mimeType = getMimeType(extension);

  //子目录
  String defaultFolderPath = getDefaultFolderPath();
  //文件的md5值
  String fileMd5 = getFileMd5(new File(localFilePath));
  String objectName = defaultFolderPath + fileMd5 + extension ;
  //上传文件到minio
     boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_mediafiles, objectName);
    if (!result){
        XueChengPlusException.cast("上传文件失败");
    }
    //入库信息
     MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
    if (mediaFiles == null){
        XueChengPlusException.cast("文件上传后保存信息失败");
    }
    //准备返回的对象
     UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
     BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
     return uploadFileResultDto;
 }

 @Transactional
    public MediaFiles addMediaFilesToDb (Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
     //将文件信息保存到数据库
     MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
     if (mediaFiles == null){
         mediaFiles = new MediaFiles();
         BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
         //文件id
         mediaFiles.setId(fileMd5);
         mediaFiles.setCompanyId(companyId);
         //桶
         mediaFiles.setBucket(bucket);
         //file_path
         mediaFiles.setFilePath(objectName);
         //file_id
         mediaFiles.setFileId(fileMd5);
         //url
         mediaFiles.setUrl("/"+ bucket + "/" + objectName);
         //上传时间
         mediaFiles.setCreateDate(LocalDateTime.now());
         //状态
         mediaFiles.setStatus("1");
         //审核状态
         mediaFiles.setAuditStatus("002003");
         //插入数据库
         int insert = mediaFilesMapper.insert(mediaFiles);
         if (insert <= 0){
             log.debug("向数据库保存文件信息失败,bucket:{},objectName:{}",bucket,objectName);
             return null;
         }
         //记录待处理的任务
         //minmeType判断如果是avi视频写入待处理任务
         addWaitingTask(mediaFiles);
         //向 MediaProcess插入记录
         return mediaFiles;
     }
     return mediaFiles;
 }

     /**
      * 添加待处理任务
      * */
     private void addWaitingTask(MediaFiles mediaFiles){
         //获取文件的mimeType
         String filename = mediaFiles.getFilename();
         //文件扩展名
         String extension = filename.substring(filename.lastIndexOf("."));
         String mimeType = getMimeType(extension);
         if (mimeType.equals("video/x-msvideo")){//如果是avi写入待处理表
             MediaProcess mediaProcess = new MediaProcess();
             BeanUtils.copyProperties(mediaFiles,mediaProcess);
             //状态是未处理
             mediaProcess.setStatus("1");//未处理
             mediaProcess.setCreateDate(LocalDateTime.now());
             mediaProcess.setFailCount(0);//失败默认0
             mediaProcess.setUrl(null);
             mediaProcessMapper.insert(mediaProcess);
         }
         //
     }


    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null){
            //桶
            String bucket = mediaFiles.getBucket();
            //objectName
            String filePath = mediaFiles.getFilePath();
            //如果数据库存在再查询Minio
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath).build();
            //查询远程服务获取一个流对象
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null){
                    //文件已存在，不用检查没关系的
                    //检查文件状态，是否上传完成
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        //文件不存在
        return RestResponse.success(false);
    }

    @Override
    @Transactional
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        //分块存储路径:md5前两位为两个目录,chunk存储分块文件
        //根据md5得到分块文件的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件路径
        //判断数据库是否存在
//        String fileId = fileMd5 + chunkIndex;
//        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(MediaProcess::getFileId, fileId);
//        MediaProcess mediaProcess = mediaProcessMapper.selectOne(queryWrapper);
//        if (mediaProcess != null && "1".equals(mediaProcess.getStatus())){
//            //数据库存在
//            return RestResponse.success(true);
//        }
        //如果数据库不存在
        //如果数不存在再查询Minio
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFileFolderPath + chunkIndex ).build();
        //查询远程服务获取一个流对象
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null){
                //文件已存在
                //清除分块文件，并且重新设置下载
                //clearChunkFiles
//                clearChunkIndexFile(chunkFileFolderPath,chunkIndex);
                return RestResponse.success(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    return RestResponse.success(false);
}

    private void clearChunkIndexFile(String chunkFileFolderPath,int chunkIndex){
        /**
         * List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i ->
         *                 ComposeSource.builder().bucket(bucket_video)
         *                         .object(chunkFileFolderPath + i)
         *                         .build()).collect(Collectors.toList());
         *
         */
        DeleteObject deleteObject = new DeleteObject(chunkFileFolderPath + chunkIndex);
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(bucket_video).object(String.valueOf(deleteObject)).build();
        try {
            minioClient.removeObject(removeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        //分块文件的路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        //获取mimeType
        String mimeType = getMimeType(null);
        //将文件上传到minio
        boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, chunkFilePath);
        if (!b){
            return RestResponse.validfail(false,"上传分块文件失败");
        }
        //上传成功之后把上传文件记录到数据库，并作标识
//        int i = InsertmediaProcess(chunk, fileMd5, bucket_video, "1", chunkFilePath);
//        if (i<=0){
//            return RestResponse.validfail(false,"记录分块文件到数据库失败");
//        }
        return RestResponse.success(true);
    }


    private int InsertmediaProcess(int chunk,String fileMd5,String bucket,String status,String chunkFilePath){
        MediaProcess mediaProcess = new MediaProcess();
        mediaProcess.setFileId(fileMd5+chunk);
        mediaProcess.setFilename(fileMd5);
        mediaProcess.setBucket(bucket);
        mediaProcess.setCreateDate(LocalDateTime.now());
//        mediaProcess.setStatus("1");
        mediaProcess.setStatus(status);
        mediaProcess.setUrl(chunkFilePath);
        int insert = mediaProcessMapper.insert(mediaProcess);
        return insert;
    }

    @Override
    @Transactional
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件所在目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //找到所有的分块文件
        //获取分块文件,调用Minio进行文件合并
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i ->
                ComposeSource.builder().bucket(bucket_video)
                        .object(chunkFileFolderPath + i)
                        .build()).collect(Collectors.toList());
        //源文件名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并后文件的objectName
        String objectName = getFilePathByMd5(fileMd5,extension);
        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName)//合并后的文件的objectName
                .sources(sources)
                .build();
        //合并文件
        //报错size,minio默认分块文件大小为5mb
        try {
            minioClient.composeObject(composeObjectArgs);
        }catch (Exception e){
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}",bucket_video,objectName,e.getMessage());
            return RestResponse.validfail(false,"合并文件异常");
        }
        //校验合并后的和源文件是否一致,视频上传成功
        //先下载合并后的文件
        File file = downloadFileFromMinIO(bucket_video, objectName);

        try (FileInputStream fileInputStream = new FileInputStream(file)){
            //计算合并后文件的md5
            String mergeFile_md5 = DigestUtils.md5Hex(fileInputStream);
            //比较原始的md5值和合并后文件的md5
            if (!fileMd5.equals(mergeFile_md5)){
                 log.error("校验合并文件md5不一致,原始文件:{},合并文件:{}",fileMd5,mergeFile_md5);
                return RestResponse.validfail(false,"文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        }catch (Exception e){
            return RestResponse.validfail(false,"文件校验失败");
        }

        //文件信息入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null){
            return RestResponse.validfail(false,"文件入库失败");
        }
        //清理分块文件
        clearChunkFiles(chunkFileFolderPath,chunkTotal);
        //记录待处理任务
        //批量清理数据库存储的分块文件信息
//        int i = clearMediaProcess(fileMd5);
//        if (i<=0){
//            return RestResponse.validfail("数据库分块文件删除失败");
//        }
        return RestResponse.success(true);
    }

    private int clearMediaProcess(String fileMd5){
     //delete medisprocess where filename = XXX;
        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MediaProcess::getFilename,fileMd5).eq(MediaProcess::getStatus,"1");
        int i = mediaProcessMapper.delete(queryWrapper);
        return i;
    }

    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){
        /**
         * List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i ->
         *                 ComposeSource.builder().bucket(bucket_video)
         *                         .object(chunkFileFolderPath + i)
         *                         .build()).collect(Collectors.toList());
         *
         */
        Iterable<DeleteObject> objects = Stream.iterate(0,i -> ++i).limit(chunkTotal).map(i ->
                new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                .collect(Collectors.toList());
        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        //要想真正的删除
        results.forEach(f->{
            try {
                DeleteError deleteError = f.get();
            } catch (Exception e) {
               e.printStackTrace();
            }
        });
    }

    public File downloadFileFromMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    //得到分块文件目录
    private String getChunkFileFolderPath(String fileMd5){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +"chunk" + "/";
    }

    //根据md5值得到合并后的文件名称
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2)
                + "/" + fileMd5 + "/" + fileMd5  + fileExt;
    }
}

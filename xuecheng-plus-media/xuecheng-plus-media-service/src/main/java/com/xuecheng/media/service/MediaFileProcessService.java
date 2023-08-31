package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author yepianer
 * @date 2023/8/30 22:21
 * @project_name yepianerxuecheng
 * @description 任务处理
 */

public interface MediaFileProcessService {

    public List<MediaProcess> selectListByShardIndex(@Param("shardTotal") int shardTotal, @Param("shardIndex") int shardIndex, @Param("count") int count);

    public boolean startTask(@Param("id") long id);

    public void saveProcessFinsihStatus(Long taskId,String status,String fileID,String url,String errorMsg);
}

package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * @author yepianer
 * @date 2023/9/17 15:09
 * @project_name yepianerxuecheng
 * @description
 */
@Slf4j
@Component
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClinet> {
    //拿到了熔断的异常信息
    @Override
    public SearchServiceClinet create(Throwable cause) {

        return new SearchServiceClinet() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                log.error("添加课程索引发生熔断,索引信息:{},熔断异常:{}",courseIndex,cause);
                //走降级了，返回false
                return false;
            }
        };

    }
}

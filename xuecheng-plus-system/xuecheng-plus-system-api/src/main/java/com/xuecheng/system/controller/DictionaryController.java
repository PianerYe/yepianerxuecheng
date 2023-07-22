package com.xuecheng.system.controller;

import com.xuecheng.system.model.po.Dictionary;
import com.xuecheng.system.service.DictionaryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 数据字典 前端控制器
 * </p>
 *
 * @author itcast
 */
@Api(value = "数据字典管理接口",tags = "数据字典管理管理接口")
@Slf4j
@RestController
public class DictionaryController  {

    @Autowired
    private DictionaryService  dictionaryService;

    @ApiOperation("数据字典的查询接口")
    @GetMapping("/dictionary/all")
    public List<Dictionary> queryAll() {
        return dictionaryService.queryAll();
    }

    @ApiOperation("根据代码查询数据字典的对应类型接口")
    @GetMapping("/dictionary/code/{code}")
    public Dictionary getByCode(@PathVariable String code) {
        return dictionaryService.getByCode(code);
    }
}

package com.hyu.dynamic.controller;

import com.hyu.dynamic.dao.TestDao;
import com.hyu.dynamic.entity.TestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("h2")
public class H2Controller {

    @Autowired
    private TestDao testDao;

    @RequestMapping("/test")
    public String jpaTest() {
        return  testDao.findAll().toString();
    }

    @RequestMapping("/findBy")
    public Page<TestEntity> findBy(@RequestParam Map<String, Object> param, @PageableDefault(value = 15, sort = { "t.createTime" }, direction = Sort.Direction.DESC) Pageable pageable) {

        testDao.save(new TestEntity(1,"我是测试1",new Date()));
        testDao.save(new TestEntity(2,"我是小2",new Date()));
        testDao.flush();
        return  testDao.findByMap(param,pageable);
    }
}
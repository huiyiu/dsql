package com.hyu.dynamic.controller;

import com.hyu.dynamic.dao.TestDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("h2")
public class H2Controller {

    @Autowired
    private TestDao testDao;

    @RequestMapping("/test")
    public String jpaTest() {

        return  testDao.findAll().toString();
    }
}
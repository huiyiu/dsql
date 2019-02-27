package com.hyu.dynamic.dao.impl;

import com.hyu.dynamic.dao.core.CommonDaoImpl;
import com.hyu.dynamic.entity.TestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public class TestDaoImpl extends CommonDaoImpl<TestEntity,Long> {

    public Page<TestEntity> findByMap(Map<String, Object> param, Pageable pageable){
        String hql = "select t from TestEntity t where 1=1 " +
                " /~ and t.id='[id]'   ~/" +
                " /~ and t.content like '%[content]%'  ~/" ;
        return query(hql,param,pageable);
    }
}

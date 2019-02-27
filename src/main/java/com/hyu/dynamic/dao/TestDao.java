package com.hyu.dynamic.dao;


import com.hyu.dynamic.dao.core.CommonDao;
import com.hyu.dynamic.entity.TestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface TestDao extends CommonDao<TestEntity,Long> {

    Page<TestEntity> findByMap(Map<String, Object> param, Pageable pageable);

}

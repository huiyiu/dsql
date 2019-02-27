package com.hyu.dynamic.dao.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;

import com.hyu.dynamic.dao.constant.EnumCollect;
import com.hyu.dynamic.dao.constant.EnumQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface BaseDao<T, K extends Serializable> {
	 Class<T> getEntityActualType();

	 EntityManager getEntityManager();

	 K saveEntity(T paramT);

	 void persist(T paramT);

	 T find(K paramK);

	default  <E> List<E> find(String queryString, Object parameter, Pageable pageable)
  {
    return find(queryString, parameter, pageable, false);
  }

	 <E> List<E> find(String paramString, Object paramObject, Pageable paramPageable,
			boolean paramBoolean);

	 <E> List<E> find(String paramString, Object paramObject, Sort paramSort, int paramInt1,
			int paramInt2);

	default <E> List<E> findByJpa(String queryString, Object parameter, Pageable pageable)
  {
    return findByJpa(queryString, parameter, pageable, false);
  }

	 <E> List<E> findByJpa(String paramString, Object paramObject, Pageable paramPageable,
			boolean paramBoolean);

	 <E> List<E> findByJpa(String paramString, Object paramObject, Sort paramSort, int paramInt1,
			int paramInt2);

	 List<T> findAll();

	 void update(T paramT);

	 int update(String paramString, Map<String, Object> paramMap);

	 void saveOrUpdate(T paramT);

	 T merge(T paramT);

	 void delete(T paramT);

	default T findByProperty(String propertyName, Object value)
  {
    return (T)findByProperty(propertyName, value, null);
  }

	 T findByProperty(String paramString, Object paramObject, Sort paramSort);

	default Long count(Map<String, Object> eqMap, Map<String, String> likeMap)
  {
    return count(eqMap, likeMap, -1);
  }

	default Long count(Map<String, Object> eqMap, Map<String, String> likeMap, int maxResult)
  {
    Map<EnumQuery, Map<String, Object>> queryMap = new EnumMap(EnumQuery.class);
    queryMap.put(EnumQuery.EQ, eqMap);
    return count(queryMap, likeMap, null, maxResult);
  }

	 Long count(Map<EnumQuery, Map<String, Object>> paramMap, Map<String, String> paramMap1,
							   Map<EnumCollect, Map<String, Collection<?>>> paramMap2, int paramInt);

	default List<T> query(Map<String, Object> eqMap, Map<String, String> likeMap, Sort sort)
  {
    Map<EnumQuery, Map<String, Object>> queryMap = new EnumMap(EnumQuery.class);
    queryMap.put(EnumQuery.EQ, eqMap);
    return query(queryMap, likeMap, null, sort, -1, -1);
  }

	default List<T> query(Map<EnumQuery, Map<String, Object>> queryMap, Map<String, String> likeMap, Sort sort, int maxResult)
  {
    return query(queryMap, likeMap, null, sort, -1, maxResult);
  }

	default List<T> query(Map<EnumQuery, Map<String, Object>> queryMap, Map<String, String> likeMap, Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, Sort sort)
  {
    return query(queryMap, likeMap, inQueryMap, sort, -1, -1);
  }

	 List<T> query(Map<EnumQuery, Map<String, Object>> paramMap, Map<String, String> paramMap1,
			Map<EnumCollect, Map<String, Collection<?>>> paramMap2, Sort paramSort, int paramInt1, int paramInt2);

	 List<T> query(String paramString, Map<String, Object> paramMap);

	 <E> Page<E> query(String paramString, Object paramObject, Pageable paramPageable);

	 <E> Page<E> queryByJpa(String paramString, Object paramObject, Pageable paramPageable);

	default T findOne(Map<String, Object> eqMap, Map<String, String> likeMap, Sort sort)
  {
    Map<EnumQuery, Map<String, Object>> queryMap = new EnumMap(EnumQuery.class);
    queryMap.put(EnumQuery.EQ, eqMap);
    return (T)findOne(queryMap, likeMap, null, sort, -1);
  }

	default T findOne(Map<EnumQuery, Map<String, Object>> queryMap, Map<String, String> likeMap, Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, Sort sort)
  {
    return (T)findOne(queryMap, likeMap, inQueryMap, sort, -1);
  }

	 T findOne(Map<EnumQuery, Map<String, Object>> paramMap, Map<String, String> paramMap1,
			Map<EnumCollect, Map<String, Collection<?>>> paramMap2, Sort paramSort, int paramInt);

	 <E> E findOne(String paramString, Object paramObject, Pageable paramPageable);

	 <E> E findOne(String paramString, Object paramObject, Sort paramSort, int paramInt);

	default <E> E findOne(String queryString, Object parameter, Sort sort)
  {
    return (E)findOne(queryString, parameter, sort, 0);
  }

	 T findOne(String paramString, Map<String, Object> paramMap);

	 <E> E findOneByJpa(String paramString, Object paramObject, Pageable paramPageable);

	 <E> E findOneByJpa(String paramString, Object paramObject, Sort paramSort, int paramInt);

	default <E> E findOneByJpa(String queryString, Object parameter, Sort sort)
  {
    return (E)findOneByJpa(queryString, parameter, sort, 0);
  }
}
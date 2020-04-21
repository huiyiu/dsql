package com.hyu.dynamic.dao.core;

import java.io.*;

import com.hyu.dynamic.dao.constant.EnumCollect;
import com.hyu.dynamic.dao.constant.EnumQuery;
import com.hyu.dynamic.dao.utils.SqlRemoveUtils;
import com.mool.xsqlbuilder.SafeSqlProcesser;
import com.mool.xsqlbuilder.SafeSqlProcesserFactory;
import com.mool.xsqlbuilder.XsqlBuilder;
import com.mool.xsqlbuilder.safesql.DirectReturnSafeSqlProcesser;
import org.hibernate.dialect.*;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.*;
import javax.persistence.*;
import org.slf4j.*;
import java.lang.reflect.*;
import org.springframework.data.domain.*;
import org.apache.commons.lang3.*;
import org.hibernate.engine.spi.*;
import org.hibernate.engine.jdbc.spi.*;
import java.util.*;
import org.apache.commons.collections4.*;

import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import javax.persistence.criteria.Predicate;

import org.hibernate.*;
import java.util.concurrent.*;

public class BaseDaoImpl<T, K extends Serializable> implements BaseDao<T, K>
{
	public static final String SORT_COLUMNS = "sortColumns";
	static final String INVALID_FIRSTRESULT = "invalid firstResult {}";
	private static Map<Dialect, SafeSqlProcesser> cacheDialectMapping;
	protected Logger logger;
	private Class<T> actualType;
	@Autowired(required = false)
	private SessionFactory sessionFactory;
	private EntityManager entityManager;

	@PersistenceContext
	public void setEntityManager(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public BaseDaoImpl() {
		this.logger = LoggerFactory.getLogger((Class)this.getClass());
		final ParameterizedType genericSuperClass = (ParameterizedType)this.getClass().getGenericSuperclass();
		this.actualType = (Class<T>)genericSuperClass.getActualTypeArguments()[0];
	}

	@Override
	public Class<T> getEntityActualType() {
		return this.actualType;
	}

	@Override
	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	@Override
	public K saveEntity(final T object) {
		return (K)this.getSession().save((Object)object);
	}

	@Override
	public void persist(final T object) {
		this.getSession().persist((Object)object);
	}

	@Override
	public T find(final K primaryKey) {
		return (T)this.getSession().get((Class)this.actualType, (Serializable)primaryKey);
	}

	@Override
	public <E> List<E> find(final String queryString, final Object parameter, final Pageable pageable, final boolean limitSize) {
		final Query<E> query = this.createQuery(queryString, parameter, pageable, limitSize);
		return (List<E>)query.list();
	}

	@Override
	public <E> List<E> find(final String queryString, final Object parameter, final Sort sort, final int startPosition, final int maxResult) {
		final Query<E> query = this.createQuery(queryString, parameter, sort, startPosition, maxResult);
		return (List<E>)query.list();
	}

	@Override
	public List<T> findAll() {
		final String findAllQueryStr = "from " + this.actualType.getName();
		final Query<T> findAllQuery = (Query<T>)this.getSession().createQuery(findAllQueryStr);
		return (List<T>)findAllQuery.list();
	}

	@Override
	public int update(final String queryString, final Map<String, Object> parameter) {
		final Query<T> query = (Query<T>)this.getSession().createQuery(queryString, (Class)this.actualType);
		if (parameter != null) {
			this.setQueryParameters(query, parameter);
		}
		return query.executeUpdate();
	}

	@Override
	public void update(final T object) {
		this.getSession().update((Object)object);
	}

	@Override
	public void saveOrUpdate(final T object) {
		this.getSession().saveOrUpdate((Object)object);
	}

	@Override
	public T merge(final T object) {
		return (T)this.getSession().merge((Object)object);
	}

	@Override
	public void delete(final T object) {
		this.getSession().delete((Object)object);
	}

	@Override
	public List<T> query(final String queryString, final Map<String, Object> parameter) {
		final Query<T> query = (Query<T>)this.getSession().createQuery(queryString, (Class)this.actualType);
		if (parameter != null) {
			this.setQueryParameters(query, parameter);
		}
		return (List<T>)query.getResultList();
	}

	@Override
	public List<T> query(final Map<EnumQuery, Map<String, Object>> queryMap, final Map<String, String> likeMap, final Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, final Sort sort, final int startPosition, final int maxResult) {
		final Query<T> query = this.createQuery(queryMap, likeMap, inQueryMap, sort);
		if (startPosition >= 0) {
			query.setFirstResult(startPosition);
		}
		if (maxResult > 0) {
			query.setMaxResults(maxResult);
		}
		return (List<T>)query.list();
	}

	@Override
	public <E> Page<E> query(final String queryString, final Object parameter, final Pageable pageable) {
		final Map<String, Object> map = new HashMap<>();
		final Sort sort = (pageable == null) ? null : pageable.getSort();
		if (sort != null) {
			final String orderString = sort.toString().replaceAll(":", " ");
			map.put("sortColumns", orderString);
		}
		final XsqlBuilder builder = this.getXsqlBuilder();
		final XsqlBuilder.XsqlFilterResult queryXsqlResult = builder.generateHql(queryString, (Map)map, parameter);
		final String countQueryString = "select count(1) " + SqlRemoveUtils.removeSelect(SqlRemoveUtils.removeFetchKeyword(queryString));
		final XsqlBuilder.XsqlFilterResult countQueryXsqlResult = builder.generateHql(countQueryString, parameter);
		final Query<E> query = this.setQueryParameters(this.getSession().createQuery(queryXsqlResult.getXsql()), (Map<String, Object>)queryXsqlResult.getAcceptedFilters());
		final Query<Long> countQuery = this.setQueryParameters(this.getSession().createQuery(SqlRemoveUtils.removeOrders(countQueryXsqlResult.getXsql())), countQueryXsqlResult.getAcceptedFilters());
		if (pageable != null) {
			final long firstRow = pageable.getOffset();
			final int maxResults = pageable.getPageSize();
			if (firstRow > 2147483647L || firstRow < 0L) {
				this.logger.warn("invalid firstResult {}", (Object)firstRow);
			}
			query.setFirstResult((int)firstRow).setMaxResults(maxResults);
		}
		final long total = (long)countQuery.uniqueResult();
		final List<E> content = (List<E>)query.list();
		return (Page<E>)new PageImpl((List)content, pageable, total);
	}

	@Override
	public Long count(final Map<EnumQuery, Map<String, Object>> queryMap, final Map<String, String> likeMap, final Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, final int maxResult) {
		final Query<Long> query = this.createCountQuery(queryMap, likeMap, inQueryMap);
		if (maxResult > 0) {
			query.setMaxResults(maxResult);
		}
		return (Long)query.getSingleResult();
	}

	@Override
	public <E> E findOne(final String queryString, final Object parameter, final Pageable pageable) {
		final Query<E> query = this.createQuery(queryString, parameter, pageable, false);
		query.setMaxResults(1);
		return (E)query.uniqueResultOptional().orElse(null);
	}

	@Override
	public <E> E findOne(final String queryString, final Object parameter, final Sort sort, final int startPosition) {
		final Query<E> query = this.createQuery(queryString, parameter, sort, startPosition, 1);
		query.setMaxResults(1);
		return (E)query.uniqueResultOptional().orElse(null);
	}

	@Override
	public T findOne(final Map<EnumQuery, Map<String, Object>> queryMap, final Map<String, String> likeMap, final Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, final Sort sort, final int startPosition) {
		final Query<T> query = this.createQuery(queryMap, likeMap, inQueryMap, sort);
		if (startPosition > 0) {
			query.setFirstResult(startPosition);
		}
		query.setMaxResults(1);
		return (T)query.uniqueResultOptional().orElse(null);
	}

	@Override
	public T findOne(final String queryString, final Map<String, Object> parameter) {
		final Query<T> query = (Query<T>)this.getSession().createQuery(queryString, (Class)this.actualType);
		if (parameter != null) {
			this.setQueryParameters(query, parameter);
		}
		query.setMaxResults(1);
		return (T)query.uniqueResultOptional().orElse(null);
	}

	@Override
	public T findByProperty(final String propertyName, final Object value, final Sort sort) {
		final CriteriaBuilder builder = this.getSession().getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>)builder.createQuery((Class)this.actualType);
		final Root<T> root = (Root<T>)criteriaQuery.from((Class)this.actualType);
		criteriaQuery = (CriteriaQuery<T>)criteriaQuery.select((Selection)root);
		if (propertyName != null) {
			if (value != null) {
				criteriaQuery.where((Expression)builder.equal((Expression)root.get(propertyName), value));
			}
			else {
				criteriaQuery.where((Expression)builder.isNull((Expression)root.get(propertyName)));
			}
		}
		final List<Order> orders = this.initSortRestrictions(builder, root, sort);
		if (!orders.isEmpty()) {
			criteriaQuery.orderBy((List)orders);
		}
		final Query<T> query = (Query<T>)this.getSession().createQuery((CriteriaQuery)criteriaQuery);
		query.setMaxResults(1);
		return (T)query.uniqueResultOptional().orElse(null);
	}

	public Query<T> setQueryParameters(final Query<T> query, final Map<String, Object> parameter) {
		for (final Map.Entry<String, Object> entry : parameter.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			this.setParameter(query, key, value);
		}
		return query;
	}

	private void setParameter(final Query<T> query, final String key, final Object value) {
		if (StringUtils.isEmpty((CharSequence)key)) {
			return;
		}
		if (value instanceof Collection) {
			final Collection<?> v = (Collection<?>)value;
			if (!v.isEmpty()) {
				query.setParameterList(key, (Collection)v);
			}
		}
		else if (value instanceof Object[]) {
			final Object[] v2 = (Object[])value;
			if (v2.length > 0) {
				query.setParameterList(key, v2);
			}
		}
		else {
			query.setParameter(key, value);
		}
	}

	public static SafeSqlProcesser getFromCacheByHibernateDialect(final Dialect arg) {
		SafeSqlProcesser arg2 = BaseDaoImpl.cacheDialectMapping.get(arg);
		if (arg2 == null) {
			arg2 = getByHibernateDialect(arg);
			BaseDaoImpl.cacheDialectMapping.put(arg, arg2);
		}
		return arg2;
	}

	public static SafeSqlProcesser getByHibernateDialect(final Dialect paramDialect) {
		SafeSqlProcesser localObject = null;
		final String str = paramDialect.getClass().getSimpleName();
		if (str.indexOf("MySQL") >= 0) {
			localObject = SafeSqlProcesserFactory.getMysql();
		}
		else if (str.indexOf("Oracle") >= 0) {
			localObject = SafeSqlProcesserFactory.getOracle();
		}
		else if (str.indexOf("DB2") >= 0) {
			localObject = SafeSqlProcesserFactory.getDB2();
		}
		else if (str.indexOf("Postgre") >= 0) {
			localObject = SafeSqlProcesserFactory.getPostgreSql();
		}
		else if (str.indexOf("Sybase") >= 0) {
			localObject = SafeSqlProcesserFactory.getSybase();
		}
		else if (str.indexOf("SQLServer") >= 0) {
			localObject = SafeSqlProcesserFactory.getMsSqlServer();
		}
		else {
			localObject = (SafeSqlProcesser)new DirectReturnSafeSqlProcesser();
		}
		return localObject;
	}

	protected XsqlBuilder getXsqlBuilder() {
		SessionFactoryImplementor sf = null;
		if (this.sessionFactory != null) {
			sf = (SessionFactoryImplementor)this.sessionFactory;
		}
		else {
			sf = (SessionFactoryImplementor)this.entityManager.getEntityManagerFactory().unwrap((Class)SessionFactoryImplementor.class);
		}
		final JdbcServices services = sf.getJdbcServices();
		final Dialect dialect = services.getDialect();
		final SafeSqlProcesser safeSqlProcesser = getFromCacheByHibernateDialect(dialect);
		final XsqlBuilder builder = new XsqlBuilder(safeSqlProcesser);
		if (builder.getSafeSqlProcesser().getClass() == DirectReturnSafeSqlProcesser.class) {
			this.logger.warn("{}", (Object)"getXsqlBuilder(): Sql security filtering is not enabled.");
		}
		return builder;
	}

	private List<Predicate> initPredicate(final CriteriaBuilder builder, final Root<T> root, final Map<EnumQuery, Map<String, Object>> queryMap, final Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, final Map<String, String> likeMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)queryMap)) {
			queryMap.forEach((k, v) -> {
				switch (k) {
					case EQ: {
						list.addAll(this.initEqRestrictions(builder, root, v));
						break;
					}
					case NOTEQ: {
						list.addAll(this.initNotEqRestrictions(builder, root, v));
						break;
					}
					case GT: {
						list.addAll(this.initGtRestrictions(builder, root, v));
						break;
					}
					case LT: {
						list.addAll(this.initLtRestrictions(builder, root, v));
						break;
					}
				}
			});
		}
		if (MapUtils.isNotEmpty((Map)inQueryMap)) {
			inQueryMap.forEach((k, v) -> {
				switch (k) {
					case IN: {
						list.addAll(this.initInRestrictions(root, v));
						break;
					}
					case NOTIN: {
						list.addAll(this.initNotInRestrictions(root, v));
						break;
					}
				}
			});
		}
		list.addAll(this.initLikeRestrictions(builder, root, likeMap));
		return list;
	}

	private Query<T> createQuery(final Map<EnumQuery, Map<String, Object>> queryMap, final Map<String, String> likeMap, final Map<EnumCollect, Map<String, Collection<?>>> inQueryMap, final Sort sort) {
		final CriteriaBuilder builder = this.getSession().getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>)builder.createQuery((Class)this.actualType);
		final Root<T> root = (Root<T>)criteriaQuery.from((Class)this.actualType);
		criteriaQuery = (CriteriaQuery<T>)criteriaQuery.select((Selection)root);
		final List<Predicate> list = this.initPredicate(builder, root, queryMap, inQueryMap, likeMap);
		final List<Order> orders = this.initSortRestrictions(builder, root, sort);
		if (!list.isEmpty()) {
			final Predicate[] restrictions = new Predicate[list.size()];
			list.toArray(restrictions);
			criteriaQuery.where(restrictions);
		}
		if (!orders.isEmpty()) {
			criteriaQuery.orderBy((List)orders);
		}
		return (Query<T>)this.getSession().createQuery((CriteriaQuery)criteriaQuery);
	}

	private <E> Query<E> createQuery(final String queryString, final Object parameter, final Pageable pageable, final boolean limitSize) {
		final Sort sort = (pageable == null) ? null : pageable.getSort();
		int startPosition = 0;
		int maxResult = -1;
		if (pageable != null) {
			final long firstRow = pageable.getOffset();
			if (firstRow > 2147483647L || firstRow < 0L) {
				this.logger.warn("invalid firstResult {}", (Object)firstRow);
			}
			startPosition = (int)firstRow;
			if (limitSize) {
				maxResult = pageable.getPageSize();
			}
		}
		return this.createQuery(queryString, parameter, sort, startPosition, maxResult);
	}

	private <E> Query<E> createQuery(final String queryString, final Object parameter, final Sort sort, final int startPosition, final int maxResult) {
		final Map<String, Object> map = new HashMap<>();
		if (sort != null) {
			final String orderString = sort.toString().replaceAll(":", " ");
			map.put("sortColumns", orderString);
		}
		final XsqlBuilder builder = this.getXsqlBuilder();
		final XsqlBuilder.XsqlFilterResult queryXsqlResult = builder.generateHql(queryString, (Map)map, parameter);
		final Query<E> query = this.setQueryParameters(this.getSession().createQuery(queryXsqlResult.getXsql()), (Map<String, Object>)queryXsqlResult.getAcceptedFilters());
		if (startPosition >= 0) {
			query.setFirstResult(startPosition);
		}
		if (maxResult > 0) {
			query.setMaxResults(maxResult);
		}
		return query;
	}

	private Collection<? extends Predicate> initLtRestrictions(final CriteriaBuilder builder, final Root<T> root, final Map<String, Object> ltMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)ltMap)) {
			for (final Map.Entry<String, Object> entry : ltMap.entrySet()) {
				final String key = entry.getKey();
				final Object value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key)) {
					final Path<Comparable> path = this.getPath(key, root);
					if (value == null) {
						continue;
					}
					list.add(builder.lessThan((Expression)path, (Comparable)value));
				}
			}
		}
		return list;
	}

	private Collection<? extends Predicate> initGtRestrictions(final CriteriaBuilder builder, final Root<T> root, final Map<String, Object> gtMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)gtMap)) {
			for (final Map.Entry<String, Object> entry : gtMap.entrySet()) {
				final String key = entry.getKey();
				final Object value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key)) {
					final Path<Comparable> path = this.getPath(key, root);
					if (value == null) {
						continue;
					}
					list.add(builder.greaterThan((Expression)path, (Comparable)value));
				}
			}
		}
		return list;
	}

	private Query<Long> createCountQuery(final Map<EnumQuery, Map<String, Object>> queryMap, final Map<String, String> likeMap, final Map<EnumCollect, Map<String, Collection<?>>> inQueryMap) {
		final CriteriaBuilder builder = this.getSession().getCriteriaBuilder();
		final CriteriaQuery<Long> criteriaQuery = (CriteriaQuery<Long>)builder.createQuery((Class)Long.class);
		final Root<T> root = (Root<T>)criteriaQuery.from((Class)this.actualType);
		criteriaQuery.select((Selection)builder.count((Expression)root));
		final List<Predicate> list = this.initPredicate(builder, root, queryMap, inQueryMap, likeMap);
		if (!list.isEmpty()) {
			final Predicate[] restrictions = new Predicate[list.size()];
			list.toArray(restrictions);
			criteriaQuery.where(restrictions);
		}
		return (Query<Long>)this.getSession().createQuery((CriteriaQuery)criteriaQuery);
	}

	private Collection<? extends Predicate> initNotInRestrictions(final Root<T> root, final Map<String, Collection<?>> notInMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)notInMap)) {
			for (final Map.Entry<String, Collection<?>> entry : notInMap.entrySet()) {
				final String key = entry.getKey();
				final Collection<?> value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key)) {
					final Path<Object> path = this.getPath(key, root);
					if (value == null || value.isEmpty() || path == null) {
						continue;
					}
					list.add(path.in((Collection)value).not());
				}
			}
		}
		return list;
	}

	private Collection<? extends Predicate> initInRestrictions(final Root<T> root, final Map<String, Collection<?>> inMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)inMap)) {
			for (final Map.Entry<String, Collection<?>> entry : inMap.entrySet()) {
				final String key = entry.getKey();
				final Collection<?> value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key)) {
					final Path<Object> path = this.getPath(key, root);
					if (value == null || value.isEmpty() || path == null) {
						continue;
					}
					list.add(path.in((Collection)value));
				}
			}
		}
		return list;
	}

	private <Y> Path<Y> getPath(final String key, final Root<T> root) {
		final String[] arr = key.split("\\.");
		Path<Y> path = null;
		for (final String k : arr) {
			if (path == null) {
				path = (Path<Y>)root.get(k);
			}
			else {
				path = (Path<Y>)path.get(k);
			}
		}
		return path;
	}

	private List<Predicate> initEqRestrictions(final CriteriaBuilder builder, final Root<T> root, final Map<String, Object> eqMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)eqMap)) {
			for (final Map.Entry<String, Object> entry : eqMap.entrySet()) {
				final String key = entry.getKey();
				final Object value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key)) {
					final Path<Object> path = this.getPath(key, root);
					if (value != null) {
						list.add(builder.equal((Expression)path, value));
					}
					else {
						list.add(builder.isNull((Expression)path));
					}
				}
			}
		}
		return list;
	}

	private List<Predicate> initLikeRestrictions(final CriteriaBuilder builder, final Root<T> root, final Map<String, String> likeMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)likeMap)) {
			for (final Map.Entry<String, String> entry : likeMap.entrySet()) {
				final String key = entry.getKey();
				final String value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key) && value != null) {
					final Path<String> path = this.getPath(key, root);
					list.add(builder.like((Expression)path, value));
				}
			}
		}
		return list;
	}

	private List<Order> initSortRestrictions(final CriteriaBuilder builder, final Root<T> root, final Sort sort) {
		final List<Order> list = new ArrayList<Order>();
		if (sort != null) {
			sort.forEach(s -> {
				final Path<String> path = this.getPath(s.getProperty(), root);
				if (s.getDirection() == Sort.Direction.DESC) {
					list.add(builder.desc((Expression)path));
				}
				else {
					list.add(builder.asc((Expression)path));
				}
			});
		}
		return list;
	}

	private List<Predicate> initNotEqRestrictions(final CriteriaBuilder builder, final Root<T> root, final Map<String, Object> notEqMap) {
		final List<Predicate> list = new ArrayList<Predicate>();
		if (MapUtils.isNotEmpty((Map)notEqMap)) {
			for (final Map.Entry<String, Object> entry : notEqMap.entrySet()) {
				final String key = entry.getKey();
				final Object value = entry.getValue();
				if (StringUtils.isNotEmpty((CharSequence)key)) {
					final Path<Object> path = this.getPath(key, root);
					if (value != null) {
						list.add(builder.notEqual((Expression)path, value));
					}
					else {
						list.add(builder.isNotNull((Expression)path));
					}
				}
			}
		}
		return list;
	}

	public Session getSession() {
		Session session = null;
		if (this.sessionFactory != null) {
			session = this.sessionFactory.getCurrentSession();
		}
		else {
			session = (Session)this.entityManager.unwrap((Class)Session.class);
		}
		return session;
	}

	@Override
	public <E> E findOneByJpa(final String queryString, final Object parameter, final Pageable pageable) {
		final javax.persistence.Query query = this.createJpaQuery(queryString, parameter, pageable, false);
		query.setMaxResults(1);
		final List<E> list = (List<E>)query.getResultList();
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	@Override
	public <E> E findOneByJpa(final String queryString, final Object parameter, final Sort sort, final int index) {
		final javax.persistence.Query query = this.createJpaQuery(queryString, parameter, sort, index, 1);
		query.setMaxResults(1);
		final List<E> list = (List<E>)query.getResultList();
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	private javax.persistence.Query createJpaQuery(final String queryString, final Object parameter, final Pageable pageable, final boolean limitSize) {
		final Sort sort = (pageable == null) ? null : pageable.getSort();
		int startPosition = 0;
		int maxResult = -1;
		if (pageable != null) {
			final long firstRow = pageable.getOffset();
			if (firstRow > 2147483647L || firstRow < 0L) {
				this.logger.warn("invalid firstResult {}", (Object)firstRow);
			}
			startPosition = (int)firstRow;
			if (limitSize) {
				maxResult = pageable.getPageSize();
			}
		}
		return this.createJpaQuery(queryString, parameter, sort, startPosition, maxResult);
	}

	private javax.persistence.Query createJpaQuery(final String queryString, final Object parameter, final Sort sort, final int startPosition, final int maxResult) {
		final Map<String, Object> map = new HashMap<String, Object>();
		if (sort != null) {
			final String orderString = sort.toString().replaceAll(":", " ");
			map.put("sortColumns", orderString);
		}
		final XsqlBuilder builder = this.getXsqlBuilder();
		final XsqlBuilder.XsqlFilterResult queryXsqlResult = builder.generateHql(queryString, (Map)map, parameter);
		final String xsqlStr = queryXsqlResult.getXsql();
		final EntityManager em = this.getEntityManager();
		javax.persistence.Query query = em.createQuery(xsqlStr);
		query = this.setJpaQueryParameters(query, queryXsqlResult.getAcceptedFilters());
		if (startPosition >= 0) {
			query.setFirstResult(startPosition);
		}
		if (maxResult > 0) {
			query.setMaxResults(maxResult);
		}
		return query;
	}

	public javax.persistence.Query setJpaQueryParameters(final javax.persistence.Query query, final Map<String, Object> parameter) {
		for (final Map.Entry<String, Object> entry : parameter.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			this.setJpaParameter(query, key, value);
		}
		return query;
	}

	private void setJpaParameter(final javax.persistence.Query query, final String key, final Object value) {
		if (StringUtils.isEmpty((CharSequence)key)) {
			return;
		}
		query.setParameter(key, value);
	}

	@Override
	public <E> List<E> findByJpa(final String queryString, final Object parameter, final Pageable pageable, final boolean limitSize) {
		final javax.persistence.Query query = this.createJpaQuery(queryString, parameter, pageable, limitSize);
		return (List<E>)query.getResultList();
	}

	@Override
	public <E> List<E> findByJpa(final String queryString, final Object parameter, final Sort sort, final int startPosition, final int maxResult) {
		final javax.persistence.Query query = this.createJpaQuery(queryString, parameter, sort, startPosition, maxResult);
		return (List<E>)query.getResultList();
	}

	@Override
	public <E> Page<E> queryByJpa(final String queryString, final Object parameter, final Pageable pageable) {
		final Map<String, Object> map = new HashMap<String, Object>();
		final Sort sort = (pageable == null) ? null : pageable.getSort();
		if (sort != null) {
			final String orderString = sort.toString().replaceAll(":", " ");
			map.put("sortColumns", orderString);
		}
		final XsqlBuilder builder = this.getXsqlBuilder();
		final XsqlBuilder.XsqlFilterResult queryXsqlResult = builder.generateHql(queryString, (Map)map, parameter);
		final String countQueryString = "select count(1) " + SqlRemoveUtils.removeSelect(SqlRemoveUtils.removeFetchKeyword(queryString));
		final XsqlBuilder.XsqlFilterResult countQueryXsqlResult = builder.generateHql(countQueryString, parameter);
		final EntityManager em = this.getEntityManager();
		javax.persistence.Query query = em.createQuery(queryXsqlResult.getXsql());
		query = this.setJpaQueryParameters(query, queryXsqlResult.getAcceptedFilters());
		javax.persistence.Query countQuery = em.createQuery(SqlRemoveUtils.removeOrders(countQueryXsqlResult.getXsql()));
		countQuery = this.setJpaQueryParameters(countQuery, countQueryXsqlResult.getAcceptedFilters());
		if (pageable != null) {
			final long firstRow = pageable.getOffset();
			final int maxResults = pageable.getPageSize();
			if (firstRow > 2147483647L || firstRow < 0L) {
				this.logger.warn("invalid firstResult {}", (Object)firstRow);
			}
			query.setFirstResult((int)firstRow).setMaxResults(maxResults);
		}
		final Number number = (Number)countQuery.getSingleResult();
		final long total = number.longValue();
		final List<E> content = (List<E>)query.getResultList();
		return (Page<E>)new PageImpl((List)content, pageable, total);
	}

	static {
		BaseDaoImpl.cacheDialectMapping = new ConcurrentHashMap<Dialect, SafeSqlProcesser>();
	}
}

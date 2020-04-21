package com.hyu.dynamic.dao.core;

import com.mool.xsqlbuilder.XsqlBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.query.Query;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.Transformers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class CommonDaoImpl<T, K extends Serializable> extends BaseDaoImpl<T, K> implements BaseDao<T, K>{
    private static final  String TMP =  " ) tmp ";
    private static final  String UNION_ALL =  " union all ";

    /**
     * 设置分页参数
     *
     * @param pageable
     * @param query
     */
    protected void setPageable(Pageable pageable, Query<?> query) {
        if (pageable != null) {
            long firstRow = pageable.getOffset();
            int maxResults = pageable.getPageSize();

            if (firstRow > Integer.MAX_VALUE || firstRow < 0) {
                logger.warn("invalid firstResult {}", firstRow);
            }
            query.setFirstResult((int) firstRow).setMaxResults(maxResults);
        }
    }

    protected String getSortStr(Pageable pageable) {
        String sortString = "";
        Sort sort = pageable.getSort();
        List<String> orderList = new ArrayList<>();
        sort.forEach(order -> orderList.add(order.getProperty() + " " + order.getDirection()));
        if (CollectionUtils.isNotEmpty(orderList)) {
            sortString = " order by " + String.join(",", orderList);
        }
        return sortString;
    }

    public String makeQuery(String queryString, Map<String, Object> param, Class<?> cls) {
        XsqlBuilder.XsqlFilterResult result = new XsqlBuilder().generateHql(queryString,param);
        return result.getXsql();
    }

    protected String getSql(String  hqlQueryString,Map<String, Object> param) {
        String hql = new XsqlBuilder().generateSql(hqlQueryString, param).getXsql();
        QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();
        SessionFactoryImplementor factory = (SessionFactoryImplementor) getSession().getSessionFactory();
        QueryTranslator translator = translatorFactory.
                createQueryTranslator(hql, hql, Collections.emptyMap(), factory, null);
        translator.compile(Collections.emptyMap(), true);
        String sql =  translator.getSQLString();
        String[][] columnNames = translator.getColumnNames();

        String[] aliase = translator.getReturnAliases();
        for(int i=0;i<columnNames.length;i++ ) {
            sql= sql.replaceAll(columnNames[i][0], aliase[i]);
        }
        return sql;
    }

    @SuppressWarnings("unchecked")
    protected Page<Map<String, Object>> unionQuery(String queryStr, String queryStrHis, Map<String, Object> param, Pageable pageable){
        String sql = getSql(queryStr,param);
        String sqlHis = getSql(queryStrHis,param);
        String unionSql = "select * from (" + sql + UNION_ALL + sqlHis + TMP + getSortStr(pageable);
        String countSql = "select count(1) from (" + sql + UNION_ALL + sqlHis + TMP;
        Query<?> query = getSession().createNativeQuery(unionSql);
        Query<?> countQuery = getSession().createNativeQuery(countSql);
        query.unwrap(NativeQueryImpl.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        setPageable(pageable, query);
        @SuppressWarnings("rawtypes")
        List data = query.list();
        long total = Long.valueOf(countQuery.getSingleResult().toString());
        return new PageImpl<>(data, pageable, total);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> unionQuery(String mapField,String hql,String hqlHis,Map<String, Object> param){
        String sql = getSql(hql,param);
        String sqlHis = getSql(hqlHis,param);
        String unionSql = mapField + " from (" + sql + UNION_ALL + sqlHis + TMP;
        Query<?> query = getSession().createNativeQuery(unionSql);
        query.unwrap(NativeQueryImpl.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        return (Map<String, Object>) query.uniqueResult();
    }
}
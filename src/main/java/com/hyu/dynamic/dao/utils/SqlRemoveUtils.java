package com.hyu.dynamic.dao.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.Assert;

public class SqlRemoveUtils {
	public static final String SQLREQUIRED = "sql is required";

	public static String removeSelect(String sql) {
		Assert.hasText(sql, "sql is required");
		int beginPos = getFirstSelcectMactchFrom(sql.toLowerCase());
		Assert.isTrue(beginPos != -1, " sql : " + sql + " must has a keyword 'from'");
		return sql.substring(beginPos);
	}

	private static int getFirstSelcectMactchFrom(String input) {
		List<Integer> selectPos = new ArrayList();
		List<Integer> fromPos = new ArrayList();
		String regex = "select";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		while (m.find()) {
			selectPos.add(Integer.valueOf(m.start()));
		}
		regex = "\\sfrom\\s";
		p = Pattern.compile(regex);
		m = p.matcher(input);
		while (m.find()) {
			fromPos.add(Integer.valueOf(m.start()));
		}
		Assert.isTrue(selectPos.size() == fromPos.size(), " sql : " + input + " 'select' must match 'from'");
		switch (selectPos.size()) {
			case 0 :
				return -1;
			case 1 :
				return ((Integer) fromPos.get(0)).intValue();
		}
		Integer firstSelectPos = (Integer) selectPos.get(0);
		Map<Integer, Integer> map = new HashMap();
		for (int i = 0; i < fromPos.size(); i++) {
			Integer fpos = (Integer) fromPos.get(i);
			for (int j = 0; j < selectPos.size() - 1; j++) {
				Integer spos = (Integer) selectPos.get(j);
				Integer sposnext = (Integer) selectPos.get(j + 1);
				boolean breakStatus = false;
				if ((fpos.intValue() > spos.intValue()) && (fpos.intValue() < sposnext.intValue())) {
					map.put(spos, fpos);
					selectPos.remove(spos);
					i--;
					fromPos.remove(fpos);
					breakStatus = true;
				} else if ((j + 1 == selectPos.size() - 1) && (fpos.intValue() > sposnext.intValue())) {
					map.put(sposnext, fpos);
					selectPos.remove(sposnext);
					i--;
					fromPos.remove(fpos);
					breakStatus = true;
				}
				if (breakStatus) {
					break;
				}
			}
		}
		return ((Integer) map.get(firstSelectPos)).intValue();
	}

	public static String removeOrders(String sql) {
		Assert.hasText(sql, "sql is required");
		Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*", 2);
		Matcher m = p.matcher(sql);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static String removeFetchKeyword(String sql) {
		return sql.replaceAll("(?i)fetch", "");
	}

	public static String removeXsqlBuilderOrders(String sql) {
		Assert.hasText(sql, "sql is required");
		Pattern p = Pattern.compile("/~.*order\\s*by[\\w|\\W|\\s|\\S]*~/", 2);
		Matcher m = p.matcher(sql);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);
		return removeOrders(sb.toString());
	}
}
/**
 * 
 */
package org.activejpa.entity;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activejpa.entity.Condition.Operator;

/**
 * @author ganeshs
 *
 */
public class Filter {
	
	private List<Condition> conditions = new ArrayList<Condition>();
	
	private List<SortField> sortFields = new ArrayList<SortField>();

	private Integer pageNo;
	
	private Integer perPage;
	
	private boolean cacheable;
	
	private boolean shouldPage;
	
	public Filter(int perPage, int pageNo, Condition... conditions) {
		this.pageNo = pageNo > 0 ? pageNo : 1;
		this.shouldPage = perPage > 0;
		this.perPage = (perPage < 1) ? Integer.MAX_VALUE : perPage;
		if (conditions != null && conditions.length > 0) {
			this.conditions.addAll(Arrays.asList(conditions));
		}
	}
	
	public Filter() {
		this(0, 0);
	}
	
	public Filter(Condition... conditions) {
		this(0, 0, conditions);
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	public Integer getPerPage() {
		return perPage;
	}

	public void setPerPage(Integer perPage) {
		this.perPage = perPage;
	}
	
	public Integer getStart() {
		return (pageNo - 1) * perPage;
	}
	
	public Integer getLimit() {
		return perPage;
	}
	
	public boolean shouldPage() {
		return shouldPage;
	}

	/**
	 * @return the conditions
	 */
	public List<Condition> getConditions() {
		return conditions;
	}

	/**
	 * @param conditions the conditions to set
	 */
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
	
	public void addCondition(String name, Object value) {
		addCondition(name, Operator.eq, value);
	}
	
	public void addCondition(String name, Operator operator, Object value) {
		this.conditions.add(new Condition(name, operator, value));
	}
	
	public String constructQuery() {
		StringWriter writer = new StringWriter();
		if (conditions != null || !conditions.isEmpty()) {
			for (int i = 0; i < conditions.size() - 1; i++) {
				writer.append(conditions.get(i).constructQuery()).append(" and ");
			}
			writer.append(conditions.get(conditions.size() - 1).constructQuery());
		}
		return writer.toString();
	}
	
	/**
	 * Constructs the select criteria query
	 * 
	 * @param builder
	 * @param query
	 * @param root
	 */
	public <T extends Model> void constructQuery(CriteriaBuilder builder, CriteriaQuery<?> query, Root<T> root) {
		if (conditions != null || !conditions.isEmpty()) {
			List<Predicate> predicates = new ArrayList<Predicate>();
			for (Condition condition : conditions) {
				predicates.add(condition.constructQuery(builder, root));
			}
			query.where(predicates.toArray(new Predicate[0]));
		}
		
		if (sortFields != null && !sortFields.isEmpty()) {
			List<Order> orders = new ArrayList<Order>();
			for (SortField sortField : sortFields) {
				orders.add(sortField.getOrder(builder, root));
			}
			query.orderBy(orders);
		}
	}
	
	/**
	 * Constructs the delete criteria query
	 * 
	 * @param builder
	 * @param query
	 * @param root
	 */
	public <T extends Model> void constructQuery(CriteriaBuilder builder, CriteriaDelete<?> query, Root<T> root) {
		if (conditions != null || !conditions.isEmpty()) {
			List<Predicate> predicates = new ArrayList<Predicate>();
			for (Condition condition : conditions) {
				predicates.add(condition.constructQuery(builder, root));
			}
			query.where(predicates.toArray(new Predicate[0]));
		}
	}
	
	public void setParameters(Query query) {
		if (conditions != null && !conditions.isEmpty()) {
			for (Condition condition : conditions) {
				condition.setParameters(query, condition.getValue());
			}
		}
	}
	
	public void setPage(Query query) {
		if (shouldPage()) {
			query.setFirstResult(getStart());
			query.setMaxResults(getPerPage());
		}
	}
	
	/**
	 * @return the cacheable
	 */
	public boolean isCacheable() {
		return cacheable;
	}

	/**
	 * @param cacheable the cacheable to set
	 */
	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}
	
	/**
	 * Adds the sort field
	 * 
	 * @param name
	 * @param asc
	 */
	public void addSortField(String name, boolean asc) {
		this.sortFields.add(new SortField(name, asc));
	}
	
	/**
	 * Adds the sort field
	 * 
	 * @param sortField
	 */
	public void addSortField(SortField sortField) {
		this.sortFields.add(sortField);
	}

	/**
	 * @return the sortFields
	 */
	public List<SortField> getSortFields() {
		return sortFields;
	}

	/**
	 * @param sortFields the sortFields to set
	 */
	public void setSortFields(List<SortField> sortFields) {
		this.sortFields = sortFields;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conditions == null) ? 0 : conditions.hashCode());
		result = prime * result + ((pageNo == null) ? 0 : pageNo.hashCode());
		result = prime * result + ((perPage == null) ? 0 : perPage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Filter other = (Filter) obj;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.containsAll(other.conditions))
			return false;
		if (pageNo == null) {
			if (other.pageNo != null)
				return false;
		} else if (!pageNo.equals(other.pageNo))
			return false;
		if (perPage == null) {
			if (other.perPage != null)
				return false;
		} else if (!perPage.equals(other.perPage))
			return false;
		return true;
	}
	
	public Filter clone(boolean paginate) {
		Filter filter = new Filter(conditions.toArray(new Condition[0]));
		if (paginate) {
			filter.perPage = perPage;
			filter.pageNo = pageNo;
			filter.shouldPage = shouldPage;
		}
		return filter;
	}
}

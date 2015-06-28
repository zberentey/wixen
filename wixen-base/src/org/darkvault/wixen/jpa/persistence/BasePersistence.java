package org.darkvault.wixen.jpa.persistence;

import java.util.List;

public interface BasePersistence<J, M> {

	public void clearCache();

	public List<M> findAll();

	public M findByPrimaryKey(long primaryKey);

	public void update(M model);

}
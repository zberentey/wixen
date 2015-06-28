package org.darkvault.wixen.example.service.persistence.impl;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.darkvault.wixen.example.model.Example;
import org.darkvault.wixen.example.model.jpa.JpaExample;
import org.darkvault.wixen.example.service.persistence.ExamplePersistence;
import org.darkvault.wixen.jpa.model.Finder;
import org.darkvault.wixen.jpa.persistence.BasePersistenceImpl;

@Component
public class ExamplePersistenceImpl extends BasePersistenceImpl<JpaExample, Example>
	implements ExamplePersistence {

	@Override
	@Finder(columns = {"code"})
	public Example findByCode(String code) {
		Example example = findUniqueCacheResult("findByCode", code);

		if (example != null) {
			return example;
		}

		TypedQuery<JpaExample> query = _entityManager.createQuery(
			"select e from JpaExample e where e.code = :code", JpaExample.class);

		query.setParameter("code", code);

		return (Example)runFinderQuery(query, "findByCode", code);
	}

	@Override
	protected EntityManager getEntityManager() {
		return _entityManager;
	}

	@Override
	protected Class<JpaExample> getJpaModelClass() {
		return JpaExample.class;
	}

	@ServiceDependency(filter="(osgi.unit.name=ExampleServicePU)")
	private volatile EntityManager _entityManager;

}
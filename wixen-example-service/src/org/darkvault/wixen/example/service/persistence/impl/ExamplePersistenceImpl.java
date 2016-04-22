package org.darkvault.wixen.example.service.persistence.impl;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.darkvault.wixen.example.model.Example;
import org.darkvault.wixen.example.model.jpa.JpaExample;
import org.darkvault.wixen.example.service.persistence.ExamplePersistence;
import org.darkvault.wixen.jpa.model.Finder;
import org.darkvault.wixen.jpa.model.ModelConverter;
import org.darkvault.wixen.jpa.persistence.BasePersistenceImpl;
import org.eclipse.persistence.config.CacheUsage;
import org.eclipse.persistence.config.QueryHints;

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
	public Example findByCodeJpa(String code) {
		TypedQuery<JpaExample> query = _entityManager.createQuery(
			"select e from JpaExample e where e.code = :code", JpaExample.class);

		query.setParameter("code", code);
		query.setHint("javax.persistence.cache.retrieveMode", "USE");
		query.setHint("javax.persistence.cache.storeMode", "USE");
		query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);

		for (JpaExample example : query.getResultList()) {
			return (Example)ModelConverter.toModel(example);
		}

		return null;
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
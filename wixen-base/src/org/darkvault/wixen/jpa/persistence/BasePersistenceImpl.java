package org.darkvault.wixen.jpa.persistence;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.darkvault.wixen.cache.CacheUtil;
import org.darkvault.wixen.cache.GenericCache;
import org.darkvault.wixen.jpa.model.Finder;
import org.darkvault.wixen.jpa.model.FinderDefinition;
import org.darkvault.wixen.jpa.model.ModelConverter;

public abstract class BasePersistenceImpl<J, M> implements BasePersistence<J, M> {

	public BasePersistenceImpl() {
		ModelConverter.registerJpaModel(getJpaModelClass());

		_registerFinders();
	}

	@Override
	public void clearCache() {
		EntityManager entityManager = getEntityManager();

		EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();

		Cache cache = entityManagerFactory.getCache();

		cache.evict(getJpaModelClass());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<M> findAll() {
		List<M> models = findCacheResult("findAll", new Object[] {""});

		if (models != null) {
			return models;
		}

		Class<J> jpaModelClass = getJpaModelClass();

		EntityManager entityManager = getEntityManager();

		TypedQuery<J> query = entityManager.createQuery(
			"select j from " + _getEntityName(jpaModelClass) + " j", jpaModelClass);

		Class<M> modelClass = _getModelClass();

		return (List<M>)runFinderQuery(query, "findAll" + modelClass.getSimpleName(), "");
	}

	@Override
	@SuppressWarnings("unchecked")
	public M findByPrimaryKey(long primaryKey) {
		M model = findUniqueCacheResult("", primaryKey);

		if (model != null) {
			return model;
		}

		EntityManager entityManager = getEntityManager();

		J jpaModel = entityManager.find(getJpaModelClass(), primaryKey);

		if (jpaModel == null) {
			return null;
		}

		model = (M)ModelConverter.toModel(jpaModel);

		cacheResult(model);

		return model;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update(M model) {
		EntityManager entityManager = getEntityManager();

		long primaryKey = ModelConverter.getPrimaryKey(model);

		if (primaryKey == 0) {
			J jpaModel = (J)ModelConverter.fromModel(model);

			// do an explicit insert

			entityManager.persist(jpaModel);
		}
		else {
			J jpaModel = entityManager.find(getJpaModelClass(), primaryKey);

			// we just need to set the properties, entity manager will automatically persist
			// the changes

			ModelConverter.fromModel(model, jpaModel);
		}

		_clearUniqueFindersCache(model);

		cacheResult(model);
	}

	protected void cacheResult(List<M> models) {
		for (M model : models) {
			cacheResult(model);
		}
	}

	protected void cacheResult(List<M> models, String finderName, Object... args) {
		GenericCache<List<M>> cache = CacheUtil.getGenericFinderCache(
			_getCacheName(finderName), _getModelClass());

		cache.put(_getCacheKey(args), models);
	}

	protected void cacheResult(M model) {
		long primaryKey = ModelConverter.getPrimaryKey(model);

		if (primaryKey > 0) {
			cacheResult(model, "", primaryKey);

			_updateUniqueFindersCache(model);
		}
	}

	protected void cacheResult(M model, String finderName, Object... args) {
		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderName), _getModelClass());

		cache.put(_getCacheKey(args), model);
	}

	protected M findUniqueCacheResult(String finderName, Object... args) {
		Class<M> modelClass = _getModelClass();

		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderName), modelClass);

		return cache.get(_getCacheKey(args));
	}

	protected List<M> findCacheResult(String finderName, Object... args) {
		Class<M> modelClass = _getModelClass();

		GenericCache<List<M>> cache = CacheUtil.getGenericFinderCache(
			_getCacheName(finderName), modelClass);

		return cache.get(_getCacheKey(args));
	}

	protected abstract EntityManager getEntityManager();

	protected abstract Class<J> getJpaModelClass();

	protected void removeResult(String finderName, Object... args) {
		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderName), _getModelClass());

		cache.remove(_getCacheKey(args));
	}

	@SuppressWarnings("unchecked")
	protected Object runFinderQuery(TypedQuery<J> query, String finderName, Object... args) {
		FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

		List<J> results = query.getResultList();

		List<M> models = new ArrayList<>();

		for (J jpaModel : results) {
			models.add((M)ModelConverter.toModel(jpaModel));
		}

		if ((finderDefinition != null) && finderDefinition.isUnique()) {
			if (models.isEmpty()) {
				return null;
			}

			M model = models.get(0);

			cacheResult(model);

			return model;
		}

		cacheResult(models);

		if (finderDefinition != null) {
			cacheResult(models, finderName, args);
		}

		return models;
	}

	protected J updateJpaModel(J jpaModel) {
		EntityManager entityManager = getEntityManager();

		return entityManager.merge(jpaModel);
	}

	private void _clearUniqueFindersCache(M model) {
		M originalModel = findUniqueCacheResult("", ModelConverter.getPrimaryKey(model));

		long changeMask = 0;

		try {
			changeMask = ModelConverter.getChangeMask(originalModel, model);
		}
		catch (Exception e) {
			CacheUtil.clearAllCache(model.getClass());
		}

		for (String finderName : _finderDefinitions.keySet()) {
			FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

			if (!finderDefinition.isUnique()) {
				continue;
			}

			removeResult(finderName, _getFinderArgs(model, finderDefinition));

			if ((changeMask & finderDefinition.getColumnBitmask()) != 0) {
				removeResult(finderName, _getFinderArgs(originalModel, finderDefinition));
			}
		}
	}

	private void _updateUniqueFindersCache(M model) {
		M originalModel = findUniqueCacheResult("", ModelConverter.getPrimaryKey(model));

		long changeMask = 0;

		try {
			changeMask = ModelConverter.getChangeMask(originalModel, model);
		}
		catch (Exception e) {
			CacheUtil.clearAllCache(model.getClass());
		}

		for (String finderName : _finderDefinitions.keySet()) {
			FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

			if (!finderDefinition.isUnique()) {
				continue;
			}

			if ((changeMask & finderDefinition.getColumnBitmask()) != 0) {
				cacheResult(model, finderName, _getFinderArgs(model, finderDefinition));
			}
		}
	}

	private String _getCacheKey(Object... args) {
		StringBuilder sb = new StringBuilder();

		for (Object arg : args) {
			if (sb.length() > 0) {
				sb.append("#");
			}

			if (arg == null) {
				sb.append("null");
			}
			else {
				sb.append(arg.toString());
			}
		}

		return sb.toString();
	}

	private String _getCacheName(String finderName) {
		Class<M> modelClass = _getModelClass();

		String cacheName = modelClass.getName();

		if ((finderName != null) && !finderName.isEmpty()) {
			cacheName = cacheName + "_" + finderName;
		}

		return cacheName;
	}

	private String _getEntityName(Class<J> jpaModelClass) {
		Entity entityAnnotation = jpaModelClass.getAnnotation(Entity.class);

		String name = entityAnnotation.name();

		if ((name == null) || name.isEmpty()) {
			return jpaModelClass.getSimpleName();
		}

		return name;
	}

	private Object[] _getFinderArgs(M model, FinderDefinition finderDefinition) {
		String[] columns = finderDefinition.getColumns();
		Object[] args = new Object[columns.length];

		for (int i = 0; i < columns.length; i++) {
			try {
				args[i] = ModelConverter.getAttributeValue(model, columns[i]);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return args;
	}

	@SuppressWarnings("unchecked")
	private Class<M> _getModelClass() {
		return (Class<M>)ModelConverter.getModelClass(getJpaModelClass());
	}

	private void _registerFinders() {
		Class<M> modelClass = _getModelClass();

		for (Method method : this.getClass().getDeclaredMethods()) {
			Finder finder = method.getAnnotation(Finder.class);

			if (finder != null) {
				Class<?> returnType = method.getReturnType();

				long columnBitmask = ModelConverter.getColumnBitmask(modelClass, finder.columns());

				FinderDefinition finderDefinition = new FinderDefinition(
					method.getName(), finder.columns(), columnBitmask);

				if (returnType.equals(modelClass)) {
					finderDefinition.setUnique(true);
				}

				_finderDefinitions.put(method.getName(), finderDefinition);
			}
		}
	}

	private Map<String, FinderDefinition> _finderDefinitions = new HashMap<>();

}
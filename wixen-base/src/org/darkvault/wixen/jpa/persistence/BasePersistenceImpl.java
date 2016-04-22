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

import org.apache.commons.lang3.StringUtils;
import org.darkvault.wixen.cache.CacheUtil;
import org.darkvault.wixen.cache.GenericCache;
import org.darkvault.wixen.jpa.model.Finder;
import org.darkvault.wixen.jpa.model.FinderDefinition;
import org.darkvault.wixen.jpa.model.ModelConverter;

public abstract class BasePersistenceImpl<J, M> implements BasePersistence<J, M> {

	@SuppressWarnings("unchecked")
	public BasePersistenceImpl() {
		ModelConverter.registerJpaModel(getJpaModelClass());

		_registerFinders();

		_nullModel = (M)ModelConverter.newModel(getJpaModelClass());
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

	@SuppressWarnings("unchecked")
	public void remove(M model) {
		J jpaModel = (J)ModelConverter.fromModel(model);

		EntityManager entityManager = getEntityManager();

		jpaModel = entityManager.merge(jpaModel);
		entityManager.remove(jpaModel);

		_clearFindersCacheAfterDelete(model);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update(M model) {
		EntityManager entityManager = getEntityManager();

		long primaryKey = ModelConverter.getPrimaryKey(model);

		removeResult(primaryKey);

		J jpaModel;

		if (primaryKey == 0) {
			jpaModel = (J)ModelConverter.fromModel(model);

			// do an explicit insert

			entityManager.persist(jpaModel);
			entityManager.flush();
		}
		else {
			jpaModel = entityManager.find(getJpaModelClass(), primaryKey);

			// we just need to set the properties, entity manager will automatically persist
			// the changes

			ModelConverter.fromModel(model, jpaModel);
		}

		ModelConverter.setPrimaryKeyFromJpaModel(model, jpaModel);

		_clearFindersCacheAfterUpdate(model);

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
			_updateUniqueFindersCache(model);

			cacheResult(model, "", primaryKey);
		}
	}

	protected void cacheResult(M model, String finderName, Object... args) {
		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderName), _getModelClass());

		cache.put(_getCacheKey(args), model);
	}

	protected void cacheResult(FinderDefinition finderDefinition, M model)
		throws Exception {

		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderDefinition.getName()), _getModelClass());

		if (!finderDefinition.hasLocalizedColumns()) {
			cache.put(_getCacheKey(_getFinderArgs(model, finderDefinition)), model);

			return;
		}

		String[] languages = ModelConverter.getLocalizations(model, finderDefinition.getColumns());

		for (String language : languages) {
			cache.put(_getCacheKey(_getFinderArgs(model, finderDefinition, language)), model);
		}

	}

	protected List<M> findCacheResult(String finderName, Object... args) {
		Class<M> modelClass = _getModelClass();

		GenericCache<List<M>> cache = CacheUtil.getGenericFinderCache(
			_getCacheName(finderName), modelClass);

		return cache.get(_getCacheKey(args));
	}

	protected M findUniqueCacheResult(String finderName, Object... args) {
		Class<M> modelClass = _getModelClass();

		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderName), modelClass);

		return cache.get(_getCacheKey(args));
	}

	protected abstract EntityManager getEntityManager();

	protected abstract Class<J> getJpaModelClass();

	protected boolean isNullModel(M model) {
		return (model == _nullModel);
	}

	protected void removeResult(long primaryKey) {
		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(""), _getModelClass());

		cache.remove(primaryKey);
	}

	protected void removeResult(String finderName, M model, FinderDefinition finderDefinition)
		throws Exception {

		GenericCache<M> cache = CacheUtil.getGenericCache(
			_getCacheName(finderName), _getModelClass());

		if (!finderDefinition.hasLocalizedColumns()) {
			cache.remove(_getCacheKey(_getFinderArgs(model, finderDefinition)));

			return;
		}

		String[] languages = ModelConverter.getLocalizations(model, finderDefinition.getColumns());

		for (String language : languages) {
			cache.remove(_getCacheKey(_getFinderArgs(model, finderDefinition, language)));
		}
	}

	@SuppressWarnings("unchecked")
	protected Object runFinderQuery(TypedQuery<J> query, String finderName, Object... args) {
		List<J> results = query.getResultList();

		List<M> models = new ArrayList<>();

		for (J jpaModel : results) {
			models.add((M)ModelConverter.toModel(jpaModel));
		}

		FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

		if ((finderDefinition == null) || !finderDefinition.isUnique()) {
			cacheResult(models, finderName, args);

			return models;
		}

		if (models.isEmpty()) {
			cacheResult(_nullModel, finderName, args);

			return null;
		}

		M model = models.get(0);

		cacheResult(model, finderName, args);

		return model;
	}

	protected J updateJpaModel(J jpaModel) {
		EntityManager entityManager = getEntityManager();

		return entityManager.merge(jpaModel);
	}

	private void _clearFindersCacheAfterDelete(M model) {
		long primaryKey = ModelConverter.getPrimaryKey(model);

		M originalModel = findUniqueCacheResult("", primaryKey);

		if (originalModel == null) {
			return;
		}

		removeResult(primaryKey);

		for (String finderName : _finderDefinitions.keySet()) {
			FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

			try {
				removeResult(finderName, model, finderDefinition);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void _clearFindersCacheAfterUpdate(M model) {
		M originalModel = findUniqueCacheResult("", ModelConverter.getPrimaryKey(model));

		long changeMask = 0;

		if (originalModel != null) {
			try {
				changeMask = ModelConverter.getChangeMask(originalModel, model);
			}
			catch (Exception e) {
				CacheUtil.clearAllCache(model.getClass());
			}
		}

		if ((originalModel == null) || (changeMask == 0)) {
			return;
		}

		for (String finderName : _finderDefinitions.keySet()) {
			FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

			try {
				removeResult(finderName, originalModel, finderDefinition);
				removeResult(finderName, model, finderDefinition);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void _updateUniqueFindersCache(M model) {
		M originalModel = findUniqueCacheResult("", ModelConverter.getPrimaryKey(model));

		long changeMask = 0;

		if (originalModel != null) {
			try {
				changeMask = ModelConverter.getChangeMask(originalModel, model);
			}
			catch (Exception e) {
				CacheUtil.clearAllCache(model.getClass());
			}
		}

		for (String finderName : _finderDefinitions.keySet()) {
			FinderDefinition finderDefinition = _finderDefinitions.get(finderName);

			if (changeMask > 0) {
				try {
					removeResult(finderName, originalModel, finderDefinition);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
 			}

			if (!finderDefinition.isUnique()) {
				continue;
			}

			if ((originalModel == null) || (changeMask > 0)) {
				try {
					cacheResult(finderDefinition, model);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private String _getCacheKey(Object... args) {
		return StringUtils.join(args, "#");
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
		return _getFinderArgs(model, finderDefinition, null);
	}

	private Object[] _getFinderArgs(M model, FinderDefinition finderDefinition, String language) {
		String[] columns = finderDefinition.getColumns();

		int argSize = columns.length;

		if (language != null) {
			argSize++;
		}

		Object[] args = new Object[argSize];

		for (int i = 0; i < columns.length; i++) {
			try {
				args[i] = ModelConverter.getAttributeValue(model, columns[i], language);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		if (language != null) {
			args[argSize - 1] = language;
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
				boolean localizedColumns = ModelConverter.containsLocalizedColumn(
					modelClass, finder.columns());

				FinderDefinition finderDefinition = new FinderDefinition(
					method.getName(), finder.columns(), columnBitmask, localizedColumns);

				if (returnType.equals(modelClass)) {
					finderDefinition.setUnique(true);
				}

				_finderDefinitions.put(method.getName(), finderDefinition);
			}
		}
	}

	private Map<String, FinderDefinition> _finderDefinitions = new HashMap<>();
	private M _nullModel;

}
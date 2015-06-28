package org.darkvault.wixen.cache;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(provides=CacheUtil.class)
public class CacheUtil {

	public static void clearAllCache(Class<?> cacheType) {
		for (String cacheName : getCacheNames(cacheType)) {
			GenericCache<?> cache = getGenericCache(cacheName, cacheType);

			try {
				cache.clear();
			}
			catch (CacheException ce) {
				_log.error("Unable to clear cache '" + cacheName + "'", ce);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static Set<String> getCacheNames(Class<?> cacheType) {
		Set<String> cacheNames = _cacheNamesByType.get(cacheType);

		if (cacheNames == null) {
			return Collections.unmodifiableSet(Collections.EMPTY_SET);
		}

		return Collections.unmodifiableSet(cacheNames);
	}

	public static <V> GenericCache<V> getGenericCache(Class<V> cacheType) {
		String cacheName = cacheType.getName();

		return getGenericCache(cacheName, cacheType);
	}

	@SuppressWarnings("unchecked")
	public static <V> GenericCache<V> getGenericCache(String name, Class<V> cacheType) {
		CachePool cachePool = _instance.getCachePool();

		try {
			GenericCache<V> cache = cachePool.getGenericCache(name, cacheType);

			Set<String> cacheNames = _cacheNamesByType.get(cacheType);

			if (cacheNames == null) {
				_cacheNamesByType.putIfAbsent(cacheType, new ConcurrentSkipListSet<String>());

				cacheNames = _cacheNamesByType.get(cacheType);
			}

			cacheNames.add(name);

			return cache;
		}
		catch (CacheException ce) {
			_log.warn(
				"Unable to get generic cache for type " + cacheType + " with name '" + name +
					"'. Returning NullCache");

			return (GenericCache<V>)_nullCache;
		}
	}

	@SuppressWarnings("unchecked")
	public static <V> GenericCache<List<V>> getGenericFinderCache(String name, Class<V> cacheType) {
		CachePool cachePool = _instance.getCachePool();

		try {
			GenericCache<List<V>> cache = cachePool.getGenericFinderCache(name, cacheType);

			return cache;
		}
		catch (CacheException ce) {
			_log.warn(
				"Unable to get generic finder cache for type " + cacheType + " with name '" + name +
					"'. Returning NullCache");

			return (GenericCache<List<V>>)_nullCache;
		}
	}

	@Start
	public void activate() {
		_instance = this;
	}

	public CachePool getCachePool() {
		return _cachePool;
	}

	private static final Logger _log = LoggerFactory.getLogger(CacheUtil.class);

	private static ConcurrentMap<Class<?>, Set<String>> _cacheNamesByType =
		new ConcurrentHashMap<>();

	private static CacheUtil _instance;
	private static GenericCache<?> _nullCache = new NullGenericCache<Object>();

	@ServiceDependency
	private volatile CachePool _cachePool;

}
package org.darkvault.wixen.cache.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.darkvault.wixen.cache.CacheException;
import org.darkvault.wixen.cache.CachePool;
import org.darkvault.wixen.cache.GenericCache;

@Component
public class SimpleCachePool implements CachePool {

	@SuppressWarnings("unchecked")
	@Override
	public <V> GenericCache<V> getGenericCache(String name, Class<V> cacheType)
		throws CacheException {

		GenericCache<V> cache = (GenericCache<V>) _pool.get(name);

		if (cache == null) {
			synchronized (_pool) {
				if (!_pool.containsKey(name)) {
					cache = new SimpleCache<V>();

					_pool.put(name, cache);
				}
				else {
					cache = (GenericCache<V>) _pool.get(name);
				}
			}
		}

		return cache;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> GenericCache<List<V>> getGenericFinderCache(String name, Class<V> cacheType)
		throws CacheException {

		GenericCache<List<V>> cache = (GenericCache<List<V>>) _pool.get(name);

		if (cache == null) {
			synchronized (_pool) {
				if (!_pool.containsKey(name)) {
					cache = new SimpleCache<List<V>>();

					_pool.put(name, cache);
				}
				else {
					cache = (GenericCache<List<V>>) _pool.get(name);
				}
			}
		}

		return cache;
	}

	private static Map<String, GenericCache<?>> _pool = new HashMap<>();

}
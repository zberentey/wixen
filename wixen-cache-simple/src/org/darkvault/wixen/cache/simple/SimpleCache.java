package org.darkvault.wixen.cache.simple;

import java.util.concurrent.ConcurrentHashMap;

import org.darkvault.wixen.cache.CacheException;
import org.darkvault.wixen.cache.GenericCache;

public class SimpleCache<V> implements GenericCache<V> {

	@Override
	public void clear() throws CacheException {
		_cache.clear();
	}

	@Override
	public V get(Object key) {
		return _cache.get(key);
	}

	@Override
	public void put(Object key, V value) {
		_cache.put(key, value);
	}

	@Override
	public void remove(Object key) {
		_cache.remove(key);
	}

	private ConcurrentHashMap<Object, V> _cache = new ConcurrentHashMap<>();

}
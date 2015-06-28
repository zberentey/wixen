package org.darkvault.wixen.cache;

import java.util.List;

public interface CachePool {

	public <V> GenericCache<V> getGenericCache(String name, Class<V> cacheType)
		throws CacheException;

	public <V> GenericCache<List<V>> getGenericFinderCache(String name, Class<V> cacheType)
		throws CacheException;

}
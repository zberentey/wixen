package org.darkvault.wixen.cache;

public interface GenericCache<V> {

	public void clear() throws CacheException;

	public V get(Object key);

	public void put(Object key, V obj);

	public void remove(Object key);

}
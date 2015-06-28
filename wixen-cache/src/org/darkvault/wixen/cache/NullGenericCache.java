package org.darkvault.wixen.cache;

public class NullGenericCache<V> implements GenericCache<V> {

	@Override
	public void clear() throws CacheException {
	}

	@Override
	public V get(Object key) {
		return null;
	}

	@Override
	public void put(Object key, V obj) {
	}

	@Override
	public void remove(Object key) {
	}

}
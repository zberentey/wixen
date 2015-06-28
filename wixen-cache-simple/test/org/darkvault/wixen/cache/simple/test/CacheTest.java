package org.darkvault.wixen.cache.simple.test;

import org.darkvault.wixen.cache.CachePool;
import org.darkvault.wixen.cache.GenericCache;
import org.darkvault.wixen.cache.simple.SimpleCachePool;
import org.junit.Assert;
import org.junit.Test;

public class CacheTest {

	@Test
	public void testCache() throws Exception {
		GenericCache<String> cache = _cachePool.getGenericCache("a", String.class);

		cache.put("1", "1");

		Assert.assertEquals("1", cache.get("1"));
		Assert.assertNull(cache.get("2"));

		cache.remove("1");

		Assert.assertNull(cache.get("1"));

		cache.put("2", "2");
		cache.clear();

		Assert.assertNull(cache.get("2"));
	}

	@Test
	public void testCachePool() throws Exception {
		GenericCache<String> cache = _cachePool.getGenericCache("a", String.class);

		cache.put("1", "1");

		cache = _cachePool.getGenericCache("a", String.class);

		Assert.assertEquals("1", cache.get("1"));
	}

	protected CachePool _cachePool = new SimpleCachePool();

}
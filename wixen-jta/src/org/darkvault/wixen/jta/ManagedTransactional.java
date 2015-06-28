package org.darkvault.wixen.jta;

public interface ManagedTransactional {
	public final String SERVICE_PROPERTY = "transactional";

	public Class<?>[] getManagedInterfaces();

}
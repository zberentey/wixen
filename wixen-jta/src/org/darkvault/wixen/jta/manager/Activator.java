package org.darkvault.wixen.jta.manager;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.darkvault.wixen.jta.ManagedTransactional;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext context, DependencyManager dm) throws Exception {
	}

	@Override
	public void init(BundleContext context, DependencyManager dm) throws Exception {
		dm.add(
			createComponent()
				.setImplementation(TransactionalAspectManager.class)
				.add(
					createServiceDependency()
						.setService(
							String.format(
								"(|(transactional=*)(objectClass=%s))",
								ManagedTransactional.class.getName()))
						.setCallbacks("onAdded", "onRemoved")));
	}

}
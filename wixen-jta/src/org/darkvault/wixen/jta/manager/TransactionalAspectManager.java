package org.darkvault.wixen.jta.manager;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.TransactionManager;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.darkvault.wixen.jta.ManagedTransactional;
import org.darkvault.wixen.jta.RegistrationCallback;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class TransactionalAspectManager {

	protected void onAdded(ServiceReference<?> ref, Object service) {
		try {
			Properties properties = new Properties();

			for (String key : ref.getPropertyKeys()) {
				if (Constants.OBJECTCLASS.equals(key)) {
					continue;
				}

				if ("transactional".equals(key)) {
					continue;
				}

				Object value = ref.getProperty(key);

				properties.put(key, value);
			}

			properties.put("isManaged", true);

			String[] interfaceClass = null;

			if (service instanceof ManagedTransactional) {
				Class<?>[] managedInterfaces =
					((ManagedTransactional)service).getManagedInterfaces();

				interfaceClass = new String[managedInterfaces.length];

				for (int i = 0; i < managedInterfaces.length; i++){
					interfaceClass[i] = managedInterfaces[i].getName();
				}
			}
			else {
				Object property = ref.getProperty("transactional");

				if (property instanceof String){
					interfaceClass = new String[]{(String) property};
				}
				else if (property instanceof String[] ){
					interfaceClass = (String[]) property;
				}
			}

			if (interfaceClass == null) {
				throw new RuntimeException(
					"Invalid managed transaction component registration, no interface set for " +
						"service [id=" + ref.getProperty(Constants.SERVICE_ID) + " class=" +
							service.getClass() +"]" );
			}

			Class<?> serviceClass = service.getClass();

			Object proxy = Proxy.newProxyInstance(
				serviceClass.getClassLoader(), serviceClass.getInterfaces(),
				new TransactionalAspect(service));

			Component component = _dependencyManager
				.createComponent()
				.setInterface(interfaceClass, properties)
				.setImplementation(proxy)
				.add(
					_dependencyManager.createServiceDependency()
						.setService(LogService.class)
						.setRequired(false))
				.add(
					_dependencyManager.createServiceDependency()
						.setService(TransactionManager.class)
						.setRequired(true));

			_map.put(ref, component);

			_dependencyManager.add(component);

			if (service instanceof RegistrationCallback) {
				try {
					((RegistrationCallback)service).onRegistered(proxy);
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void onRemoved(ServiceReference<?> ref, Object service) {
		Component component = _map.remove(ref);

		if (component != null) {
			_dependencyManager.remove(component);
		}
	}

	private volatile DependencyManager _dependencyManager;
	private volatile Map<ServiceReference<?>, Component> _map = new ConcurrentHashMap<>();

}
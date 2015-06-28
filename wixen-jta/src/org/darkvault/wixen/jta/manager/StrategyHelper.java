package org.darkvault.wixen.jta.manager;

import java.lang.reflect.Method;

import org.darkvault.wixen.jta.Transactional;
import org.darkvault.wixen.jta.Transactional.TxType;

public class StrategyHelper {

	public static TxType determineStrategy(Method method, Class<?> clazz) {
		TxType strategy = _determineTypeStrategy(clazz);

		if (strategy == null) {
			for (Class<?> interfaceClass : clazz.getInterfaces()) {
				try {
					interfaceClass.getMethod(method.getName(), method.getParameterTypes());

					strategy = _determineTypeStrategy(interfaceClass);

					if (strategy != null) {
						break;
					}
				}
				catch (NoSuchMethodException e) {
				}
			}
		}

		TxType methodStrategy = _determineMethodStrategy(method, clazz);

		if (methodStrategy == null) {
			for (Class<?> interfaceClass : clazz.getInterfaces()) {
				methodStrategy = _determineMethodStrategy(method, interfaceClass);

				if (methodStrategy != null) {
					break;
				}
			}
		}

		if (methodStrategy != null) {
			return methodStrategy;
		}

		return  strategy;
	}

	private static TxType _determineMethodStrategy(Method method, Class<?> clazz) {
		try {
			Method implementationMethod = clazz.getMethod(
				method.getName(), method.getParameterTypes());

			Transactional annotation = implementationMethod.getAnnotation(Transactional.class);

			if (annotation != null) {
				if (annotation.value() != null) {
					return annotation.value();
				}
			}
		}
		catch (NoSuchMethodException e) {
		}

		if (clazz.getSuperclass() != null) {
			return _determineMethodStrategy(method, clazz.getSuperclass());
		}

		return null;
	}

	private static TxType _determineTypeStrategy(Class<?> clazz) {
		Transactional annotation = clazz.getAnnotation(Transactional.class);

		if (annotation != null) {
			if (annotation.value() != null) {
				return annotation.value();
			}

			return TxType.REQUIRED;
		}

		if (clazz.getSuperclass() != null) {
			return _determineTypeStrategy(clazz.getSuperclass());
		}

		return null;
	}

}
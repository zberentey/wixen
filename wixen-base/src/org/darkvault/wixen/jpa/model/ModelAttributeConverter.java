package org.darkvault.wixen.jpa.model;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelAttributeConverter {

	public ModelAttributeConverter(
		ModelAttribute modelAttribute, long bitmask, Method jpaModelGetter, Class<?> modelClass) {

		Class<?> jpaModelClass = jpaModelGetter.getDeclaringClass();

		String jpaModelGetterName = jpaModelGetter.getName();

		_attributeName = modelAttribute.name();

		if (_attributeName.isEmpty()) {
			_attributeName = jpaModelGetterName.substring(3, 4);

			_attributeName = _attributeName.toLowerCase() + jpaModelGetterName.substring(4);
		}

		_bitmask = bitmask;

		_jpaModelClass = jpaModelClass;
		_jpaModelGetter = jpaModelGetter;
		_jpaModelName = jpaModelClass.getSimpleName();

		String jpaModelSetterName = "set" + jpaModelGetterName.substring(3);

		try {
			_jpaModelSetter = jpaModelClass.getMethod(
				jpaModelSetterName, jpaModelGetter.getReturnType());

			_modelClass = modelClass;
			_modelGetter = modelClass.getMethod(_toMethodName(_attributeName, "get"), new Class[0]);
			_modelSetter = modelClass.getMethod(
				_toMethodName(_attributeName, "set"), jpaModelGetter.getReturnType());
		}
		catch (NoSuchMethodException nsme) {
			_log.warn("Unable to register model attribute conversion. ", nsme);
		}
	}

	public void convertAttribute(Object source, Object target) {
		String sourceName = source.getClass().getSimpleName();
		String targetName = target.getClass().getSimpleName();

		Method getterMethod;
		Method setterMethod;

		if (sourceName.equals(_jpaModelName)) {
			getterMethod = _jpaModelGetter;
			setterMethod = _modelSetter;
		}
		else {
			getterMethod = _modelGetter;
			setterMethod = _jpaModelSetter;
		}

		if (getterMethod == null) {
			_log.error(sourceName + " doesn't have getter method for " + _attributeName);

			return;
		}

		if (setterMethod == null) {
			_log.error(targetName + " doesn't have setter method for " + _attributeName);

			return;
		}

		try {
			setterMethod.invoke(target, getterMethod.invoke(source, new Object[0]));
		}
		catch (Exception e) {
			_log.error(
				"Unable copy attribute '" + _attributeName + "' from " + sourceName + " to " +
					targetName + ". ", e);

			throw new RuntimeException(e);
		}
	}

	public String getAttributeName() {
		return _attributeName;
	}

	public Object getAttributeValue(Object model) throws Exception {
		if (isJpaModel(model)) {
			return _jpaModelGetter.invoke(model, new Object[0]);
		}

		return _modelGetter.invoke(model, new Object[0]);
	}

	public long getBitmask() {
		return _bitmask;
	}

	public boolean isJpaModel(Object model) {
		return model.getClass().isAssignableFrom(_jpaModelClass);
	}

	public boolean isPrimaryKeyAttribute() {
		return (_bitmask == -1);
	}

	public Object newJpaModel() {
		try {
			return _jpaModelClass.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object newModel() {
		try {
			return _modelClass.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String _toMethodName(String name, String type) {
		String s = name.substring(0, 1);

		return type.toLowerCase() + s.toUpperCase() + name.substring(1);
	}

	private static Logger _log = LoggerFactory.getLogger(ModelAttributeConverter.class);

	private String _attributeName;
	private long _bitmask;
	private Class<?> _jpaModelClass;
	private Method _jpaModelGetter;
	private String _jpaModelName;
	private Method _jpaModelSetter;
	private Class<?> _modelClass;
	private Method _modelGetter;
	private Method _modelSetter;

}
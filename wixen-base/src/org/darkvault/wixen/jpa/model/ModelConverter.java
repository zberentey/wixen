package org.darkvault.wixen.jpa.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ModelConverter {

	public static boolean containsLocalizedColumn(Class<?> modelClass, String[] columns) {
		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(
			modelClass.getName());

		if (converters == null) {
			throw new NoSuchModelConversionException(modelClass.getName());
		}

		String columnsJoined = StringUtils.join(columns, "#");

		for (ModelAttributeConverter converter : converters) {
			if (converter.isLocalized() && columnsJoined.contains(converter.getAttributeName())) {
				return true;
			}
		}

		return false;
	}

	public static Object fromModel(Object model) throws NoSuchModelConversionException {
		return fromModel(model, null);
	}

	public static Object fromModel(Object model, Object jpaModel)
		throws NoSuchModelConversionException {

		String modelName = model.getClass().getName();

		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(modelName);

		if ((converters == null) || converters.isEmpty()) {
			throw new NoSuchModelConversionException(modelName);
		}

		ModelAttributeConverter modelAttributeConverter = converters.get(0);

		if (jpaModel == null) {
			jpaModel = modelAttributeConverter.newJpaModel();
		}

		for (ModelAttributeConverter converter : converters) {
			converter.convertAttribute(model, jpaModel);
		}

		return jpaModel;
	}

	public static Object getAttributeValue(Object model, String attributeName) throws Exception  {
		return getAttributeValue(model, attributeName, null);
	}

	public static Object getAttributeValue(Object model, String attributeName, String language)
		throws Exception  {

		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(
			model.getClass().getName());

		if (converters == null) {
			throw new NoSuchModelConversionException(model.getClass().getName());
		}

		for (ModelAttributeConverter converter : converters) {
			if (attributeName.equals(converter.getAttributeName())) {
				if ((language == null) || !converter.isLocalized()) {
					return converter.getAttributeValue(model);
				}
				else {
					return converter.getAttributeValue(model, language);
				}
			}
		}

		throw new RuntimeException(
			"No '" + attributeName + "' found in model " + model.getClass().getName());
	}


	public static long getChangeMask(Object originalModel, Object newModel) throws Exception {
		long changeMask = 0;

		String key = originalModel.getClass().getName();

		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(key);

		if (converters == null) {
			throw new NoSuchModelConversionException(key);
		}

		for (ModelAttributeConverter converter : converters) {
			if (converter.isPrimaryKeyAttribute()) {
				continue;
			}

			if (originalModel == null) {
				changeMask = changeMask | converter.getBitmask();

				continue;
			}

			Object v1 = converter.getAttributeValue(originalModel);
			Object v2 = converter.getAttributeValue(newModel);

			if ((v1 != null) && (v2 != null) && !v1.equals(v2)) {
				changeMask = changeMask | converter.getBitmask();
			}
		}

		return changeMask;
	}

	public static long getColumnBitmask(Class<?> modelClass, String[] columns) {
		long bitmask = 0;

		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(
			modelClass.getName());

		if (converters == null) {
			throw new NoSuchModelConversionException(modelClass.getName());
		}

		for (ModelAttributeConverter converter : converters) {
			String attributeName = converter.getAttributeName();

			for (String column : columns) {
				if (column.equals(attributeName)) {
					bitmask = bitmask | converter.getBitmask();
				}
			}
		}

		return bitmask;
	}

	public static String[] getLocalizations(Object model, String[] columns) throws Exception {
		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(
			model.getClass().getName());

		if (converters == null) {
			throw new NoSuchModelConversionException(model.getClass().getName());
		}

		for (ModelAttributeConverter converter : converters) {
			if (converter.isLocalized()) {
				I18nString i18nValue = new I18nString((String)converter.getAttributeValue(model));

				Set<String> languages = i18nValue.getLanguages();

				return languages.toArray(new String[languages.size()]);
			}
		}

		return new String[0];
	}

	public static Class<?> getModelClass(Class<?> jpaModelClass) {
		Model model = jpaModelClass.getAnnotation(Model.class);

		if (model == null) {
			_log.error(
				"Unable to register JPA model, because it doesn't have the Model annotation");

			throw new RuntimeException();
		}

		return model.modelClass();
	}

	public static long getPrimaryKey(Object model) {
		ModelAttributeConverter converter = _primaryConverters.get(model.getClass().getName());

		try {
			return (long)converter.getAttributeValue(model);
		}
		catch (Exception e) {
			_log.error("Unable to get primary key from " + model.getClass(), e);

			return 0L;
		}
	}

	public static Object newModel(Class<?> jpaModelClass) {
		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(
			jpaModelClass.getName());

		ModelAttributeConverter modelAttributeConverter = converters.get(0);

		return modelAttributeConverter.newModel();
	}

	public static void registerJpaModel(Class<?> jpaModelClass) {
		Model model = jpaModelClass.getAnnotation(Model.class);

		if (model == null) {
			_log.error(
				"Unable to register JPA model, because it doesn't have the Model annotation");

			throw new RuntimeException();
		}

		Class<?> modelClass = model.modelClass();

		List<ModelAttributeConverter> converters = new ArrayList<>();

		long bitmask = 1L;

		for (Method jpaModelGetter : jpaModelClass.getDeclaredMethods()) {
			ModelAttribute modelAttribute = jpaModelGetter.getAnnotation(ModelAttribute.class);

			if (modelAttribute != null) {
				ModelAttributeConverter converter;

				if (jpaModelGetter.isAnnotationPresent(Id.class)) {
					 converter = new ModelAttributeConverter(
						modelAttribute, -1, jpaModelGetter, modelClass);

					_primaryConverters.put(jpaModelClass.getName(), converter);
					_primaryConverters.put(modelClass.getName(), converter);
				}
				else {
					 converter = new ModelAttributeConverter(
						modelAttribute, bitmask, jpaModelGetter, modelClass);
				}

				converters.add(converter);
			}

			bitmask = bitmask * 2;
		}

		_modelAttributeConverters.put(jpaModelClass.getName(), converters);
		_modelAttributeConverters.put(modelClass.getName(), converters);

		_log.info("Registered jpa model converter for " + jpaModelClass.getName());
	}

	public static void setPrimaryKeyFromJpaModel(Object model, Object jpaModel) {
		ModelAttributeConverter converter = _primaryConverters.get(model.getClass().getName());

		converter.convertAttribute(jpaModel, model);
	}

	public static Object toModel(Object jpaModel) throws NoSuchModelConversionException {
		String jpaModelName = jpaModel.getClass().getName();

		List<ModelAttributeConverter> converters = _modelAttributeConverters.get(jpaModelName);

		if ((converters == null) || converters.isEmpty()) {
			registerJpaModel(jpaModel.getClass());

			converters = _modelAttributeConverters.get(jpaModelName);
		}

		ModelAttributeConverter modelAttributeConverter = converters.get(0);

		Object model = modelAttributeConverter.newModel();

		for (ModelAttributeConverter converter : converters) {
			converter.convertAttribute(jpaModel, model);
		}

		return model;
	}

	public static void unregisterJpaModel(Class<?> jpaModelClass) {
		Model model = jpaModelClass.getAnnotation(Model.class);

		if (model == null) {
			return;
		}

		Class<?> modelClass = model.modelClass();

		_modelAttributeConverters.remove(jpaModelClass.getName());
		_modelAttributeConverters.remove(modelClass.getName());

		_log.info("Unregistered jpa model converter for " + jpaModelClass.getName());
	}

	private static final Logger _log = LoggerFactory.getLogger(ModelConverter.class);

	private static Map<String, List<ModelAttributeConverter>> _modelAttributeConverters =
		new HashMap<>();

	private static Map<String, ModelAttributeConverter> _primaryConverters = new HashMap<>();

}
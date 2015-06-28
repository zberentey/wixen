package org.darkvault.wixen.jpa.model;

@SuppressWarnings("serial")
public class NoSuchModelConversionException extends RuntimeException {

	public NoSuchModelConversionException(String modelName) {
		_modelName = modelName;
	}

	@Override
	public String getMessage() {
		return "No model conversion is registered for " + _modelName;
	}

	private String _modelName;
}
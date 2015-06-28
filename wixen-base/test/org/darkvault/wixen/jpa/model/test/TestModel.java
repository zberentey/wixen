package org.darkvault.wixen.jpa.model.test;

public class TestModel {

	public long getId() {
		return _id;
	}

	public String getModelValue() {
		return _modelValue;
	}

	public String getName() {
		return _name;
	}

	public int getNumber() {
		return _number;
	}

	public void setId(long id) {
		_id = id;
	}

	public void setModelValue(String modelValue) {
		_modelValue = modelValue;
	}

	public void setName(String name) {
		_name = name;
	}

	public void setNumber(int number) {
		_number = number;
	}

	private long _id;
	private String _modelValue;
	private String _name;
	private int _number;

}
package org.darkvault.wixen.example.model;

public class Example {

	public String getCode() {
		return _code;
	}

	public long getExampleId() {
		return _exampleId;
	}

	public String getName() {
		return _name;
	}

	public void setCode(String code) {
		_code = code;
	}

	public void setExampleId(long exampleId) {
		_exampleId = exampleId;
	}

	public void setName(String name) {
		_name = name;
	}

	@Override
	public String toString() {
		return "Example{" + _code + ":" + _name + "}";
	}

	private String _code;
	private long _exampleId;
	private String _name;

}
package org.darkvault.wixen.jpa.model.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.darkvault.wixen.jpa.model.Model;
import org.darkvault.wixen.jpa.model.ModelAttribute;

@Entity
@Model(modelClass=TestModel.class)
public class JpaTestModel {

	@Id
	@ModelAttribute
	public long getId() {
		return _id;
	}

	@ModelAttribute
	public String getName() {
		return _name;
	}

	public int getNumber() {
		return _number;
	}

	@ModelAttribute(name="modelValue")
	public String getValue() {
		return _value;
	}

	public void setId(long id) {
		_id = id;
	}

	public void setName(String name) {
		_name = name;
	}

	public void setNumber(int number) {
		_number = number;
	}

	public void setValue(String value) {
		_value = value;
	}

	private long _id;
	private String _name;
	private int _number;
	private String _value;

}

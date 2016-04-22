package org.darkvault.wixen.example.model.jpa;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.darkvault.wixen.example.model.Example;
import org.darkvault.wixen.jpa.model.Model;
import org.darkvault.wixen.jpa.model.ModelAttribute;

@Cacheable(true)
@Entity
@Model(modelClass = Example.class)
@Table(name = "example", indexes = {@Index(name = "code_index",  columnList="code", unique = false)})
public class JpaExample {

	@ModelAttribute
	public String getCode() {
		return _code;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ModelAttribute
	public long getExampleId() {
		return _exampleId;
	}

	@ModelAttribute
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
		return "JpaExample{" + _code + ":" + _name + "}";
	}

	private String _code;
	private long _exampleId;
	private String _name;

}
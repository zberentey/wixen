package org.darkvault.wixen.example.service.impl;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.darkvault.wixen.example.model.Example;
import org.darkvault.wixen.example.service.ExampleService;
import org.darkvault.wixen.example.service.persistence.ExamplePersistence;
import org.darkvault.wixen.jta.ManagedTransactional;
import org.darkvault.wixen.jta.Transactional;

@Component(provides = ManagedTransactional.class)
@Transactional
public class ExampleServiceImpl implements ExampleService, ManagedTransactional{

	@Start
	public void activate() {
		System.out.println("dsfsdfsdfdsffdsfds");
	}

	@Override
	public List<Example> getExamples() {
		return _examplePersistence.findAll();
	}

	@Override
	public Example getExample(long exampleId) {
		return _examplePersistence.findByPrimaryKey(exampleId);
	}

	@Override
	public Example getExample(String code) {
		return _examplePersistence.findByCode(code);
	}

	@Override
	public Class<?>[] getManagedInterfaces() {
		return new Class<?>[] {ExampleService.class};
	}

	@Override
	public void initData() {
		Example example = new Example();

		example.setCode("CODE-1");
		example.setName("NAME-1");

		_examplePersistence.update(example);
	}

	@ServiceDependency
	private ExamplePersistence _examplePersistence;

}
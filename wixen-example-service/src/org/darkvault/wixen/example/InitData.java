package org.darkvault.wixen.example;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.darkvault.wixen.example.service.ExampleService;

@Component
public class InitData {

	@Start
	public void activate() {
		_exampleService.initData();

		System.out.println(_exampleService.getExample(1));
	}

	@ServiceDependency
	private volatile ExampleService _exampleService;

}
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

		long iterations = 1000000;

		long time1 = _exampleService.runTest(iterations);

		System.out.println("Persistence test took " + time1 + "ms.");

		long time2 = _exampleService.runTestJpa(iterations);

		System.out.println("JPA test took " + time2 + "ms.");
	}

	@ServiceDependency
	private volatile ExampleService _exampleService;

}
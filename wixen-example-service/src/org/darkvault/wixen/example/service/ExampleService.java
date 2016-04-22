package org.darkvault.wixen.example.service;

import java.util.List;

import org.darkvault.wixen.example.model.Example;

public interface ExampleService {

	public List<Example> getExamples();

	public Example getExample(long exampleId);

	public Example getExample(String code);

	public void initData();

	public long runTest(long iterations);

	public long runTestJpa(long iterations);

}
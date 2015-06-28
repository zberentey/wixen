package org.darkvault.wixen.example.service.persistence;

import org.darkvault.wixen.example.model.Example;
import org.darkvault.wixen.example.model.jpa.JpaExample;
import org.darkvault.wixen.jpa.persistence.BasePersistence;

public interface ExamplePersistence extends BasePersistence<JpaExample, Example> {

	public Example findByCode(String countryCode);

}
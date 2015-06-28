package org.darkvault.wixen.jpa.model.test;

import org.darkvault.wixen.jpa.model.ModelConverter;
import org.junit.Assert;
import org.junit.Test;

public class ModelConverterTest {

	@Test
	public void testModelAttributes() throws Exception {
		ModelConverter.registerJpaModel(JpaTestModel.class);

		JpaTestModel jpaModel = new JpaTestModel();

		jpaModel.setId(1);
		jpaModel.setName("name");
		jpaModel.setNumber(1);
		jpaModel.setValue("value");

		TestModel model = (TestModel)ModelConverter.toModel(jpaModel);

		Assert.assertEquals(jpaModel.getName(), model.getName());
		Assert.assertEquals(jpaModel.getValue(), model.getModelValue());
		Assert.assertEquals(jpaModel.getId(), model.getId());
		Assert.assertEquals(0, model.getNumber());

		model.setNumber(2);
		model.setName("newName");
		model.setModelValue("newValue");

		JpaTestModel newJpaModel = (JpaTestModel)ModelConverter.fromModel(model);

		Assert.assertEquals(model.getName(), newJpaModel.getName());
		Assert.assertEquals(model.getModelValue(), newJpaModel.getValue());
		Assert.assertEquals(0, newJpaModel.getNumber());

		Assert.assertEquals("newName", ModelConverter.getAttributeValue(model, "name"));

		Assert.assertEquals(1, ModelConverter.getPrimaryKey(model));
	}

	@Test
	public void testBitmasks() throws Exception {
		ModelConverter.registerJpaModel(JpaTestModel.class);

		Assert.assertEquals(
			3,
			ModelConverter.getColumnBitmask(TestModel.class, new String[] {"name", "modelValue"}));

		JpaTestModel jpaModel = new JpaTestModel();

		jpaModel.setId(1);
		jpaModel.setName("name");
		jpaModel.setValue("value");

		TestModel model = new TestModel();

		model.setId(1);
		model.setName("name");
		model.setModelValue("value");

		Assert.assertEquals(
			0, ModelConverter.getChangeMask(ModelConverter.toModel(jpaModel), model));

		model.setModelValue("x");

		Assert.assertEquals(
			2, ModelConverter.getChangeMask(ModelConverter.toModel(jpaModel), model));

		model.setName("x");
		model.setModelValue("value");

		Assert.assertEquals(
			1, ModelConverter.getChangeMask(ModelConverter.toModel(jpaModel), model));

		model.setModelValue("y");

		Assert.assertEquals(
			3, ModelConverter.getChangeMask(ModelConverter.toModel(jpaModel), model));
	}

}
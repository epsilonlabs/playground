package org.eclipse.epsilon.live;

import java.io.File;

import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;

import com.google.gson.JsonObject;

public class EmfaticToGraphvizFunction extends EpsilonLiveFunction {
	
	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		String graphviz = run(request.get("emfatic").getAsString());
		response.addProperty("metamodelDiagram", graphviz);
	}
	
	protected String run(String emfatic) throws Exception {
		EglTemplateFactoryModuleAdapter module = new EglTemplateFactoryModuleAdapter();
		module.parse(new File("src/main/resources/classdiagram.egl"));
		module.getContext().getModelRepository().addModel(getInMemoryEmfaticModel(emfatic));
		return module.execute() + "";
	}
	
}

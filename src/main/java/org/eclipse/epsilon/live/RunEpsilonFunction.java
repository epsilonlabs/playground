package org.eclipse.epsilon.live;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.egl.IEglModule;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.epl.concurrent.EplModuleParallelPatterns;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.evl.EvlModule;

import com.google.gson.JsonObject;

public class RunEpsilonFunction extends EpsilonLiveFunction {
	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		run(request.get("language").getAsString(),
				request.get("program").getAsString(), 
				request.get("flexmi").getAsString(), 
				request.get("emfatic").getAsString(), 
				request.get("secondFlexmi").getAsString(), 
				request.get("secondEmfatic").getAsString(), 
				bos, response);
		response.addProperty("output", bos.toString());
	}
	
	public void run(String language, String program, String flexmi, String emfatic, String secondFlexmi, String secondEmfatic, OutputStream outputStream, JsonObject response) throws Exception {
		
		IEolModule module = createModule(language);
		module.parse(program);
		if (!module.getParseProblems().isEmpty()) {
			response.addProperty("error", module.getParseProblems().get(0).toString());
			return;
		}
		
		module.getContext().setOutputStream(new PrintStream(outputStream));
		
		switch (language) {
			case "etl": runEtl((EtlModule) module, flexmi, emfatic, secondFlexmi, secondEmfatic, response); return;
			case "evl": runEvl((EvlModule) module, flexmi, emfatic, response); return;
			case "epl": runEpl((EplModule) module, flexmi, emfatic, response); return;
			case "egl": runEgl((IEglModule) module, flexmi, emfatic, response); return;
			default: runEol((EolModule) module, flexmi, emfatic);
		}
		
	}
	
	protected void runEtl(EtlModule module, String flexmi, String emfatic, String secondFlexmi, String secondEmfatic, JsonObject response) throws Exception {
		InMemoryEmfModel sourceModel = getInMemoryFlexmiModel(flexmi, emfatic);
		sourceModel.setName("Source");
		InMemoryEmfModel targetModel = getBlankInMemoryModel(secondEmfatic);
		targetModel.setName("Target");
		
		module.getContext().getModelRepository().addModel(sourceModel);
		module.getContext().getModelRepository().addModel(targetModel);
		
		module.execute();
		
		response.addProperty("targetModelDiagram", new FlexmiToPlantUMLFunction().run(targetModel));
	}
	
	protected void runEvl(EvlModule module, String flexmi, String emfatic, JsonObject response) throws Exception {
		InMemoryEmfModel model = getInMemoryFlexmiModel(flexmi, emfatic);
		model.setName("M");
		module.getContext().getModelRepository().addModel(model);
		module.execute();
		response.addProperty("validatedModelDiagram", new FlexmiToPlantUMLFunction().run(model, 
				Variable.createReadOnlyVariable("unsatisfiedConstraints", module.getContext().getUnsatisfiedConstraints())));
	}
	
	protected void runEpl(EplModule module, String flexmi, String emfatic, JsonObject response) throws Exception {
		InMemoryEmfModel model = getInMemoryFlexmiModel(flexmi, emfatic);
		model.setName("M");
		module.getContext().getModelRepository().addModel(model);
		module.execute();
		response.addProperty("patternMatchedModelDiagram", new FlexmiToPlantUMLFunction().run(model, 
				Variable.createReadOnlyVariable("matches", module.getContext().getPatternMatchTrace().getMatches())));
	}
	
	protected void runEgl(IEglModule module, String flexmi, String emfatic, JsonObject response) throws Exception {
		InMemoryEmfModel model = getInMemoryFlexmiModel(flexmi, emfatic);
		model.setName("M");
		module.getContext().getModelRepository().addModel(model);
		String generatedText = module.execute() + "";
		response.addProperty("generatedText", generatedText);
	}
	
	protected void runEol(EolModule module, String flexmi, String emfatic) throws Exception {
		InMemoryEmfModel model = getInMemoryFlexmiModel(flexmi, emfatic);
		model.setName("M");
		module.getContext().getModelRepository().addModel(model);
		module.execute();
	}
	
	protected IEolModule createModule(String language) {
		switch (language) {
		case "etl": return new EtlModule();
		case "evl": return new EvlModule();
		case "epl": return new EplModuleParallelPatterns();
		case "egl": return new EglTemplateFactoryModuleAdapter();
		default: return new EolModule();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		new RunEpsilonFunction().getEPackage("package foo");
		
		/*
		new RunEpsilonFunction().run("", "",
				"<?nsuri http://www.eclipse.org/emf/2002/Ecore?>\n<package/>", "package tree; class Tree{}",
				"<?nsuri http://www.eclipse.org/emf/2002/Ecore?>\n<package/>", "package tree; class Tree{}",
				System.out, new JsonObject());*/
	}

}

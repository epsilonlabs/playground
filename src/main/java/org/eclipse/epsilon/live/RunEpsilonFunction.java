package org.eclipse.epsilon.live;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.eclipse.epsilon.ecl.EclModule;
import org.eclipse.epsilon.ecl.trace.MatchTrace;
import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.egl.EgxModule;
import org.eclipse.epsilon.egl.IEglModule;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eml.EmlModule;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.flock.FlockModule;
import org.eclipse.epsilon.live.egl.StringGeneratingTemplateFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RunEpsilonFunction extends EpsilonLiveFunction {
	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		run(request.get("language").getAsString(),
				request.get("program").getAsString(), 
				request.get("secondProgram").getAsString(), 
				request.get("flexmi").getAsString(), 
				request.get("emfatic").getAsString(), 
				request.get("secondFlexmi").getAsString(), 
				request.get("secondEmfatic").getAsString(), 
				request.get("thirdFlexmi") != null ? request.get("thirdFlexmi").getAsString() : "", 
				request.get("thirdEmfatic") != null ? request.get("thirdEmfatic").getAsString() : "", 
				bos, response);
		response.addProperty("output", bos.toString());
	}
	
	public void run(String language, String program, String secondProgram, String flexmi, String emfatic, String secondFlexmi, String secondEmfatic, String thirdFlexmi, String thirdEmfatic, OutputStream outputStream, JsonObject response) throws Exception {
		
		IEolModule module = createModule(language);
		module.parse(program, new File("/program." + language));
		if (!module.getParseProblems().isEmpty()) {
			response.addProperty("error", module.getParseProblems().get(0).toString());
			return;
		}
		
		module.getContext().setOutputStream(new PrintStream(outputStream));
		
		switch (language) {
			case "etl": runEtl((EtlModule) module, flexmi, emfatic, secondEmfatic, response); return;
			case "flock": runFlock((FlockModule) module, flexmi, emfatic, secondEmfatic, response); return;
			case "evl": runEvl((EvlModule) module, flexmi, emfatic, response); return;
			case "epl": runEpl((EplModule) module, flexmi, emfatic, response); return;
			case "egl": runEgl((IEglModule) module, flexmi, emfatic, response); return;
			case "egx": runEgx((EgxModule) module, secondProgram, flexmi, emfatic, response); return;
			case "eml": runEml((EmlModule) module, secondProgram, flexmi, emfatic, thirdFlexmi, thirdEmfatic, secondEmfatic, response); return;
			default: runEol((EolModule) module, flexmi, emfatic);
		}
		
	}
	
	protected void runEml(EmlModule module, String ecl, String leftFlexmi, String leftEmfatic, String rightFlexmi, String rightEmfatic, String mergedEmfatic, JsonObject response) throws Exception {
		
		EclModule eclModule = new EclModule();
		
		eclModule.parse(ecl, new File("/program.ecl"));
		if (!eclModule.getParseProblems().isEmpty()) {
			response.addProperty("error", eclModule.getParseProblems().get(0).toString());
			return;
		}
		
		InMemoryEmfModel leftModel = getInMemoryFlexmiModel(leftFlexmi, leftEmfatic);
		leftModel.setName("Left");
		leftModel.getAliases().add("Source");
		
		InMemoryEmfModel rightModel = getInMemoryFlexmiModel(rightFlexmi, rightEmfatic);
		rightModel.setName("Right");
		rightModel.getAliases().add("Source");
		
		InMemoryEmfModel mergedModel = getBlankInMemoryModel(mergedEmfatic);
		mergedModel.setName("Merged");
		mergedModel.getAliases().add("Target");
		
		eclModule.getContext().getModelRepository().addModel(leftModel);
		eclModule.getContext().getModelRepository().addModel(rightModel);
		
		MatchTrace matchTrace = eclModule.execute();
		
		module.getContext().setMatchTrace(matchTrace.getReduced());
		module.getContext().getModelRepository().addModel(leftModel);
		module.getContext().getModelRepository().addModel(rightModel);
		module.getContext().getModelRepository().addModel(mergedModel);
		
		module.execute();
		
		response.addProperty("targetModelDiagram", new FlexmiToPlantUMLFunction().run(mergedModel));
	}
	
	protected void runEtl(EtlModule module, String flexmi, String emfatic, String secondEmfatic, JsonObject response) throws Exception {
		InMemoryEmfModel sourceModel = getInMemoryFlexmiModel(flexmi, emfatic);
		sourceModel.setName("Source");
		InMemoryEmfModel targetModel = getBlankInMemoryModel(secondEmfatic);
		targetModel.setName("Target");
		
		module.getContext().getModelRepository().addModel(sourceModel);
		module.getContext().getModelRepository().addModel(targetModel);
		
		module.execute();
		
		response.addProperty("targetModelDiagram", new FlexmiToPlantUMLFunction().run(targetModel));
	}
	
	protected void runFlock(FlockModule module, String flexmi, String emfatic, String secondEmfatic, JsonObject response) throws Exception {
		InMemoryEmfModel originalModel = getInMemoryFlexmiModel(flexmi, emfatic);
		originalModel.setName("Original");
		InMemoryEmfModel migratedModel = getBlankInMemoryModel(secondEmfatic);
		migratedModel.setName("Migrated");
		
		module.getContext().getModelRepository().addModel(originalModel);
		module.getContext().getModelRepository().addModel(migratedModel);
		
		module.getContext().setOriginalModel(originalModel);
		module.getContext().setMigratedModel(migratedModel);
		
		module.execute();
		
		response.addProperty("targetModelDiagram", new FlexmiToPlantUMLFunction().run(migratedModel));
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
	
	protected void runEgx(EgxModule module, String templateCode, String flexmi, String emfatic, JsonObject response) throws Exception {
		InMemoryEmfModel model = getInMemoryFlexmiModel(flexmi, emfatic);
		model.setName("M");
		module.getContext().getModelRepository().addModel(model);
		
		// Regardless of which template EGX tries to parse, it will end up parsing the EGL template in the example
		((StringGeneratingTemplateFactory) module.getTemplateFactory()).setTemplateCode(templateCode);
		module.execute();
		
		// Put the generated files in the response
		JsonArray generatedFiles = new JsonArray();
		
		Map<String, String> results = ((StringGeneratingTemplateFactory) module.getTemplateFactory()).getResults();
		for (String key : results.keySet()) {
			JsonObject generatedFile = new JsonObject();
			generatedFile.addProperty("path", key);
			generatedFile.addProperty("content", results.get(key));
			generatedFiles.add(generatedFile);
		}
		response.add("generatedFiles", generatedFiles);
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
		case "flock": return new FlockModule();
		case "evl": return new EvlModule();
		case "epl": return new EplModule();
		case "egl": return new EglTemplateFactoryModuleAdapter();
		case "egx": return new EgxModule(new StringGeneratingTemplateFactory());
		case "eml": return new EmlModule();
		default: return new EolModule();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		JsonObject response = new JsonObject();
		
		new RunEpsilonFunction().run("egx", 
				"rule T2T transform t : Tree { template : 'foo.egl' target: t.name + '.txt'}", 
				"Tree [%=t.name%]", 
				"<?nsuri tree?><_><tree name=\"t1\"/><tree name=\"t2\"/></_>",
				"package tree; class Tree { attr String name; }", 
				"", "", "", "", System.out, response);
		
		System.out.println(response.get("generatedFiles"));
		
		//new RunEpsilonFunction().getEPackage("package foo");
		
		/*
		new RunEpsilonFunction().run("", "",
				"<?nsuri http://www.eclipse.org/emf/2002/Ecore?>\n<package/>", "package tree; class Tree{}",
				"<?nsuri http://www.eclipse.org/emf/2002/Ecore?>\n<package/>", "package tree; class Tree{}",
				System.out, new JsonObject());*/
	}

}

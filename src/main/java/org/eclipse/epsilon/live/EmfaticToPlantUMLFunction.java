package org.eclipse.epsilon.live;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;

import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;

import com.google.gson.JsonObject;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class EmfaticToPlantUMLFunction extends EpsilonLiveFunction {
	
	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		String plantuml = run(request.get("emfatic").getAsString());
		response.addProperty("metamodelDiagram", plantuml);
	}
	
	protected String run(String emfatic) throws Exception {
		EglTemplateFactoryModuleAdapter module = new EglTemplateFactoryModuleAdapter();
		module.parse(new File("src/main/resources/emfatic2plantuml.egl"));
		module.getContext().getModelRepository().addModel(getInMemoryEmfaticModel(emfatic));
		String plantUml = module.execute() + "";
		
		SourceStringReader reader = new SourceStringReader(plantUml);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		reader.outputImage(os, new FileFormatOption(FileFormat.SVG));
		os.close();

		return new String(os.toByteArray(), Charset.forName("UTF-8"));
	}
	
}

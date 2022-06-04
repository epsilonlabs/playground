package org.eclipse.epsilon.live;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;

import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.execute.context.Variable;

import com.google.gson.JsonObject;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class FlexmiToPlantUMLFunction extends EpsilonLiveFunction {

	public static void main(String[] args) throws Exception {
		System.out.println(
				new FlexmiToPlantUMLFunction().
				run("<?nsuri http://www.eclipse.org/emf/2002/Ecore?>\n<package name=\"p1\"/>", 
						""));
	}
	
	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		String graphviz = run(request.get("flexmi").getAsString(), request.get("emfatic").getAsString());
		response.addProperty("modelDiagram", graphviz);
	}
	
	protected String run(String flexmi, String emfatic) throws Exception {
		return run(getInMemoryFlexmiModel(flexmi, emfatic));
	}
	
	protected String run(InMemoryEmfModel model, Variable... variables) throws Exception {
		EglTemplateFactoryModuleAdapter module = new EglTemplateFactoryModuleAdapter();
		module.parse(new File("src/main/resources/flexmi2plantuml.egl"));
		model.setName("M");
		module.getContext().getModelRepository().addModel(model);
		module.getContext().getFrameStack().put(variables);
		String plantUml = module.execute() + "";
		
		SourceStringReader reader = new SourceStringReader(plantUml);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		reader.outputImage(os, new FileFormatOption(FileFormat.SVG));
		os.close();

		return new String(os.toByteArray(), Charset.forName("UTF-8"));
		
	}
	
}

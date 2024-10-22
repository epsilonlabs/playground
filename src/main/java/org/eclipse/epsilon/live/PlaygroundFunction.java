package org.eclipse.epsilon.live;

import com.google.gson.JsonObject;

public class PlaygroundFunction extends EpsilonLiveFunction {

	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		String functionName = request.get("function").getAsString();
		EpsilonLiveFunction function = null;
		switch (functionName) {
			case "EmfaticToPlantUML": function = new EmfaticToPlantUMLFunction(); break;
			case "FlexmiToPlantUML": function = new FlexmiToPlantUMLFunction(); break;
			case "ShortURL": function = new ShortURLFunction(); break;
			default: function = new RunEpsilonFunction();
		}
		function.serviceImpl(request, response);
	}

}

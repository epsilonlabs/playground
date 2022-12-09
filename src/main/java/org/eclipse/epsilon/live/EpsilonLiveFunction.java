package org.eclipse.epsilon.live;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.emfatic.core.EmfaticResource;
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.flexmi.FlexmiResourceFactory;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class EpsilonLiveFunction implements HttpFunction {
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		response.appendHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		
		if ("OPTIONS".equals(request.getMethod())) {
			response.appendHeader("Access-Control-Allow-Methods", "GET");
			response.appendHeader("Access-Control-Allow-Headers", "Content-Type");
			response.appendHeader("Access-Control-Max-Age", "3600");
			response.setStatusCode(HttpURLConnection.HTTP_NO_CONTENT);
			return;
		}
		else {
			JsonObject responseJson = new JsonObject();
			
			try {
				serviceImpl(getJsonObject(request), responseJson);
			}
			catch (Throwable t){
				responseJson.addProperty("output", t.getMessage());
				responseJson.addProperty("error", t.getMessage());
			}
			
			response.getWriter().write(responseJson.toString());
			
		}
	}
	
	public abstract void serviceImpl(JsonObject request, JsonObject response) throws Exception;
	
	protected InMemoryEmfModel getInMemoryFlexmiModel(String flexmi, String emfatic) throws Exception {
		ResourceSet resourceSet = new ResourceSetImpl();
		EPackage ePackage = getEPackage(emfatic);
		resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new FlexmiResourceFactory());
		Resource resource = resourceSet.createResource(URI.createURI("flexmi.flexmi"));
		resource.load(new ByteArrayInputStream(flexmi.getBytes()), null);

		InMemoryEmfModel model = new InMemoryEmfModel(resource);
		model.setName("M");
		return model;
	}
	
	protected InMemoryEmfModel getBlankInMemoryModel(String emfatic) throws Exception {
		ResourceSet resourceSet = new ResourceSetImpl();
		EPackage ePackage = getEPackage(emfatic);
		resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource resource = resourceSet.createResource(URI.createURI("xmi.xmi"));

		InMemoryEmfModel model = new InMemoryEmfModel(resource);
		model.setName("M");
		return model;
	}
	
	protected InMemoryEmfModel getInMemoryEmfaticModel(String emfatic) throws Exception {
		InMemoryEmfModel model = new InMemoryEmfModel(getEPackage(emfatic).eResource());
		model.setName("M");
		return model;
	}
	
	protected EPackage getEPackage(String emfatic) throws Exception {
		
		if (emfatic == null || emfatic.trim().isEmpty()) return EcorePackage.eINSTANCE;
		
		EmfaticResource emfaticResource = new EmfaticResource(URI.createURI("emfatic.emf")); 
		emfaticResource.load(new ByteArrayInputStream(emfatic.getBytes()), null); 
		if (emfaticResource.getParseContext().hasErrors()) {
			throw new RuntimeException(emfaticResource.getParseContext().getMessages()[0].getMessage());
		}
		else {
			return (EPackage) emfaticResource.getContents().get(0);
		}
	}
	
	protected JsonObject getJsonObject(HttpRequest request) throws Exception {
		String json = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		return getJsonObject(json);
	}
	
	protected JsonObject getJsonObject(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}
}

package org.eclipse.epsilon.live;

import java.io.FileInputStream;
import java.util.UUID;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.JsonObject;

public class ShortURLFunction extends EpsilonLiveFunction {
	
	public static void main(String[] args) throws Exception {
		JsonObject shortenRequest = new JsonObject();
		JsonObject shortenResponse = new JsonObject();
		JsonObject expandResponse = new JsonObject();
		
		shortenRequest.addProperty("content", "another string");
		
		new ShortURLFunction().serviceImpl(shortenRequest, shortenResponse);
		
		System.out.println(shortenResponse.get("shortened").getAsString());
		
		new ShortURLFunction().serviceImpl(shortenResponse, expandResponse);
		
		System.out.println(expandResponse.get("content").getAsString());
		
	}
	
	protected String getShortened(String content) {
		return UUID.nameUUIDFromBytes(content.getBytes()).toString().substring(0,8);
	}
	
	@Override
	public void serviceImpl(JsonObject request, JsonObject response) throws Exception {
		Storage storage = StorageOptions.newBuilder().setProjectId("epsilon-live-gcp").
				setCredentials(GoogleCredentials.fromStream(new FileInputStream("epsilon-live-gcp.json"))).
				build().getService();
		
		
		if (request.has("content")) {
			String content = request.get("content").getAsString();
			String shortened = getShortened(content);
			BlobId blobId = BlobId.of("epsilon-live-gcp.appspot.com", shortened);
			
			Blob blob = storage.get(blobId);
			if (blob == null) {
				storage.create(BlobInfo.newBuilder(blobId).setContentType("text/plain").build(), content.getBytes());
			}
			
			response.addProperty("shortened", shortened);
		}
		else if (request.has("shortened")) {
			String shortened = request.get("shortened").getAsString();
			BlobId blobId = BlobId.of("epsilon-live-gcp.appspot.com", shortened);
			
			Blob blob = storage.get(blobId);
			if (blob != null) {
				response.addProperty("content", new String(blob.getContent()));
			}
		}
	}
}

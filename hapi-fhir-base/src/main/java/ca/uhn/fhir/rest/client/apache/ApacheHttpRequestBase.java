package ca.uhn.fhir.rest.client.apache;

/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2016 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import ca.uhn.fhir.rest.client.api.IHttpRequestBase;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

/**
 * A Http Request based on Apache. This is an adapter around the class {@link org.apache.http.client.methods.HttpRequestBase HttpRequestBase}
 * @author Peter Van Houte | peter.vanhoute@agfa.com | Agfa Healthcare
 */
public class ApacheHttpRequestBase implements IHttpRequestBase {

    private HttpRequestBase myRequest;
    private HttpClient myClient;

    public ApacheHttpRequestBase(HttpClient theClient, HttpRequestBase theApacheRequest) {
        this.myClient = theClient;
        this.myRequest = theApacheRequest;
    }

    @Override
    public void addHeader(String theName, String theValue) {
        getApacheRequest().addHeader(theName, theValue);
    }

    /**
     * Get the myApacheRequest
     * @return the myApacheRequest
     */
    public HttpRequestBase getApacheRequest() {
        return myRequest;
    }

    @Override
    public IHttpResponse execute() throws IOException {
        return new ApacheHttpResponse(myClient.execute(getApacheRequest()));
    }

	@Override
	public Map<String, List<String>> getAllHeaders() {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for (Header header : myRequest.getAllHeaders()) {
			if(!result.containsKey(header.getName())) {
				result.put(header.getName(), new LinkedList<String>());
			}
			result.get(header.getName()).add(header.getValue());		
		}
		return result;
	}

	@Override
	public String toString() {
		return myRequest.toString();
	}

}
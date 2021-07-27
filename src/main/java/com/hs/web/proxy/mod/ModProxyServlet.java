/*
 * Copyright MITRE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hs.web.proxy.mod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModProxyServlet extends ProxyServlet {

	private static final long serialVersionUID = SERIAL_VERSION_UID;

	private static final String ATTR_QUERY_STRING = ModProxyServlet.class.getSimpleName() + ".queryString";

	String protocolParamName;
	String serverParamName;
	String portParamName;
	String pathparamName;
	String defaultTargetServer;

	@Override
	protected void initTarget() throws ServletException {
		protocolParamName 	= getConfigParam("protocolParamName");
		serverParamName 	= getConfigParam("serverParamName");
		portParamName 		= getConfigParam("portParamName");
		pathparamName 		= getConfigParam("pathparamName");
		defaultTargetServer = getConfigParam("defaultTargetServer");
	}

	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		String protocol = null, server = null, port = null, path = null;
		String queryString = "";
		String requestQueryString = servletRequest.getQueryString();
		if (requestQueryString != null) {
			queryString = "?" + requestQueryString;// no "?" but might have "#"
		}
		int hash = queryString.indexOf('#');
		if (hash >= 0) {
			queryString = queryString.substring(0, hash);
		}
		log.debug("queryString " + queryString);

		List<NameValuePair> pairs;
		try {
			// note: HttpClient 4.2 lets you parse the string without building the URI
			pairs = URLEncodedUtils.parse(new URI(queryString), Charset.forName("UTF-8"));
		} catch (URISyntaxException e) {
			throw new ServletException("Unexpected URI parsing error on " + queryString, e);
		}
		log.debug("pairs " + pairs);

		StringBuilder newQueryBuf = new StringBuilder(queryString.length());
		for (NameValuePair pair : pairs) {
			String name = pair.getName();
			String value = StringUtils.trimToEmpty(pair.getValue());
//			String encodedValue = URLEncoder.encode(value, "UTF-8");

			if (newQueryBuf.length() > 0)
				newQueryBuf.append('&');

			newQueryBuf.append(name).append('=').append(StringUtils.trimToEmpty(value));

			if (StringUtils.equals(name, protocolParamName)) {
				protocol = value;
			}
			if (StringUtils.equals(name, serverParamName)) {
				server = value;
			}
			if (StringUtils.equals(name, portParamName)) {
				port = value;
			}
			if (StringUtils.equals(name, pathparamName)) {
				path = value;
			}

		}
		servletRequest.setAttribute(ATTR_QUERY_STRING, newQueryBuf.toString());
		log.debug("newQueryBuf " + newQueryBuf.toString());

		// Now rewrite the URL
		String newTargetUri = defaultTargetServer;
		if (StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(server) && StringUtils.isNotBlank(port)) {
			newTargetUri = String.format("%s://%s:%s", protocol, server, port);
		}
		servletRequest.setAttribute(ATTR_TARGET_URI, newTargetUri);
		log.debug("newTargetUri " + newTargetUri);

		if (StringUtils.isNotBlank(path)) {
			servletRequest.setAttribute(ATTR_TARGET_PATH, path);
		}

		URI targetUriObj;
		try {
			targetUriObj = new URI(newTargetUri);
		} catch (URISyntaxException e) {
			throw new ServletException("Rewritten targetUri is invalid: " + newTargetUri, e);
		}
		servletRequest.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(targetUriObj));

		super.service(servletRequest, servletResponse);
	}

	@Override
	protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
		return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
	}
}

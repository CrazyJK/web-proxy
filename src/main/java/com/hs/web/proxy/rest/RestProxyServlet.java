package com.hs.web.proxy.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.hs.web.proxy.WebProxyConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestProxyServlet extends HttpServlet {

	private static final long serialVersionUID = WebProxyConfig.serialVersionUID;

	RestTemplate restTemplate;

	String protocolParamName;
	String serverParamName;
	String portParamName;
	String pathparamName;
	String defaultTargetServer;

	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public void init() throws ServletException {
		protocolParamName 	= getServletConfig().getInitParameter("protocolParamName");
		serverParamName 	= getServletConfig().getInitParameter("serverParamName");
		portParamName 		= getServletConfig().getInitParameter("portParamName");
		pathparamName 		= getServletConfig().getInitParameter("pathparamName");
		defaultTargetServer = getServletConfig().getInitParameter("defaultTargetServer");
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String protocol = null, server = null, port = null, path = null;

		MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();

		for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			log.debug("param " + entry.getKey() + " = " + Arrays.toString(entry.getValue()));

			if (StringUtils.equals(entry.getKey(), protocolParamName)) {
				protocol = entry.getValue()[0];
			}
			if (StringUtils.equals(entry.getKey(), serverParamName)) {
				server = entry.getValue()[0];
			}
			if (StringUtils.equals(entry.getKey(), portParamName)) {
				port = entry.getValue()[0];
			}
			if (StringUtils.equals(entry.getKey(), pathparamName)) {
				path = entry.getValue()[0];
			}

			String[] value = entry.getValue();
			if (value != null) {
				if (value.length == 0) {
					requestMap.add(entry.getKey(), "");
				} else if (value.length == 1) {
					requestMap.add(entry.getKey(), entry.getValue()[0]);
				} else if (value.length > 1) {
					for (int i = 0; i < value.length; i++) {
						requestMap.add(entry.getKey(), entry.getValue()[i]);
					}
				}
			} else {
				requestMap.add(entry.getKey(), "");
			}
		}

		String targerServerUri = defaultTargetServer;
		if (StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(server) && StringUtils.isNotBlank(port)) {
			targerServerUri = String.format("%s://%s:%s", protocol, server, port);
		}

		String targetServerPath = request.getPathInfo();
		if (StringUtils.isNotBlank(path)) {
			targetServerPath = path;
		}

		String method = request.getMethod();

		log.debug("rest {} uri: {} --> {}", method, request.getRequestURI(), targerServerUri + targetServerPath);

		ResponseEntity<String> responseEntity = null;
		if (method.equalsIgnoreCase("GET")) {
			responseEntity = restTemplate.getForEntity(targerServerUri + targetServerPath, String.class, requestMap);
		} else if (method.equalsIgnoreCase("POST")) {
			responseEntity = restTemplate.postForEntity(targerServerUri + targetServerPath, requestMap, String.class);
		} else {
			throw new IllegalStateException("NOT support method " + method);
		}

		MediaType contentType = responseEntity.getHeaders().getContentType();
		response.setContentType(contentType.toString());
		log.debug("response contentType: " + contentType.toString());

		String body = responseEntity.getBody();
//		log.debug("response BODY: " + body);

		PrintWriter writer = response.getWriter();
		writer.append(body);
		writer.flush();
	}

}

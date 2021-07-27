package com.hs.web.proxy.rest;

import java.util.Arrays;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class RestController {

	@Autowired
	RestTemplate restTemplate;

	@Value("${proxy.path.rest}")
	String restPath;

	@Value("${proxy.target.default}")
	String defaultTargetServer;

	@Value("${proxy.param.name.path}")
	String pathName;

	@PostConstruct
	public void init() {
		log.debug("RestController: /restController?{}=[target path] -> {}/[target path]", pathName, defaultTargetServer);
	}

	@RequestMapping("/restController")
	@ResponseBody
	public String get(HttpServletRequest request) {
		String urn = request.getParameter(pathName);
		Assert.hasText(urn, "need to " + pathName);
		log.debug("GET - " + defaultTargetServer + urn);

		MultiValueMap<String, String> requestMap = getRequestMap(request);

		String method = request.getMethod();
		ResponseEntity<String> responseEntity = null;
		if (method.equalsIgnoreCase("GET")) {
			responseEntity = restTemplate.getForEntity(defaultTargetServer + urn, String.class, requestMap);
		} else if (method.equalsIgnoreCase("POST")) {
			responseEntity = restTemplate.postForEntity(defaultTargetServer + urn, requestMap, String.class);
		} else {
			throw new IllegalStateException("NOT support method " + method);
		}

		String body = responseEntity.getBody();
		log.debug("response BODY: " + body);

		return body;
	}

	private MultiValueMap<String, String> getRequestMap(HttpServletRequest request) {
		MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();

		for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			log.debug("param " + entry.getKey() + " = " + Arrays.toString(entry.getValue()));

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

		return requestMap;
	}

}

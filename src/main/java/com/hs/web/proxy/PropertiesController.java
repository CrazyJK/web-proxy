package com.hs.web.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
public class PropertiesController {

	@Autowired
	Environment environment;

	@RequestMapping("/get/{key}")
	public String getProperties(@PathVariable String key) {
		return environment.getProperty(key);
	}

}

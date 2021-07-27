package com.hs.web.proxy;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.hs.web.proxy.mod.ModProxyServlet;
import com.hs.web.proxy.mod.ProxyServlet;
import com.hs.web.proxy.rest.RestProxyServlet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebProxyConfig {

	public static final long serialVersionUID = -7981140488413654907L;

	@Value("${proxy.timeout.connect}") 		long timeoutConnect;
	@Value("${proxy.timeout.read}")			long timeoutRead;
	@Value("${proxy.max.conn.total}")		int maxConnTotal;
	@Value("${proxy.max.conn.per.route}") 	int maxConnPerRoute;

	@Value("${proxy.path.mod}")				String modPath;
	@Value("${proxy.path.rest}")			String restPath;

	@Value("${proxy.param.name.protocal}")	String protocolParamName;
	@Value("${proxy.param.name.server}")	String serverParamName;
	@Value("${proxy.param.name.port}")		String portParamName;
	@Value("${proxy.param.name.path}")		String pathparamName;

	@Value("${proxy.target.default}")		String defaultTargetServer;

	@PostConstruct
	public void init() {
		log.debug("timeout: ConnectTimeout=" + timeoutConnect + ", ReadTimeout=" + timeoutRead);
		log.debug("pool size: MaxConnTotal=" + maxConnTotal + ", MaxConnPerRoute=" + maxConnPerRoute);
	}

    @Bean
	public ServletRegistrationBean<ModProxyServlet> getWebProxyServletRegistrationBean() {
		ServletRegistrationBean<ModProxyServlet> registrationBean = new ServletRegistrationBean<>(new ModProxyServlet());
		registrationBean.addUrlMappings(modPath + "/*");
		registrationBean.addInitParameter(ProxyServlet.P_READTIMEOUT, String.valueOf(timeoutRead));
		registrationBean.addInitParameter(ProxyServlet.P_CONNECTTIMEOUT, String.valueOf(timeoutConnect));
		registrationBean.addInitParameter(ProxyServlet.P_MAXCONNECTIONS, String.valueOf(maxConnTotal));
		registrationBean.addInitParameter(ProxyServlet.P_MAXCONNECTIONS_PER_ROUTE, String.valueOf(maxConnPerRoute));
		registrationBean.addInitParameter("protocolParamName", 	 protocolParamName);
		registrationBean.addInitParameter("serverParamName", 	 serverParamName);
		registrationBean.addInitParameter("portParamName", 		 portParamName);
		registrationBean.addInitParameter("pathparamName", 		 pathparamName);
		registrationBean.addInitParameter("defaultTargetServer", defaultTargetServer);

    	log.debug("ModProxyServlet: {}/*", modPath);
    	return registrationBean;
	}

    @Bean
	public ServletRegistrationBean<RestProxyServlet> getRestServletRegistrationBean() {
    	RestProxyServlet restProxyServlet = new RestProxyServlet();
    	restProxyServlet.setRestTemplate(restTemplate(new RestTemplateBuilder()));

		ServletRegistrationBean<RestProxyServlet> registrationBean = new ServletRegistrationBean<>(restProxyServlet);
		registrationBean.addUrlMappings(restPath + "/*");
		registrationBean.addInitParameter("protocolParamName", 	 protocolParamName);
		registrationBean.addInitParameter("serverParamName", 	 serverParamName);
		registrationBean.addInitParameter("portParamName", 		 portParamName);
		registrationBean.addInitParameter("pathparamName", 		 pathparamName);
		registrationBean.addInitParameter("defaultTargetServer", defaultTargetServer);

    	log.debug("RestProxyServlet: {}/*", restPath);
    	return registrationBean;
	}

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    	HttpClient httpClient = HttpClientBuilder.create()
    			.setMaxConnTotal(maxConnTotal)
    			.setMaxConnPerRoute(maxConnPerRoute)
    			.build();

		RestTemplate restTemplate = restTemplateBuilder
    			.requestFactory(() -> new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient)))
    			.setConnectTimeout(Duration.ofMillis(timeoutConnect))
    			.setReadTimeout(Duration.ofMillis(timeoutRead))
    			.build();

		return restTemplate;
    }

}

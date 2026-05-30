package com.codeistari.probe.client.rest;

import com.codeistari.probe.config.RestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.codeistari.probe.config.ForgeClientProperties;
import org.springframework.web.client.RestClient;

@Configuration
public class RestForgeClientConfig {

	@Bean
	RestClient forgeRestClient(RestClientFactory restClientFactory, ForgeClientProperties properties) {
		return restClientFactory.buildRestClient(
				properties.rest().baseUrl(), properties.rest().timeoutMs());
	}
}

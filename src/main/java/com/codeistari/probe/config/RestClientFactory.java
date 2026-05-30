package com.codeistari.probe.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientFactory {

	public RestClient buildRestClient(String baseUrl, int timeoutMillis) {
		JdkClientHttpRequestFactory requestFactory =
				new JdkClientHttpRequestFactory(
						HttpClient.newBuilder()
								.connectTimeout(Duration.ofMillis(timeoutMillis))
								.build());
		requestFactory.setReadTimeout(Duration.ofMillis(timeoutMillis));

		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.defaultHeader("Content-Type", "application/json")
				.defaultHeader("User-Agent", "probe-service/1.0")
				.build();
	}
}

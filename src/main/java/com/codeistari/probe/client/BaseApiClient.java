package com.codeistari.probe.client;

import com.codeistari.probe.exception.ForgeRemoteCallException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class for downstream REST clients. Uses {@code RestClient.exchange()} so 4xx/5xx responses
 * are returned as {@link ResponseEntity} instead of throwing, which keeps error mapping and
 * Resilience4j predicates consistent.
 */
public abstract class BaseApiClient {

	protected final RestClient restClient;
	protected final String serviceName;
	protected final ObjectMapper objectMapper;
	private final Logger log = LoggerFactory.getLogger(getClass());

	protected BaseApiClient(RestClient restClient, String serviceName, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.serviceName = serviceName;
		this.objectMapper = objectMapper;
	}

	protected record HttpResult<T>(HttpStatusCode status, T body, String rawBody) {}

	protected <T> HttpResult<T> get(String uriTemplate, Class<T> responseType, Object... uriVariables) {
		log.info("Calling {} → GET {}", serviceName, uriTemplate);
		try {
			return restClient
					.get()
					.uri(uriTemplate, uriVariables)
					.exchange((request, response) -> toHttpResult(response, responseType));
		} catch (ForgeRemoteCallException ex) {
			throw ex;
		} catch (Exception ex) {
			throw handleNetworkException(ex, "GET", uriTemplate);
		}
	}

	protected <T, R> HttpResult<R> post(
			String uriTemplate, T requestBody, Class<R> responseType, Object... uriVariables) {
		log.info("Calling {} → POST {}", serviceName, uriTemplate);
		try {
			return restClient
					.post()
					.uri(uriTemplate, uriVariables)
					.body(requestBody)
					.exchange((request, response) -> toHttpResult(response, responseType));
		} catch (ForgeRemoteCallException ex) {
			throw ex;
		} catch (Exception ex) {
			throw handleNetworkException(ex, "POST", uriTemplate);
		}
	}

	protected <T> T handleResponse(HttpResult<T> result, String context) {
		if (result.status().is2xxSuccessful()) {
			return result.body();
		}

		if (result.status().is4xxClientError()) {
			throw new ForgeRemoteCallException(
					HttpStatus.valueOf(result.status().value()), extractErrorMessage(result.rawBody(), context));
		}

		if (result.status().is5xxServerError()) {
			throw new ForgeRemoteCallException(
					HttpStatus.BAD_GATEWAY,
					serviceName + " returned HTTP " + result.status().value() + ": " + context);
		}

		throw new ForgeRemoteCallException(
				HttpStatus.BAD_GATEWAY, "Unexpected response from " + serviceName + ": " + context);
	}

	private <T> HttpResult<T> toHttpResult(
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response, Class<T> responseType)
			throws IOException {
		String rawBody = response.bodyTo(String.class);
		T body = null;
		if (response.getStatusCode().is2xxSuccessful()
				&& rawBody != null
				&& !rawBody.isBlank()) {
			body = objectMapper.readValue(rawBody, responseType);
		}
		log.info("{} ← status {}", serviceName, response.getStatusCode().value());
		return new HttpResult<>(response.getStatusCode(), body, rawBody);
	}

	protected String extractErrorMessage(String rawBody, String fallback) {
		if (rawBody == null || rawBody.isBlank()) {
			return fallback;
		}
		try {
			com.codeistari.probe.exception.ErrorResponse error =
					objectMapper.readValue(rawBody, com.codeistari.probe.exception.ErrorResponse.class);
			if (error.message() != null && !error.message().isBlank()) {
				return error.message();
			}
		} catch (Exception ignored) {
			// fall through
		}
		return rawBody;
	}

	private ForgeRemoteCallException handleNetworkException(Exception ex, String method, String endpoint) {
		Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

		if (cause instanceof HttpTimeoutException) {
			log.error("{} network timeout on {} {}", serviceName, method, endpoint);
			return new ForgeRemoteCallException(
					HttpStatus.GATEWAY_TIMEOUT, "Request timeout while calling " + serviceName);
		}

		if (cause instanceof ConnectException) {
			log.error("{} connection failed on {} {}", serviceName, method, endpoint);
			return new ForgeRemoteCallException(
					HttpStatus.BAD_GATEWAY, serviceName + " is unavailable");
		}

		if (cause instanceof IOException) {
			log.error("{} I/O error on {} {}", serviceName, method, endpoint);
			return new ForgeRemoteCallException(
					HttpStatus.BAD_GATEWAY, "Network error while calling " + serviceName);
		}

		log.error("{} unexpected error on {} {}", serviceName, method, endpoint, ex);
		return new ForgeRemoteCallException(
				HttpStatus.BAD_GATEWAY, "Unexpected error calling " + serviceName + ": " + ex.getMessage());
	}
}

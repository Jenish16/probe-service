package com.codeistari.probe.client.grpc;

import com.codeistari.forge.artifact.grpc.proto.CancelLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.CreateLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.GetLoadoutGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.GetLoadoutItemAttemptsGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.LoadoutGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutItemAttemptsGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.LoadoutServiceGrpc;
import com.codeistari.forge.artifact.grpc.proto.RetryLoadoutGrpcRequest;
import com.codeistari.probe.client.LoadoutClient;
import com.codeistari.probe.config.ForgeClientProperties;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.request.ForgeRetryLoadoutRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutAttemptsRestResponse;
import com.codeistari.probe.dto.client.forge.response.ForgeLoadoutRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The {@link LoadoutClient} interface methods here are unused pass-through-shape overloads (same
 * convention as {@code GrpcForgeJobClient}); {@code ProbeLoadoutService} calls the gRPC-specific
 * overloads directly.
 */
@Component
@Qualifier("grpcLoadoutClient")
public class GrpcLoadoutClient implements LoadoutClient {

	private static final Logger log = LoggerFactory.getLogger(GrpcLoadoutClient.class);

	private final LoadoutServiceGrpc.LoadoutServiceBlockingStub blockingStub;
	private final ForgeClientProperties properties;

	public GrpcLoadoutClient(
			LoadoutServiceGrpc.LoadoutServiceBlockingStub blockingStub, ForgeClientProperties properties) {
		this.blockingStub = blockingStub;
		this.properties = properties;
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackCreateLoadoutGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackCreateLoadoutGrpc")
	public LoadoutGrpcResponse createLoadout(CreateLoadoutGrpcRequest request) {
		try {
			return stub().createLoadout(request);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackGetLoadoutGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackGetLoadoutGrpc")
	public LoadoutGrpcResponse getLoadoutGrpc(String loadoutId) {
		try {
			return stub().getLoadout(GetLoadoutGrpcRequest.newBuilder().setLoadoutId(loadoutId).build());
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackCancelLoadoutGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackCancelLoadoutGrpc")
	public LoadoutGrpcResponse cancelLoadoutGrpc(String loadoutId) {
		try {
			return stub().cancelLoadout(CancelLoadoutGrpcRequest.newBuilder().setLoadoutId(loadoutId).build());
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackRetryLoadoutGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackRetryLoadoutGrpc")
	public LoadoutGrpcResponse retryLoadout(RetryLoadoutGrpcRequest request) {
		try {
			return stub().retryLoadout(request);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackGetLoadoutItemAttemptsGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackGetLoadoutItemAttemptsGrpc")
	public LoadoutItemAttemptsGrpcResponse getLoadoutItemAttemptsGrpc(String loadoutId, String loadoutItemId) {
		try {
			return stub()
					.getLoadoutItemAttempts(GetLoadoutItemAttemptsGrpcRequest.newBuilder()
							.setLoadoutId(loadoutId)
							.setLoadoutItemId(loadoutItemId)
							.build());
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@Override
	public ForgeLoadoutRestResponse createLoadout(ForgeCreateLoadoutRestRequest request) {
		throw new UnsupportedOperationException("Use createLoadout(CreateLoadoutGrpcRequest) directly");
	}

	@Override
	public ForgeLoadoutRestResponse getLoadout(String loadoutId) {
		throw new UnsupportedOperationException("Use getLoadoutGrpc(String) directly");
	}

	@Override
	public ForgeLoadoutRestResponse cancelLoadout(String loadoutId) {
		throw new UnsupportedOperationException("Use cancelLoadoutGrpc(String) directly");
	}

	@Override
	public ForgeLoadoutRestResponse retryLoadout(String loadoutId, ForgeRetryLoadoutRestRequest request) {
		throw new UnsupportedOperationException("Use retryLoadout(RetryLoadoutGrpcRequest) directly");
	}

	@Override
	public ForgeLoadoutAttemptsRestResponse getItemAttempts(String loadoutId, String loadoutItemId) {
		throw new UnsupportedOperationException("Use getLoadoutItemAttemptsGrpc(String, String) directly");
	}

	private LoadoutServiceGrpc.LoadoutServiceBlockingStub stub() {
		return blockingStub.withDeadlineAfter(properties.grpc().deadlineMs(), TimeUnit.MILLISECONDS);
	}

	public LoadoutGrpcResponse fallbackCreateLoadoutGrpc(CreateLoadoutGrpcRequest request, Throwable throwable) {
		return rethrowFallback("createLoadout", throwable);
	}

	public LoadoutGrpcResponse fallbackGetLoadoutGrpc(String loadoutId, Throwable throwable) {
		return rethrowFallback("getLoadout(" + loadoutId + ")", throwable);
	}

	public LoadoutGrpcResponse fallbackCancelLoadoutGrpc(String loadoutId, Throwable throwable) {
		return rethrowFallback("cancelLoadout(" + loadoutId + ")", throwable);
	}

	public LoadoutGrpcResponse fallbackRetryLoadoutGrpc(RetryLoadoutGrpcRequest request, Throwable throwable) {
		return rethrowFallback("retryLoadout", throwable);
	}

	public LoadoutItemAttemptsGrpcResponse fallbackGetLoadoutItemAttemptsGrpc(
			String loadoutId, String loadoutItemId, Throwable throwable) {
		return rethrowFallback("getLoadoutItemAttempts(" + loadoutId + ", " + loadoutItemId + ")", throwable);
	}

	private <T> T rethrowFallback(String operation, Throwable throwable) {
		log.error("Resilience fallback triggered for forge-service gRPC {}: {}", operation, throwable.getMessage());
		if (throwable instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new ForgeRemoteCallException(
				HttpStatus.BAD_GATEWAY, "Unexpected error calling forge-service gRPC: " + throwable.getMessage());
	}

	/**
	 * Own gRPC-status-mapping method (not shared with {@code GrpcForgeJobClient}) — mirrors its
	 * cases, plus {@code ALREADY_EXISTS -> 409 Conflict} for the Loadout duplicate-conflict case
	 * (research.md D11, Gate 2 IC-002).
	 */
	private ForgeRemoteCallException mapGrpcException(StatusRuntimeException ex) {
		Status status = ex.getStatus();
		String description =
				status.getDescription() != null && !status.getDescription().isBlank()
						? status.getDescription()
						: status.getCode().name();

		return switch (status.getCode()) {
			case INVALID_ARGUMENT -> new ForgeRemoteCallException(HttpStatus.BAD_REQUEST, description);
			case NOT_FOUND -> new ForgeRemoteCallException(HttpStatus.NOT_FOUND, description);
			case ALREADY_EXISTS -> new ForgeRemoteCallException(HttpStatus.CONFLICT, description);
			case DEADLINE_EXCEEDED ->
					new ForgeRemoteCallException(
							HttpStatus.GATEWAY_TIMEOUT,
							"forge-service gRPC call timed out after " + properties.grpc().deadlineMs() + "ms");
			case INTERNAL, UNAVAILABLE, RESOURCE_EXHAUSTED ->
					new ForgeRemoteCallException(HttpStatus.BAD_GATEWAY, "forge-service internal error: " + description);
			default -> new ForgeRemoteCallException(HttpStatus.BAD_GATEWAY, "forge-service gRPC error: " + description);
		};
	}
}

package com.codeistari.probe.client.grpc;

import com.codeistari.forge.artifact.grpc.proto.ArtifactForgeServiceGrpc;
import com.codeistari.forge.artifact.grpc.proto.CreateForgeJobGrpcRequest;
import com.codeistari.forge.artifact.grpc.proto.ForgeJobGrpcResponse;
import com.codeistari.forge.artifact.grpc.proto.GetForgeJobGrpcRequest;
import com.codeistari.probe.client.ForgeJobClient;
import com.codeistari.probe.config.ForgeClientProperties;
import com.codeistari.probe.dto.client.forge.request.ForgeCreateJobRestRequest;
import com.codeistari.probe.dto.client.forge.response.ForgeJobRestResponse;
import com.codeistari.probe.exception.ForgeRemoteCallException;
import com.codeistari.probe.mapper.ProbeRequestMapper;
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

@Component
@Qualifier("grpcForgeJobClient")
public class GrpcForgeJobClient implements ForgeJobClient {

	private static final Logger log = LoggerFactory.getLogger(GrpcForgeJobClient.class);

	private final ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub blockingStub;
	private final ForgeClientProperties properties;
	private final ProbeRequestMapper mapper;

	public GrpcForgeJobClient(
			ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub blockingStub,
			ForgeClientProperties properties,
			ProbeRequestMapper mapper) {
		this.blockingStub = blockingStub;
		this.properties = properties;
		this.mapper = mapper;
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackCreateForgeJobGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackCreateForgeJobGrpc")
	public ForgeJobGrpcResponse createForgeJob(CreateForgeJobGrpcRequest request) {
		try {
			return blockingStub
					.withDeadlineAfter(properties.grpc().deadlineMs(), TimeUnit.MILLISECONDS)
					.createForgeJob(request);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@CircuitBreaker(name = "FORGE_GRPC", fallbackMethod = "fallbackGetForgeJobGrpc")
	@Retry(name = "FORGE_GRPC", fallbackMethod = "fallbackGetForgeJobGrpc")
	public ForgeJobGrpcResponse getForgeJobGrpc(String forgeJobId) {
		try {
			GetForgeJobGrpcRequest request =
					GetForgeJobGrpcRequest.newBuilder().setForgeJobId(forgeJobId).build();
			return blockingStub
					.withDeadlineAfter(properties.grpc().deadlineMs(), TimeUnit.MILLISECONDS)
					.getForgeJob(request);
		} catch (StatusRuntimeException ex) {
			throw mapGrpcException(ex);
		}
	}

	@Override
	public ForgeJobRestResponse createForgeJob(ForgeCreateJobRestRequest request) {
		CreateForgeJobGrpcRequest grpcRequest =
				CreateForgeJobGrpcRequest.newBuilder()
						.setArtifactName(request.artifactName())
						.setArtifactType(request.artifactType())
						.setMaterial(request.material())
						.setRequestedBy(request.requestedBy())
						.setPowerLevel(request.powerLevel())
						.build();
		return toRestResponse(createForgeJob(grpcRequest));
	}

	@Override
	public ForgeJobRestResponse getForgeJob(String forgeJobId) {
		return toRestResponse(getForgeJobGrpc(forgeJobId));
	}

	public ForgeJobGrpcResponse fallbackCreateForgeJobGrpc(
			CreateForgeJobGrpcRequest request, Throwable throwable) {
		return rethrowFallback("createForgeJob", throwable);
	}

	public ForgeJobGrpcResponse fallbackGetForgeJobGrpc(String forgeJobId, Throwable throwable) {
		return rethrowFallback("getForgeJob(" + forgeJobId + ")", throwable);
	}

	private ForgeJobRestResponse toRestResponse(ForgeJobGrpcResponse response) {
		return new ForgeJobRestResponse(
				response.getForgeJobId(),
				response.getArtifactName(),
				response.getArtifactType(),
				response.getMaterial(),
				response.getRequestedBy(),
				response.getPowerLevel(),
				response.getStatus(),
				mapper.toInstant(response.getCreatedAt()));
	}

	private ForgeJobGrpcResponse rethrowFallback(String operation, Throwable throwable) {
		log.error("Resilience fallback triggered for forge-service gRPC {}: {}", operation, throwable.getMessage());
		if (throwable instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new ForgeRemoteCallException(
				HttpStatus.BAD_GATEWAY,
				"Unexpected error calling forge-service gRPC: " + throwable.getMessage());
	}

	private ForgeRemoteCallException mapGrpcException(StatusRuntimeException ex) {
		Status status = ex.getStatus();
		String description =
				status.getDescription() != null && !status.getDescription().isBlank()
						? status.getDescription()
						: status.getCode().name();

		return switch (status.getCode()) {
			case INVALID_ARGUMENT -> new ForgeRemoteCallException(HttpStatus.BAD_REQUEST, description);
			case NOT_FOUND -> new ForgeRemoteCallException(HttpStatus.NOT_FOUND, description);
			case DEADLINE_EXCEEDED ->
					new ForgeRemoteCallException(
							HttpStatus.GATEWAY_TIMEOUT,
							"forge-service gRPC call timed out after "
									+ properties.grpc().deadlineMs()
									+ "ms");
			case INTERNAL, UNAVAILABLE, RESOURCE_EXHAUSTED ->
					new ForgeRemoteCallException(
							HttpStatus.BAD_GATEWAY, "forge-service internal error: " + description);
			default ->
					new ForgeRemoteCallException(
							HttpStatus.BAD_GATEWAY, "forge-service gRPC error: " + description);
		};
	}
}

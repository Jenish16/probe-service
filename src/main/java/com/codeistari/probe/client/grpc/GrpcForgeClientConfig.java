package com.codeistari.probe.client.grpc;

import com.codeistari.forge.artifact.grpc.proto.ArtifactForgeServiceGrpc;
import com.codeistari.probe.config.ForgeClientProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcForgeClientConfig {

	@Bean(destroyMethod = "shutdown")
	ManagedChannel forgeManagedChannel(ForgeClientProperties properties) {
		ForgeClientProperties.Grpc grpc = properties.grpc();
		return ManagedChannelBuilder.forAddress(grpc.host(), grpc.port()).usePlaintext().build();
	}

	@Bean
	ArtifactForgeServiceGrpc.ArtifactForgeServiceBlockingStub forgeBlockingStub(
			ManagedChannel forgeManagedChannel) {
		return ArtifactForgeServiceGrpc.newBlockingStub(forgeManagedChannel);
	}
}

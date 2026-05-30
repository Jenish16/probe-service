package com.codeistari.probe.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ForgeRemoteCallException extends RuntimeException {

	private final HttpStatus status;

	public ForgeRemoteCallException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}

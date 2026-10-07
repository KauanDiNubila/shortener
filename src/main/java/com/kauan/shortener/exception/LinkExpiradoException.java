package com.kauan.shortener.exception;

public class LinkExpiradoException extends RuntimeException {

	public LinkExpiradoException(String codigo) {
		super("O link '" + codigo + "' expirou");
	}
}

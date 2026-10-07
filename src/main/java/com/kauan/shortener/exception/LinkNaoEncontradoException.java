package com.kauan.shortener.exception;

public class LinkNaoEncontradoException extends RuntimeException {

	public LinkNaoEncontradoException(String codigo) {
		super("Nenhum link encontrado para o código '" + codigo + "'");
	}
}

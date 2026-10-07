package com.kauan.shortener.exception;

public class CodigoIndisponivelException extends RuntimeException {

	public CodigoIndisponivelException(int tentativas) {
		super("Não foi possível gerar um código único após " + tentativas + " tentativas");
	}
}

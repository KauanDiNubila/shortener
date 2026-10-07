package com.kauan.shortener.link;

public class CodigoIndisponivelException extends RuntimeException {

	public CodigoIndisponivelException(int tentativas) {
		super("Não foi possível gerar um código único após " + tentativas + " tentativas");
	}
}

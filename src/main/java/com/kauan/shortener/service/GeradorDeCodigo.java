package com.kauan.shortener.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GeradorDeCodigo {

	public static final int TAMANHO = 7;

	private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private final SecureRandom random = new SecureRandom();

	public String gerar() {
		StringBuilder codigo = new StringBuilder(TAMANHO);
		for (int i = 0; i < TAMANHO; i++) {
			codigo.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
		}
		return codigo.toString();
	}
}

package com.kauan.shortener.link;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeCodigoTest {

	private final GeradorDeCodigo gerador = new GeradorDeCodigo();

	@Test
	void geraCodigoComSeteCaracteres() {
		assertThat(gerador.gerar()).hasSize(7);
	}

	@Test
	void usaSomenteLetrasENumeros() {
		for (int i = 0; i < 1000; i++) {
			assertThat(gerador.gerar()).matches("[A-Za-z0-9]{7}");
		}
	}

	@Test
	void geraCodigosDiferentesACadaChamada() {
		Set<String> codigos = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			codigos.add(gerador.gerar());
		}

		assertThat(codigos).hasSizeGreaterThan(990);
	}
}

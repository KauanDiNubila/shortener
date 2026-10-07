package com.kauan.shortener.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CriarLinkRequestTest {

	private static ValidatorFactory fabrica;
	private static Validator validator;

	@BeforeAll
	static void criarValidator() {
		fabrica = Validation.buildDefaultValidatorFactory();
		validator = fabrica.getValidator();
	}

	@AfterAll
	static void fecharFabrica() {
		fabrica.close();
	}

	private Set<String> camposInvalidos(CriarLinkRequest request) {
		return validator.validate(request).stream()
				.map(violacao -> violacao.getPropertyPath().toString())
				.collect(Collectors.toSet());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://exemplo.com",
			"https://exemplo.com",
			"HTTPS://EXEMPLO.COM/Caminho",
			"https://exemplo.com/artigos/2026?utm_source=news&id=7#secao"
	})
	void aceitaUrlsHttpEHttps(String url) {
		assertThat(camposInvalidos(new CriarLinkRequest(url, null))).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"   ",
			"exemplo.com",
			"ftp://exemplo.com",
			"file:///etc/passwd",
			"javascript:alert(1)",
			"https://exemplo.com/com espaco",
			"https://"
	})
	void recusaUrlsInvalidas(String url) {
		assertThat(camposInvalidos(new CriarLinkRequest(url, null))).containsExactly("url");
	}

	@Test
	void recusaUrlMaiorQue2048Caracteres() {
		String url = "https://exemplo.com/" + "a".repeat(2048);

		assertThat(camposInvalidos(new CriarLinkRequest(url, null))).containsExactly("url");
	}

	@Test
	void aceitaUrlNoLimiteDe2048Caracteres() {
		String url = "https://exemplo.com/" + "a".repeat(2048 - "https://exemplo.com/".length());

		assertThat(url).hasSize(2048);
		assertThat(camposInvalidos(new CriarLinkRequest(url, null))).isEmpty();
	}

	@Test
	void expiracaoEhOpcional() {
		assertThat(camposInvalidos(new CriarLinkRequest("https://exemplo.com", null))).isEmpty();
	}

	@Test
	void aceitaExpiracaoNoFuturo() {
		Instant futuro = Instant.now().plusSeconds(3600);

		assertThat(camposInvalidos(new CriarLinkRequest("https://exemplo.com", futuro))).isEmpty();
	}

	@Test
	void recusaExpiracaoNoPassado() {
		Instant passado = Instant.now().minusSeconds(3600);

		assertThat(camposInvalidos(new CriarLinkRequest("https://exemplo.com", passado))).containsExactly("expiraEm");
	}
}

package com.kauan.shortener.link;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LinkTest {

	private static final Instant AGORA = Instant.parse("2026-01-01T12:00:00Z");

	@Test
	void linkSemExpiracaoNuncaExpira() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", null);

		assertThat(link.estaExpirado(AGORA.plus(Duration.ofDays(36500)))).isFalse();
	}

	@Test
	void linkComExpiracaoNoFuturoNaoEstaExpirado() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", AGORA.plusSeconds(60));

		assertThat(link.estaExpirado(AGORA)).isFalse();
	}

	@Test
	void linkComExpiracaoNoPassadoEstaExpirado() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", AGORA.minusSeconds(60));

		assertThat(link.estaExpirado(AGORA)).isTrue();
	}

	@Test
	void linkExpiraNoExatoInstanteDaExpiracao() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", AGORA);

		assertThat(link.estaExpirado(AGORA)).isTrue();
	}
}

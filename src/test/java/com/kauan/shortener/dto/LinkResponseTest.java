package com.kauan.shortener.dto;

import com.kauan.shortener.entity.Link;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LinkResponseTest {

	@Test
	void copiaOsCamposDoLinkEAcrescentaAUrlCurta() {
		Instant expiracao = Instant.parse("2030-01-01T00:00:00Z");
		Link link = new Link("aB3x9Kq", "https://exemplo.com/artigo", expiracao);

		LinkResponse response = LinkResponse.de(link, "http://localhost:8080/aB3x9Kq");

		assertThat(response.codigo()).isEqualTo("aB3x9Kq");
		assertThat(response.urlCurta()).isEqualTo("http://localhost:8080/aB3x9Kq");
		assertThat(response.urlOriginal()).isEqualTo("https://exemplo.com/artigo");
		assertThat(response.criadoEm()).isEqualTo(link.getCriadoEm());
		assertThat(response.expiraEm()).isEqualTo(expiracao);
	}
}

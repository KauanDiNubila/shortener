package com.kauan.shortener.dto;

import com.kauan.shortener.entity.Link;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LinkEstatisticasResponseTest {

	@Test
	void copiaOsCamposDoLinkIncluindoOsCliques() {
		Instant expiracao = Instant.parse("2030-01-01T00:00:00Z");
		Link link = new Link("aB3x9Kq", "https://exemplo.com/artigo", expiracao);
		ReflectionTestUtils.setField(link, "cliques", 7L);

		LinkEstatisticasResponse response = LinkEstatisticasResponse.de(link, "http://localhost:8080/aB3x9Kq", false);

		assertThat(response.codigo()).isEqualTo("aB3x9Kq");
		assertThat(response.urlCurta()).isEqualTo("http://localhost:8080/aB3x9Kq");
		assertThat(response.urlOriginal()).isEqualTo("https://exemplo.com/artigo");
		assertThat(response.criadoEm()).isEqualTo(link.getCriadoEm());
		assertThat(response.expiraEm()).isEqualTo(expiracao);
		assertThat(response.expirado()).isFalse();
		assertThat(response.cliques()).isEqualTo(7);
	}

	@Test
	void repassaAFlagDeExpirado() {
		Link link = new Link("velho12", "https://exemplo.com", Instant.parse("2020-01-01T00:00:00Z"));

		assertThat(LinkEstatisticasResponse.de(link, "http://localhost/velho12", true).expirado()).isTrue();
	}
}

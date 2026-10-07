package com.kauan.shortener.link;

import com.kauan.shortener.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LinkRepositoryTest {

	@Autowired
	private LinkRepository repository;

	@Test
	void salvaELeLinkPeloCodigo() {
		repository.saveAndFlush(new Link("aB3x9Kq", "https://exemplo.com/artigo", null));

		Link encontrado = repository.findByCodigo("aB3x9Kq").orElseThrow();

		assertThat(encontrado.getId()).isNotNull();
		assertThat(encontrado.getUrlOriginal()).isEqualTo("https://exemplo.com/artigo");
		assertThat(encontrado.getCriadoEm()).isNotNull();
		assertThat(encontrado.getExpiraEm()).isNull();
		assertThat(encontrado.getCliques()).isZero();
	}

	@Test
	void guardaADataDeExpiracao() {
		Instant expiracao = Instant.parse("2030-05-10T08:30:00Z");
		repository.saveAndFlush(new Link("expira1", "https://exemplo.com", expiracao));

		Link encontrado = repository.findByCodigo("expira1").orElseThrow();

		assertThat(encontrado.getExpiraEm()).isEqualTo(expiracao);
	}

	@Test
	void devolveVazioQuandoOCodigoNaoExiste() {
		assertThat(repository.findByCodigo("naoExiste")).isEmpty();
	}

	@Test
	void existsByCodigoRefleteAExistencia() {
		repository.saveAndFlush(new Link("existe1", "https://exemplo.com", null));

		assertThat(repository.existsByCodigo("existe1")).isTrue();
		assertThat(repository.existsByCodigo("outro12")).isFalse();
	}

	@Test
	void bancoRecusaCodigoDuplicado() {
		repository.saveAndFlush(new Link("duplic1", "https://exemplo.com/a", null));

		assertThatThrownBy(() -> repository.saveAndFlush(new Link("duplic1", "https://exemplo.com/b", null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void codigosDiferenciamMaiusculasDeMinusculas() {
		repository.saveAndFlush(new Link("AbCdEfG", "https://exemplo.com/a", null));
		repository.saveAndFlush(new Link("abcdefg", "https://exemplo.com/b", null));

		assertThat(repository.findByCodigo("AbCdEfG").orElseThrow().getUrlOriginal()).isEqualTo("https://exemplo.com/a");
		assertThat(repository.findByCodigo("abcdefg").orElseThrow().getUrlOriginal()).isEqualTo("https://exemplo.com/b");
	}
}

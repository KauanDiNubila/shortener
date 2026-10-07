package com.kauan.shortener.service;

import com.kauan.shortener.entity.Link;
import com.kauan.shortener.repository.LinkRepository;
import com.kauan.shortener.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LinkServiceIntegrationTest {

	@Autowired
	private LinkService service;

	@Autowired
	private LinkRepository repository;

	@MockitoBean
	private GeradorDeCodigo gerador;

	@Test
	void tentaNovamenteQuandoOBancoRecusaOCodigoDuplicado() {
		repository.saveAndFlush(new Link("colide1", "https://exemplo.com/existente", null));
		when(gerador.gerar()).thenReturn("colide1", "colide1", "livre12");

		Link criado = service.criar("https://exemplo.com/novo", null);

		assertThat(criado.getCodigo()).isEqualTo("livre12");
		assertThat(repository.findByCodigo("colide1").orElseThrow().getUrlOriginal())
				.isEqualTo("https://exemplo.com/existente");
		assertThat(repository.findByCodigo("livre12")).isPresent();
	}
}

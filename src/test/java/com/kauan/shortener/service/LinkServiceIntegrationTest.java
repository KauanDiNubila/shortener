package com.kauan.shortener.service;

import com.kauan.shortener.TestcontainersConfiguration;
import com.kauan.shortener.entity.Link;
import com.kauan.shortener.exception.LinkExpiradoException;
import com.kauan.shortener.exception.LinkNaoEncontradoException;
import com.kauan.shortener.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	@Test
	void buscaUmLinkValidoNoBanco() {
		repository.saveAndFlush(new Link("valido1", "https://exemplo.com/valido", Instant.now().plusSeconds(3600)));

		Link encontrado = service.redirecionar("valido1");

		assertThat(encontrado.getUrlOriginal()).isEqualTo("https://exemplo.com/valido");
	}

	@Test
	void cadaRedirecionamentoValidoSomaUmCliqueNoBanco() {
		repository.saveAndFlush(new Link("contado", "https://exemplo.com/contado", null));

		service.redirecionar("contado");
		service.redirecionar("contado");
		service.redirecionar("contado");

		assertThat(repository.findByCodigo("contado").orElseThrow().getCliques()).isEqualTo(3);
	}

	@Test
	void redirecionamentoRecusadoNaoContaClique() {
		repository.saveAndFlush(new Link("semclic", "https://exemplo.com/velho", Instant.now().minusSeconds(3600)));

		assertThatThrownBy(() -> service.redirecionar("semclic")).isInstanceOf(LinkExpiradoException.class);

		assertThat(repository.findByCodigo("semclic").orElseThrow().getCliques()).isZero();
	}

	@Test
	void recusaUmLinkExpiradoNoBanco() {
		repository.saveAndFlush(new Link("velho99", "https://exemplo.com/velho", Instant.now().minusSeconds(3600)));

		assertThatThrownBy(() -> service.redirecionar("velho99"))
				.isInstanceOf(LinkExpiradoException.class);
	}

	@Test
	void recusaUmCodigoQueNaoExisteNoBanco() {
		assertThatThrownBy(() -> service.redirecionar("fantasm"))
				.isInstanceOf(LinkNaoEncontradoException.class);
	}
}

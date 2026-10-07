package com.kauan.shortener.service;

import com.kauan.shortener.entity.Link;
import com.kauan.shortener.exception.CodigoIndisponivelException;
import com.kauan.shortener.exception.LinkExpiradoException;
import com.kauan.shortener.exception.LinkNaoEncontradoException;
import com.kauan.shortener.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

	private static final Instant AGORA = Instant.parse("2026-01-01T12:00:00Z");

	@Mock
	private LinkRepository repository;

	@Mock
	private GeradorDeCodigo gerador;

	private LinkService service;

	@BeforeEach
	void criarServico() {
		service = new LinkService(repository, gerador, Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	@Test
	void salvaNaPrimeiraTentativaQuandoNaoHaColisao() {
		Instant expiracao = Instant.parse("2030-01-01T00:00:00Z");
		when(gerador.gerar()).thenReturn("aB3x9Kq");
		when(repository.saveAndFlush(any(Link.class))).thenAnswer(i -> i.getArgument(0));

		Link link = service.criar("https://exemplo.com", expiracao);

		assertThat(link.getCodigo()).isEqualTo("aB3x9Kq");
		assertThat(link.getUrlOriginal()).isEqualTo("https://exemplo.com");
		assertThat(link.getExpiraEm()).isEqualTo(expiracao);
		verify(gerador, times(1)).gerar();
	}

	@Test
	void geraOutroCodigoQuandoHaColisao() {
		when(gerador.gerar()).thenReturn("colide1", "livre12");
		when(repository.saveAndFlush(any(Link.class)))
				.thenThrow(new DataIntegrityViolationException("duplicado"))
				.thenAnswer(i -> i.getArgument(0));

		Link link = service.criar("https://exemplo.com", null);

		assertThat(link.getCodigo()).isEqualTo("livre12");
		verify(gerador, times(2)).gerar();
	}

	@Test
	void desistePosEsgotarAsTentativas() {
		when(gerador.gerar()).thenReturn("colide1");
		when(repository.saveAndFlush(any(Link.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

		assertThatThrownBy(() -> service.criar("https://exemplo.com", null))
				.isInstanceOf(CodigoIndisponivelException.class);

		verify(gerador, times(LinkService.MAX_TENTATIVAS)).gerar();
	}

	@Test
	void contaUmCliqueQuandoORedirecionamentoEhValido() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", null);
		when(repository.findByCodigo("aB3x9Kq")).thenReturn(Optional.of(link));

		service.redirecionar("aB3x9Kq");

		verify(repository, times(1)).incrementarCliques("aB3x9Kq");
	}

	@Test
	void naoContaCliqueQuandoOCodigoNaoExiste() {
		when(repository.findByCodigo("naoExiste")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.redirecionar("naoExiste"))
				.isInstanceOf(LinkNaoEncontradoException.class);

		verify(repository, never()).incrementarCliques(any());
	}

	@Test
	void naoContaCliqueQuandoOLinkExpirou() {
		Link link = new Link("velho12", "https://exemplo.com", AGORA.minusSeconds(1));
		when(repository.findByCodigo("velho12")).thenReturn(Optional.of(link));

		assertThatThrownBy(() -> service.redirecionar("velho12"))
				.isInstanceOf(LinkExpiradoException.class);

		verify(repository, never()).incrementarCliques(any());
	}

	@Test
	void devolveOLinkQuandoNaoTemExpiracao() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", null);
		when(repository.findByCodigo("aB3x9Kq")).thenReturn(Optional.of(link));

		assertThat(service.redirecionar("aB3x9Kq")).isSameAs(link);
	}

	@Test
	void devolveOLinkQuandoAindaNaoExpirou() {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", AGORA.plusSeconds(1));
		when(repository.findByCodigo("aB3x9Kq")).thenReturn(Optional.of(link));

		assertThat(service.redirecionar("aB3x9Kq")).isSameAs(link);
	}

	@Test
	void lancaNaoEncontradoQuandoOCodigoNaoExiste() {
		when(repository.findByCodigo("naoExiste")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.redirecionar("naoExiste"))
				.isInstanceOf(LinkNaoEncontradoException.class)
				.hasMessageContaining("naoExiste");
	}

	@Test
	void lancaExpiradoQuandoAExpiracaoJaPassou() {
		Link link = new Link("velho12", "https://exemplo.com", AGORA.minusSeconds(1));
		when(repository.findByCodigo("velho12")).thenReturn(Optional.of(link));

		assertThatThrownBy(() -> service.redirecionar("velho12"))
				.isInstanceOf(LinkExpiradoException.class)
				.hasMessageContaining("velho12");
	}

	@Test
	void lancaExpiradoNoExatoInstanteDaExpiracao() {
		Link link = new Link("limite1", "https://exemplo.com", AGORA);
		when(repository.findByCodigo("limite1")).thenReturn(Optional.of(link));

		assertThatThrownBy(() -> service.redirecionar("limite1"))
				.isInstanceOf(LinkExpiradoException.class);
	}
}

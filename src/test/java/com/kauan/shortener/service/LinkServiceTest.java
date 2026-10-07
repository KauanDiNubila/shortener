package com.kauan.shortener.service;

import com.kauan.shortener.entity.Link;
import com.kauan.shortener.exception.CodigoIndisponivelException;
import com.kauan.shortener.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

	@Mock
	private LinkRepository repository;

	@Mock
	private GeradorDeCodigo gerador;

	@InjectMocks
	private LinkService service;

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
}

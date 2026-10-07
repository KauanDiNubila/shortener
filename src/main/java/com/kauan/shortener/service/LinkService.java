package com.kauan.shortener.service;

import com.kauan.shortener.entity.Link;
import com.kauan.shortener.exception.CodigoIndisponivelException;
import com.kauan.shortener.exception.LinkExpiradoException;
import com.kauan.shortener.exception.LinkNaoEncontradoException;
import com.kauan.shortener.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

	static final int MAX_TENTATIVAS = 5;

	private final LinkRepository repository;
	private final GeradorDeCodigo gerador;
	private final Clock clock;

	public Link criar(String urlOriginal, Instant expiraEm) {
		for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
			try {
				return repository.saveAndFlush(new Link(gerador.gerar(), urlOriginal, expiraEm));
			} catch (DataIntegrityViolationException e) {
				log.warn("Colisão de código na tentativa {} de {}", tentativa, MAX_TENTATIVAS);
			}
		}
		throw new CodigoIndisponivelException(MAX_TENTATIVAS);
	}

	public Link redirecionar(String codigo) {
		Link link = repository.findByCodigo(codigo)
				.orElseThrow(() -> new LinkNaoEncontradoException(codigo));

		if (link.estaExpirado(clock.instant())) {
			throw new LinkExpiradoException(codigo);
		}

		repository.incrementarCliques(codigo);
		return link;
	}
}

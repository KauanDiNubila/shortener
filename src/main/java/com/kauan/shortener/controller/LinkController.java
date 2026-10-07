package com.kauan.shortener.controller;

import com.kauan.shortener.dto.CriarLinkRequest;
import com.kauan.shortener.dto.LinkEstatisticasResponse;
import com.kauan.shortener.dto.LinkResponse;
import com.kauan.shortener.entity.Link;
import com.kauan.shortener.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class LinkController {

	private static final String CODIGO = "{codigo:[A-Za-z0-9_-]{1,32}}";

	private final LinkService service;

	@PostMapping("/links")
	public ResponseEntity<LinkResponse> criar(@Valid @RequestBody CriarLinkRequest request) {
		Link link = service.criar(request.url(), request.expiraEm());
		URI urlCurta = montarUrlCurta(link.getCodigo());

		return ResponseEntity.created(urlCurta).body(LinkResponse.de(link, urlCurta.toString()));
	}

	@GetMapping("/links/" + CODIGO)
	public LinkEstatisticasResponse consultar(@PathVariable String codigo) {
		Link link = service.consultar(codigo);

		return LinkEstatisticasResponse.de(link, montarUrlCurta(codigo).toString(), service.estaExpirado(link));
	}

	@GetMapping("/" + CODIGO)
	public ResponseEntity<Void> redirecionar(@PathVariable String codigo) {
		Link link = service.redirecionar(codigo);

		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(link.getUrlOriginal()))
				.cacheControl(CacheControl.noStore())
				.build();
	}

	private URI montarUrlCurta(String codigo) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/{codigo}")
				.buildAndExpand(codigo)
				.toUri();
	}
}

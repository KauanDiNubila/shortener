package com.kauan.shortener.dto;

import com.kauan.shortener.entity.Link;

import java.time.Instant;

public record LinkEstatisticasResponse(
		String codigo,
		String urlCurta,
		String urlOriginal,
		Instant criadoEm,
		Instant expiraEm,
		boolean expirado,
		long cliques
) {

	public static LinkEstatisticasResponse de(Link link, String urlCurta, boolean expirado) {
		return new LinkEstatisticasResponse(
				link.getCodigo(),
				urlCurta,
				link.getUrlOriginal(),
				link.getCriadoEm(),
				link.getExpiraEm(),
				expirado,
				link.getCliques()
		);
	}
}

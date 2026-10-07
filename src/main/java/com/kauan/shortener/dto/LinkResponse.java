package com.kauan.shortener.dto;

import com.kauan.shortener.entity.Link;

import java.time.Instant;

public record LinkResponse(
		String codigo,
		String urlCurta,
		String urlOriginal,
		Instant criadoEm,
		Instant expiraEm
) {

	public static LinkResponse de(Link link, String urlCurta) {
		return new LinkResponse(
				link.getCodigo(),
				urlCurta,
				link.getUrlOriginal(),
				link.getCriadoEm(),
				link.getExpiraEm()
		);
	}
}

package com.kauan.shortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CriarLinkRequest(

		@NotBlank(message = "é obrigatória")
		@Size(max = 2048, message = "deve ter no máximo 2048 caracteres")
		@Pattern(regexp = "(?i)^https?://\\S+$", message = "deve começar com http:// ou https://, sem espaços")
		String url,

		@Future(message = "deve estar no futuro")
		Instant expiraEm
) {
}

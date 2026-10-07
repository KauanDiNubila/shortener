package com.kauan.shortener.exception;

import com.kauan.shortener.dto.CriarLinkRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

	private MockMvc mockMvc;

	@RestController
	static class ControllerDeTeste {

		@GetMapping("/teste/nao-encontrado")
		void naoEncontrado() {
			throw new LinkNaoEncontradoException("abc1234");
		}

		@GetMapping("/teste/expirado")
		void expirado() {
			throw new LinkExpiradoException("abc1234");
		}

		@GetMapping("/teste/indisponivel")
		void indisponivel() {
			throw new CodigoIndisponivelException(5);
		}

		@PostMapping("/teste/validar")
		void validar(@Valid @RequestBody CriarLinkRequest request) {
		}
	}

	@BeforeEach
	void configurar() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void linkNaoEncontradoViraProblemDetail404() throws Exception {
		mockMvc.perform(get("/teste/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Link não encontrado"))
				.andExpect(jsonPath("$.detail").value("Nenhum link encontrado para o código 'abc1234'"))
				.andExpect(jsonPath("$.instance").value("/teste/nao-encontrado"));
	}

	@Test
	void linkExpiradoViraProblemDetail410() throws Exception {
		mockMvc.perform(get("/teste/expirado"))
				.andExpect(status().isGone())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(410))
				.andExpect(jsonPath("$.title").value("Link expirado"));
	}

	@Test
	void codigoIndisponivelViraProblemDetail503SemVazarDetalhesInternos() throws Exception {
		mockMvc.perform(get("/teste/indisponivel"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.detail").value("Não foi possível criar o link agora. Tente novamente."));
	}

	@Test
	void validacaoFalhaViraProblemDetail400ComOsErrosPorCampo() throws Exception {
		String corpo = """
				{"url": "ftp://exemplo.com", "expiraEm": "2020-01-01T00:00:00Z"}
				""";

		mockMvc.perform(post("/teste/validar").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.erros.url").value("deve começar com http:// ou https://, sem espaços"))
				.andExpect(jsonPath("$.erros.expiraEm").value("deve estar no futuro"))
				.andExpect(jsonPath("$.instance").value("/teste/validar"));
	}

	@Test
	void jsonMalFormadoViraProblemDetail400() throws Exception {
		mockMvc.perform(post("/teste/validar").contentType(MediaType.APPLICATION_JSON).content("{ url:"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}
}

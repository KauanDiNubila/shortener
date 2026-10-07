package com.kauan.shortener.controller;

import com.kauan.shortener.entity.Link;
import com.kauan.shortener.exception.CodigoIndisponivelException;
import com.kauan.shortener.exception.LinkExpiradoException;
import com.kauan.shortener.exception.LinkNaoEncontradoException;
import com.kauan.shortener.service.LinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkController.class)
class LinkControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService service;

	@Test
	void criaOLinkEDevolve201ComLocationEOCorpo() throws Exception {
		Instant expiracao = Instant.parse("2030-01-01T00:00:00Z");
		Link link = new Link("aB3x9Kq", "https://exemplo.com/artigo", expiracao);
		when(service.criar("https://exemplo.com/artigo", expiracao)).thenReturn(link);

		String corpo = """
				{"url": "https://exemplo.com/artigo", "expiraEm": "2030-01-01T00:00:00Z"}
				""";

		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/aB3x9Kq"))
				.andExpect(jsonPath("$.codigo").value("aB3x9Kq"))
				.andExpect(jsonPath("$.urlCurta").value("http://localhost/aB3x9Kq"))
				.andExpect(jsonPath("$.urlOriginal").value("https://exemplo.com/artigo"))
				.andExpect(jsonPath("$.expiraEm").value("2030-01-01T00:00:00Z"))
				.andExpect(jsonPath("$.criadoEm").isString());

		verify(service).criar("https://exemplo.com/artigo", expiracao);
	}

	@Test
	void criaOLinkSemExpiracao() throws Exception {
		Link link = new Link("aB3x9Kq", "https://exemplo.com", null);
		when(service.criar("https://exemplo.com", null)).thenReturn(link);

		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"https://exemplo.com\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.expiraEm").isEmpty());
	}

	@Test
	void recusaUrlInvalidaSemChamarOServico() throws Exception {
		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"ftp://exemplo.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.erros.url").exists());

		verifyNoInteractions(service);
	}

	@Test
	void recusaCorpoSemUrl() throws Exception {
		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.url").exists());

		verifyNoInteractions(service);
	}

	@Test
	void recusaExpiracaoNoPassado() throws Exception {
		String corpo = """
				{"url": "https://exemplo.com", "expiraEm": "2020-01-01T00:00:00Z"}
				""";

		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.expiraEm").exists());

		verifyNoInteractions(service);
	}

	@Test
	void devolve503QuandoNaoConseguirGerarCodigo() throws Exception {
		when(service.criar(any(), any())).thenThrow(new CodigoIndisponivelException(5));

		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"https://exemplo.com\"}"))
				.andExpect(status().isServiceUnavailable());
	}

	@Test
	void redirecionaCom302ParaAUrlOriginal() throws Exception {
		Link link = new Link("aB3x9Kq", "https://exemplo.com/artigo?id=7", null);
		when(service.redirecionar("aB3x9Kq")).thenReturn(link);

		mockMvc.perform(get("/aB3x9Kq"))
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, "https://exemplo.com/artigo?id=7"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(content().string(""));
	}

	@Test
	void devolve404QuandoOCodigoNaoExiste() throws Exception {
		when(service.redirecionar("naoExiste")).thenThrow(new LinkNaoEncontradoException("naoExiste"));

		mockMvc.perform(get("/naoExiste"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Link não encontrado"));
	}

	@Test
	void devolve410QuandoOLinkExpirou() throws Exception {
		when(service.redirecionar("velho12")).thenThrow(new LinkExpiradoException("velho12"));

		mockMvc.perform(get("/velho12"))
				.andExpect(status().isGone())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Link expirado"));
	}

	@Test
	void naoConsultaOBancoParaCodigosComFormatoInvalido() throws Exception {
		mockMvc.perform(get("/favicon.ico"))
				.andExpect(status().isNotFound());

		verifyNoInteractions(service);
	}
}

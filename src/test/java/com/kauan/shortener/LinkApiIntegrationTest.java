package com.kauan.shortener;

import com.kauan.shortener.entity.Link;
import com.kauan.shortener.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LinkApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LinkRepository repository;

	private String criarLink(String url) throws Exception {
		String corpo = "{\"url\": \"" + url + "\"}";

		String location = mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION);

		return location.substring(location.lastIndexOf('/') + 1);
	}

	@Test
	void criaUmLinkEDepoisRedirecionaParaAUrlOriginal() throws Exception {
		String codigo = criarLink("https://exemplo.com/artigos/kubernetes?id=7");

		assertThat(codigo).matches("[A-Za-z0-9]{7}");

		mockMvc.perform(get("/" + codigo))
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, "https://exemplo.com/artigos/kubernetes?id=7"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void cadaVisitaAoLinkCurtoSomaUmClique() throws Exception {
		String codigo = criarLink("https://exemplo.com/visitado");
		assertThat(repository.findByCodigo(codigo).orElseThrow().getCliques()).isZero();

		mockMvc.perform(get("/" + codigo)).andExpect(status().isFound());
		mockMvc.perform(get("/" + codigo)).andExpect(status().isFound());

		assertThat(repository.findByCodigo(codigo).orElseThrow().getCliques()).isEqualTo(2);
	}

	@Test
	void consultaMostraOsCliquesSemContarComoClique() throws Exception {
		String codigo = criarLink("https://exemplo.com/estatisticas");
		mockMvc.perform(get("/" + codigo)).andExpect(status().isFound());
		mockMvc.perform(get("/" + codigo)).andExpect(status().isFound());
		mockMvc.perform(get("/" + codigo)).andExpect(status().isFound());

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(get("/links/" + codigo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.codigo").value(codigo))
					.andExpect(jsonPath("$.urlOriginal").value("https://exemplo.com/estatisticas"))
					.andExpect(jsonPath("$.expirado").value(false))
					.andExpect(jsonPath("$.cliques").value(3));
		}
		assertThat(repository.findByCodigo(codigo).orElseThrow().getCliques()).isEqualTo(3);
	}

	@Test
	void consultaDeUmLinkExpiradoAindaMostraOsDadosEMarcaComoExpirado() throws Exception {
		repository.saveAndFlush(new Link("expstat", "https://exemplo.com/velho", Instant.now().minusSeconds(60)));

		mockMvc.perform(get("/links/expstat"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.expirado").value(true))
				.andExpect(jsonPath("$.cliques").value(0));
	}

	@Test
	void consultaDeUmCodigoInexistenteDevolve404() throws Exception {
		mockMvc.perform(get("/links/zzzzzzz"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Link não encontrado"));
	}

	@Test
	void visitaAUmLinkExpiradoNaoSomaClique() throws Exception {
		repository.saveAndFlush(new Link("expcont", "https://exemplo.com/velho", Instant.now().minusSeconds(60)));

		mockMvc.perform(get("/expcont")).andExpect(status().isGone());

		assertThat(repository.findByCodigo("expcont").orElseThrow().getCliques()).isZero();
	}

	@Test
	void aMesmaUrlGeraCodigosDiferentes() throws Exception {
		String primeiro = criarLink("https://exemplo.com/repetida");
		String segundo = criarLink("https://exemplo.com/repetida");

		assertThat(primeiro).isNotEqualTo(segundo);
	}

	@Test
	void guardaAExpiracaoInformadaEAindaRedirecionaAntesDelaChegar() throws Exception {
		Instant expiracao = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
		String corpo = "{\"url\": \"https://exemplo.com/temporario\", \"expiraEm\": \"" + expiracao + "\"}";

		String location = mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.expiraEm").exists())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION);
		String codigo = location.substring(location.lastIndexOf('/') + 1);

		mockMvc.perform(get("/" + codigo)).andExpect(status().isFound());
		assertThat(repository.findByCodigo(codigo).orElseThrow().getExpiraEm()).isEqualTo(expiracao);
	}

	@Test
	void devolve410ParaUmLinkQueJaExpirou() throws Exception {
		repository.saveAndFlush(new Link("expirad", "https://exemplo.com/velho", Instant.now().minusSeconds(60)));

		mockMvc.perform(get("/expirad"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.title").value("Link expirado"));
	}

	@Test
	void devolve404ParaUmCodigoQueNaoExiste() throws Exception {
		mockMvc.perform(get("/zzzzzzz"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Link não encontrado"));
	}

	@Test
	void naoGravaNadaQuandoOsDadosSaoInvalidos() throws Exception {
		long antes = repository.count();

		mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"javascript:alert(1)\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.url").exists());

		assertThat(repository.count()).isEqualTo(antes);
	}
}

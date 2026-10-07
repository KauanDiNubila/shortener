package com.kauan.shortener.repository;

import com.kauan.shortener.TestcontainersConfiguration;
import com.kauan.shortener.entity.Link;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LinkRepositoryConcorrenciaTest {

	private static final int THREADS = 20;
	private static final int CLIQUES_POR_THREAD = 25;

	@Autowired
	private LinkRepository repository;

	@Test
	void nenhumCliqueSePerdeQuandoVariasThreadsIncrementamAoMesmoTempo() throws Exception {
		repository.saveAndFlush(new Link("concorr", "https://exemplo.com", null));

		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<?>> tarefas = new ArrayList<>();

		for (int t = 0; t < THREADS; t++) {
			tarefas.add(pool.submit(() -> {
				largada.await();
				for (int i = 0; i < CLIQUES_POR_THREAD; i++) {
					repository.incrementarCliques("concorr");
				}
				return null;
			}));
		}

		largada.countDown();
		for (Future<?> tarefa : tarefas) {
			tarefa.get();
		}
		pool.shutdown();

		long esperado = (long) THREADS * CLIQUES_POR_THREAD;
		assertThat(repository.findByCodigo("concorr").orElseThrow().getCliques()).isEqualTo(esperado);
	}
}

package com.kauan.shortener.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false, length = 32)
	private String codigo;

	@Column(nullable = false, updatable = false, length = 2048)
	private String urlOriginal;

	@Column(nullable = false, updatable = false)
	private Instant criadoEm;

	private Instant expiraEm;

	@Column(nullable = false)
	private long cliques;

	public Link(String codigo, String urlOriginal, Instant expiraEm) {
		this.codigo = codigo;
		this.urlOriginal = urlOriginal;
		this.expiraEm = expiraEm;
		this.criadoEm = Instant.now();
	}

	public boolean estaExpirado(Instant agora) {
		return expiraEm != null && !expiraEm.isAfter(agora);
	}
}

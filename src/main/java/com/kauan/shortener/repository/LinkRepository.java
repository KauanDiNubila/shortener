package com.kauan.shortener.repository;

import com.kauan.shortener.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

	Optional<Link> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);
}

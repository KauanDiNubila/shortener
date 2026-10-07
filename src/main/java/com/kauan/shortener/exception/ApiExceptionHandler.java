package com.kauan.shortener.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(LinkNaoEncontradoException.class)
	ProblemDetail linkNaoEncontrado(LinkNaoEncontradoException e) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
		problema.setTitle("Link não encontrado");
		return problema;
	}

	@ExceptionHandler(LinkExpiradoException.class)
	ProblemDetail linkExpirado(LinkExpiradoException e) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage());
		problema.setTitle("Link expirado");
		return problema;
	}

	@ExceptionHandler(CodigoIndisponivelException.class)
	ProblemDetail codigoIndisponivel(CodigoIndisponivelException e) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível criar o link agora. Tente novamente.");
		problema.setTitle("Serviço temporariamente indisponível");
		return problema;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> erros = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(erro -> erros.merge(erro.getField(), erro.getDefaultMessage(), (a, b) -> a + "; " + b));

		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Os dados enviados são inválidos");
		problema.setTitle("Requisição inválida");
		problema.setProperty("erros", erros);
		return handleExceptionInternal(e, problema, headers, status, request);
	}
}

package com.autobots.automanager.controles;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Violação de restrição do banco (ex.: número de documento repetido) vira 409 em vez de 500.
// O sendError reaproveita o corpo de erro padrão do Spring Boot, o mesmo das demais respostas de erro.
@RestControllerAdvice
public class TratadorConflito {

	@ExceptionHandler(DataIntegrityViolationException.class)
	public void tratar(DataIntegrityViolationException excecao, HttpServletResponse resposta) throws IOException {
		resposta.sendError(HttpStatus.CONFLICT.value(), "Os dados conflitam com um registro existente (ex.: número de documento já cadastrado)");
	}
}

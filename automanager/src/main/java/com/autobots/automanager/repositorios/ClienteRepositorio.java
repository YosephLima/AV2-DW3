package com.autobots.automanager.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autobots.automanager.entidades.Cliente;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
	// Documento, Endereco e Telefone não referenciam o cliente, então o dono é encontrado a partir dele
	Optional<Cliente> findByDocumentosId(Long id);

	Optional<Cliente> findByEnderecoId(Long id);

	Optional<Cliente> findByTelefonesId(Long id);
}

package com.autobots.automanager.controles;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.modelos.AdicionadorLinkTelefone;
import com.autobots.automanager.modelos.TelefoneAtualizador;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import com.autobots.automanager.repositorios.TelefoneRepositorio;

@RestController
public class TelefoneControle {
	@Autowired
	private TelefoneRepositorio repositorio;
	@Autowired
	private ClienteRepositorio clienteRepositorio;
	@Autowired
	private AdicionadorLinkTelefone adicionadorLink;

	private Telefone buscar(long id) {
		return repositorio.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telefone " + id + " não encontrado"));
	}

	@GetMapping("/telefones/{id}")
	public ResponseEntity<Telefone> obterTelefone(@PathVariable long id) {
		Telefone telefone = buscar(id);
		adicionadorLink.adicionarLink(telefone);
		return new ResponseEntity<>(telefone, HttpStatus.OK);
	}

	@GetMapping("/telefones")
	public ResponseEntity<CollectionModel<Telefone>> obterTelefones() {
		List<Telefone> telefones = repositorio.findAll();
		adicionadorLink.adicionarLink(telefones);
		CollectionModel<Telefone> colecao = CollectionModel.of(telefones,
				linkTo(methodOn(TelefoneControle.class).obterTelefones()).withSelfRel());
		return new ResponseEntity<>(colecao, HttpStatus.OK);
	}

	// A criação fica aninhada no cliente porque Telefone só existe vinculado a um cliente
	@PostMapping("/clientes/{clienteId}/telefones")
	public ResponseEntity<Telefone> cadastrarTelefone(@PathVariable long clienteId, @RequestBody Telefone telefone) {
		if (telefone.getId() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "O id é gerado pelo sistema e não deve ser enviado no cadastro");
		}
		Cliente cliente = clienteRepositorio.findById(clienteId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente " + clienteId + " não encontrado"));
		Telefone salvo = repositorio.save(telefone);
		cliente.getTelefones().add(salvo);
		clienteRepositorio.save(cliente);
		adicionadorLink.adicionarLink(salvo, clienteId);
		return ResponseEntity.created(salvo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(salvo);
	}

	@PutMapping("/telefones/{id}")
	public ResponseEntity<Telefone> atualizarTelefone(@PathVariable long id, @RequestBody Telefone atualizacao) {
		Telefone telefone = buscar(id);
		TelefoneAtualizador atualizador = new TelefoneAtualizador();
		atualizador.atualizar(telefone, atualizacao);
		Telefone salvo = repositorio.save(telefone);
		adicionadorLink.adicionarLink(salvo);
		return new ResponseEntity<>(salvo, HttpStatus.OK);
	}

	@DeleteMapping("/telefones/{id}")
	public ResponseEntity<Void> excluirTelefone(@PathVariable long id) {
		Telefone telefone = buscar(id);
		Optional<Cliente> dono = clienteRepositorio.findByTelefonesId(id);
		if (dono.isPresent()) {
			// Remove pela lista do cliente para o orphanRemoval apagar o registro e o vínculo
			dono.get().getTelefones().removeIf(item -> item.getId().equals(id));
			clienteRepositorio.save(dono.get());
		} else {
			repositorio.delete(telefone);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}

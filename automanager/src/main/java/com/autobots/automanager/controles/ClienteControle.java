package com.autobots.automanager.controles;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Date;
import java.util.List;

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
import com.autobots.automanager.modelos.AdicionadorLinkCliente;
import com.autobots.automanager.modelos.ClienteAtualizador;
import com.autobots.automanager.repositorios.ClienteRepositorio;

@RestController
public class ClienteControle {
	@Autowired
	private ClienteRepositorio repositorio;
	@Autowired
	private AdicionadorLinkCliente adicionadorLink;

	private Cliente buscar(long id) {
		return repositorio.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente " + id + " não encontrado"));
	}

	@GetMapping("/clientes/{id}")
	public ResponseEntity<Cliente> obterCliente(@PathVariable long id) {
		Cliente cliente = buscar(id);
		adicionadorLink.adicionarLink(cliente);
		return new ResponseEntity<>(cliente, HttpStatus.OK);
	}

	@GetMapping("/clientes")
	public ResponseEntity<CollectionModel<Cliente>> obterClientes() {
		List<Cliente> clientes = repositorio.findAll();
		adicionadorLink.adicionarLink(clientes);
		CollectionModel<Cliente> colecao = CollectionModel.of(clientes,
				linkTo(methodOn(ClienteControle.class).obterClientes()).withSelfRel());
		return new ResponseEntity<>(colecao, HttpStatus.OK);
	}

	@PostMapping("/clientes")
	public ResponseEntity<Cliente> cadastrarCliente(@RequestBody Cliente cliente) {
		if (cliente.getId() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "O id é gerado pelo sistema e não deve ser enviado no cadastro");
		}
		if (cliente.getDataCadastro() == null) {
			cliente.setDataCadastro(new Date());
		}
		Cliente salvo = repositorio.save(cliente);
		adicionadorLink.adicionarLink(salvo);
		return ResponseEntity.created(salvo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(salvo);
	}

	@PutMapping("/clientes/{id}")
	public ResponseEntity<Cliente> atualizarCliente(@PathVariable long id, @RequestBody Cliente atualizacao) {
		Cliente cliente = buscar(id);
		ClienteAtualizador atualizador = new ClienteAtualizador();
		atualizador.atualizar(cliente, atualizacao);
		Cliente salvo = repositorio.save(cliente);
		adicionadorLink.adicionarLink(salvo);
		return new ResponseEntity<>(salvo, HttpStatus.OK);
	}

	@DeleteMapping("/clientes/{id}")
	public ResponseEntity<Void> excluirCliente(@PathVariable long id) {
		repositorio.delete(buscar(id));
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}

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
import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.modelos.AdicionadorLinkEndereco;
import com.autobots.automanager.modelos.EnderecoAtualizador;
import com.autobots.automanager.modelos.StringVerificadorNulo;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import com.autobots.automanager.repositorios.EnderecoRepositorio;

@RestController
public class EnderecoControle {
	@Autowired
	private EnderecoRepositorio repositorio;
	@Autowired
	private ClienteRepositorio clienteRepositorio;
	@Autowired
	private AdicionadorLinkEndereco adicionadorLink;

	private Endereco buscar(long id) {
		return repositorio.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço " + id + " não encontrado"));
	}

	@GetMapping("/enderecos/{id}")
	public ResponseEntity<Endereco> obterEndereco(@PathVariable long id) {
		Endereco endereco = buscar(id);
		adicionadorLink.adicionarLink(endereco);
		return new ResponseEntity<>(endereco, HttpStatus.OK);
	}

	@GetMapping("/enderecos")
	public ResponseEntity<CollectionModel<Endereco>> obterEnderecos() {
		List<Endereco> enderecos = repositorio.findAll();
		adicionadorLink.adicionarLink(enderecos);
		CollectionModel<Endereco> colecao = CollectionModel.of(enderecos,
				linkTo(methodOn(EnderecoControle.class).obterEnderecos()).withSelfRel());
		return new ResponseEntity<>(colecao, HttpStatus.OK);
	}

	// Rota no singular porque o cliente tem no máximo um endereço
	@PostMapping("/clientes/{clienteId}/endereco")
	public ResponseEntity<Endereco> cadastrarEndereco(@PathVariable long clienteId, @RequestBody Endereco endereco) {
		if (endereco.getId() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "O id é gerado pelo sistema e não deve ser enviado no cadastro");
		}
		StringVerificadorNulo verificador = new StringVerificadorNulo();
		if (verificador.verificar(endereco.getCidade()) || verificador.verificar(endereco.getRua())
				|| verificador.verificar(endereco.getNumero())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cidade, rua e número do endereço são obrigatórios");
		}
		Cliente cliente = clienteRepositorio.findById(clienteId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente " + clienteId + " não encontrado"));
		if (cliente.getEndereco() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Cliente " + clienteId + " já possui endereço, use PUT /enderecos/" + cliente.getEndereco().getId());
		}
		Endereco salvo = repositorio.save(endereco);
		cliente.setEndereco(salvo);
		clienteRepositorio.save(cliente);
		adicionadorLink.adicionarLink(salvo, clienteId);
		return ResponseEntity.created(salvo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(salvo);
	}

	@PutMapping("/enderecos/{id}")
	public ResponseEntity<Endereco> atualizarEndereco(@PathVariable long id, @RequestBody Endereco atualizacao) {
		Endereco endereco = buscar(id);
		EnderecoAtualizador atualizador = new EnderecoAtualizador();
		atualizador.atualizar(endereco, atualizacao);
		Endereco salvo = repositorio.save(endereco);
		adicionadorLink.adicionarLink(salvo);
		return new ResponseEntity<>(salvo, HttpStatus.OK);
	}

	@DeleteMapping("/enderecos/{id}")
	public ResponseEntity<Void> excluirEndereco(@PathVariable long id) {
		Endereco endereco = buscar(id);
		Optional<Cliente> dono = clienteRepositorio.findByEnderecoId(id);
		if (dono.isPresent()) {
			// Desvincula do cliente para o orphanRemoval apagar o registro
			dono.get().setEndereco(null);
			clienteRepositorio.save(dono.get());
		} else {
			repositorio.delete(endereco);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}

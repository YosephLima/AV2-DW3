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
import com.autobots.automanager.entidades.Documento;
import com.autobots.automanager.modelos.AdicionadorLinkDocumento;
import com.autobots.automanager.modelos.DocumentoAtualizador;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import com.autobots.automanager.repositorios.DocumentoRepositorio;

@RestController
public class DocumentoControle {
	@Autowired
	private DocumentoRepositorio repositorio;
	@Autowired
	private ClienteRepositorio clienteRepositorio;
	@Autowired
	private AdicionadorLinkDocumento adicionadorLink;

	private Documento buscar(long id) {
		return repositorio.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento " + id + " não encontrado"));
	}

	@GetMapping("/documentos/{id}")
	public ResponseEntity<Documento> obterDocumento(@PathVariable long id) {
		Documento documento = buscar(id);
		adicionadorLink.adicionarLink(documento);
		return new ResponseEntity<>(documento, HttpStatus.OK);
	}

	@GetMapping("/documentos")
	public ResponseEntity<CollectionModel<Documento>> obterDocumentos() {
		List<Documento> documentos = repositorio.findAll();
		adicionadorLink.adicionarLink(documentos);
		CollectionModel<Documento> colecao = CollectionModel.of(documentos,
				linkTo(methodOn(DocumentoControle.class).obterDocumentos()).withSelfRel());
		return new ResponseEntity<>(colecao, HttpStatus.OK);
	}

	// A criação fica aninhada no cliente porque Documento só existe vinculado a um cliente
	@PostMapping("/clientes/{clienteId}/documentos")
	public ResponseEntity<Documento> cadastrarDocumento(@PathVariable long clienteId, @RequestBody Documento documento) {
		if (documento.getId() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "O id é gerado pelo sistema e não deve ser enviado no cadastro");
		}
		Cliente cliente = clienteRepositorio.findById(clienteId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente " + clienteId + " não encontrado"));
		Documento salvo = repositorio.save(documento);
		cliente.getDocumentos().add(salvo);
		clienteRepositorio.save(cliente);
		adicionadorLink.adicionarLink(salvo, clienteId);
		return ResponseEntity.created(salvo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(salvo);
	}

	@PutMapping("/documentos/{id}")
	public ResponseEntity<Documento> atualizarDocumento(@PathVariable long id, @RequestBody Documento atualizacao) {
		Documento documento = buscar(id);
		DocumentoAtualizador atualizador = new DocumentoAtualizador();
		atualizador.atualizar(documento, atualizacao);
		Documento salvo = repositorio.save(documento);
		adicionadorLink.adicionarLink(salvo);
		return new ResponseEntity<>(salvo, HttpStatus.OK);
	}

	@DeleteMapping("/documentos/{id}")
	public ResponseEntity<Void> excluirDocumento(@PathVariable long id) {
		Documento documento = buscar(id);
		Optional<Cliente> dono = clienteRepositorio.findByDocumentosId(id);
		if (dono.isPresent()) {
			// Remove pela lista do cliente para o orphanRemoval apagar o registro e o vínculo
			dono.get().getDocumentos().removeIf(item -> item.getId().equals(id));
			clienteRepositorio.save(dono.get());
		} else {
			repositorio.delete(documento);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}

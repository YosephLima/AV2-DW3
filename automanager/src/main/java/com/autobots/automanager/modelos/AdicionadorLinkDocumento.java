package com.autobots.automanager.modelos;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.stereotype.Component;

import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.DocumentoControle;
import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.entidades.Documento;
import com.autobots.automanager.repositorios.ClienteRepositorio;

@Component
public class AdicionadorLinkDocumento implements AdicionadorLink<Documento> {
	@Autowired
	private ClienteRepositorio clienteRepositorio;

	@Override
	public void adicionarLink(List<Documento> lista) {
		for (Documento documento : lista) {
			adicionarLink(documento);
		}
	}

	@Override
	public void adicionarLink(Documento documento) {
		Long clienteId = clienteRepositorio.findByDocumentosId(documento.getId()).map(Cliente::getId).orElse(null);
		adicionarLink(documento, clienteId);
	}

	// Usado quando quem chama já sabe o cliente dono, evitando a consulta
	public void adicionarLink(Documento documento, Long clienteId) {
		long id = documento.getId();
		documento.add(linkTo(methodOn(DocumentoControle.class).obterDocumento(id)).withSelfRel());
		documento.add(linkTo(methodOn(DocumentoControle.class).obterDocumentos()).withRel(IanaLinkRelations.COLLECTION));
		documento.add(linkTo(methodOn(DocumentoControle.class).atualizarDocumento(id, null)).withRel(IanaLinkRelations.EDIT));
		if (clienteId != null) {
			// up: o recurso acima na hierarquia, aqui o cliente dono
			documento.add(linkTo(methodOn(ClienteControle.class).obterCliente(clienteId)).withRel(IanaLinkRelations.UP));
		}
	}
}

package com.autobots.automanager.modelos;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.stereotype.Component;

import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.DocumentoControle;
import com.autobots.automanager.controles.EnderecoControle;
import com.autobots.automanager.controles.TelefoneControle;
import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.entidades.Documento;
import com.autobots.automanager.entidades.Telefone;

@Component
public class AdicionadorLinkCliente implements AdicionadorLink<Cliente> {
	@Autowired
	private AdicionadorLinkDocumento adicionadorDocumento;
	@Autowired
	private AdicionadorLinkEndereco adicionadorEndereco;
	@Autowired
	private AdicionadorLinkTelefone adicionadorTelefone;

	@Override
	public void adicionarLink(List<Cliente> lista) {
		for (Cliente cliente : lista) {
			adicionarLink(cliente);
		}
	}

	@Override
	public void adicionarLink(Cliente cliente) {
		long id = cliente.getId();
		// Relações padronizadas pela IANA: self (o próprio recurso), collection (a coleção a que pertence)
		// e edit (URI que aceita PUT e DELETE)
		cliente.add(linkTo(methodOn(ClienteControle.class).obterCliente(id)).withSelfRel());
		cliente.add(linkTo(methodOn(ClienteControle.class).obterClientes()).withRel(IanaLinkRelations.COLLECTION));
		cliente.add(linkTo(methodOn(ClienteControle.class).atualizarCliente(id, null)).withRel(IanaLinkRelations.EDIT));
		// A IANA não tem relação para "criar filho", então essas são próprias da API
		cliente.add(linkTo(methodOn(DocumentoControle.class).cadastrarDocumento(id, null)).withRel("cadastrar-documento"));
		cliente.add(linkTo(methodOn(TelefoneControle.class).cadastrarTelefone(id, null)).withRel("cadastrar-telefone"));
		if (cliente.getEndereco() == null) {
			// O cliente tem no máximo um endereço, então o link só aparece enquanto ele não existe
			cliente.add(linkTo(methodOn(EnderecoControle.class).cadastrarEndereco(id, null)).withRel("cadastrar-endereco"));
		} else {
			adicionadorEndereco.adicionarLink(cliente.getEndereco(), id);
		}
		for (Documento documento : cliente.getDocumentos()) {
			adicionadorDocumento.adicionarLink(documento, id);
		}
		for (Telefone telefone : cliente.getTelefones()) {
			adicionadorTelefone.adicionarLink(telefone, id);
		}
	}
}

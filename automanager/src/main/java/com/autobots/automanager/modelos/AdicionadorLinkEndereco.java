package com.autobots.automanager.modelos;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.stereotype.Component;

import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.EnderecoControle;
import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.repositorios.ClienteRepositorio;

@Component
public class AdicionadorLinkEndereco implements AdicionadorLink<Endereco> {
	@Autowired
	private ClienteRepositorio clienteRepositorio;

	@Override
	public void adicionarLink(List<Endereco> lista) {
		for (Endereco endereco : lista) {
			adicionarLink(endereco);
		}
	}

	@Override
	public void adicionarLink(Endereco endereco) {
		Long clienteId = clienteRepositorio.findByEnderecoId(endereco.getId()).map(Cliente::getId).orElse(null);
		adicionarLink(endereco, clienteId);
	}

	// Usado quando quem chama já sabe o cliente dono, evitando a consulta
	public void adicionarLink(Endereco endereco, Long clienteId) {
		long id = endereco.getId();
		endereco.add(linkTo(methodOn(EnderecoControle.class).obterEndereco(id)).withSelfRel());
		endereco.add(linkTo(methodOn(EnderecoControle.class).obterEnderecos()).withRel(IanaLinkRelations.COLLECTION));
		endereco.add(linkTo(methodOn(EnderecoControle.class).atualizarEndereco(id, null)).withRel(IanaLinkRelations.EDIT));
		if (clienteId != null) {
			// up: o recurso acima na hierarquia, aqui o cliente dono
			endereco.add(linkTo(methodOn(ClienteControle.class).obterCliente(clienteId)).withRel(IanaLinkRelations.UP));
		}
	}
}

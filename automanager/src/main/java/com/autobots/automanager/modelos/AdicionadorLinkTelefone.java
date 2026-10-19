package com.autobots.automanager.modelos;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.stereotype.Component;

import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.TelefoneControle;
import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.repositorios.ClienteRepositorio;

@Component
public class AdicionadorLinkTelefone implements AdicionadorLink<Telefone> {
	@Autowired
	private ClienteRepositorio clienteRepositorio;

	@Override
	public void adicionarLink(List<Telefone> lista) {
		for (Telefone telefone : lista) {
			adicionarLink(telefone);
		}
	}

	@Override
	public void adicionarLink(Telefone telefone) {
		Long clienteId = clienteRepositorio.findByTelefonesId(telefone.getId()).map(Cliente::getId).orElse(null);
		adicionarLink(telefone, clienteId);
	}

	// Usado quando quem chama já sabe o cliente dono, evitando a consulta
	public void adicionarLink(Telefone telefone, Long clienteId) {
		long id = telefone.getId();
		telefone.add(linkTo(methodOn(TelefoneControle.class).obterTelefone(id)).withSelfRel());
		telefone.add(linkTo(methodOn(TelefoneControle.class).obterTelefones()).withRel(IanaLinkRelations.COLLECTION));
		telefone.add(linkTo(methodOn(TelefoneControle.class).atualizarTelefone(id, null)).withRel(IanaLinkRelations.EDIT));
		if (clienteId != null) {
			// up: o recurso acima na hierarquia, aqui o cliente dono
			telefone.add(linkTo(methodOn(ClienteControle.class).obterCliente(clienteId)).withRel(IanaLinkRelations.UP));
		}
	}
}

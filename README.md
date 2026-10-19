# AutoBots Automanager (ATVII)

Microsserviço da AutoBots para o cadastro de clientes de lojas de manutenção veicular. Esta entrega completa os CRUDs de documento, endereço e telefone e leva a API até o nível 3 do Richardson Maturity Model, a partir do código deixado no repositório da atividade.

Java 17, Spring Boot 2.6 (Web, Data JPA, HATEOAS) e banco H2 em memória.

## Executando

```bash
cd automanager
./mvnw spring-boot:run        # Linux/macOS (se faltar permissão: chmod +x mvnw)
mvnw.cmd spring-boot:run      # Windows
```

A API fica em `http://localhost:8080`. O banco é em memória: os dados somem ao encerrar a aplicação, e a cada inicialização um cliente de exemplo é recriado. Um bom ponto de partida é `GET /clientes/1`, que já traz os links para todo o resto.

## Como os recursos se relacionam

Um **cliente** tem vários **documentos**, vários **telefones** e no máximo um **endereço**. Esses três só existem vinculados a um cliente: se o cliente é excluído, eles saem junto (`cascade` + `orphanRemoval`, como já estava no modelo original).

Por isso a API segue esta regra:

- **Criar** um documento, telefone ou endereço é uma ação *sobre o cliente*, então a rota fica aninhada: `POST /clientes/{clienteId}/telefones`.
- **Consultar, alterar e excluir** é uma ação *sobre o próprio recurso*, que já tem identidade: `GET`, `PUT` e `DELETE` em `/telefones/{id}`.

Dessa forma, nenhuma URI tem um `{id}` que às vezes é do cliente e às vezes do recurso.

## Decisões de projeto

**Descobrir o dono de um documento, telefone ou endereço.** No modelo original, só o cliente conhece os filhos: `Telefone` não tem referência ao `Cliente`. Em vez de alterar as entidades para criar essa referência, o `ClienteRepositorio` ganhou consultas derivadas (`findByTelefonesId`, `findByDocumentosId`, `findByEnderecoId`). Elas são usadas para montar o link até o cliente e para excluir o filho pela lista do dono, o que deixa o `orphanRemoval` cuidar da remoção.

**Links com relações padronizadas.** Os links usam as relações registradas na IANA e expostas pelo Spring em `IanaLinkRelations`, em vez de nomes inventados:

| Relação | Significado |
| ------- | ----------- |
| `self` | o próprio recurso |
| `collection` | a coleção a que o recurso pertence |
| `edit` | a URI que aceita `PUT` (alterar) e `DELETE` (excluir) |
| `up` | o recurso acima na hierarquia (em documento, telefone e endereço, o cliente dono) |

A IANA não define uma relação para "criar um filho deste recurso". Por isso o cliente traz três relações próprias: `cadastrar-documento`, `cadastrar-telefone` e `cadastrar-endereco`. Essa última só aparece enquanto o cliente não tem endereço, porque os links refletem o que é possível fazer naquele estado.

**Coleções como `CollectionModel`.** As listagens retornam o tipo de coleção do Spring HATEOAS, que traz o link `self` da própria coleção e agrupa os itens em `_embedded`. Lista vazia é `200` com coleção vazia, e não `404`, porque a coleção existe mesmo sem itens.

**Erros pelo mecanismo do Spring Boot.** Recurso inexistente, dado inválido ou conflito lançam `ResponseStatusException` com uma mensagem. O Spring Boot transforma isso no seu corpo de erro padrão, e nenhuma classe de exceção própria foi necessária:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Telefone 42 não encontrado", "path": "/telefones/42" }
```

Violações de restrição do banco, como cadastrar um número de documento repetido, viram `409` pelo `TratadorConflito`, no mesmo formato.

**Ajustes no código recebido.** Alguns pontos do código base foram corrigidos durante o trabalho:

- os `GET` respondiam `302 Found`, que significa redirecionamento; agora respondem `200 OK`;
- `getById` nunca retorna `null`, então a verificação de cliente inexistente no `PUT` e no `DELETE` não funcionava e o erro virava `500`; agora usa `findById`;
- os atualizadores comparavam ids `Long` com `==`, o que só funciona até 127; agora usam `equals`;
- o `EnderecoAtualizador` ignorava o `codigoPostal`;
- o `PUT` de cliente quebrava quando o cliente não tinha endereço;
- o cadastro de cliente agora preenche `dataCadastro` quando ela não é enviada;
- no cliente de exemplo, o CPF estava cadastrado com tipo "RG" e alguns acentos estavam corrompidos.

## Maturidade REST (RMM)

- **Nível 0, HTTP como transporte:** toda a comunicação é HTTP com corpo JSON.
- **Nível 1, recursos com URI própria:** cada recurso tem a sua coleção (`/clientes`, `/documentos`, `/enderecos`, `/telefones`) e cada registro tem o seu endereço (`/telefones/3`). As rotas do código base que traziam a ação no caminho (`/cliente/cadastro`, `/cliente/atualizar`, `/cliente/excluir`) foram substituídas.
- **Nível 2, métodos e status com significado:** a ação é dada pelo método (`GET`, `POST`, `PUT`, `DELETE`) e o resultado pelo status: `200`, `201` com cabeçalho `Location` apontando o recurso criado, `204` na exclusão, `400`, `404` e `409`.
- **Nível 3, HATEOAS:** as entidades estendem `RepresentationModel` e cada resposta traz em `_links` as ações possíveis a partir dali, montadas pelos `AdicionadorLink*` (pacote `modelos`). Documentos, telefones e endereço embutidos na resposta do cliente também trazem seus próprios links.

## Navegando pela API

`GET /clientes/1` (trecho):

```json
{
  "id": 1,
  "nome": "Pedro Alcântara de Bragança e Bourbon",
  "nomeSocial": "Dom Pedro",
  "telefones": [
    {
      "id": 1,
      "ddd": "21",
      "numero": "981234576",
      "_links": {
        "self": { "href": "http://localhost:8080/telefones/1" },
        "collection": { "href": "http://localhost:8080/telefones" },
        "edit": { "href": "http://localhost:8080/telefones/1" },
        "up": { "href": "http://localhost:8080/clientes/1" }
      }
    }
  ],
  "_links": {
    "self": { "href": "http://localhost:8080/clientes/1" },
    "collection": { "href": "http://localhost:8080/clientes" },
    "edit": { "href": "http://localhost:8080/clientes/1" },
    "cadastrar-documento": { "href": "http://localhost:8080/clientes/1/documentos" },
    "cadastrar-telefone": { "href": "http://localhost:8080/clientes/1/telefones" }
  }
}
```

Seguindo o link `cadastrar-telefone`:

```bash
curl -i -X POST http://localhost:8080/clientes/1/telefones \
  -H 'Content-Type: application/json' \
  -d '{"ddd":"12","numero":"988887777"}'
```

```
HTTP/1.1 201
Location: http://localhost:8080/telefones/2
```

Um cliente também pode ser cadastrado de uma vez, com documentos, telefones e endereço no mesmo corpo do `POST /clientes`.

## Referência das rotas

O `PUT` é parcial em todos os recursos: campos ausentes ou em branco mantêm o valor atual.

| Método | Rota | Sucesso | Erros |
| ------ | ---- | ------- | ----- |
| `GET` | `/clientes` | 200 | |
| `GET` | `/clientes/{id}` | 200 | 404 |
| `POST` | `/clientes` | 201 | 409 (id enviado, documento repetido) |
| `PUT` | `/clientes/{id}` | 200 | 404 |
| `DELETE` | `/clientes/{id}` | 204 | 404 |
| `GET` | `/documentos`, `/telefones`, `/enderecos` | 200 | |
| `GET` | `/documentos/{id}`, `/telefones/{id}`, `/enderecos/{id}` | 200 | 404 |
| `POST` | `/clientes/{clienteId}/documentos` | 201 | 404 (cliente), 409 (número repetido) |
| `POST` | `/clientes/{clienteId}/telefones` | 201 | 404 (cliente) |
| `POST` | `/clientes/{clienteId}/endereco` | 201 | 400 (sem cidade, rua ou número), 404 (cliente), 409 (já tem endereço) |
| `PUT` | `/documentos/{id}`, `/telefones/{id}`, `/enderecos/{id}` | 200 | 404, 409 (número de documento repetido) |
| `DELETE` | `/documentos/{id}`, `/telefones/{id}`, `/enderecos/{id}` | 204 | 404 |

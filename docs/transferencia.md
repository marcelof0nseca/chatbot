# Transferência de caso — Tino

**Projeto:** Tino, chatbot de atendimento da WXN · **Grupo:** 10 · **Épico 4**, segunda história
**Camadas entregues:** domínio, acesso a dados, serviço e API. A tela vem depois do wireframe.

---

## 1. História

> Como **atendente**, quero **passar um caso que estou atendendo para um colega, dizendo o motivo**,
> para que **o cliente seja atendido por quem resolve o assunto, sem voltar para a fila nem repetir a história**.

O gestor também transfere, inclusive casos que não são dele: é assim que ele redistribui a equipe
quando alguém sai do turno ou fica sobrecarregado.

---

## 2. Critérios de aceite

Cada critério tem pelo menos um teste automatizado; a coluna da direita diz qual.

| # | Dado | Quando | Então | Teste |
|---|---|---|---|---|
| CA1 | um caso em atendimento com o Bruno e a Carla em serviço | o Bruno transfere para a Carla, com motivo | o caso fica com a Carla, continua **em atendimento** e a conversa continua com um humano | `TransferenciaTest.atendenteTransfere` |
| CA2 | um caso transferido | o painel abre a conversa | aparecem o atendente atual e o caminho do caso: de quem, para quem, quem decidiu, o motivo e quando | `AtendimentoPersistenciaTest.casoTransferido` |
| CA3 | um caso que o cliente esperou 5 minutos na fila | o caso é transferido | o tempo de espera continua 5 minutos: transferir não zera a espera | `TransferenciaTest.preservaAssumidoEm` |
| CA4 | um caso com o Bruno | a gestora Ana transfere para a Carla | a transferência acontece e o registro diz que **a Ana** decidiu, com origem Bruno | `TransferenciaTest.gestorTransfere` |
| CA5 | um caso com o Bruno | a Carla, atendente comum, tenta transferir | é recusado (`409`) e o caso fica com o Bruno | `TransferenciaTest.atendenteDeForaNaoTransfere` |
| CA6 | um caso ainda na fila ou já encerrado | alguém tenta transferir | é recusado (`409`): caso na fila se assume, não se transfere | `TransferenciaTest.ticketNaFila`, `ticketEncerrado` |
| CA7 | um destino que já está com o caso ou fora de serviço | alguém tenta transferir | é recusado (`409`) e nada muda | `TransferenciaTest.destinoIgualAoAtual`, `destinoForaDeServico` |
| CA8 | um pedido sem motivo, com motivo só de espaços ou com mais de 280 caracteres | alguém tenta transferir | é recusado (`400`) | `TransferenciaTest.motivo`, `ApiDoPainelTest.transferenciaSemMotivo` |
| CA9 | um ticket ou atendente que não existe | alguém tenta transferir | responde `404` | `FilaDeConversasServiceTest.transferirInexistente`, `ApiDoPainelTest.destinoInexistente` |
| CA10 | uma transferência recusada | o painel consulta o caso | não ficou registro e o caso está com quem estava | `FilaDeConversasServiceTest.recusadaNaoGrava`, `AtendimentoPersistenciaTest.transferenciaRecusada` |
| CA11 | uma transferência aceita | o banco é consultado | o ticket com o novo atendente e o registro da transferência foram gravados juntos | `AtendimentoPersistenciaTest.transferirPeloServico`, `TransferenciaPersistenciaTest.gravaNoFimDoCaminho` |

---

## 3. Regras adotadas e por quê

| Regra | Decisão | Alternativa considerada | Por que esta |
|---|---|---|---|
| **Status depois de transferir** | Continua `EM_ATENDIMENTO`; a conversa continua `COM_ATENDENTE` | Voltar a `ABERTO` e o destino assumir pela fila | O cliente não deve voltar para a fila, nem o bot voltar a responder no meio de um atendimento humano |
| **Quem pode transferir** | O atendente atual do caso ou um gestor | Qualquer atendente | Atendente comum pegando caso de colega bagunça a fila. O gestor precisa poder redistribuir |
| **De que estado** | Só de `EM_ATENDIMENTO` | Também de `ABERTO` | Caso na fila não tem dono para passar adiante: quem quiser, assume |
| **Destino** | Diferente do atual e em serviço | Qualquer atendente | Transferir para si mesmo não muda nada, e para quem está fora de serviço deixa o cliente sem resposta |
| **Motivo** | Obrigatório, sem espaços nas pontas, até 280 caracteres | Opcional | Quem recebe precisa saber por que o caso chegou, sem perguntar ao cliente de novo. 280 cabe numa linha do histórico |
| **`assumidoEm`** | Preservado | Atualizar para a hora da transferência | É o fim da espera do cliente na fila. Atualizar faria o caso parecer ter esperado mais do que esperou |
| **Onde fica o histórico** | Tabela `transferencia`, uma linha por transferência; o ticket guarda só o atendente atual | Coluna "atendente anterior" no ticket | Uma coluna só lembra a última troca; a tabela guarda o caminho todo e quem decidiu cada passo |
| **Ordem das validações** | Primeiro os dados do pedido (`400`), depois o estado do caso (`409`) | Estado primeiro | Pedido malformado é erro de quem chamou, qualquer que seja o estado do caso |
| **Atomicidade** | Ticket e registro gravados na mesma transação | Gravar em dois passos | Não pode existir caso transferido sem registro, nem registro de transferência que não aconteceu |

As regras ficam em `Ticket.transferirPara()`. O serviço só busca, chama e grava, como já faz no
`assumir`: a validação não é repetida em lugar nenhum.

---

## 4. Contrato da API

```
POST /api/fila/{ticketId}/transferir
Content-Type: application/json

{ "quemTransfereId": 1001, "destinoId": 1002, "motivo": "Cliente quer negociar desconto" }
```

Responde `200` com o item da fila já com o novo atendente:

```json
{ "ticketId": 1004, "contato": "*********6650", "motivo": "ASSUNTO_NAO_SUPORTADO",
  "esperaEmSegundos": 180, "status": "EM_ATENDIMENTO",
  "ultimaMensagem": "preciso da segunda via do boleto, o meu venceu ontem", "atendente": "Ana Lima" }
```

| Código | Quando | Exemplo de `erro` |
|---|---|---|
| `400` | Falta dado, motivo inválido, corpo que não é JSON | `O motivo da transferência é obrigatório.` |
| `404` | Ticket, autor ou destino não existe | `Atendente não encontrado: 9` |
| `409` | A regra recusou | `Só o atendente do caso ou um gestor pode transferi-lo.` |

O `GET /api/fila/{ticketId}/conversa` passa a trazer `transferencias`, da mais antiga para a mais
recente, com `de`, `para`, `realizadaPor`, `motivo` e `realizadaEm`. Vem vazia se o caso nunca mudou de mãos.

`quemTransfereId` vem no corpo porque ainda não há login. Quando houver, sai do corpo e passa a ser
o usuário autenticado.

---

## 5. Carga de exemplo

O `data.sql` traz o ticket **1004** (cliente Fernanda Costa, segunda via de boleto): a Ana assumiu,
viu que era assunto do financeiro e transferiu para o Bruno. O caso aparece na fila como em atendimento
com o Bruno, e a conversa mostra a fala dos dois e a transferência.

---

## 6. Fora desta entrega

| Item | Situação |
|---|---|
| Tela de transferência no painel | Aguarda o wireframe, pedido antes da tela |
| Atendente autenticado no lugar de `quemTransfereId` | Depende do login (Épico 5) |
| Avisar o destino que recebeu um caso | Próxima história |
| Destino recusar o caso | Não previsto; se surgir, vira outra transferência |

---

## 7. Verificação

```bash
.\mvnw.cmd test
```

```
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Além dos testes, o endpoint foi exercitado com a aplicação no ar, num H2 em memória: transferência
aceita (`200`), autor sem permissão (`409`), motivo em branco (`400`), destino inexistente (`404`),
destino ausente (`400`), ticket na fila (`409`), corpo `null` (`400`) e o preflight de CORS a partir
de `http://localhost:5500`.

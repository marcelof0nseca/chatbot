# Atendimento guiado — Tino

**Projeto:** Tino, chatbot de atendimento da WXN · **Grupo:** 10
**Camadas entregues:** domínio, acesso a dados, serviço, API e simulador do cliente.

---

## 1. História

> Como **cliente da WXN**, quero que **o Tino me conduza no meu problema, perguntando o que precisa e me explicando o próximo passo**,
> para **resolver sem ficar adivinhando o que escrever e, quando precisar de uma pessoa, não ter que contar tudo de novo**.

Antes, o Tino só fazia duas coisas: respondia uma pergunta da base de conhecimento ou, quando não sabia, passava o caso direto para a fila. "Meu pedido chegou quebrado" virava um caso sem nenhuma informação. Agora ele reconhece o assunto, coleta o que o atendente vai precisar (número do pedido, o que aconteceu, uma foto), explica a política e só então passa o caso, com um resumo.

A regra central continua valendo: **o Tino nunca inventa**. Tudo o que ele diz num fluxo foi escrito pela WXN, como os artigos da base. Conduzir não é improvisar.

---

## 2. Os fluxos

| Assunto | Como o cliente chega | O que o Tino pergunta | Como termina |
|---|---|---|---|
| **Menu** | "oi", "menu", ou qualquer coisa que ele não entenda | Sobre o que você quer falar? | Abre um dos assuntos abaixo, ou passa para um atendente |
| **Acompanhar um pedido** | "meu pedido está atrasado", "cadê meu pedido" | Número do pedido; o prazo já passou? | No prazo: explica o rastreio. Atrasado: oferece um atendente para verificar com a transportadora |
| **Produto com defeito** | "meu pedido chegou quebrado", "não funciona" | Número do pedido; o que aconteceu; uma foto; troca ou reembolso | Explica como devolver e, com o aceite, abre a solicitação com um atendente |
| **Trocar um produto** | "quero trocar um produto", "trocar o tamanho" | Motivo; número do pedido; está sem uso?; recebeu há quanto tempo? | Explica como trocar e abre a troca. Usado ou fora do prazo: explica a política |
| **Cancelar uma compra** | "preciso cancelar uma compra" | Número do pedido; já foi enviado? | Explica o cancelamento ou o direito de arrependimento e passa para um atendente concluir |
| **Pagamento** | "fui cobrado duas vezes", "segunda via do boleto" | Qual o problema; número do pedido; detalhes da cobrança | Dicas para pagamento recusado; cobrança indevida e segunda via vão para um atendente |

Perguntas de informação ("Qual o prazo para troca?") continuam com a base de conhecimento. Depois da resposta, o Tino pergunta se resolveu: "sim" encerra a conversa, que conta como resolvida pelo Tino na taxa de contenção; "não" leva ao menu.

**Os textos de política** (prazos, postagem, estorno) estão em `config/TinoConfig.java` e **são exemplos**. A WXN os substitui pelo conteúdo real, como os artigos da base.

---

## 3. Critérios de aceite

Cada critério tem pelo menos um teste automatizado.

| # | Dado | Quando | Então | Teste |
|---|---|---|---|---|
| CA1 | um cliente que já aceitou o aviso | ele escreve o assunto do seu jeito | o Tino abre o fluxo certo | `FluxoGuiadoTest.reconheceOAssunto` (7 frases) |
| CA2 | o aviso de privacidade aceito | — | o cliente vê o menu de assuntos como botões | `CanalDoClienteServiceTest.menuDepoisDoAceite` |
| CA3 | um fluxo em andamento | o cliente responde pelo número, pelo botão ou com uma palavra parecida | a resposta é entendida | `FluxoGuiadoTest.escolhaNoMenu` |
| CA4 | a etapa do número do pedido | o cliente escreve "é o pedido nº 123456" | só o número é guardado; sem número, o Tino explica onde encontrar | `FluxoGuiadoTest.numeroDoPedido` |
| CA5 | o fluxo de defeito completo | o cliente aceita abrir a solicitação | o caso vai para a fila com um resumo, e a prévia da fila é o resumo | `CanalDoClienteServiceTest.fluxoDoDefeito`, `JornadaCompletaTest.jornada` |
| CA6 | qualquer etapa | o cliente pede um atendente | o caso passa na hora, com o que já foi coletado | `FluxoGuiadoTest.atendenteNoMeio` |
| CA7 | qualquer etapa | três respostas seguidas que não servem | o caso passa para uma pessoa, com o motivo "tentativas esgotadas" | `FluxoGuiadoTest.tentativasEsgotadas` |
| CA8 | um fluxo em andamento | o cliente muda de assunto ("na verdade quero cancelar") | o Tino vai para o fluxo novo e não pede de novo o número do pedido | `FluxoGuiadoTest.mudaDeAssunto` |
| CA9 | a etapa da foto | o cliente não consegue mandar agora | o atendimento segue sem a foto | `FluxoGuiadoTest.fotoOpcional` |
| CA10 | uma pergunta da base, ou um fluxo concluído | o cliente diz que resolveu | a conversa termina sem ticket e entra na taxa de contenção | `FluxoGuiadoTest.resolvidoPelaBase`, `concluirSemAtendente`, `CanalDoClienteServiceTest.resolvidaPeloTino` |
| CA11 | um fluxo que leva a uma etapa ou a um fluxo que não existe | a aplicação sobe | a subida falha, em vez de travar no meio de um atendimento | `FluxoGuiadoTest.etapaInexistente`, `fluxoInexistente`, `TinoConfigTest.fluxosValidos` |
| CA12 | o contexto da conversa | ele é gravado e lido do banco | volta igual; um contexto ilegível recomeça pelo menu | `FluxoGuiadoTest.contextoEmJson` |

---

## 4. Como funciona por dentro

| Classe | Papel |
|---|---|
| `Fluxo` | Um assunto: as frases e palavras-chave que o reconhecem, as etapas e o que entra no resumo |
| `Etapa` (abstrata) | Um passo: a pergunta e como ler a resposta |
| `EtapaDeEscolha` | Pergunta com respostas prontas, que viram botões. Aceita o número, o rótulo ou um sinônimo |
| `EtapaDeTexto` | Pergunta aberta, com formato opcional (o número do pedido) |
| `EtapaDeFoto` | Pede uma foto; não ter a foto não trava o atendimento |
| `Opcao` | Uma resposta pronta e para onde ela leva |
| `Transicao` | O que vem depois: outra etapa, outro fluxo, repetir, concluir, encerrar ou passar para um atendente |
| `ContextoDoFluxo` | Onde a conversa está: fluxo, etapa, dados coletados e tentativas |

**Polimorfismo.** O `Chatbot` conversa com qualquer etapa pelo mesmo contrato (`responder`, `getOpcoes`, `pularSeJaRespondida`). Cada tipo lê a resposta do seu jeito. Criar um tipo novo de etapa, como uma que confirma o CEP, não exige mexer no `Chatbot`.

**O contexto vai para o banco.** Cada mensagem do WhatsApp chega como uma requisição separada. Por isso o estado de trabalho fica na coluna `conversa.contexto`, em JSON, no formato da seção 4 do [modelo de dados](modelo-de-dados.md): `versao`, `intencao`, `etapa`, `entidades` e `tentativas`. A coluna estava desenhada como planejada e agora existe. O resumo de uma linha que vira a prévia da fila fica na coluna nova `conversa.assunto`.

**Sem fluxos, nada muda.** O Tino só entra no modo guiado quando há um fluxo `menu` publicado. Sem ele, responde pela base ou escala, como antes. Os testes de domínio anteriores continuam valendo sem alteração.

---

## 5. Regras adotadas

| Regra | Por quê |
|---|---|
| Pergunta de informação fica com a base, mesmo que cite um assunto | "Qual o prazo para troca?" quer a resposta, não abrir uma troca. No empate, a base vence |
| O que o Tino não entende abre o menu, e só na terceira vez seguida vai para a fila | Antes, uma frase fora do roteiro virava um caso sem informação. Agora o cliente tem um caminho |
| "Atendente" ou "humano" passa o caso a qualquer momento | Saída para uma pessoa sempre visível, como no wireframe (Tela 2, nota 7) |
| A foto é opcional | Quem está sem a foto não fica preso; o atendente pede depois |
| O Tino pede só os 4 últimos dígitos do cartão | Ninguém deve mandar o número completo do cartão por chat |
| As opções vão numeradas no texto da mensagem | Funciona em qualquer canal só de texto, e o atendente vê o que foi oferecido |
| Um fluxo que passa o caso usa o motivo "o cliente pediu um atendente" | O cliente aceitou ser passado. Um motivo novo exigiria alterar o tipo `ENUM` da coluna no H2, que já existe nas máquinas do grupo |

---

## 6. Contrato da API

`POST /api/simulador/mensagens` e `GET /api/simulador/conversa` ganharam o campo `opcoes`: a lista de respostas prontas da pergunta que o Tino acabou de fazer.

```json
{
  "conversaId": 7,
  "situacao": "COM_BOT",
  "ticketId": null,
  "atendente": null,
  "mensagens": [ … ],
  "opcoes": ["Chegou quebrado ou amassado", "Não funciona direito", "Veio diferente do que comprei"]
}
```

No simulador, as opções viram botões. No WhatsApp, viram a mensagem interativa: até 3 botões, ou uma lista de até 10 itens para o menu. A foto chega como uma mensagem que começa com `[Foto]`; com a integração, o webhook escreve a mesma marcação ao receber a imagem.

---

## 7. Fora desta entrega

- **Fluxos no banco e editáveis pelo gestor.** Hoje vivem em memória, em `TinoConfig`, como os artigos da base. A tela de edição é a mesma da base de conhecimento (Tela 8 do wireframe).
- **Expiração do contexto em 24 horas**, descrita na seção 4 do modelo de dados.
- **Guardar a imagem da foto.** Hoje o Tino registra que a foto foi enviada e o nome do arquivo.
- **Consultar o pedido de verdade** (status, rastreio). Depende de integração com o sistema de pedidos da WXN.

# Painel de atendimento do Tino

Projeto web separado do back-end: HTML, CSS e JavaScript, sem build. É o painel
que o atendente da WXN usa para ver a fila e as conversas encaminhadas pelo Tino.

| Página | Tela | Consome |
|---|---|---|
| `index.html` | Tela 6, fila de atendimento | `GET /api/fila`, `GET /api/fila/resumo`, `POST /api/fila/{id}/assumir`, `GET /api/atendentes` |
| `conversa.html?ticket={id}` | Tela 7, conversa assumida | `GET /api/fila/{id}/conversa`, `POST /api/fila/{id}/assumir` |

## Como rodar

1. Suba o back-end na raiz do projeto: `.\mvnw.cmd spring-boot:run`, que abre em `http://localhost:8080`.
2. Sirva esta pasta **na porta 5500**, a única origem que o CORS do back-end aceita:
   - no VS Code, com a extensão Live Server: abra `painel/index.html` e clique em **Go Live**;
   - ou no terminal, dentro de `painel/`: `python -m http.server 5500`.
3. Abra `http://localhost:5500/index.html` (ou `/painel/index.html`, se o Live Server servir a raiz do projeto).

Para usar outra porta, acrescente a origem em `app.cors.allowed-origins`, no
`application.properties` do back-end. Para apontar o painel para outro
servidor, defina `window.TINO_API` antes dos scripts.

## O que cada tela trata

- **Fila vazia, carregando e sem conexão**, cada um com uma mensagem que diz o
  que aconteceu e o que fazer.
- **Caso já assumido por outra pessoa:** a API responde `409`, a tela consulta a
  fila de novo e diz quem assumiu, em vez de falhar em silêncio.
- **Falha numa atualização:** os casos que já estavam na tela continuam visíveis,
  com um aviso.
- A fila se atualiza sozinha a cada 20 segundos.

## Decisões

- **Atendendo como:** enquanto não existe login (Tela 5 do wireframe), o
  atendente escolhe o próprio nome numa lista vinda da API. A escolha fica salva
  no navegador.
- **Texto do cliente nunca vira HTML.** Tudo que vem da API entra na página como
  texto, para uma mensagem maliciosa não conseguir injetar código no painel.
- **Horários lidos como texto.** O back-end já entrega os horários no fuso de
  Recife; o painel não os converte pelo fuso do navegador.
- **Responder ao cliente** ainda está desativado na Tela 7: depende da
  integração com o WhatsApp (Épico 1).
- O layout das duas telas, com todos os estados, está em
  [docs/Wireframe-Tino-Grupo10-v2.pdf](../docs/Wireframe-Tino-Grupo10-v2.pdf).

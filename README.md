# Tino — assistente de atendimento

Back-end e painel de atendimento do chatbot desenvolvido para a **WXN**. O Tino responde
pelo WhatsApp às perguntas frequentes a partir de uma base de conhecimento
editável, informa o próximo prazo quando está fora do expediente e, quando não
tem certeza, encaminha o caso para um atendente humano com o histórico completo
da conversa.

Projeto acadêmico — Ciência da Computação, 3º período, Grupo 10.

---

## Como rodar

Requisito único: **JDK 17 ou superior**. O Maven vem junto, pelo wrapper.

```bash
git clone https://github.com/marcelof0nseca/chatbot.git
cd chatbot
```

```bash
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # Linux e macOS
```

A aplicação sobe em `http://localhost:8080`.

| Endereço | O que é |
|---|---|
| `GET /api/health` | Verificação de que o servidor subiu. Responde `ok` |
| `GET /api/fila` | Fila de atendimento: quem aguarda primeiro, o caso mais antigo no topo |
| `GET /api/fila/resumo` | Indicadores do dia: na fila, em atendimento, espera média e taxa de contenção |
| `POST /api/fila/{ticketId}/assumir?atendenteId={id}` | O atendente assume um caso. Responde `404` se o ticket ou o atendente não existe, `409` se a regra recusar |
| `GET /api/fila/{ticketId}/conversa` | A conversa de um caso, com o histórico completo, para a Tela 7 |
| `GET /api/atendentes` | A equipe que pode assumir casos, em ordem alfabética |
| `/h2-console` | Console do banco. JDBC URL `jdbc:h2:file:./data/chatbotdb`, usuário `sa`, senha vazia |

Para rodar os testes:

```bash
.\mvnw.cmd test
```

### Painel de atendimento

O painel web fica em [painel/](painel/), um projeto separado servido na porta
5500, a origem que o CORS do back-end aceita. Com o back-end no ar, dentro de
`painel/`:

```bash
python -m http.server 5500
```

e abra `http://localhost:5500/index.html`. Pelo VS Code, o Live Server faz o
mesmo. Detalhes em [painel/README.md](painel/README.md).

---

## Stack

| Componente | Versão |
|---|---|
| Java | 17 (compilação); roda em JDK 17+ |
| Spring Boot | 4.1.1 — Web MVC, Data JPA |
| Banco | H2 em arquivo (desenvolvimento) · PostgreSQL (produção, planejado) |
| Build | Maven 3.9.16 via wrapper |
| Fuso | `America/Recife`, fixo no código (`Relogio` e `Clock`), independente do fuso do servidor. No banco, o deslocamento de 3h num servidor em UTC tinha duas causas, com duas correções: veja o bloco de fuso no `application.properties`. Os testes rodam com a JVM em UTC |

O banco de desenvolvimento é o H2 em modo arquivo, gravando em `data/`, que não
é versionado. Em produção será PostgreSQL: a nuvem recria o disco a cada deploy,
e o arquivo do H2 seria perdido.

---

## Estrutura

```
src/main/java/com/wxn/chatbot/
├── ChatbotApplication.java   # classe principal
├── config/                   # CORS e relogio com o fuso fixo
├── controller/               # API REST: fila, conversa, equipe, erros
├── dominio/                  # modelo de entidades e regras de negocio
├── dto/                      # o que a API entrega ao painel
├── repositorio/              # acesso a dados com Spring Data JPA
└── servico/                  # regras da fila e do atendimento

painel/                       # painel web: Telas 6 e 7
```

O pacote `dominio` concentra as regras de negócio. A classe central é
`Conversa`, dona das mensagens e da máquina de estados que decide quem fala —
bot ou humano. A regra "nunca inventar resposta" está em `Chatbot.atender()`:
abaixo do limite de confiança, nenhum caminho do código devolve conteúdo da base.

---

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [docs/ambientes.md](docs/ambientes.md) | Ambientes de desenvolvimento e deploy |
| [docs/modelo-de-classes.md](docs/modelo-de-classes.md) | Modelo de entidades em POO, com diagrama de classes |
| [docs/modelo-de-dados.md](docs/modelo-de-dados.md) | Dicionário de dados das 10 tabelas e como o contexto da conversa é salvo |
| [docs/schema.sql](docs/schema.sql) | DDL executável, validado em H2 |
| [docs/Wireframe-Tino-Grupo10.pdf](docs/Wireframe-Tino-Grupo10.pdf) | Wireframe v1, baixa fidelidade, das 9 telas |
| [docs/Wireframe-Tino-Grupo10-v2.pdf](docs/Wireframe-Tino-Grupo10-v2.pdf) | Wireframe v2, média fidelidade, das Telas 6 e 7 com todos os estados |

---

## Equipe

Marcelo Andrade da Fonseca · Vinicius Cezar · Eduardo Esteves de Albuquerque ·
Luísa Feitosa Magalhães · Laura Vitória da Silva Alves · Pedro José Oliveira e
Silva · Pedro Coutinho · Mateus Reinaux Batista Meira

# Tino — assistente de atendimento

Backend do chatbot de atendimento desenvolvido para a **WXN**. O Tino responde
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
| `/h2-console` | Console do banco. JDBC URL `jdbc:h2:file:./data/chatbotdb`, usuário `sa`, senha vazia |

Para rodar os testes:

```bash
.\mvnw.cmd test
```

---

## Stack

| Componente | Versão |
|---|---|
| Java | 17 (compilação); roda em JDK 17+ |
| Spring Boot | 4.1.1 — Web MVC, Data JPA |
| Banco | H2 em arquivo (desenvolvimento) · PostgreSQL (produção, planejado) |
| Build | Maven 3.9.16 via wrapper |

O banco de desenvolvimento é o H2 em modo arquivo, gravando em `data/`, que não
é versionado. Em produção será PostgreSQL: a nuvem recria o disco a cada deploy,
e o arquivo do H2 seria perdido.

---

## Estrutura

```
src/main/java/com/nxn/chatbot/
├── ChatbotApplication.java          # classe principal
├── config/CorsConfig.java           # libera o dashboard em outra origem
├── controller/HealthController.java # GET /api/health
└── dominio/                         # modelo de entidades (17 classes)
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
| [docs/Wireframe-Tino-Grupo10.pdf](docs/Wireframe-Tino-Grupo10.pdf) | Wireframes das 9 telas |

---

## Equipe

Marcelo Andrade da Fonseca · Vinicius Cezar · Eduardo Esteves de Albuquerque ·
Luísa Feitosa Magalhães · Laura Vitória da Silva Alves · Pedro José Oliveira e
Silva · Pedro Coutinho · Mateus Reinaux Batista Meira

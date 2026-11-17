# Tino — assistente de atendimento

Back-end e painel de atendimento do chatbot desenvolvido para a **WXN**. O Tino responde
pelo WhatsApp às perguntas frequentes a partir de uma base de conhecimento
editável, informa o próximo prazo quando está fora do expediente e, quando não
tem certeza, encaminha o caso para um atendente humano com o histórico completo
da conversa.

Projeto acadêmico — Ciência da Computação, 3º período, Grupo 10.

---

## Como rodar

Do zero ao sistema no ar, em dois terminais. Estes passos foram seguidos num
clone novo, com o cache do Maven vazio, em 17/11/2026.

### 1. O que instalar

| Programa | Para quê | Para conferir |
|---|---|---|
| **JDK 17 ou superior** | O back-end. O Maven vem junto, pelo wrapper | `java -version` |
| **Git** | Baixar o projeto | `git --version` |
| **Um servidor de arquivos**, qualquer um destes: Python 3, o `jwebserver` (vem no JDK 18 ou superior) ou a extensão Live Server do VS Code | O painel | `python --version` ou `jwebserver --version` |

A primeira execução precisa de internet: o wrapper baixa o Maven e as
dependências, o que leva cerca de 2 minutos. Das próximas vezes, sobe em
segundos.

### 2. Baixar o projeto

```bash
git clone https://github.com/marcelof0nseca/chatbot.git
cd chatbot
```

### 3. Terminal 1: o back-end

```bash
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # Linux e macOS
```

Está pronto quando aparecer `Started ChatbotApplication`. O back-end fica em
`http://localhost:8080` e cria o banco na pasta `data/`, já com casos de exemplo.

### 4. Terminal 2: o painel

Na pasta do projeto, entre em `painel/` e sirva os arquivos **na porta 5500**,
a única que o CORS do back-end aceita:

```bash
cd painel
python -m http.server 5500      # com Python (no Windows também funciona: py -m http.server 5500)
jwebserver -p 5500              # sem Python, com JDK 18 ou superior
```

Pelo VS Code, abra `painel/index.html` e clique em **Go Live**: o Live Server
usa a porta 5500.

Abra **http://localhost:5500/index.html** no navegador.

### 5. As duas histórias, numa sessão

Com os casos de exemplo:

1. Em **Atendendo como**, escolha **Bruno Dias**. Em **Meus casos** aparece o
   caso `••• 6650`, que a Ana já transferiu para ele.
2. **A fila e o assumir (primeira história).** Em **Aguardando**, clique em
   **Assumir** no caso do topo, `••• 4821`. Ele passa para **Meus casos**.
   **Abrir conversa** mostra o histórico desde a primeira mensagem, com a
   marcação de onde o Tino passou o caso para atendimento humano.
3. **A transferência (segunda história).** Nessa conversa, transfira o caso para
   a Ana, com o motivo.
4. Volte à fila e escolha **Ana Lima** em **Atendendo como**. O caso aparece em
   **Meus casos** dela e, ao abrir a conversa, o histórico está inteiro.

Enquanto o botão **Transferir** não chega à Tela 7, o passo 3 pode ser feito
pelo PowerShell, com o back-end no ar:

```powershell
$corpo = '{"quemTransfereId":1001,"destinoId":1002,"motivo":"Cliente quer negociar desconto"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/fila/1001/transferir -ContentType "application/json" -Body $corpo
```

### Voltar ao estado inicial

A cada início, o back-end devolve os casos de exemplo ao estado original, mas o
que foi criado durante o uso, como as transferências, continua gravado. Para
começar do zero: pare o back-end com **Ctrl + C** e apague a pasta `data/`.

### Se algo der errado

| O que aparece | O que fazer |
|---|---|
| `java` não é reconhecido, ou erro sobre `JAVA_HOME` | Instale o JDK 17 ou superior, ou ponha a pasta `bin` dele no `PATH`. Confira com `java -version` |
| `Port 8080 was already in use` | Outro programa, ou outro back-end, está na porta. Feche-o e rode de novo |
| O painel diz **Sem conexão com o servidor** | O back-end não está no ar, ou ainda está subindo. Espere o `Started ChatbotApplication` e clique em **Tentar de novo** |
| O painel abre, mas a fila não carrega, e o console do navegador (F12) fala em CORS | O painel não está na porta 5500 |
| No Windows, `python` abre a Microsoft Store | O Python não está instalado: use `jwebserver -p 5500` ou instale o Python |
| O visual não mudou depois de atualizar o projeto | Recarregue sem cache: **Ctrl + Shift + R** |

### Testes

```bash
.\mvnw.cmd test      # Windows
./mvnw test          # Linux e macOS
```

### API

| Endereço | O que é |
|---|---|
| `GET /api/health` | Verificação de que o servidor subiu. Responde `ok` |
| `GET /api/fila` | Fila de atendimento: quem aguarda primeiro, o caso mais antigo no topo |
| `GET /api/fila/resumo` | Indicadores do dia: na fila, em atendimento, espera média e taxa de contenção |
| `POST /api/fila/{ticketId}/assumir?atendenteId={id}` | O atendente assume um caso. Responde `404` se o ticket ou o atendente não existe, `400` se faltar dado, `409` se a regra recusar |
| `POST /api/fila/{ticketId}/transferir` | Passa um caso em atendimento a outro atendente. Corpo `{"quemTransfereId", "destinoId", "motivo"}`. Mesmos códigos de erro; regras em [docs/transferencia.md](docs/transferencia.md) |
| `GET /api/fila/{ticketId}/conversa` | A conversa de um caso, com o histórico completo e as transferências, para a Tela 7 |
| `GET /api/atendentes` | A equipe que pode assumir casos, em ordem alfabética |
| `/h2-console` | Console do banco. JDBC URL `jdbc:h2:file:./data/chatbotdb`, usuário `sa`, senha vazia |

Detalhes do painel em [painel/README.md](painel/README.md).

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
└── servico/                  # regras da fila, do atendimento e da transferência

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
| [docs/modelo-de-dados.md](docs/modelo-de-dados.md) | Dicionário de dados das 11 tabelas (7 já no código, 4 planejadas) e como o contexto da conversa é salvo |
| [docs/schema.sql](docs/schema.sql) | DDL executável, validado em H2 |
| [docs/transferencia.md](docs/transferencia.md) | Transferência de caso: critérios de aceite, regras adotadas e contrato da API |
| [docs/Wireframe-Tino-Grupo10.pdf](docs/Wireframe-Tino-Grupo10.pdf) | Wireframe v1, baixa fidelidade, das 9 telas |
| [docs/Wireframe-Tino-Grupo10-v2.pdf](docs/Wireframe-Tino-Grupo10-v2.pdf) | Wireframe v2, média fidelidade, das Telas 6 e 7 com todos os estados |

---

## Equipe

Marcelo Andrade da Fonseca · Vinicius Cezar · Eduardo Esteves de Albuquerque ·
Luísa Feitosa Magalhães · Laura Vitória da Silva Alves · Pedro José Oliveira e
Silva · Pedro Coutinho · Mateus Reinaux Batista Meira

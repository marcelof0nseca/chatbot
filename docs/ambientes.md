# Montagem dos Ambientes de Desenvolvimento e Deploy

**Projeto:** Chatbot NXN
**Repositório:** https://github.com/marcelof0nseca/chatbot
**Data:** 04/09/2026

| Campo |  |
|---|---|
| Disciplina | Projetos 3 |
| Orientador | Victor |
| Grupo | 10 |

---

## 1. Repositório Git

| Item | Valor |
|---|---|
| Hospedagem | GitHub |
| URL | https://github.com/marcelof0nseca/chatbot |
| Branch principal | `main` |

Os commits seguem o padrão **Conventional Commits**, com um commit por etapa da configuração em vez de um único commit com todo o setup, tornando o histórico legível e permitindo reverter uma decisão isolada.

O `.gitignore` exclui o build (`target/`), os arquivos de IDE (`.classpath`, `.project`, `.settings/`, `.idea/`, `.vscode/`) e o banco local (`data/`, `*.mv.db`). O banco fica de fora porque são dados de teste de cada desenvolvedor — se fossem versionados, cada `git pull` sobrescreveria o banco do colega.

---

## 2. IDE

**Visual Studio Code**, com as extensões:

- **Extension Pack for Java** (Microsoft) — compilação, debug e testes
- **Spring Boot Extension Pack** (VMware) — autocomplete de propriedades e navegação de endpoints
- **Live Server** — servirá o dashboard em `http://localhost:5500`

Escolhido por atender no mesmo ambiente as duas partes do projeto — backend Java e dashboard web — e por ter custo zero, o que padroniza o uso entre todos os integrantes do grupo.

O projeto **não depende de IDE alguma**: o build é feito pelo Maven Wrapper via linha de comando e nenhum arquivo de IDE é versionado, de modo que qualquer integrante pode usar IntelliJ ou Eclipse sem afetar o repositório.

---

## 3. Banco de dados

**H2 em modo arquivo**, gravando em `data/chatbotdb.mv.db`.

Escolhido por não exigir instalação de servidor de banco por parte dos integrantes e por ser compatível com JPA/Hibernate. Usamos o modo **arquivo**, e não memória, para que os dados persistam entre execuções — um chatbot depende de histórico de conversas.

```properties
spring.datasource.url=jdbc:h2:file:./data/chatbotdb
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
```

Console web em **http://localhost:8080/h2-console** — JDBC URL `jdbc:h2:file:./data/chatbotdb`, usuário `sa`, senha vazia.

---

## 4. Ambiente de execução

### 4.1 Local — implementado e testado

| Componente | Versão |
|---|---|
| JDK | OpenJDK 25.0.3 LTS (compilação para Java 17) |
| Spring Boot | 4.1.1 |
| Maven | 3.9.16 via Maven Wrapper |
| Servidor | Tomcat embarcado, porta 8080 |

Dependências: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-h2console` e `h2`.

O Maven não precisa estar instalado: o **Maven Wrapper** (`mvnw`) acompanha o projeto e baixa a versão correta automaticamente, garantindo que todos os integrantes usem o mesmo build.

```bash
git clone https://github.com/marcelof0nseca/chatbot.git
cd chatbot
.\mvnw.cmd spring-boot:run
```

**CORS.** O dashboard é um projeto separado, servido em outra origem, e o navegador bloqueia requisições entre origens diferentes por padrão. O backend autoriza explicitamente a origem do dashboard nas rotas `/api/**`:

```properties
app.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500
```

As origens vêm do `application.properties` e não do código, permitindo acrescentar a URL do dashboard publicado sem alterar Java.

**Testes realizados:**

| Verificação | Resultado |
|---|---|
| Inicialização | `Started ChatbotApplication in 3.357 seconds`, sem warnings ou erros |
| `GET /api/health` | `HTTP 200`, corpo `ok` |
| Console H2 | `HTTP 200`, arquivo do banco criado em disco |
| CORS — origem autorizada | `Access-Control-Allow-Origin: http://localhost:5500` |
| CORS — origem não autorizada | `HTTP 403` (bloqueado) |

### 4.2 Nuvem — planejado

| Camada | Plataforma |
|---|---|
| Backend | Render — Web Service (plano gratuito) |
| Banco de dados | Render PostgreSQL gerenciado |
| Dashboard | GitHub Pages |

O Render foi escolhido por conectar direto ao repositório GitHub, fazendo deploy automático a cada `push` na `main`, e por oferecer o PostgreSQL gerenciado na mesma plataforma.

**O H2 em arquivo não pode ser usado em produção.** Plataformas de deploy gratuitas usam sistema de arquivos efêmero: o disco é recriado a cada novo deploy, e todo o histórico de conversas seria perdido. Por isso o ambiente de nuvem usará PostgreSQL. Como o acesso a dados é feito via JPA/Hibernate, a troca não exige reescrever consultas.

A alternância entre os ambientes será feita por **profiles do Spring**:

- `application-dev.properties` — H2 em arquivo, console habilitado, `ddl-auto=update`
- `application-prod.properties` — PostgreSQL, console desativado, `ddl-auto=validate`, credenciais em variáveis de ambiente

Após publicar o dashboard no GitHub Pages, a URL resultante deve ser adicionada às origens autorizadas do CORS em produção.

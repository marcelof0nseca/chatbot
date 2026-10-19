# Modelo de Dados — Tino

**Projeto:** Tino — assistente de atendimento · **Cliente:** WXN · **Grupo:** 10
**10 tabelas · 69 colunas** · DDL em [schema.sql](schema.sql), validado em H2 2.4

---

## 1. Convenções

| Regra | Decisão | Por quê |
|---|---|---|
| **Chave primária** | `BIGINT` de identidade em todas as tabelas | Acompanha o `Long id` do modelo de classes e mapeia direto em `@GeneratedValue`. Como o id aparece só no painel autenticado, a exposição de volume não é um risco aqui |
| **Nomes** | `snake_case`, tabela no singular | Padrão que o PostgreSQL trata sem precisar de aspas |
| **Carimbos de tempo** | `criado_em` e `atualizado_em` em toda tabela de entidade | Auditoria e diagnóstico. Tabelas de evento usam o nome do fato: `enviada_em`, `aberto_em`, `aceito_em` |
| **Enums** | `VARCHAR` + `CHECK` | O banco recusa valor fora da lista, e o JPA mapeia direto com `@Enumerated(STRING)`. Guardar o número do enum quebraria se a ordem mudasse |
| **Fuso** | Tudo gravado em UTC, convertido para `America/Sao_Paulo` na aplicação | Servidor em nuvem roda em UTC |
| **Exclusão** | Sem `DELETE` em `mensagem` | Histórico de atendimento não se reescreve. O expurgo da LGPD apaga a conversa inteira |

---

## 2. As tabelas e os épicos que as exigem

| # | Tabela | Épico | O que sustenta |
|---|---|---|---|
| 1 | `cliente` | 1, 5 | Quem fala com o bot, e se é externo ou interno |
| 2 | `consentimento` | 5 | Trilha de aceite e revogação do aviso de privacidade |
| 3 | `atendente` | 4, 5 | Usuários do painel e seus perfis |
| 4 | `artigo_conhecimento` | 2 | A base que o gestor edita sem desenvolvedor |
| 5 | `artigo_variacao` | 2 | Outras formas de fazer a mesma pergunta |
| 6 | `conversa` | 1, 3, 4 | Estado do diálogo e o contexto entre turnos |
| 7 | `mensagem` | 1, 4 | O histórico que acompanha o escalonamento |
| 8 | `ticket` | 4 | A fila de atendimento humano |
| 9 | `horario_atendimento` | 3 | Expediente por dia da semana |
| 10 | `feriado` | 3 | Calendário nacional importado + feriados da empresa |

---

## 3. Dicionário de dados

### 3.1 `cliente`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `nome` | VARCHAR(120) | não | | |
| `telefone` | VARCHAR(20) | não | UK | Só dígitos, com DDI. É a identidade do cliente no WhatsApp |
| `email` | VARCHAR(160) | sim | | Opcional: quem chega pelo WhatsApp só informa o telefone |
| `tipo` | VARCHAR(10) | não | | `EXTERNO` \| `INTERNO` |
| `criado_em` | TIMESTAMP | não | | |
| `atualizado_em` | TIMESTAMP | não | | |

O telefone é único porque é por ele que o webhook encontra o cliente a cada mensagem. O tipo decide se artigos internos podem ser respondidos.

### 3.2 `consentimento`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `cliente_id` | BIGINT | não | FK → cliente | |
| `versao_politica` | VARCHAR(20) | não | | Qual texto a pessoa aceitou |
| `aceito_em` | TIMESTAMP | não | | |
| `revogado_em` | TIMESTAMP | sim | | Nulo enquanto o aceite vale |

Tabela própria, e não duas colunas em `cliente`, porque o titular pode revogar e aceitar de novo — e a LGPD exige a trilha, não só o estado atual. O consentimento válido é a linha mais recente com `revogado_em` nulo.

### 3.3 `atendente`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `nome` | VARCHAR(120) | não | | |
| `email` | VARCHAR(160) | não | UK | Login do painel |
| `senha_hash` | VARCHAR(100) | não | | Hash BCrypt. A senha em texto nunca é gravada |
| `perfil` | VARCHAR(10) | não | | `ATENDENTE` \| `GESTOR` |
| `disponivel` | BOOLEAN | não | | Em serviço ou não; só quem está disponível assume ticket |
| `criado_em` | TIMESTAMP | não | | |
| `atualizado_em` | TIMESTAMP | não | | |

### 3.4 `artigo_conhecimento`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `pergunta` | VARCHAR(300) | não | | Pergunta principal |
| `resposta` | VARCHAR(4000) | não | | Texto enviado ao cliente |
| `categoria` | VARCHAR(60) | não | | Trocas, Entrega, Pagamento… |
| `publico` | BOOLEAN | não | | `TRUE` responde a qualquer um; `FALSE` só a colaborador interno |
| `ativo` | BOOLEAN | não | | Desativar em vez de apagar preserva o vínculo com mensagens antigas |
| `criado_em` | TIMESTAMP | não | | |
| `atualizado_em` | TIMESTAMP | não | | Conteúdo velho é o que faz o bot errar — a coluna deixa isso visível |
| `atualizado_por` | BIGINT | sim | FK → atendente | Quem fez a última edição |

### 3.5 `artigo_variacao`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `artigo_id` | BIGINT | não | FK → artigo_conhecimento | |
| `texto` | VARCHAR(300) | não | UK (artigo_id, texto) | |

Tabela separada porque é coleção 1:N. Guardar as variações numa string separada por vírgula impediria busca e deixaria o campo crescer sem controle.

### 3.6 `conversa`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `cliente_id` | BIGINT | não | FK → cliente | |
| `canal` | VARCHAR(20) | não | | `WHATSAPP` \| `WEB`. Já previsto para o núcleo não travar num canal só |
| `estado` | VARCHAR(30) | não | | `AGUARDANDO_CONSENTIMENTO` \| `COM_BOT` \| `AGUARDANDO_ATENDENTE` \| `COM_ATENDENTE` \| `ENCERRADA` |
| `contexto` | VARCHAR(8000) | sim | | Documento JSON com o estado de trabalho do diálogo — **seção 4** |
| `tentativas_sem_sucesso` | SMALLINT | não | | Zera a cada acerto; ao chegar em 3 escalona |
| `iniciada_em` | TIMESTAMP | não | | |
| `ultima_atividade_em` | TIMESTAMP | não | | Base da expiração do contexto |
| `encerrada_em` | TIMESTAMP | sim | | |
| `expurgo_previsto_para` | TIMESTAMP | sim | | Data de descarte pela política de retenção |

### 3.7 `mensagem`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `conversa_id` | BIGINT | não | FK → conversa | |
| `tipo_remetente` | VARCHAR(10) | não | | `CLIENTE` \| `CHATBOT` \| `ATENDENTE` |
| `remetente_id` | BIGINT | sim | | Aponta para cliente ou atendente; nulo quando o autor é o bot |
| `conteudo` | VARCHAR(4000) | não | | |
| `data_hora` | TIMESTAMP | não | | |
| `id_externo` | VARCHAR(80) | sim | UK | Id da mensagem no WhatsApp |
| `artigo_id` | BIGINT | sim | FK → artigo_conhecimento | Qual artigo originou a resposta |
| `confianca` | NUMERIC(4,3) | sim | | 0 a 1, só quando quem respondeu foi o bot |

Três decisões aqui merecem explicação:

**O autor é polimórfico.** `tipo_remetente` diz a origem e `remetente_id` aponta para a linha correspondente. A alternativa — três colunas de chave estrangeira, das quais duas sempre nulas — deixaria a tabela mais larga sem ganhar integridade.

**`id_externo` é UNIQUE.** A Meta reenvia o webhook quando não recebe `200`. Sem essa restrição, uma instabilidade de rede gravaria a mesma mensagem do cliente duas vezes e o bot responderia em duplicidade. A restrição transforma o reenvio num erro previsível que a aplicação ignora.

**`artigo_id` e `confianca` ficam na mensagem.** São o registro de por que o bot respondeu aquilo. É o que alimenta o marcador *"bot encerrou aqui · confiança 0,38"* na tela do atendente e o que permite revisar depois quais artigos vêm acertando.

### 3.8 `ticket`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `conversa_id` | BIGINT | não | UK, FK → conversa | Único: uma conversa gera no máximo um ticket |
| `atendente_id` | BIGINT | sim | FK → atendente | Nulo enquanto está na fila |
| `motivo` | VARCHAR(25) | não | | `BAIXA_CONFIANCA` \| `SOLICITACAO_CLIENTE` \| `ASSUNTO_NAO_SUPORTADO` \| `FORA_DO_EXPEDIENTE` \| `TENTATIVAS_ESGOTADAS` |
| `status` | VARCHAR(15) | não | | `ABERTO` \| `EM_ATENDIMENTO` \| `RESOLVIDO` \| `CANCELADO` |
| `aberto_em` | TIMESTAMP | não | | |
| `assumido_em` | TIMESTAMP | sim | | `assumido_em − aberto_em` é o tempo de espera da fila |
| `encerrado_em` | TIMESTAMP | sim | | `encerrado_em − assumido_em` é o tempo de atendimento |

Os três carimbos não são redundantes: são as duas métricas que o painel mostra, calculadas sem tabela extra.

### 3.9 `horario_atendimento`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `dia_semana` | SMALLINT | não | UK | 1 = segunda … 7 = domingo |
| `atende` | BOOLEAN | não | | |
| `abre` | TIME | sim | | Obrigatório quando `atende` é verdadeiro |
| `fecha` | TIME | sim | | `CHECK` garante `abre < fecha` |

### 3.10 `feriado`

| Coluna | Tipo | Nulo | Chave | Regra |
|---|---|---|---|---|
| `id` | BIGINT | não | PK | |
| `data_feriado` | DATE | não | UK | |
| `descricao` | VARCHAR(120) | não | | |
| `origem` | VARCHAR(10) | não | | `NACIONAL` (importado da API) \| `MANUAL` (recesso da empresa) |
| `importado_em` | TIMESTAMP | não | | |

`origem` separa o que a tarefa anual pode sobrescrever do que foi cadastrado à mão e precisa ser preservado.

---

## 4. Como o contexto da conversa será salvo

Esta foi a pergunta levantada na reunião, e ela tem uma armadilha: *"contexto"* costuma significar três coisas diferentes ao mesmo tempo.

**O problema.** O HTTP não tem estado, e o Spring Boot segue esse modelo. Cada mensagem do WhatsApp chega ao webhook como uma requisição independente, sem memória da anterior. Se nada for persistido, o bot não lembra que o cliente disse "quero trocar" e que o "123456" da mensagem seguinte é o número do pedido.

**Os três contextos, e onde cada um fica:**

| O que é | Onde fica | Tempo de vida |
|---|---|---|
| **Histórico** — todas as falas da conversa | Tabela `mensagem` | Permanente, até o expurgo da LGPD |
| **Estado de trabalho** — intenção corrente, dados já coletados, etapa do fluxo | Coluna `conversa.contexto`, em JSON | Expira por inatividade |
| **Janela enviada ao modelo** — as últimas N mensagens | Não é guardada | Montada a cada turno a partir de `mensagem` |

O terceiro é derivado: guardá-lo seria duplicar o histórico e criar duas versões da verdade.

**Formato do `contexto`:**

```json
{
  "versao": 1,
  "intencao": "TROCA_PRODUTO",
  "etapa": "AGUARDANDO_NUMERO_PEDIDO",
  "entidades": {
    "numero_pedido": "123456",
    "motivo": "tamanho errado"
  },
  "ultimo_artigo_id": "9f1c…",
  "ultima_confianca": 0.812,
  "atualizado_em": "2026-10-15T14:07:33-03:00"
}
```

**Por que JSON numa coluna, e não uma tabela chave-valor.** O conteúdo do contexto vai mudar a cada iteração do bot — campos novos aparecem conforme os fluxos crescem. Com tabela ou coluna fixa, cada campo novo viraria uma migração. E o contexto é sempre lido e gravado inteiro, junto com a conversa: não existe consulta por campo isolado que justifique normalizar. Em desenvolvimento é `VARCHAR` no H2; em produção vira `JSONB` no PostgreSQL, que ainda permite indexar um campo se algum dia for preciso.

**Expiração.** Uma tarefa agendada limpa o `contexto` das conversas sem atividade há mais de 24 horas e as encerra — prazo alinhado com a janela de 24h em que o WhatsApp permite resposta livre. O histórico em `mensagem` **não** é afetado: só o estado de trabalho é descartado. Sem isso, um cliente que some no meio de uma troca e volta duas semanas depois seria atendido como se a conversa nunca tivesse parado.

**Por que não Redis agora.** A pesquisa de similares aponta memória de conversa em Redis com TTL como padrão de mercado, e é o caminho correto sob volume alto. Para o MVP seria mais um serviço para provisionar e manter, e com as 5.000 mensagens/dia informadas pelo cliente o PostgreSQL atende sem dificuldade. A coluna `contexto` guarda exatamente a mesma informação — se a latência exigir, o Redis entra como cache na frente do banco, sem alterar o modelo de dados.

---

## 5. Dados necessários por tipo de usuário

Aqui está a origem de cada coluna: nenhuma existe por precaução, todas atendem a alguém.

| Usuário | Precisa de | Tabelas |
|---|---|---|
| **Cliente externo** | Ser reconhecido pelo telefone, ter o consentimento registrado, receber resposta da base e saber quando será atendido | `cliente`, `consentimento`, `conversa`, `mensagem`, `horario_atendimento`, `feriado` |
| **Colaborador interno** | O mesmo, mais acesso a artigos marcados como internos | As acima + `artigo_conhecimento.publico` |
| **Atendente** | Ver a fila ordenada por espera, abrir a conversa com o histórico completo e saber por que o caso foi escalonado | `ticket`, `conversa`, `mensagem`, `atendente` |
| **Gestor** | Tudo do atendente, mais editar a base de conhecimento, o expediente e os feriados | As acima + `artigo_conhecimento`, `artigo_variacao`, `horario_atendimento`, `feriado` |

Duas colunas existem só por causa do painel, e vale dizer isso em voz alta: `ticket.assumido_em` e `ticket.encerrado_em` dão as duas métricas de tempo sem nenhuma tabela de apoio. E `mensagem.confianca` existe para o atendente entender, em um segundo, por que o caso chegou até ele.

---

## 6. Retenção e LGPD

| Regra | Implementação |
|---|---|
| Minimização | Nenhuma coluna de CPF, endereço ou dado de pagamento. Esses dados ficam no sistema da WXN e são consultados, não copiados |
| Retenção | `conversa.expurgo_previsto_para` é preenchido no encerramento. Tarefa agendada apaga conversa e mensagens após o prazo |
| Direito de exclusão | Apaga as conversas do cliente e anonimiza `cliente.nome` e `cliente.telefone`, preservando as métricas agregadas |
| Trilha de consentimento | Tabela `consentimento` guarda todos os aceites e revogações |
| Minimização na tela | O telefone completo fica no banco; o painel exibe só os 4 últimos dígitos |

---

## 7. H2 e PostgreSQL

O mesmo DDL roda nos dois. Três pontos onde a produção usa um tipo melhor:

| Coluna | H2 (dev) | PostgreSQL (prod) |
|---|---|---|
| `conversa.contexto` | `VARCHAR(8000)` | `JSONB` — consultável e indexável |
| `mensagem.conteudo`, `artigo.resposta` | `VARCHAR(4000)` | `TEXT` — sem limite fixo |
| Busca na base | Comparação em Java | `pg_trgm` ou `tsvector` para busca textual |

Em desenvolvimento o Hibernate gera essas tabelas a partir das entidades (`ddl-auto=update`). Em produção o `ddl-auto` passa a `validate` e este arquivo vira a primeira migração versionada do Flyway.

---

## 8. Verificação

O DDL foi executado contra o H2 2.4.240 e as restrições foram testadas:

```
10 tabelas criadas, 69 colunas
INSERT do fluxo completo (cliente → consentimento → conversa → mensagem → ticket): OK

estado 'PENSANDO'          → Check constraint violation: CK_CONVERSA_ESTADO
id_externo repetido        → Unique index violation: UK_MENSAGEM_ID_EXTERNO
confianca = 1.5            → Check constraint violation: CK_MENSAGEM_CONFIANCA
```

O segundo teste é o mais relevante na prática: ele simula o reenvio do webhook pela Meta e confirma que a mensagem duplicada é recusada pelo banco, e não pela aplicação.

```bash
java -cp h2-2.4.240.jar org.h2.tools.RunScript \
     -url "jdbc:h2:./validacao" -user sa -script docs/schema.sql
```

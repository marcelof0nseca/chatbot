-- Carga de exemplo da fila. Pode rodar a cada inicializacao: MERGE ... KEY(id)
-- atualiza a linha se ela ja existe e insere se nao existe, entao nao duplica.
-- IDs altos (1001+) para nao colidir com os que o banco gera em cadastros reais.
-- Os horarios sao relativos a agora, entao a fila sempre mostra esperas recentes.

-- ATENDENTES (Bruno e Ana em servico)
MERGE INTO atendente (id, nome, criado_em, email, perfil, disponivel) KEY (id) VALUES
    (1001, 'Bruno Dias', CURRENT_TIMESTAMP, 'bruno@wxn.com.br', 'ATENDENTE', TRUE);
MERGE INTO atendente (id, nome, criado_em, email, perfil, disponivel) KEY (id) VALUES
    (1002, 'Ana Lima', CURRENT_TIMESTAMP, 'ana@wxn.com.br', 'GESTOR', TRUE);

-- CLIENTES (todos EXTERNO, e-mail opcional fica nulo)
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1001, 'Maria Souza', CURRENT_TIMESTAMP, '5581999994821', NULL, 'EXTERNO');
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1002, 'João Pereira', CURRENT_TIMESTAMP, '5581988881190', NULL, 'EXTERNO');
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1003, 'Carla Nunes', CURRENT_TIMESTAMP, '5581977777735', NULL, 'EXTERNO');
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1004, 'Pedro Alves', CURRENT_TIMESTAMP, '5581966663064', NULL, 'EXTERNO');
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1005, 'Lucia Ramos', CURRENT_TIMESTAMP, '5581955552218', NULL, 'EXTERNO');
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1006, 'Rafael Dias', CURRENT_TIMESTAMP, '5581944449907', NULL, 'EXTERNO');

-- CONSENTIMENTO (tabela secundaria de cliente: chave = cliente_id)
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1001, CURRENT_TIMESTAMP, 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1002, CURRENT_TIMESTAMP, 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1003, CURRENT_TIMESTAMP, 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1004, CURRENT_TIMESTAMP, 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1005, CURRENT_TIMESTAMP, 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1006, CURRENT_TIMESTAMP, 'v1');

-- CONVERSAS escalonadas (aguardando atendente)
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1001, 1001, DATEADD('MINUTE', -42, CURRENT_TIMESTAMP), 'AGUARDANDO_ATENDENTE', NULL, 0);
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1002, 1002, DATEADD('MINUTE', -25, CURRENT_TIMESTAMP), 'AGUARDANDO_ATENDENTE', NULL, 0);
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1003, 1003, DATEADD('MINUTE', -10, CURRENT_TIMESTAMP), 'AGUARDANDO_ATENDENTE', NULL, 0);

-- CONVERSAS resolvidas pelo bot (encerradas, sem ticket)
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1004, 1004, DATEADD('HOUR', -3, CURRENT_TIMESTAMP), 'ENCERRADA', DATEADD('HOUR', -3, CURRENT_TIMESTAMP), 0);
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1005, 1005, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 'ENCERRADA', DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 0);
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1006, 1006, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 'ENCERRADA', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 0);

-- TICKETS abertos (a fila)
MERGE INTO ticket (id, conversa_id, motivo, aberto_em, atendente_id, assumido_em, encerrado_em, status) KEY (id) VALUES
    (1001, 1001, 'BAIXA_CONFIANCA', DATEADD('MINUTE', -41, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'ABERTO');
MERGE INTO ticket (id, conversa_id, motivo, aberto_em, atendente_id, assumido_em, encerrado_em, status) KEY (id) VALUES
    (1002, 1002, 'SOLICITACAO_CLIENTE', DATEADD('MINUTE', -24, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'ABERTO');
MERGE INTO ticket (id, conversa_id, motivo, aberto_em, atendente_id, assumido_em, encerrado_em, status) KEY (id) VALUES
    (1003, 1003, 'FORA_DO_EXPEDIENTE', DATEADD('MINUTE', -9, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'ABERTO');

-- MENSAGENS (uma fala do cliente em cada conversa)
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1001, 1001, 'CLIENTE', 1001, 'o cupom BLACK30 não aplicou no meu carrinho e a cobrança veio dobrada', DATEADD('MINUTE', -42, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1002, 1002, 'CLIENTE', 1002, 'quero falar com um atendente', DATEADD('MINUTE', -25, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1003, 1003, 'CLIENTE', 1003, 'preciso cancelar uma compra', DATEADD('MINUTE', -10, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1004, 1004, 'CLIENTE', 1004, 'qual o prazo para troca?', DATEADD('HOUR', -3, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1005, 1005, 'CLIENTE', 1005, 'qual o prazo para troca?', DATEADD('HOUR', -2, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1006, 1006, 'CLIENTE', 1006, 'qual o prazo para troca?', DATEADD('HOUR', -1, CURRENT_TIMESTAMP));
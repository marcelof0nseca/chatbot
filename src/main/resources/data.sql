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
-- O aceite e registrado no inicio de cada conversa, como no fluxo real.
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1001, DATEADD('MINUTE', -43, CURRENT_TIMESTAMP), 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1002, DATEADD('MINUTE', -25, CURRENT_TIMESTAMP), 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1003, DATEADD('MINUTE', -10, CURRENT_TIMESTAMP), 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1004, DATEADD('HOUR', -3, CURRENT_TIMESTAMP), 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1005, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 'v1');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1006, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 'v1');

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

-- RESPOSTAS DO BOT, para a Tela 7 mostrar o historico que o atendente recebe
-- (tipo CHATBOT: sem remetente_id). IDs 1101+ para nao colidir com as falas acima.
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1101, 1001, 'CLIENTE', 1001, 'oi, tenho uma dúvida na cobrança', DATEADD('MINUTE', -43, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1102, 1001, 'CHATBOT', NULL, 'Olá! Eu sou o Tino, o assistente virtual da WXN. Como posso ajudar?', DATEADD('MINUTE', -43, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1103, 1001, 'CHATBOT', NULL, 'Não tenho certeza sobre esse caso e prefiro não te dar uma resposta errada. Vou te passar para um atendente, com o histórico desta conversa.', DATEADD('MINUTE', -41, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1104, 1002, 'CHATBOT', NULL, 'Claro! Vou te passar para um atendente, com o histórico desta conversa.', DATEADD('MINUTE', -24, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1105, 1003, 'CHATBOT', NULL, 'Consigo te dar informações agora, mas o cancelamento precisa passar por um atendente. Seu caso fica na fila e é atendido no próximo horário de atendimento.', DATEADD('MINUTE', -9, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1106, 1004, 'CHATBOT', NULL, 'A troca pode ser solicitada em até 30 dias corridos após o recebimento. O produto precisa estar sem uso e com a etiqueta original.', DATEADD('MINUTE', -179, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1107, 1005, 'CHATBOT', NULL, 'A troca pode ser solicitada em até 30 dias corridos após o recebimento. O produto precisa estar sem uso e com a etiqueta original.', DATEADD('MINUTE', -119, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1108, 1006, 'CHATBOT', NULL, 'A troca pode ser solicitada em até 30 dias corridos após o recebimento. O produto precisa estar sem uso e com a etiqueta original.', DATEADD('MINUTE', -59, CURRENT_TIMESTAMP));

-- CASO EM ATENDIMENTO JA TRANSFERIDO (segunda historia do Epico 4)
-- A Ana assumiu, viu que era segunda via de boleto e passou para o Bruno, que
-- cuida do financeiro. O ticket guarda so o atendente atual (Bruno); o caminho
-- fica na tabela transferencia. Como o resto da carga, volta a este estado a
-- cada inicializacao.
MERGE INTO cliente (id, nome, criado_em, telefone, email, tipo) KEY (id) VALUES
    (1007, 'Fernanda Costa', CURRENT_TIMESTAMP, '5581933336650', NULL, 'EXTERNO');
MERGE INTO consentimento (cliente_id, aceito_em, versao_politica) KEY (cliente_id) VALUES
    (1007, DATEADD('MINUTE', -31, CURRENT_TIMESTAMP), 'v1');
MERGE INTO conversa (id, cliente_id, iniciada_em, estado, encerrada_em, tentativas_sem_sucesso) KEY (id) VALUES
    (1007, 1007, DATEADD('MINUTE', -30, CURRENT_TIMESTAMP), 'COM_ATENDENTE', NULL, 0);
MERGE INTO ticket (id, conversa_id, motivo, aberto_em, atendente_id, assumido_em, encerrado_em, status) KEY (id) VALUES
    (1004, 1007, 'ASSUNTO_NAO_SUPORTADO', DATEADD('MINUTE', -29, CURRENT_TIMESTAMP), 1001, DATEADD('MINUTE', -26, CURRENT_TIMESTAMP), NULL, 'EM_ATENDIMENTO');
MERGE INTO transferencia (id, ticket_id, origem_id, destino_id, realizada_por_id, motivo, realizada_em) KEY (id) VALUES
    (1001, 1004, 1002, 1001, 1002, 'Segunda via de boleto é com o financeiro; o Bruno cuida desses casos.', DATEADD('MINUTE', -20, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1109, 1007, 'CLIENTE', 1007, 'preciso da segunda via do boleto, o meu venceu ontem', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1110, 1007, 'CHATBOT', NULL, 'Segunda via de boleto precisa passar por um atendente. Vou te transferir, com o histórico desta conversa.', DATEADD('MINUTE', -29, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1111, 1007, 'ATENDENTE', 1002, 'Oi, Fernanda! Aqui é a Ana. Vou te passar para o Bruno, do financeiro, que emite a segunda via.', DATEADD('MINUTE', -21, CURRENT_TIMESTAMP));
MERGE INTO mensagem (id, conversa_id, tipo_remetente, remetente_id, conteudo, data_hora) KEY (id) VALUES
    (1112, 1007, 'ATENDENTE', 1001, 'Oi, Fernanda, aqui é o Bruno. Já estou gerando a segunda via com a nova data de vencimento.', DATEADD('MINUTE', -18, CURRENT_TIMESTAMP));

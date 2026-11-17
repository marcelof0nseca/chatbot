// Tela 7: conversa de um caso. Consome GET /api/fila/{ticketId}/conversa,
// se o caso ainda estiver na fila, POST /api/fila/{ticketId}/assumir,
// e, se ele estiver em atendimento, POST /api/fila/{ticketId}/transferir.

import { api, API, ErroDaApi, el, motivo, contato, hora, dia, montarSeletorDeAtendente } from "./api.js";

const $conteudo = document.getElementById("conteudo");
const $aviso = document.getElementById("aviso");
const $atendente = document.getElementById("atendente");

const ticketId = new URLSearchParams(location.search).get("ticket");

let atendenteEscolhido = () => null;

const AUTORES = { CLIENTE: "Cliente", CHATBOT: "Tino" };
const CLASSES = { CLIENTE: "de-cliente", CHATBOT: "de-bot", ATENDENTE: "de-atendente" };
const TIPOS_DE_CLIENTE = { EXTERNO: "Cliente externo", INTERNO: "Colaborador interno" };

const LIMITE_DO_MOTIVO = 280;

// Estado do painel de transferencia. Vive fora de desenhar() porque a tela se
// redesenha depois de cada chamada, e o que o atendente digitou nao pode se perder.
let painelAberto = false;
let equipe = [];
let rascunho = { destinoId: "", motivo: "" };
let erroDoMotivo = "";
let enviando = false;

function avisar(tipo, texto) {
  $aviso.replaceChildren(
    el(
      "div",
      { class: `aviso ${tipo}` },
      el("p", {}, texto),
      el("button", { class: "aviso-fechar", type: "button", onclick: () => $aviso.replaceChildren() }, "Fechar"),
    ),
  );
}

function estado(classe, titulo, texto, acao) {
  $conteudo.replaceChildren(
    el("div", { class: `estado ${classe}` }, el("p", { class: "estado-titulo" }, titulo), el("p", {}, texto), acao),
  );
}

function selo(conversa) {
  switch (conversa.status) {
    case "ABERTO":
      return el("span", { class: "selo aguardando" }, "Aguardando na fila");
    case "EM_ATENDIMENTO":
      return el("span", { class: "selo atendendo" }, `Em atendimento com ${conversa.atendente}`);
    case "RESOLVIDO":
      return el("span", { class: "selo encerrado" }, "Resolvido");
    default:
      return el("span", { class: "selo encerrado" }, "Cancelado");
  }
}

function mensagem(m) {
  const autor = AUTORES[m.autor] ?? m.identificacao;
  return el(
    "li",
    { class: `mensagem ${CLASSES[m.autor] ?? ""}` },
    el("p", { class: "mensagem-autor" }, el("strong", {}, autor), ` às ${hora(m.dataHora)}`),
    el("p", { class: "mensagem-texto" }, m.conteudo),
  );
}

/** Linha que marca onde o Tino passou o caso adiante, entre as mensagens. */
function marco(conversa) {
  return el(
    "li",
    { class: "marco" },
    `O Tino passou o caso para atendimento humano às ${hora(conversa.abertoEm)}. ${motivo(conversa.motivo)}.`,
  );
}

/** O nome, venha o atendente como texto ou como objeto. */
const nomeDe = (quem) => (typeof quem === "string" ? quem : (quem?.nome ?? ""));

/**
 * Linha que marca uma transferencia, no ponto da conversa em que aconteceu.
 * Se quem decidiu e quem tinha o caso, a frase e simples; se foi outra pessoa
 * (um gestor), ela diz quem decidiu e de quem o caso saiu.
 */
function marcoDeTransferencia(t) {
  const de = nomeDe(t.de);
  const para = nomeDe(t.para);
  const quem = nomeDe(t.realizadaPor) || de;
  const frase =
    quem === de
      ? `${de} transferiu o caso para ${para} às ${hora(t.realizadaEm)}.`
      : `${quem} transferiu o caso de ${de} para ${para} às ${hora(t.realizadaEm)}.`;
  return el(
    "li",
    { class: "marco marco-transferencia" },
    el("span", { class: "marco-titulo" }, frase),
    t.motivo ? el("span", { class: "marco-motivo" }, `Motivo: ${t.motivo}`) : null,
  );
}

/**
 * Onde entra o marco: depois da fala com que o Tino passa o caso adiante.
 * O bot abre o ticket e so entao grava essa fala, entao ela vem logo depois
 * de abertoEm. Se a primeira mensagem apos abertoEm nao e do bot, o marco
 * entra antes dela. (Os horarios chegam no mesmo formato ISO, entao
 * comparar como texto respeita a ordem cronologica.)
 */
function posicaoDoMarco(conversa) {
  const mensagens = conversa.mensagens;
  const depois = mensagens.findIndex((m) => m.dataHora >= conversa.abertoEm);
  if (depois === -1) return mensagens.length;
  return mensagens[depois].autor === "CHATBOT" ? depois + 1 : depois;
}

/** Mensagens, marco do Tino e transferencias, na ordem em que aconteceram. */
function linhaDoTempo(conversa) {
  const itens = conversa.mensagens.map((m) => ({ quando: m.dataHora, no: mensagem(m) }));
  if (conversa.abertoEm) itens.splice(posicaoDoMarco(conversa), 0, { quando: conversa.abertoEm, no: marco(conversa) });

  // Da mais antiga para a mais recente, como a API entrega. Uma transferencia
  // entra depois de tudo o que aconteceu antes ou no mesmo instante dela.
  for (const t of conversa.transferencias ?? []) {
    const antes = itens.findIndex((item) => item.quando > t.realizadaEm);
    const entrada = { quando: t.realizadaEm, no: marcoDeTransferencia(t) };
    if (antes === -1) itens.push(entrada);
    else itens.splice(antes, 0, entrada);
  }
  return itens.map((item) => item.no);
}

function historico(conversa) {
  return el(
    "section",
    { class: "historico", "aria-label": "Histórico da conversa" },
    el("ol", { class: "mensagens" }, ...linhaDoTempo(conversa)),
    el(
      "div",
      { class: "resposta" },
      el("label", { for: "resposta", class: "resposta-nota" }, "Resposta ao cliente"),
      el("textarea", {
        id: "resposta",
        disabled: true,
        placeholder: "Escreva a resposta ao cliente",
      }),
      el(
        "div",
        { class: "resposta-linha" },
        el("p", { class: "resposta-nota" }, "O envio pelo WhatsApp chega com a integração do Épico 1."),
        el("button", { class: "botao", type: "button", disabled: true }, "Enviar"),
      ),
    ),
  );
}

function linhaDaFicha(nome, valor) {
  return [el("dt", {}, nome), el("dd", {}, valor)];
}

function ficha(conversa) {
  const transferencias = conversa.transferencias ?? [];
  // Quem assumiu e quem tinha o caso antes da primeira transferencia.
  const quemAssumiu = transferencias.length ? nomeDe(transferencias[0].de) : conversa.atendente;
  const assumido = conversa.assumidoEm ? `Às ${hora(conversa.assumidoEm)}, por ${quemAssumiu}` : "Ainda não";
  const consentimento = conversa.consentimentoEm
    ? `Aceito às ${hora(conversa.consentimentoEm)} de ${dia(conversa.consentimentoEm)}`
    : "Não registrado";

  const ultima = transferencias[transferencias.length - 1];
  const transferido = ultima
    ? `${transferencias.length} ${transferencias.length === 1 ? "vez" : "vezes"}, a última às ${hora(ultima.realizadaEm)}`
    : null;

  return el(
    "aside",
    { class: "ficha" },
    el("h2", {}, "Sobre o caso"),
    el(
      "dl",
      {},
      ...linhaDaFicha("Contato", contato(conversa.contato)),
      ...linhaDaFicha("Tipo", TIPOS_DE_CLIENTE[conversa.tipoCliente] ?? conversa.tipoCliente),
      ...linhaDaFicha("Motivo do encaminhamento", motivo(conversa.motivo)),
      ...linhaDaFicha("Na fila desde", `${hora(conversa.abertoEm)} de ${dia(conversa.abertoEm)}`),
      ...linhaDaFicha("Assumido", assumido),
      ...(transferido ? linhaDaFicha("Transferido", transferido) : []),
      ...linhaDaFicha("Consentimento (LGPD)", consentimento),
    ),
  );
}

// ---------- transferencia ----------

/** Quem pode receber o caso: em servico e diferente de quem ja esta com ele. */
function destinosPossiveis(conversa) {
  return equipe.filter((a) => a.disponivel && a.nome !== conversa.atendente);
}

function painelDeTransferencia(conversa) {
  const destinos = destinosPossiveis(conversa);
  if (!destinos.some((a) => String(a.id) === rascunho.destinoId)) rascunho.destinoId = "";

  const $destino = el(
    "select",
    { id: "destino", name: "destino", disabled: destinos.length === 0 || enviando },
    el("option", { value: "" }, destinos.length ? "Escolha quem vai receber o caso" : "Ninguém mais está em serviço agora"),
    ...destinos.map((a) => el("option", { value: a.id, selected: String(a.id) === rascunho.destinoId }, a.nome)),
  );

  const $motivo = el("textarea", {
    id: "motivo-transferencia",
    name: "motivo",
    rows: 3,
    maxlength: LIMITE_DO_MOTIVO,
    required: true,
    disabled: enviando,
    placeholder: "Ex.: cliente quer negociar desconto",
    "aria-describedby": "motivo-ajuda",
  });
  $motivo.value = rascunho.motivo;

  const $contador = el("span", { class: "campo-contador" });
  const $ajuda = el(
    "span",
    { id: "motivo-ajuda", class: erroDoMotivo ? "campo-erro" : "" },
    erroDoMotivo || "Quem receber lê isto no histórico.",
  );
  const $enviar = el("button", { class: "botao", type: "submit" }, enviando ? "Transferindo…" : "Transferir caso");
  const $cancelar = el(
    "button",
    { class: "botao-secundario", type: "button", disabled: enviando, onclick: () => fecharPainel(true) },
    "Cancelar",
  );

  function atualizar() {
    rascunho.destinoId = $destino.value;
    rascunho.motivo = $motivo.value;
    $contador.textContent = `${$motivo.value.length} / ${LIMITE_DO_MOTIVO}`;
    $enviar.disabled = enviando || !$destino.value || $motivo.value.trim() === "";
  }
  $destino.addEventListener("change", atualizar);
  $motivo.addEventListener("input", () => {
    if (erroDoMotivo) {
      erroDoMotivo = "";
      $ajuda.textContent = "Quem receber lê isto no histórico.";
      $ajuda.className = "";
      $motivo.removeAttribute("aria-invalid");
    }
    atualizar();
  });
  if (erroDoMotivo) $motivo.setAttribute("aria-invalid", "true");
  atualizar();

  const $painel = el(
    "form",
    { class: "transferencia", id: "transferencia", "aria-labelledby": "transferir-titulo", novalidate: true },
    el("h2", { id: "transferir-titulo" }, "Transferir este caso"),
    el("p", { class: "transferencia-nota" }, "O cliente continua em atendimento humano, sem voltar para a fila. A espera dele não zera."),
    el("div", { class: "campo" }, el("label", { for: "destino" }, "Para quem"), $destino),
    el(
      "div",
      { class: `campo${erroDoMotivo ? " com-erro" : ""}` },
      el("label", { for: "motivo-transferencia" }, "Motivo da transferência ", el("span", { class: "campo-opcao" }, "(obrigatório)")),
      $motivo,
      el("p", { class: "campo-rodape" }, $ajuda, $contador),
    ),
    el("div", { class: "transferencia-acoes" }, $cancelar, $enviar),
  );

  $painel.addEventListener("submit", (e) => {
    e.preventDefault();
    transferir(conversa);
  });
  $painel.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !enviando) fecharPainel(true);
  });
  return $painel;
}

function fecharPainel(devolverFoco) {
  painelAberto = false;
  erroDoMotivo = "";
  if (devolverFoco) {
    $conteudo.querySelector("#transferencia")?.remove();
    const botao = document.getElementById("botao-transferir");
    botao?.setAttribute("aria-expanded", "false");
    botao?.focus();
  }
}

async function carregarEquipe() {
  try {
    equipe = await api.atendentes();
  } catch {
    equipe = [];
  }
}

async function abrirPainel(conversa) {
  if (painelAberto) {
    fecharPainel(true);
    return;
  }
  if (!atendenteEscolhido()) {
    avisar("erro", "Escolha em “Atendendo como” quem está transferindo o caso.");
    return;
  }
  await carregarEquipe();
  painelAberto = true;
  desenhar(conversa);
  document.getElementById("destino")?.focus();
}

/** O POST da transferencia, com o mesmo tratamento de erro do restante da API. */
async function postarTransferencia(corpo) {
  let resposta;
  try {
    resposta = await fetch(`${API}/api/fila/${encodeURIComponent(ticketId)}/transferir`, {
      method: "POST",
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      body: JSON.stringify(corpo),
    });
  } catch {
    throw new ErroDaApi(0, `Não foi possível falar com o servidor em ${API}.`);
  }
  if (!resposta.ok) {
    let mensagemDeErro = `O servidor respondeu com o código ${resposta.status}.`;
    try {
      const dados = await resposta.json();
      if (dados && dados.erro) mensagemDeErro = dados.erro;
    } catch {
      // corpo vazio ou nao-JSON: fica a mensagem com o codigo
    }
    throw new ErroDaApi(resposta.status, mensagemDeErro);
  }
  return resposta.json();
}

async function transferir(conversa) {
  const quemTransfereId = Number(atendenteEscolhido());
  if (!quemTransfereId) {
    avisar("erro", "Escolha em “Atendendo como” quem está transferindo o caso.");
    return;
  }
  const destinoId = Number(rascunho.destinoId);
  const destino = equipe.find((a) => a.id === destinoId);
  const texto = rascunho.motivo.trim();

  erroDoMotivo = "";
  enviando = true;
  desenhar(conversa);
  try {
    await postarTransferencia({ quemTransfereId, destinoId, motivo: texto });
    painelAberto = false;
    rascunho = { destinoId: "", motivo: "" };
    avisar("sucesso", `Caso transferido para ${destino?.nome ?? "outro atendente"}. A conversa agora está com essa pessoa.`);
  } catch (erro) {
    await tratarErroDaTransferencia(erro);
  } finally {
    enviando = false;
  }
  await carregar();
  if (painelAberto) {
    const campoComErro = erroDoMotivo ? "motivo-transferencia" : "destino";
    document.getElementById(campoComErro)?.focus();
  }
}

/** Decide, para cada tipo de erro, o que a pessoa le e o que fica na tela. */
async function tratarErroDaTransferencia(erro) {
  if (erro.status === 400) {
    // O servidor repete a regra do motivo; a mensagem dele vai no proprio campo.
    erroDoMotivo = erro.message;
    return;
  }
  if (erro.status === 404) {
    if (/^atendente/i.test(erro.message)) {
      await carregarEquipe();
      rascunho.destinoId = "";
      avisar("erro", "Esse atendente não está mais disponível. Escolha outro e tente de novo.");
    } else {
      painelAberto = false;
      avisar("erro", `${erro.message} A tela foi atualizada.`);
    }
    return;
  }
  if (erro.status === 409) {
    let agora = null;
    try {
      agora = await api.conversa(ticketId);
    } catch {
      // sem conexao: cai na mensagem do servidor, abaixo
    }
    if (agora && agora.status !== "EM_ATENDIMENTO") {
      painelAberto = false;
      avisar("erro", "Este caso não está mais em atendimento. A tela foi atualizada.");
      return;
    }
    await carregarEquipe();
    const dica = /gestor/i.test(erro.message) ? " Em “Atendendo como”, escolha quem está com o caso ou um gestor." : "";
    avisar("erro", `${erro.message}${dica}`);
    return;
  }
  avisar("erro", `Não foi possível transferir: ${erro.message} Tente de novo em instantes.`);
}

/** Duas setas em sentidos opostos, no mesmo traco dos outros icones do painel. */
function iconeTransferir() {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  const atributos = {
    class: "icone", viewBox: "0 0 24 24", width: "18", height: "18", fill: "none",
    stroke: "currentColor", "stroke-width": "2", "stroke-linecap": "round", "stroke-linejoin": "round",
    "aria-hidden": "true",
  };
  for (const [nome, valor] of Object.entries(atributos)) svg.setAttribute(nome, valor);
  for (const d of ["M16 3l4 4-4 4", "M20 7H4", "M8 21l-4-4 4-4", "M4 17h16"]) {
    const caminho = document.createElementNS("http://www.w3.org/2000/svg", "path");
    caminho.setAttribute("d", d);
    svg.append(caminho);
  }
  return svg;
}

function desenhar(conversa) {
  document.title = `Conversa com ${contato(conversa.contato)} | Tino`;

  const emAtendimento = conversa.status === "EM_ATENDIMENTO";
  if (!emAtendimento) painelAberto = false;

  const topo = el(
    "div",
    { class: "conversa-topo" },
    el(
      "div",
      { class: "conversa-titulo" },
      el("h1", {}, `Conversa com ${contato(conversa.contato)}`),
      el("p", { class: "conversa-status" }, selo(conversa)),
    ),
    emAtendimento
      ? el(
          "button",
          {
            id: "botao-transferir",
            class: "botao-secundario",
            type: "button",
            "aria-expanded": String(painelAberto),
            "aria-controls": "transferencia",
            onclick: () => abrirPainel(conversa),
          },
          iconeTransferir(),
          "Transferir",
        )
      : null,
  );

  const partes = [topo];

  if (conversa.status === "ABERTO") {
    partes.push(
      el(
        "div",
        { class: "na-fila" },
        el("p", {}, "Este caso ainda está na fila, sem ninguém atendendo."),
        el("button", { class: "botao", type: "button", onclick: (e) => assumir(e.currentTarget) }, "Assumir este caso"),
      ),
    );
  }

  if (emAtendimento && painelAberto) partes.push(painelDeTransferencia(conversa));

  partes.push(el("div", { class: "conversa-grade" }, historico(conversa), ficha(conversa)));
  $conteudo.replaceChildren(...partes);
}

async function carregar() {
  $conteudo.setAttribute("aria-busy", "true");
  try {
    desenhar(await api.conversa(ticketId));
  } catch (erro) {
    if (erro.status === 404) {
      estado(
        "",
        "Este caso não foi encontrado.",
        "Ele pode não existir mais, ou o endereço da página está incompleto.",
        el("a", { href: "index.html" }, "Voltar para a fila"),
      );
    } else {
      estado(
        "erro",
        erro.semConexao ? "Sem conexão com o servidor" : "A conversa não pôde ser carregada",
        erro.semConexao ? `${erro.message} Confira se o back-end está rodando e tente de novo.` : erro.message,
        el("button", { class: "botao", type: "button", onclick: carregar }, "Tentar de novo"),
      );
    }
  } finally {
    $conteudo.setAttribute("aria-busy", "false");
  }
}

async function assumir(botao) {
  const atendenteId = atendenteEscolhido();
  if (!atendenteId) {
    avisar("erro", "Escolha em “Atendendo como” quem vai assumir o caso.");
    return;
  }
  botao.disabled = true;
  botao.textContent = "Assumindo…";
  try {
    await api.assumir(ticketId, atendenteId);
    avisar("sucesso", "Caso assumido. A conversa agora está com você.");
  } catch (erro) {
    if (erro.status === 409) {
      try {
        const agora = await api.conversa(ticketId);
        avisar(
          "erro",
          agora.atendente
            ? `Este caso já foi assumido por ${agora.atendente}.`
            : `Não foi possível assumir este caso: ${erro.message}`,
        );
      } catch {
        avisar("erro", `Não foi possível assumir este caso: ${erro.message}`);
      }
    } else {
      avisar("erro", `${erro.message} Tente de novo em instantes.`);
    }
  }
  await carregar();
}

montarSeletorDeAtendente($atendente).then((escolhido) => (atendenteEscolhido = escolhido));

if (!ticketId || !/^\d+$/.test(ticketId)) {
  estado(
    "",
    "Nenhum caso escolhido.",
    "Abra uma conversa a partir da fila de atendimento.",
    el("a", { href: "index.html" }, "Ir para a fila"),
  );
  $conteudo.setAttribute("aria-busy", "false");
} else {
  carregar();
}

// Tela 7: conversa de um caso. Consome GET /api/fila/{ticketId}/conversa
// e, se o caso ainda estiver na fila, POST /api/fila/{ticketId}/assumir.

import { api, el, motivo, contato, hora, dia, montarSeletorDeAtendente } from "./api.js";

const $conteudo = document.getElementById("conteudo");
const $aviso = document.getElementById("aviso");
const $atendente = document.getElementById("atendente");

const ticketId = new URLSearchParams(location.search).get("ticket");

let atendenteEscolhido = () => null;

const AUTORES = { CLIENTE: "Cliente", CHATBOT: "Tino" };
const CLASSES = { CLIENTE: "de-cliente", CHATBOT: "de-bot", ATENDENTE: "de-atendente" };
const TIPOS_DE_CLIENTE = { EXTERNO: "Cliente externo", INTERNO: "Colaborador interno" };

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

function historico(conversa) {
  const itens = conversa.mensagens.map(mensagem);
  if (conversa.abertoEm) itens.splice(posicaoDoMarco(conversa), 0, marco(conversa));

  return el(
    "section",
    { class: "historico", "aria-label": "Histórico da conversa" },
    el("ol", { class: "mensagens" }, ...itens),
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
  const assumido = conversa.assumidoEm
    ? `Às ${hora(conversa.assumidoEm)}, por ${conversa.atendente}`
    : "Ainda não";
  const consentimento = conversa.consentimentoEm
    ? `Aceito às ${hora(conversa.consentimentoEm)} de ${dia(conversa.consentimentoEm)}`
    : "Não registrado";

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
      ...linhaDaFicha("Consentimento (LGPD)", consentimento),
    ),
  );
}

function desenhar(conversa) {
  document.title = `Conversa com ${contato(conversa.contato)} | Tino`;

  const partes = [
    el(
      "div",
      { class: "conversa-topo" },
      el("h1", {}, `Conversa com ${contato(conversa.contato)}`),
      el("p", { class: "conversa-status" }, selo(conversa)),
    ),
  ];

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

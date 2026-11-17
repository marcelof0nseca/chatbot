// Tela 6: fila de atendimento. Consome GET /api/fila, GET /api/fila/resumo
// e POST /api/fila/{ticketId}/assumir.
//
// Os casos de quem esta usando o painel ("Atendendo como") aparecem primeiro,
// em "Meus casos": e assim que quem recebe uma transferencia encontra o caso.

import { api, el, motivo, contato, partesDaEspera, esperaEmTexto, montarSeletorDeAtendente } from "./api.js";

/** A partir desta espera, em segundos, o tempo do caso aparece em ambar. */
const ESPERA_LONGA = 30 * 60;

/** A fila se atualiza sozinha neste intervalo, em milissegundos. */
const INTERVALO_DE_ATUALIZACAO = 20000;

const $fila = document.getElementById("fila");
const $resumo = document.getElementById("resumo");
const $aviso = document.getElementById("aviso");
const $atualizado = document.getElementById("atualizado");
const $atualizar = document.getElementById("atualizar");
const $atendente = document.getElementById("atendente");

const horaDeRecife = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "America/Recife",
});

let atendenteEscolhido = () => null;
let temDadosNaTela = false;
/** A ultima fila recebida: trocar o atendente redesenha sem ir ao servidor. */
let ultimaFila = null;
let assumindo = false;

function avisar(tipo, ...conteudo) {
  $aviso.replaceChildren(
    el(
      "div",
      { class: `aviso ${tipo}` },
      el("p", {}, ...conteudo),
      el("button", { class: "aviso-fechar", type: "button", onclick: () => $aviso.replaceChildren() }, "Fechar"),
    ),
  );
}

function nomeDoAtendenteEscolhido() {
  const opcao = $atendente.selectedOptions[0];
  return opcao ? opcao.textContent.replace(" (fora de serviço)", "") : "";
}

// ---------- indicadores ----------

function indicador(valor, nome, detalhe) {
  return el(
    "div",
    { class: "indicador" },
    el("span", { class: "indicador-valor" }, valor),
    el("span", { class: "indicador-nome" }, nome),
    detalhe ? el("span", { class: "indicador-detalhe" }, detalhe) : null,
  );
}

function desenharResumo(resumo) {
  const contencao =
    resumo.total === 0 ? "—" : `${Math.round(resumo.taxaContencao * 100)}%`;
  const detalheContencao =
    resumo.total === 0
      ? "nenhuma conversa hoje"
      : `${resumo.resolvidasPeloBot} de ${resumo.total} conversas hoje`;

  $resumo.replaceChildren(
    indicador(resumo.naFila, "aguardando"),
    indicador(resumo.emAtendimento, "em atendimento"),
    indicador(resumo.naFila === 0 ? "—" : esperaEmTexto(resumo.esperaMediaEmSegundos), "espera média na fila"),
    indicador(contencao, "resolvidas pelo Tino", detalheContencao),
  );
}

// ---------- casos ----------

/** "Bruno Dias" vira "BD": identifica de relance quem esta com o caso. */
function iniciais(nome) {
  const partes = (nome ?? "").trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return "?";
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
  return (primeira + ultima).toUpperCase();
}

function caso(item, meu = false) {
  const aguardando = item.status === "ABERTO";
  const { numero, unidade } = partesDaEspera(item.esperaEmSegundos);
  const longa = aguardando && item.esperaEmSegundos >= ESPERA_LONGA;
  const linkConversa = `conversa.html?ticket=${encodeURIComponent(item.ticketId)}`;

  const acao = meu
    ? [el("a", { class: "botao", href: linkConversa }, "Abrir conversa")]
    : aguardando
    ? [
        el("a", { class: "acao-texto", href: linkConversa }, "Ver conversa"),
        el(
          "button",
          {
            class: "botao",
            type: "button",
            onclick: (evento) => assumir(item, evento.currentTarget),
          },
          "Assumir",
        ),
      ]
    : [
        el(
          "span",
          { class: "com-quem" },
          el("span", { class: "iniciais", "aria-hidden": "true" }, iniciais(item.atendente)),
          `com ${item.atendente}`,
        ),
        el("a", { class: "botao-secundario", href: linkConversa }, "Abrir conversa"),
      ];

  return el(
    "li",
    { class: meu ? "caso meu" : aguardando ? "caso" : "caso atendendo" },
    el(
      "div",
      { class: longa ? "espera longa" : "espera", title: longa ? "Esperando há mais de 30 minutos" : null },
      el("span", { class: "espera-numero" }, numero),
      el("span", { class: "espera-unidade" }, unidade || "de espera"),
    ),
    el(
      "div",
      { class: "caso-corpo" },
      el(
        "p",
        { class: "caso-topo" },
        el("span", { class: "contato" }, contato(item.contato)),
        el("span", { class: "motivo" }, motivo(item.motivo)),
      ),
      el(
        "p",
        { class: "previa", title: item.ultimaMensagem || null },
        item.ultimaMensagem ? `“${item.ultimaMensagem}”` : "Sem mensagem do cliente",
      ),
    ),
    el("div", { class: "caso-acao" }, ...acao),
  );
}

function titulo(texto, quantidade) {
  return el("h2", { class: "grupo" }, texto, el("span", { class: "grupo-contagem" }, String(quantidade)));
}

function grupo(texto, itens, meus = false) {
  return [titulo(texto, itens.length), el("ol", { class: meus ? "casos meus" : "casos" }, ...itens.map((i) => caso(i, meus)))];
}

/** "Meus casos": vazio tambem aparece, para dizer o que fazer. */
function grupoDosMeus(meus) {
  if (meus.length) return grupo("Meus casos", meus, true);
  return [
    titulo("Meus casos", 0),
    el("p", { class: "grupo-vazio" }, "Nenhum caso com você agora. Assuma um caso da fila abaixo."),
  ];
}

function desenharFila(fila) {
  ultimaFila = fila;
  const eu = atendenteEscolhido();
  const ehMeu = (item) => eu != null && item.atendenteId != null && String(item.atendenteId) === String(eu);

  const aguardando = fila.filter((item) => item.status === "ABERTO");
  const emAtendimento = fila.filter((item) => item.status === "EM_ATENDIMENTO");
  const meus = emAtendimento.filter(ehMeu);
  const deOutros = emAtendimento.filter((item) => !ehMeu(item));

  if (fila.length === 0) {
    $fila.replaceChildren(
      el(
        "div",
        { class: "estado" },
        el("p", { class: "estado-titulo" }, "Ninguém aguardando agora."),
        el("p", {}, "Quando o Tino passar uma conversa para atendimento humano, ela aparece aqui."),
      ),
    );
    return;
  }

  const partes = eu != null ? grupoDosMeus(meus) : [];
  partes.push(
    ...(aguardando.length
      ? grupo("Aguardando", aguardando)
      : [
          titulo("Aguardando", 0),
          el("p", { class: "grupo-vazio" }, "Ninguém aguardando agora. Os casos abaixo já estão com alguém."),
        ]),
  );
  if (deOutros.length) {
    partes.push(...grupo(eu != null ? "Com outros atendentes" : "Em atendimento", deOutros));
  }
  $fila.replaceChildren(...partes);
}

function desenharFalha(erro) {
  if (temDadosNaTela) {
    avisar("erro", `${erro.message} A fila mostrada é a da última atualização.`);
    return;
  }
  $resumo.replaceChildren();
  $fila.replaceChildren(
    el(
      "div",
      { class: "estado erro" },
      el("p", { class: "estado-titulo" }, erro.semConexao ? "Sem conexão com o servidor" : "A fila não pôde ser carregada"),
      el(
        "p",
        {},
        erro.semConexao ? `${erro.message} Confira se o back-end está rodando e tente de novo.` : erro.message,
      ),
      el("button", { class: "botao", type: "button", onclick: carregar }, "Tentar de novo"),
    ),
  );
}

async function carregar() {
  $fila.setAttribute("aria-busy", "true");
  $atualizar.disabled = true;
  try {
    const [fila, resumo] = await Promise.all([api.fila(), api.resumo()]);
    desenharResumo(resumo);
    desenharFila(fila);
    temDadosNaTela = true;
    $atualizado.textContent = `Atualizado às ${horaDeRecife.format(new Date())}`;
  } catch (erro) {
    desenharFalha(erro);
  } finally {
    $fila.setAttribute("aria-busy", "false");
    $atualizar.disabled = false;
  }
}

// ---------- assumir ----------

async function assumir(item, botao) {
  const atendenteId = atendenteEscolhido();
  if (!atendenteId) {
    avisar("erro", "Escolha em “Atendendo como” quem vai assumir o caso.");
    return;
  }

  botao.disabled = true;
  botao.textContent = "Assumindo…";
  assumindo = true;
  try {
    const assumido = await api.assumir(item.ticketId, atendenteId);
    avisar(
      "sucesso",
      `Caso de ${contato(assumido.contato)} assumido por ${nomeDoAtendenteEscolhido()}. `,
      el("a", { href: `conversa.html?ticket=${encodeURIComponent(assumido.ticketId)}` }, "Abrir conversa"),
    );
  } catch (erro) {
    await explicarFalhaAoAssumir(item, erro);
  } finally {
    assumindo = false;
    await carregar();
  }
}

/** 409 pode ser "alguem assumiu antes" ou outra regra; a fila atual diz qual. */
async function explicarFalhaAoAssumir(item, erro) {
  if (erro.status === 409) {
    let agora = null;
    try {
      agora = (await api.fila()).find((i) => i.ticketId === item.ticketId);
    } catch {
      // sem a fila atual, fica a mensagem do servidor
    }
    if (agora && agora.status !== "ABERTO" && agora.atendente) {
      avisar("erro", `Este caso já foi assumido por ${agora.atendente}. A fila foi atualizada.`);
    } else {
      avisar("erro", `Não foi possível assumir este caso: ${erro.message}`);
    }
  } else if (erro.status === 404) {
    avisar("erro", "Este caso não está mais na fila. A fila foi atualizada.");
  } else {
    avisar("erro", `${erro.message} Tente de novo em instantes.`);
  }
}

// ---------- inicio ----------

$atualizar.addEventListener("click", carregar);
// A fila so e desenhada depois de saber quem esta usando o painel, para
// "Meus casos" nao piscar vazio na primeira carga.
montarSeletorDeAtendente($atendente).then((escolhido) => {
  atendenteEscolhido = escolhido;
  carregar();
});
$atendente.addEventListener("change", () => {
  if (ultimaFila) desenharFila(ultimaFila);
});

setInterval(() => {
  if (!assumindo && !document.hidden) carregar();
}, INTERVALO_DE_ATUALIZACAO);

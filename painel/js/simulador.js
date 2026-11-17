// Simulador do cliente: faz o papel do WhatsApp. Consome
// POST /api/simulador/mensagens e GET /api/simulador/conversa.

import { API, ErroDaApi, el, hora } from "./api.js";

/** Perguntas sugeridas. A base de exemplo responde as tres primeiras (TinoConfigTest). */
const SUGESTOES = [
  "Qual o prazo para troca?",
  "Como acompanho meu pedido?",
  "Quais as formas de pagamento?",
  "Meu pedido chegou quebrado",
];

/** Enquanto a conversa esta aberta, a pagina confere se um atendente assumiu. */
const INTERVALO_DE_ATUALIZACAO = 4000;

const SITUACOES = {
  AGUARDANDO_CONSENTIMENTO: "Aguardando o seu aceite do aviso de privacidade",
  COM_BOT: "Você está falando com o Tino, o assistente virtual",
  AGUARDANDO_ATENDENTE: "Aguardando um atendente. Seu caso está na fila",
  ENCERRADA: "Conversa encerrada",
};

const $inicio = document.getElementById("inicio");
const $telefone = document.getElementById("telefone");
const $nome = document.getElementById("nome");
const $outro = document.getElementById("outro-numero");
const $situacao = document.getElementById("situacao");
const $mensagens = document.getElementById("mensagens");
const $sugestoes = document.getElementById("sugestoes");
const $envio = document.getElementById("envio");
const $texto = document.getElementById("texto");
const $enviar = $envio.querySelector("button");

let telefoneAtual = null;
let enviando = false;
let ultimaQuantidade = 0;

// ---------- API ----------

async function chamar(caminho, opcoes = {}) {
  let resposta;
  try {
    resposta = await fetch(API + caminho, {
      ...opcoes,
      headers: { Accept: "application/json", ...(opcoes.headers ?? {}) },
    });
  } catch {
    throw new ErroDaApi(0, `Não foi possível falar com o servidor em ${API}.`);
  }
  if (!resposta.ok) {
    let mensagem = `O servidor respondeu com o código ${resposta.status}.`;
    try {
      const corpo = await resposta.json();
      if (corpo && corpo.erro) mensagem = corpo.erro;
    } catch {
      // corpo vazio
    }
    throw new ErroDaApi(resposta.status, mensagem);
  }
  return resposta.json();
}

const enviarMensagem = (telefone, nome, texto) =>
  chamar("/api/simulador/mensagens", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ telefone, nome, texto }),
  });

const buscarConversa = (telefone) => chamar(`/api/simulador/conversa?telefone=${encodeURIComponent(telefone)}`);

// ---------- tela ----------

/** Um numero de teste novo a cada clique: cada um vira um cliente diferente. */
function numeroDeTeste() {
  const final = String(Math.floor(Math.random() * 1e8)).padStart(8, "0");
  return `+55 81 9${final.slice(0, 4)}-${final.slice(4)}`;
}

function situacao(conversa) {
  if (!conversa) return "Mande uma mensagem para começar";
  if (conversa.situacao === "COM_ATENDENTE") {
    return conversa.atendente ? `Em atendimento com ${conversa.atendente}` : "Em atendimento com um atendente";
  }
  return SITUACOES[conversa.situacao] ?? conversa.situacao;
}

/** No celular do cliente, as falas dele ficam a direita; o Tino e o atendente, a esquerda. */
function balao(m) {
  const doCliente = m.autor === "CLIENTE";
  const quem = m.autor === "ATENDENTE" ? m.identificacao : null;
  return el(
    "li",
    { class: doCliente ? "balao do-cliente" : m.autor === "ATENDENTE" ? "balao do-atendente" : "balao do-tino" },
    quem ? el("span", { class: "balao-quem" }, quem) : null,
    el("span", { class: "balao-texto" }, m.conteudo),
    el("span", { class: "balao-hora" }, hora(m.dataHora)),
  );
}

function desenhar(conversa) {
  $situacao.textContent = situacao(conversa);
  $situacao.dataset.situacao = conversa ? conversa.situacao : "";
  const mensagens = conversa ? conversa.mensagens : [];
  $mensagens.replaceChildren(...mensagens.map(balao));
  if (mensagens.length !== ultimaQuantidade) {
    $mensagens.scrollTop = $mensagens.scrollHeight;
    ultimaQuantidade = mensagens.length;
  }
  desenharSugestoes(conversa);
}

function desenharSugestoes(conversa) {
  const estado = conversa ? conversa.situacao : null;
  let opcoes = [];
  if (!conversa) opcoes = ["Oi"];
  else if (estado === "AGUARDANDO_CONSENTIMENTO") opcoes = ["Sim", "Não"];
  else if (estado === "COM_BOT") opcoes = SUGESTOES;
  $sugestoes.hidden = opcoes.length === 0;
  $sugestoes.replaceChildren(
    ...opcoes.map((texto) =>
      el("button", { class: "sugestao", type: "button", onclick: () => enviar(texto) }, texto),
    ),
  );
}

function erroNaConversa(erro) {
  const texto = erro.semConexao
    ? `${erro.message} Confira se o back-end está rodando.`
    : erro.message;
  $mensagens.append(el("li", { class: "balao-erro", role: "alert" }, texto));
  $mensagens.scrollTop = $mensagens.scrollHeight;
}

function habilitarEnvio(sim) {
  $texto.disabled = !sim;
  $enviar.disabled = !sim;
}

// ---------- acoes ----------

async function enviar(texto) {
  const fala = texto.trim();
  if (!fala || enviando) return;
  if (!telefoneAtual) telefoneAtual = $telefone.value;
  enviando = true;
  habilitarEnvio(false);
  try {
    desenhar(await enviarMensagem(telefoneAtual, $nome.value, fala));
    $texto.value = "";
  } catch (erro) {
    erroNaConversa(erro);
  } finally {
    enviando = false;
    habilitarEnvio(true);
    $texto.focus();
  }
}

async function comecar(telefone) {
  telefoneAtual = telefone;
  try {
    sessionStorage.setItem("tino.simulador.telefone", telefone);
  } catch {
    // sem armazenamento: vale ate recarregar
  }
  habilitarEnvio(true);
  ultimaQuantidade = -1;
  try {
    desenhar(await buscarConversa(telefone));
  } catch (erro) {
    if (erro.status === 404) desenhar(null);
    else {
      desenhar(null);
      erroNaConversa(erro);
    }
  }
  $texto.focus();
}

async function atualizar() {
  if (!telefoneAtual || enviando || document.hidden) return;
  try {
    desenhar(await buscarConversa(telefoneAtual));
  } catch {
    // sem conversa ainda, ou o servidor caiu: a proxima mensagem mostra o erro
  }
}

// ---------- inicio ----------

$inicio.addEventListener("submit", (evento) => {
  evento.preventDefault();
  comecar($telefone.value);
});

$outro.addEventListener("click", () => {
  $telefone.value = numeroDeTeste();
  $nome.value = "";
  comecar($telefone.value);
});

$envio.addEventListener("submit", (evento) => {
  evento.preventDefault();
  enviar($texto.value);
});

let salvo = null;
try {
  salvo = sessionStorage.getItem("tino.simulador.telefone");
} catch {
  salvo = null;
}
$telefone.value = salvo ?? numeroDeTeste();
desenhar(null);
if (salvo) comecar(salvo);

setInterval(atualizar, INTERVALO_DE_ATUALIZACAO);

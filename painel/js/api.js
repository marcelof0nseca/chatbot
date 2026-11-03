// Comunicacao com o back-end do Tino e utilidades comuns as duas telas.

/** Endereco do back-end. Para apontar para outro servidor, defina window.TINO_API antes deste script. */
export const API = window.TINO_API ?? "http://localhost:8080";

/** Erro de uma chamada: status 0 quando nem chegou ao servidor. */
export class ErroDaApi extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }

  get semConexao() {
    return this.status === 0;
  }
}

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
      // corpo vazio ou nao-JSON: fica a mensagem com o codigo
    }
    throw new ErroDaApi(resposta.status, mensagem);
  }
  return resposta.json();
}

export const api = {
  fila: () => chamar("/api/fila"),
  resumo: () => chamar("/api/fila/resumo"),
  atendentes: () => chamar("/api/atendentes"),
  conversa: (ticketId) => chamar(`/api/fila/${encodeURIComponent(ticketId)}/conversa`),
  assumir: (ticketId, atendenteId) =>
    chamar(
      `/api/fila/${encodeURIComponent(ticketId)}/assumir?atendenteId=${encodeURIComponent(atendenteId)}`,
      { method: "POST" },
    ),
};

/** Por que o caso saiu do bot, nas palavras de quem atende. */
export const MOTIVOS = {
  BAIXA_CONFIANCA: "O Tino não teve certeza da resposta",
  SOLICITACAO_CLIENTE: "O cliente pediu um atendente",
  ASSUNTO_NAO_SUPORTADO: "Assunto que o Tino não responde",
  FORA_DO_EXPEDIENTE: "Chegou fora do expediente",
  TENTATIVAS_ESGOTADAS: "Três tentativas sem resposta certa",
};

export const motivo = (codigo) => MOTIVOS[codigo] ?? codigo;

/** "*********4821" vira "••• 4821": a API ja mascara, aqui so fica mais legivel. */
export const contato = (mascarado) => (mascarado ? mascarado.replace(/^\*+/, "••• ") : "");

/** Espera em segundos dividida em numero e unidade: (41, "min"), (1h05, ""), ("<1", "min"). */
export function partesDaEspera(segundos) {
  const minutos = Math.max(0, Math.floor(segundos / 60));
  if (minutos < 1) return { numero: "<1", unidade: "min" };
  if (minutos < 60) return { numero: String(minutos), unidade: "min" };
  const resto = String(minutos % 60).padStart(2, "0");
  return { numero: `${Math.floor(minutos / 60)}h${resto}`, unidade: "" };
}

/** A mesma espera em uma linha: "41 min", "1h05", "<1 min". */
export function esperaEmTexto(segundos) {
  const { numero, unidade } = partesDaEspera(segundos);
  return unidade ? `${numero} ${unidade}` : numero;
}

/**
 * Horarios chegam do back-end como "2026-11-03T08:58:47.123", ja no fuso de
 * Recife. Sao lidos como texto, sem passar pelo fuso do navegador.
 */
export const hora = (dataHora) => (dataHora ? dataHora.slice(11, 16) : "");
export const dia = (dataHora) => (dataHora ? `${dataHora.slice(8, 10)}/${dataHora.slice(5, 7)}` : "");

/** Cria um elemento com texto seguro: conteudo vindo do cliente nunca vira HTML. */
export function el(tag, atributos = {}, ...filhos) {
  const elemento = document.createElement(tag);
  for (const [nome, valor] of Object.entries(atributos)) {
    if (valor === false || valor == null) continue;
    if (nome === "class") elemento.className = valor;
    else if (nome.startsWith("on")) elemento.addEventListener(nome.slice(2), valor);
    else if (valor === true) elemento.setAttribute(nome, "");
    else elemento.setAttribute(nome, valor);
  }
  for (const filho of filhos.flat()) {
    if (filho == null || filho === false) continue;
    elemento.append(filho instanceof Node ? filho : document.createTextNode(String(filho)));
  }
  return elemento;
}

// ---------- atendente da sessao (enquanto nao ha login) ----------

const CHAVE_ATENDENTE = "tino.atendenteId";

function lerAtendente() {
  try {
    return localStorage.getItem(CHAVE_ATENDENTE);
  } catch {
    return null;
  }
}

function gravarAtendente(id) {
  try {
    localStorage.setItem(CHAVE_ATENDENTE, id);
  } catch {
    // navegador sem armazenamento: a escolha vale so ate recarregar
  }
}

/**
 * Preenche o seletor "Atendendo como" com a equipe vinda da API.
 * Devolve uma funcao que diz o id escolhido.
 */
export async function montarSeletorDeAtendente(select) {
  try {
    const equipe = await api.atendentes();
    const salvo = lerAtendente();
    select.replaceChildren(
      ...equipe.map((a) =>
        el("option", { value: a.id, selected: String(a.id) === salvo }, a.disponivel ? a.nome : `${a.nome} (fora de serviço)`),
      ),
    );
    if (!equipe.some((a) => String(a.id) === salvo) && equipe.length) gravarAtendente(String(equipe[0].id));
    select.disabled = equipe.length === 0;
  } catch {
    select.replaceChildren(el("option", {}, "equipe indisponível"));
    select.disabled = true;
  }
  select.addEventListener("change", () => gravarAtendente(select.value));
  return () => (select.disabled ? null : select.value);
}

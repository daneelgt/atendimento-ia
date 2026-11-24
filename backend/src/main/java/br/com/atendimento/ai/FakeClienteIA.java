package br.com.atendimento.ai;

import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Prioridade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "IA" local por palavras-chave. Permite rodar o sistema sem chave de API e serve de
 * linha de base na avaliação. Devolve JSON como um provedor real, então passa pelo mesmo validador.
 */
@Component
@ConditionalOnProperty(name = "app.ia.provedor", havingValue = "fake", matchIfMissing = true)
public class FakeClienteIA implements ClienteIA {

    private record Regra(Pattern padrao, Assunto assunto, Prioridade prioridade, String resposta) {
        Regra(String regex, Assunto assunto, Prioridade prioridade, String resposta) {
            this(Pattern.compile(regex), assunto, prioridade, resposta);
        }
    }

    private static final Pattern MENSAGEM = Pattern.compile("<mensagem>(.*)</mensagem>", Pattern.DOTALL);

    // A ordem importa: regras mais específicas primeiro.
    private static final List<Regra> REGRAS = List.of(
            new Regra("invadi|hacke|senha (foi )?alterada sem|acesso nao autorizado|compras? que nao fiz",
                    Assunto.CONTA_INVADIDA, Prioridade.URGENTE,
                    "Olá! Recebemos seu relato e ele foi encaminhado com prioridade ao nosso suporte técnico, que vai analisar a segurança da sua conta."),
            new Regra("fora do ar|nao (esta )?funciona(ndo)? nada|ninguem consegue|servico (caiu|parado)|indisponivel",
                    Assunto.SERVICO_INDISPONIVEL, Prioridade.URGENTE,
                    "Olá! Recebemos seu relato sobre a indisponibilidade. Nossa equipe técnica vai verificar a situação."),
            new Regra("duas vezes|cobranca duplicada|cobrado em dobro|duplicad|cobrado 2 vezes",
                    Assunto.COBRANCA_DUPLICADA, Prioridade.ALTA,
                    "Olá! Vamos verificar sua cobrança. Nosso setor financeiro irá analisar o ocorrido."),
            new Regra("reembols|estorno|devolu(cao|cao do dinheiro)|dinheiro de volta",
                    Assunto.REEMBOLSO, Prioridade.MEDIA,
                    "Olá! Recebemos sua solicitação. Nosso setor financeiro vai analisar o pedido e retornar com as orientações."),
            new Regra("cancel|encerrar (minha )?(conta|assinatura)|nao quero mais",
                    Assunto.CANCELAMENTO_ASSINATURA, Prioridade.MEDIA,
                    "Olá! Recebemos sua solicitação de cancelamento. Nossa equipe vai dar andamento e entrar em contato."),
            new Regra("cobra(do|ram|nca) (indevid|errad|a mais)|nao reconheco|valor (errado|diferente)|cobrado sem",
                    Assunto.COBRANCA_INDEVIDA, Prioridade.ALTA,
                    "Olá! Vamos verificar a cobrança informada. Nosso setor financeiro irá analisar o ocorrido."),
            new Regra("segunda via|boleto|nota fiscal|fatura (do mes|atual)",
                    Assunto.SEGUNDA_VIA, Prioridade.BAIXA,
                    "Olá! Recebemos seu pedido. Nosso setor financeiro vai enviar as orientações."),
            new Regra("trocar (o )?cartao|alterar (a )?forma de pagamento|mudar (o )?cartao|forma de pagamento",
                    Assunto.ALTERACAO_PAGAMENTO, Prioridade.BAIXA,
                    "Olá! Recebemos seu pedido. Nosso setor financeiro vai orientar sobre a alteração."),
            new Regra("entrar na (minha )?conta|login|senha|acessar (minha )?conta|codigo de verificacao",
                    Assunto.ACESSO_CONTA, Prioridade.ALTA,
                    "Olá! Recebemos seu relato. Nosso suporte técnico vai ajudar a recuperar o acesso."),
            new Regra("lent|demora (muito )?para carregar|travando",
                    Assunto.LENTIDAO, Prioridade.MEDIA,
                    "Olá! Recebemos seu relato. Nosso suporte técnico vai investigar a lentidão."),
            new Regra("erro|bug|nao consigo (acessar|usar)|problema(s)? para acessar|falha",
                    Assunto.ERRO_SISTEMA, Prioridade.MEDIA,
                    "Olá! Recebemos seu relato. Nosso suporte técnico vai analisar o problema."),
            new Regra("empresa|corporativ|licencas|cnpj|equipe de \\d+",
                    Assunto.PROPOSTA_EMPRESARIAL, Prioridade.MEDIA,
                    "Olá! Obrigado pelo interesse. Nossa equipe comercial vai entrar em contato."),
            new Regra("upgrade|plano (superior|maior|premium)|mudar de plano|aumentar (meu )?plano",
                    Assunto.UPGRADE_PLANO, Prioridade.BAIXA,
                    "Olá! Obrigado pelo interesse. Nossa equipe comercial vai apresentar as opções."),
            new Regra("planos?|preco|valores|quanto custa|desconto|promocao",
                    Assunto.INFORMACAO_PLANOS, Prioridade.BAIXA,
                    "Olá! Obrigado pelo interesse. Nossa equipe comercial vai enviar as informações."),
            new Regra("parabens|otimo atendimento|adorei|excelente|obrigad",
                    Assunto.ELOGIO, Prioridade.BAIXA,
                    "Olá! Muito obrigado pelo retorno, ficamos felizes com sua mensagem."),
            new Regra("sugest|seria bom|poderiam (adicionar|criar)",
                    Assunto.SUGESTAO, Prioridade.BAIXA,
                    "Olá! Obrigado pela sugestão, ela será encaminhada à equipe responsável.")
    );

    private final ObjectMapper mapper;

    public FakeClienteIA(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String completar(String instrucoesSistema, String conteudoUsuario) {
        Matcher m = MENSAGEM.matcher(conteudoUsuario);
        String texto = normalizar(m.find() ? m.group(1) : conteudoUsuario);

        Regra regra = REGRAS.stream().filter(r -> r.padrao().matcher(texto).find()).findFirst().orElse(null);
        ObjectNode json = mapper.createObjectNode();
        if (regra == null) {
            json.put("categoria", "OUTROS").put("prioridade", "BAIXA").put("assunto", "DUVIDA_GERAL")
                    .put("confianca", 0.40)
                    .put("resposta_sugerida", "Olá! Recebemos sua mensagem e nossa equipe vai analisá-la.");
        } else {
            json.put("categoria", regra.assunto().getCategoria().name()).put("prioridade", regra.prioridade().name())
                    .put("assunto", regra.assunto().name()).put("confianca", 0.90)
                    .put("resposta_sugerida", regra.resposta());
        }
        return json.toString();
    }

    @Override
    public String modelo() {
        return "fake-palavras-chave";
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}

package br.com.atendimento.service;

import br.com.atendimento.config.ProcessamentoProperties;
import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Departamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Decide para onde vai um atendimento classificado pela IA.
 * Confiança >= limite: encaminha ao departamento da categoria (status CLASSIFICADO).
 * Abaixo do limite: fica sem departamento aguardando revisão humana da classificação.
 */
@Service
public class EncaminhamentoService {

    private final ProcessamentoProperties props;

    public EncaminhamentoService(ProcessamentoProperties props) {
        this.props = props;
    }

    public boolean confiancaSuficiente(BigDecimal confianca) {
        return confianca != null && confianca.compareTo(props.confiancaMinima()) >= 0;
    }

    public void encaminharAutomaticamente(Atendimento atendimento) {
        if (confiancaSuficiente(atendimento.getConfianca())) {
            atendimento.encaminharPara(departamentoPara(atendimento.getCategoria()));
        } else {
            atendimento.aguardarRevisaoManual();
        }
    }

    public Departamento departamentoPara(Categoria categoria) {
        return categoria.getDepartamentoPadrao();
    }
}

/**
 * 
 */
package br.com.sixinf.sockettracket.persistencia;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import br.com.sixinf.ferramentas.persistencia.Entidade;

/**
 * @author maicon
 *
 */
@Entity
@Table(name="tracker")
public class Tracker implements Entidade {
	
	@Id
	@SequenceGenerator(name="seqTracker", sequenceName="tracker_id_seq")
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="seqTracker")
	@Column(name="id")
	private Long id;
	
	@Column(name="numero_serie")
	private String numeroSerie;
	
	@Column(name="data_fabricacao")
	@Temporal(TemporalType.DATE)
	private Date dataFabricacao;
	
	@Column(name="modelo_tracker")
	private String modeloTracker;
	
	@Column(name="fabricante_tracker")
	private String fabricanteTracker;
	
	@Column(name="data_registro_tracker")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataRegistroTracker;
	
	@Column(name="data_ativacao_tracker")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataAtivacaoTracker;
	
	@Column(name="status_registro")
	private Character statusRegistro;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumeroSerie() {
		return numeroSerie;
	}

	public void setNumeroSerie(String numeroSerie) {
		this.numeroSerie = numeroSerie;
	}

	public Date getDataFabricacao() {
		return dataFabricacao;
	}

	public void setDataFabricacao(Date dataFabricacao) {
		this.dataFabricacao = dataFabricacao;
	}

	public String getModeloTracker() {
		return modeloTracker;
	}

	public void setModeloTracker(String modeloTracker) {
		this.modeloTracker = modeloTracker;
	}

	public String getFabricanteTracker() {
		return fabricanteTracker;
	}

	public void setFabricanteTracker(String fabricanteTracker) {
		this.fabricanteTracker = fabricanteTracker;
	}

	public Date getDataRegistroTracker() {
		return dataRegistroTracker;
	}

	public void setDataRegistroTracker(Date dataRegistroTracker) {
		this.dataRegistroTracker = dataRegistroTracker;
	}

	public Date getDataAtivacaoTracker() {
		return dataAtivacaoTracker;
	}

	public void setDataAtivacaoTracker(Date dataAtivacaoTracker) {
		this.dataAtivacaoTracker = dataAtivacaoTracker;
	}

	public Character getStatusRegistro() {
		return statusRegistro;
	}

	public void setStatusRegistro(Character statusRegistro) {
		this.statusRegistro = statusRegistro;
	}

	@Override
	public Long getIdentificacao() {
		return id;
	}

}

/**
 * 
 */
package br.com.sixinf.sockettracket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import br.com.sixinf.ferramentas.Utilitarios;
import br.com.sixinf.ferramentas.dao.DAOException;
import br.com.sixinf.sockettracket.persistencia.Posicao;
import br.com.sixinf.sockettracket.persistencia.SocketTrackerDAO;
import br.com.sixinf.sockettracket.persistencia.Tracker;

/**
 * @author maicon
 * 
 */
public class ThreadMensagemSocket implements Runnable {

	private static Logger LOG = Logger.getLogger(ThreadMensagemSocket.class);

	private Socket socket;

	public ThreadMensagemSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {

		try {
			
			BufferedReader br = null;
			BufferedWriter bw = null;
			
			try {

				LOG.debug("Conexão recebida do IP: " + socket.getInetAddress());
	
				br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				bw = new BufferedWriter(new OutputStreamWriter(
						socket.getOutputStream()));
	
				bw.write("%001\n\n");
				bw.flush();
	
				// NANO ,024249.000,V, 0.000000,N, 0.000000,E,0.0,
				// 0.0,101010,020,005
				// NANO ,000321.000,V,8960.000000,N, 0.000000,E,0.0, 0.0,101010,
				// ,E , ,025,E2P
				// NANO ,182605.000,A,2028.772932,S,
				// 5436.260003,W,0.00,187.13,130214,,E,,025,���
				// $GPRMC,232029.000,A,2028.746172,S,05435.774688,W,0.7,170.4,291213,
				// , ,A*6A
				String line = null;
				while ((line = br.readLine()) != null) {
					LOG.debug("Recebido: " + line);
					if (line.startsWith("NANO")) {
						String[] partes = line.split(",");
	
						StringBuilder str = new StringBuilder();
						String strId = partes[13];
						if (strId.isEmpty())
							strId = "000";
						ByteBuffer id = ByteBuffer.wrap(("0" + strId).getBytes());
						str.append("NANO").append(',');
						str.append(id.getInt()).append(','); // id dispositivo
						str.append("$GPRMC").append(',');
						str.append(partes[1]).append(',');
						str.append(partes[2]).append(',');
						str.append(partes[3]).append(',');
						str.append(partes[4]).append(',');
						str.append(partes[5]).append(',');
						str.append(partes[6]).append(',');
						str.append(partes[7]).append(',');
						str.append(partes[8]).append(',');
						str.append(partes[9]);
	
						gravaMensagemBanco(str.toString());
	
					} else // TKSIM - Android Tracker Simulator
					if (line.startsWith("TKSIM")) {
	
						gravaMensagemBanco(line);
	
					} else if (line.startsWith("STX")) {
						String[] partes = line.split(",");
	
						if ("F".equals(partes[15]))
							gravaMensagemBanco(line);
	
					}
	
				}
	
				LOG.debug("Finalizado");
	
				/*
				 * byte[] bytes = Utilitarios.fazLeituraStreamEmByteArray(is);
				 * String mensagem = new String(bytes); LOG.debug("Recebido: " +
				 * mensagem); counter.decrementAndGet();
				 * 
				 * // fazer verificação de checksum e controle de retransmissão //
				 * TODO [Maicon] Implementar...
				 * 
				 * // grava mensagem no banco de dados gravaMensagemBanco(mensagem);
				 */
				
			} finally {
				if (br != null)
					br.close();
				if (bw != null)
					bw.close();
				
				socket.close();
			}

		} catch (Exception e) {
			LOG.error("Erro ao processar mensagem de posição", e);
		}

	}

	// NANO,[id],$GPRMC,024249.000,V,2028.746172,S,05435.774688,W,0.0,170.0,101010,020,005
	// TKSIM,1111,$GPRMC,232029.000,A,2028.746172,S,05435.774688,W,0.7,170.4,291213,,
	// ,A*6A
	// STX ,7777,$GPRMC,010105.000,A,2029.290310,S,05435.288530,W,1.6,
	// 0.0,180314, ,
	// ,A*68,F,,imei:013227009739313,0/8,600.3,Battery=96%,,0,724,06,04F3,7910;03
	private void gravaMensagemBanco(String mensagem) throws DAOException {
		String[] partes = mensagem.split(",");
		if (partes.length == 0)
			throw new UnsupportedOperationException("Mensagem inválida! "
					+ mensagem);

		// String p0 = partes[0]; // Dispositivo Origem
		String serial = partes[1]; // serial do equipamento

		SocketTrackerDAO dao = SocketTrackerDAO.getInstance();
		Tracker t = dao.buscarTodosTrackersPeloSerial(serial);
		if (t == null)
			throw new UnsupportedOperationException(
					"Tracker com o número de série " + serial
							+ " não foi encontrado");

		String tipoMensagem = partes[2];
		MessageType m = MessageType.valueOf(tipoMensagem);
		if (m == null)
			throw new UnsupportedOperationException(
					"Tipo de mensagem inválida! " + tipoMensagem);

		switch (m) {
		case $GPRMC:
			String strHoraCoordenada = partes[3];
			String coordenadaValida = partes[4];
			if (!coordenadaValida.equals("A")) { // coordenada não é válida
				LOG.warn("Coordenadas não fixadas pelos satélites (status = V): "
						+ mensagem);
				return;
			}
			String latitude = partes[5];
			String latitudeQ = partes[6];
			String longitude = partes[7];
			String longitudeQ = partes[8];
			String strVelocidade = partes[9];
			String strCurso = partes[10];
			String strDataCoordenada = partes[11];
			// String declinacaoMag = partes[12];
			// String declinacaoMagQ = partes[13];

			Calendar c = GregorianCalendar.getInstance(TimeZone
					.getTimeZone("GMT"));
			int hora = Integer.parseInt(strHoraCoordenada.substring(0, 2));
			int minuto = Integer.parseInt(strHoraCoordenada.substring(2, 4));
			int segundo = Integer.parseInt(strHoraCoordenada.substring(4, 6));
			int dia = Integer.parseInt(strDataCoordenada.substring(0, 2));
			int mes = Integer.parseInt(strDataCoordenada.substring(2, 4));
			int ano = Integer.parseInt(strDataCoordenada.substring(4, 6));
			ano += 2000; // corrige ano de 2 para 4 dígitos

			c.set(Calendar.HOUR_OF_DAY, hora);
			c.set(Calendar.MINUTE, minuto);
			c.set(Calendar.SECOND, segundo);
			c.set(Calendar.DAY_OF_MONTH, dia);
			c.set(Calendar.MONTH, mes);
			c.set(Calendar.YEAR, ano);

			c.setTimeZone(TimeZone.getDefault());

			// convertendo as coordenadas para graus decimais
			int idx = latitude.indexOf('.');

			int graus = Integer.parseInt(latitude.substring(0, idx - 2));
			double min = Double.parseDouble(latitude.substring(idx - 2));
			// double sec = Double.parseDouble(latitude.substring(5, 11));
			// sec /= 10000; // ajusta casas decimais para o formato 74.6172

			double latitudeDec = graus + (min / 60);
			if (latitudeQ.equals("S"))
				latitudeDec = -latitudeDec;

			latitudeDec = Utilitarios.round(latitudeDec, 6);

			idx = longitude.indexOf('.');

			graus = Integer.parseInt(longitude.substring(0, idx - 2));
			min = Double.parseDouble(longitude.substring(idx - 2));
			// sec = Double.parseDouble(longitude.substring(6, 12));
			// sec /= 10000; // ajusta casas decimais para o formato 74.6172

			double longitudeDec = graus + (min / 60);
			if (longitudeQ.equals("W"))
				longitudeDec = -longitudeDec;

			longitudeDec = Utilitarios.round(longitudeDec, 6);

			double velocidade = Double.parseDouble(strVelocidade);
			double curso = Double.parseDouble(strCurso);

			Posicao p = new Posicao();
			p.setDataHoraCoordenada(c.getTime());
			p.setDataHoraMensagem(Calendar.getInstance().getTime());
			p.setLatitude(latitude);
			p.setLatitudeQuadrante(latitudeQ.charAt(0));
			p.setLongitude(longitude);
			p.setLongitudeQuadrande(longitudeQ.charAt(0));
			p.setMensagem(mensagem);
			p.setLatitudeDecimal(latitudeDec);
			p.setLongitudeDecimal(longitudeDec);
			p.setStatusRegistro('A');
			p.setVelocidade(velocidade);
			p.setCurso(curso);
			p.setTracker(t);

			dao.adicionar(p);

			LOG.debug("Mensagem gravada no banco.");

			break;
		default:
			break;
		}

	}

	private enum MessageType {
		$GPRMC;
	}

}

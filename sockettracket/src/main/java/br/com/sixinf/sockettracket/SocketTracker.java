/**
 * 
 */
package br.com.sixinf.sockettracket;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import br.com.sixinf.ferramentas.Utilitarios;
import br.com.sixinf.ferramentas.dao.DAOException;
import br.com.sixinf.ferramentas.persistencia.AdministradorPersistencia;
import br.com.sixinf.ferramentas.persistencia.PersistenciaException;
import br.com.sixinf.sockettracket.persistencia.Posicao;
import br.com.sixinf.sockettracket.persistencia.SocketTrackerDAO;
import br.com.sixinf.sockettracket.persistencia.Tracker;

/**
 * @author maicon
 *
 */
public class SocketTracker {
	
	private static Logger LOG = Logger.getLogger(SocketTracker.class);
	
	private List<ThreadSocket> threads = new ArrayList<ThreadSocket>();
	private static final int NUM_THREADS_POOL = 10;
	private static AtomicInteger counter = new AtomicInteger(0);
	
		
	private void criaPoolThreads(){
		for (int i=0; i<NUM_THREADS_POOL; i++) {
			ThreadSocket t = new ThreadSocket("Thread Socket Pool " + i);
			t.start();
			threads.add(t);
		}
	}
	
	public void esperaConexoes(){
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(2828);
			while (true) {
				Socket s = ss.accept();
				int i = counter.getAndIncrement();
				if (i > NUM_THREADS_POOL - 1)
					i %= NUM_THREADS_POOL;					
				
				ThreadSocket t = threads.get(i);
				t.putSocket(s);
			}
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ss != null) 				
					ss.close();				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args){
		//LOG.info("Esperando conexão...");
		try {
			AdministradorPersistencia.iniciaUnidadeDePersistencia("trackerUnit");
			
		} catch (PersistenciaException e) {
			LOG.error("Erro na inicialização da persistência", e);
		}
		SocketTracker st = new SocketTracker();
		st.criaPoolThreads();
		st.esperaConexoes();
	}
	
	private static class ThreadSocket extends Thread {
		
		private boolean isAtivo = true;
		
		private BlockingQueue<Socket> sockets = new LinkedBlockingQueue<>();
		
		public ThreadSocket(String threadName) {
			super(threadName);
		}
		
		public void putSocket(Socket s) throws InterruptedException{
			sockets.put(s);
		}
		
		@Override
		public void run() {
			
			while (isAtivo) {
				
				InputStream is = null;
				
				try {
				
					Socket socket = sockets.take();
					
					LOG.debug("Conexão recebida do IP: " + socket.getInetAddress());
					
					// recebe
					is = socket.getInputStream();
					byte[] bytes = Utilitarios.fazLeituraStreamEmByteArray(is);
					
					String mensagem = new String(bytes);
					
					LOG.debug("Recebido: " + mensagem);
					
					socket.close();
					
					counter.decrementAndGet();
					
					// fazer verificação de checksum e controle de retransmissão
					// TODO [Maicon] Implementar...
					
					// grava mensagem no banco de dados
					gravaMensagemBanco(mensagem);
					
				} catch (IOException | InterruptedException | DAOException e) {
					LOG.error("Erro ao processar mensagem de posição", e);
				} finally {
					try {
						if (is != null)
							is.close();
					} catch (IOException e) {
						LOG.error("Erro ao fechar o stream", e);
					}
				}
				
			}
			
		}

		// $GPRMC,232029.000,A,2028.746172,S,05435.774688,W,0.7,170.4,291213,,,A*6A
		private void gravaMensagemBanco(String mensagem) throws DAOException {
			String[] partes = mensagem.split(",");
			if (partes.length == 0)
				throw new UnsupportedOperationException("Mensagem inválida! " + mensagem);
			
			String p0 = partes[0];
			
			String serial = p0.substring(0, p0.indexOf('$'));
				
			SocketTrackerDAO dao = SocketTrackerDAO.getInstance();
			Tracker t = dao.buscarTodosTrackersPeloSerial(serial);
			if (t == null)
				throw new UnsupportedOperationException("Tracker com o número de série " + serial + " não foi encontrado");
			
			String tipoMensagem = p0.substring(p0.indexOf('$'));
			MessageType m = MessageType.valueOf(tipoMensagem);
			if (m == null)
				throw new UnsupportedOperationException("Tipo de mensagem inválida! " + tipoMensagem);
			
			switch (m) {
				case $GPRMC:
					String strHoraCoordenada = partes[1];
					String coordenadaValida = partes[2];
					if (!coordenadaValida.equals("A")) { // coordenada não é válida
						LOG.warn("Coordenada com indicação que não é válida na string. " + mensagem);
						return;
					}
					String latitude = partes[3];
					String latitudeQ = partes[4];
					String longitude = partes[5];
					String longitudeQ = partes[6];
					String strVelocidade = partes[7];
					String strCurso = partes[8];
					String strDataCoordenada = partes[9];
					//String declinacaoMag = partes[10];
					//String declinacaoMagQ = partes[11];
					
					Calendar c = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
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
					int graus = Integer.parseInt(latitude.substring(0, 2));
					double min = Double.parseDouble(latitude.substring(2, 11));
					//double sec = Double.parseDouble(latitude.substring(5, 11));
					//sec /= 10000; // ajusta casas decimais para o formato 74.6172
					
					double latitudeDec = graus + (min / 60);
					if (latitudeQ.equals("S"))
						latitudeDec = -latitudeDec;
										
					graus = Integer.parseInt(longitude.substring(0, 3));
					min = Double.parseDouble(longitude.substring(3, 5));
					//sec = Double.parseDouble(longitude.substring(6, 12));
					//sec /= 10000; // ajusta casas decimais para o formato 74.6172
					
					double longitudeDec = graus + (min / 60);
					if (longitudeQ.equals("W"))
						longitudeDec = -longitudeDec;
					
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
					
					break;
				default: 
					break;
			}
			
		}
		
		
		
	}
	
	private enum MessageType {
		$GPRMC;
	}

}

/**
 * 
 */
package br.com.sixinf.sockettracket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import br.com.sixinf.ferramentas.persistencia.AdministradorPersistencia;
import br.com.sixinf.ferramentas.persistencia.PersistenciaException;

/**
 * @author maicon
 *
 */
public class SocketTracker {
	
	private static Logger LOG = Logger.getLogger(SocketTracker.class);
	
	public static void esperaConexoes(){
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(2828);
			while (true) {
				Socket s = ss.accept();				
				new Thread(new ThreadSocket(s)).start();
			}
			
		} catch (IOException e) {
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
		
		esperaConexoes();
	}
	
	private static class ThreadSocket implements Runnable {
		
		private Socket s;
		
		public ThreadSocket(Socket s) {
			this.s = s;
		}

		@Override
		public void run() {
			LOG.debug("Conexão recebida do IP: " + s.getInetAddress());
			
			DataInputStream dis = null;
			DataOutputStream dos = null;
			try {
				// recebe
				dis = new DataInputStream(s.getInputStream());
				String mensagem = dis.readUTF();
				LOG.debug("Recebido: " + mensagem);
				
				if (mensagem.isEmpty())
					mensagem = "EM BRANCO";
				// envia
				dos = new DataOutputStream(s.getOutputStream());
				dos.writeUTF("Resposta: " + mensagem);
				dos.flush();
				LOG.debug("Enviado: " + mensagem);
				
			} catch (IOException e) {
				LOG.error("Erro de IO no socket", e);
			} finally {
				try {
					if (dis != null)
						dis.close();
					if (dos != null)
						dos.close();
				} catch (IOException e) {
					LOG.error("Erro ao fechar o socket", e);
				}
			}
			
		}
		
	}

}

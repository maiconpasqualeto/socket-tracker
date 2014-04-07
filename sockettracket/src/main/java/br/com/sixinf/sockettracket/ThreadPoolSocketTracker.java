/**
 * 
 */
package br.com.sixinf.sockettracket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

/**
 * @author maicon
 * 
 */
public class ThreadPoolSocketTracker implements Runnable {
	
	private static final Logger LOG = Logger.getLogger(ThreadPoolSocketTracker.class);

	protected int serverPort = 0;
	protected ServerSocket serverSocket = null;
	protected boolean isStopped = false;
	protected Thread runningThread = null;
	protected ExecutorService threadPool = Executors.newFixedThreadPool(100);

	public ThreadPoolSocketTracker(int port) {
		this.serverPort = port;
	}

	public void run() {
		synchronized (this) {
			this.runningThread = Thread.currentThread();
		}
		openServerSocket();
		while (!isStopped()) {
			Socket clientSocket = null;
			try {
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				if (isStopped()) {
					LOG.debug("Server Stopped.");
					return;
				}
				throw new RuntimeException("Error accepting client connection", e);
			}
			this.threadPool.execute(new ThreadMensagemSocket(clientSocket));
		}
		this.threadPool.shutdown();
		LOG.debug("Server Stopped.");
	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void stop() {
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	private void openServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port", e);
		}
	}
}
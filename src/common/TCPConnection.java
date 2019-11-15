package common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPConnection{
	public InetAddress address;
	private int port;
	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private boolean ready;

	public TCPConnection(InetAddress address, int port){
		this.address = address;
		this.port = port;
		this.ready = false;
	}

	/** Opens a connection with the receiver.
	 */
	public void openConnection(){
		try{
			this.socket = new Socket(InetAddress.getLocalHost(), port);
			this.output = new ObjectOutputStream(socket.getOutputStream());
			this.input = new ObjectInputStream(socket.getInputStream());
			this.ready = true;
		}catch(UnknownHostException e){
			System.out.println("ERROR-TCP: unknown host." + e.getMessage());
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("ERROR-TCP: IO exception." + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** Closes the connection with the receiver.
	 */
	public void closeConnection(){
		try{
			this.socket.close();
			this.output.close();
			this.input.close();
			this.ready = false;
		}catch(IOException e){
			System.out.println("ERROR-TCP: Closing connection." + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** Gets the socket used by the connection.
	 * @return a Socket
	 */
	public Socket getSocket(){
		return this.socket;
	}
	
	/** Gets the output stream of the connection.
	 * @return an ObjectOutputStream 
	 */
	public ObjectOutputStream getOutput(){
		return this.output;
	}
	
	/** Gets the input stream of the connection.
	 * @return an ObjectInputStream 
	 */
	public ObjectInputStream getInput(){
		return this.input;
	}
	
	/** Checks if the connection is open.
	 * @return true if the connection is open, false otherwise.
	 */
	public boolean isOpen(){
		return this.ready;
	}
	
}

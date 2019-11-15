package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.*;

public class ServerMain{
	
	public static void main(String[] args){
		SocialManager manager = new SocialManager();
		setupRMI(manager);
		Thread threadTCP = new Thread(){
			  public void run(){
				  setupTCP(manager);
			  }
		};
		Thread threadUDP = new Thread(){
			public void run(){
				setupUDP(manager);
			}
		};
		threadTCP.start();
		threadUDP.start();
	}
	
	/** creates an RMI listener to handle RMI requests.
	 * @param manager 
	 */
	private static void setupRMI(SocialManager manager){
		try{
			SocialManagerInterface managerStub = (SocialManagerInterface) UnicastRemoteObject.exportObject(manager,0);
			Registry registry = LocateRegistry.createRegistry(manager.settings.REGISTRY_PORT);
			registry.rebind(manager.settings.OBJECT_NAME, managerStub);
			System.out.println("[SERVER-RMI] RMI ready.");
		}catch(RemoteException e){
			System.out.println("[SERVER-RMI] ERROR: Server RemoteException. " + e.getMessage());
		}
	}
	
	/** Creates a TCP listener that waits for TCP connections and passes them to a new thread to handle them.
	 * @param manager
	 */
	private static void setupTCP(SocialManager manager){
		// Setting up TCP
		ExecutorService requests = null;
		try(ServerSocket server = new ServerSocket()){
			server.bind(new InetSocketAddress(InetAddress.getLocalHost(), manager.settings.TCP_PORT));
			requests = Executors.newCachedThreadPool();
			System.out.println("[SERVER-TCP] TCP ready.");
			while(true){
				try{
					Socket client = server.accept();
					TCPHandler handler = new TCPHandler(manager, client);
					requests.submit(handler);
				}catch(IOException e){
					System.out.println("[SERVER-TCP] ERROR: Server IOException." + e.getMessage());
				}
			}
		}catch(IOException e){
			System.out.println("[SERVER-TCP] ERROR: Server IOException." + e.getMessage());
		}finally{
			if(requests != null){
				requests.shutdown();
				manager.keeper.shutdown();
			}
		}
	}
	
	/** Creates an UDP listener that waits for UDP connections and passes them to a new thread to handle them.
	 * @param manager
	 */
	private static void setupUDP(SocialManager manager){
		ExecutorService requests = null;
		try(DatagramSocket socket = new DatagramSocket(manager.settings.UDP_PORT)){
			DatagramPacket packet = new DatagramPacket(new byte[manager.settings.UDP_LENGTH], manager.settings.UDP_LENGTH);
			requests = Executors.newCachedThreadPool();
			System.out.println("[SERVER-UDP] UDP ready.");
			while(true){
				socket.receive(packet);
				String jmessage = new String(
						packet.getData(),
						packet.getOffset(),
						packet.getLength(),
						"UTF-8");
				UDPHandler handler = new UDPHandler(manager, socket, jmessage);
				requests.submit(handler);
			}
			
		}catch(SocketException e){
			System.out.println("[SERVER-UDP] ERROR: Server SocketException." + e.getMessage());
		}catch(UnknownHostException e){
			System.out.println("[SERVER-UDP] ERROR: Server UnknownHostException." + e.getMessage());
		}catch(IOException e){
			System.out.println("[SERVER-UDP] ERROR: Server IOException." + e.getMessage());
		}finally{
			if(requests != null){
				requests.shutdown();
				manager.keeper.shutdown();
			}
		}
	}

}

package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.*;

import org.json.simple.JSONObject;

public class TCPListener implements Runnable{
	private SocialClient client;
	private int listenerPort;
	private ServerSocket listenerSocket;
	private ExecutorService fileReceiver;
	
	public TCPListener(SocialClient client){
		try{
			this.client = client;
			this.listenerSocket = new ServerSocket(0);
			this.listenerPort = listenerSocket.getLocalPort();
			this.fileReceiver = Executors.newCachedThreadPool();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/** Gets the address used by the client to wait for TCP requests.
	 * @return the InetAddress used by the client. 
	 */
	public InetAddress getAddress(){
		return this.listenerSocket.getInetAddress();
	}
	
	/** Gets the public IP address of the client.
	 * @return a String containing the IP address. 
	 */
	public String getMyIp(){
		try{
			URL url = new URL("http://checkip.amazonaws.com/");
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String ip = br.readLine();
			System.out.println("IP: " + ip);
			return ip;
		}catch(MalformedURLException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	/** Gets the port used by the client to wait for TCP requests.
	 * @return an int containing the port. 
	 */
	public int getListenerPort(){
		return this.listenerPort;
	}

	@Override
	public void run(){
		//will stay up and running all the time on the same socket
		System.out.println("[TPC-LISTENER] Running.");
		while(true){
			try(Socket socket = this.listenerSocket.accept();
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					){
				//get message
				JSONObject jmessage = (JSONObject) input.readObject();
				Message message = new Message(jmessage);
				String sender = message.getSender();
				String text = message.getData();
				//check message
				if(message.getType() == Message.MSG 
						&& message.getReceiver().compareTo(client.getUsername())==0
						&& text != null){
					System.out.println("[TPC-LISTENER] Message from [" + sender + "]");
					String signedText = Translate.getTranslation(message, client.getLanguage());
					ArrayList<String> messages = client.friendMessages.get(sender);
					messages.add(signedText);
					//update GUI's messages
                  	client.getGui().updateChats(sender);
                  	client.getGui().unreadMessages(sender);
                  	
				}else if(message.getType() == Message.FILE
						&& message.getReceiver().compareTo(client.getUsername()) == 0){
					System.out.println("[TPC-LISTENER] File from [" + sender + "]:" + message.getPort());
					//get data from your NIOListener
					Message ack = new Message(Message.ACK, client.getUsername());
					output.writeObject(ack.toJson());
					output.flush();
					//splitting message payload since it contains "address fileName"
					String[] split = message.getData().split(" ");
					String address = split[0].replace("/", ""); //erasing / from the address
					String fileName = split[1];
					InetAddress senderAddress = InetAddress.getByName(address);
					//i have all the data that i need, so i just connect to the sender using its address and port
					this.fileReceiver.submit(new Runnable(){
						public void run(){
							receiveFile(senderAddress, message.getPort(), fileName);
						}
					});
				}
			}catch(IOException e){
				System.out.println("[TPC-LISTENER] ERROR: IOException.");
				e.printStackTrace();
			}catch(ClassNotFoundException e){
				System.out.println("[TPC-LISTENER] ERROR: ClassNotFound.");
				e.printStackTrace();
			}
		}
	}
	
	/** Starts receiving a new file from a friend. All new files will be saved locally in a directory called Download.
	 * @param senderAddress address used by the file sender
	 * @param senderPort port used by the file sender
	 * @param fileName name of the file that has to be received
	 * @return a String containing the receiver. 
	 */
	public void receiveFile(InetAddress senderAddress, long senderPort, String fileName){
		System.out.println("[FILE] Receiving " + fileName + " from " + senderAddress.toString());
		try(SocketChannel socket = SocketChannel.open(new InetSocketAddress(
				InetAddress.getLocalHost(), (int) senderPort))){
			ByteBuffer buffer = ByteBuffer.allocate(client.settings.PACKET_SIZE);
			//creating Download folder if it doesn't exist yet
			String name = Paths.get(fileName).getFileName().toString();
			File folder = new File("download");
			if(!folder.exists()){
				folder.mkdirs();
			}
			//opening the file in write mode
			String downloadPath = new String(folder.getName() + "/" + name);
			FileChannel out = FileChannel.open(Paths.get(downloadPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			//send to the sender (myName, fileName)
			buffer.put(fileName.getBytes(), 0, fileName.getBytes().length);
			int nameLength = buffer.position();
			System.out.println("[RECEIVER] Writing back: " + fileName + ":" + fileName.getBytes().length);
			//buffer that stores fileName dimensions
			ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
			lengthBuffer.putInt(nameLength);
			lengthBuffer.flip();
			//writing from buffer to channel
			socket.write(lengthBuffer);
			buffer.flip();
			socket.write(buffer);
			buffer.clear();
			//creating bufferArray that has to be sent to my peer
			ByteBuffer[] bufferArray = new ByteBuffer[2];
			bufferArray[0] = ByteBuffer.allocate(Integer.BYTES);
			bufferArray[1] = buffer;
			//reading the file from my peer
			int iter = 0;
			while(socket.read(buffer) != -1){
				buffer.flip();
				out.write(buffer);
				buffer.compact();
				System.out.println("[RECEIVING] Receiving " + fileName + "... [part:" + iter +"]");
				iter++;
			}
			out.close();
			System.out.println("[FILE-Handler] Finished transferring file.");
			this.client.getGui().printMessage("[FILE] File " + name + " received!");
		}catch(IOException e){
			e.printStackTrace();
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	}

}

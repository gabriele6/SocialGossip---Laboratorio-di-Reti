package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class NIOListener implements Runnable{
	private SocialClient client;
	private int port;
	private ServerSocketChannel server;
	private Selector selector; 
	
	public NIOListener(SocialClient client){
		this.client = client;
		try{
			this.server = ServerSocketChannel.open();
			this.selector = Selector.open();
			InetSocketAddress inetAddress = new InetSocketAddress(client.tcp.address,0);
			server.bind(inetAddress);
			this.port = server.socket().getLocalPort();
			server.configureBlocking(false);
			server.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("[NIO-Listener] NIO Listener ready! (port:" + port + ")");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	/** Gets the port used by the NIOListener.
	 * @return port an int rappresenting the port number.
	 */
	public int getPort(){
		return this.port;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(){
		while(true){
			try{
				this.selector.selectedKeys().clear();
				this.selector.select();
				for(SelectionKey key :  this.selector.selectedKeys()){
					if(key.isAcceptable()){
	                  	try{
	                  		SocketChannel socket = ((ServerSocketChannel) key.channel()).accept();
	                      	socket.configureBlocking(false);
	                      	ByteBuffer[] attachments = new ByteBuffer[2];
	                      	attachments[0] = ByteBuffer.allocate(Integer.BYTES); //for the length
	                      	attachments[1] = ByteBuffer.allocate(client.settings.PACKET_SIZE); //for the data
	                      	//registering the receiver and preparing for reading his attachments
	                      	socket.register(selector, SelectionKey.OP_READ, attachments);
	                  	}catch(IOException e){
	                      	e.printStackTrace();
	                    }
	                }
					if(key.isReadable()){
	                  	try{
	                  		System.out.println("[FILE] New key is Readable!");
	                    	SocketChannel socket = (SocketChannel) key.channel();
	                      	ByteBuffer[] buffers = (ByteBuffer[]) key.attachment();
	                      	long response = socket.read(buffers);
	                      	//check if it has reached the end of the stream
	                      	if(response == -1){
	                      		socket.close();
	                      		key.cancel();
	                      		System.out.println("[FILE] Client went home.");
	                      		continue;
	                      	}
	                      	if(!buffers[0].hasRemaining()){
	                      		buffers[0].flip();
	                          	int length = buffers[0].getInt();
	                          	if(length == buffers[1].position()){
	                            	String fileName = new String(buffers[1].array(), 0, buffers[1].position()).trim();
	                              	System.out.println("[FILE] Requested file: " + fileName);
	                              	ByteBuffer buffer = ByteBuffer.allocate(client.settings.PACKET_SIZE);
	                              	ArrayList<Object> attachments = new ArrayList<Object>();
	                              	attachments.add(buffer);
	                              	FileChannel file = FileChannel.open(Paths.get(fileName), StandardOpenOption.READ);
	                              	attachments.add(file);
	                              	socket.register(selector, SelectionKey.OP_WRITE, attachments);
	                              	buffer.flip(); //so that i'm able to write direcly later
	                            }
	                        }
	                    }catch(IOException e){
	                    	System.out.println("[NIO-Listener] Error reading from client: " + e.getMessage());
	                    	key.cancel();
	                    }
					}
					if(key.isWritable()){
						try{
							System.out.println("[FILE] New key is Writable!");
							SocketChannel socket = (SocketChannel) key.channel();
							ArrayList<Object> attachments = (ArrayList<Object>) key.attachment();
							ByteBuffer buffer = (ByteBuffer) attachments.get(0);
							socket.write(buffer);
							if(!buffer.hasRemaining()){ 
								FileChannel file = (FileChannel) attachments.get(1);
								long transfered = file.transferTo(file.position(), file.size()-file.position(), socket);
								file.position(file.position() + transfered);
								//if there's nothing else that has to be written
								if(file.position() == file.size()){
									file.close();
									socket.close();
									System.out.println("[FILE] Done transfering!");
									this.client.getGui().printMessage("Transfer completed!");
								}
							}
						}catch(IOException e){
	                    	System.out.println("[NIOListener] Error writing to client: " + e.getMessage());
	                    	e.printStackTrace();
	                    	key.cancel();
	                    }
					}
				}
			}catch(IOException e){
            	System.out.println("[NIOListener] Error reading from client: " + e.getMessage());
            }
		}
			
	}
	
}

package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.*;

public class UDPListener implements Runnable{
	private SocialClient client;
	private int groupPort;
	private boolean running;
	
	public UDPListener(SocialClient client, int groupPort){
		this.client = client;
		this.groupPort = groupPort;
		this.running = true;
	}
	
	@Override
	public void run(){
		System.out.println("[UDP-LISTENER] Running on port: " + groupPort);
		try(MulticastSocket socket = new MulticastSocket(this.groupPort)){
			DatagramPacket packet = new DatagramPacket(new byte[client.settings.UDP_LENGTH], client.settings.UDP_LENGTH);
			InetAddress multicastAddress = InetAddress.getByName(client.settings.MULTICAST_ADDRESS);
			socket.setSoTimeout(0);
			socket.joinGroup(multicastAddress);
			while(running){
				//waiting for a UDP packet from the group
				socket.receive(packet);
				System.out.println("[UDP-LISTENER] Received packet!");
				String jmessage = new String(
						packet.getData(),
						packet.getOffset(),
						packet.getLength(),
						"UTF-8");
				JSONParser parser = new JSONParser();
		  		JSONObject jobj = (JSONObject) parser.parse(jmessage);
		  		Message message = new Message(jobj);
		  		if(message.getType() == Message.DELETEGROUP){
		  			System.out.println("[UDP-Listener] Got a delete message!");
		  			//stopping the listener since the group doesn't exist anymore
		  			running = false;
		  		}else{
		  			String groupName = message.getReceiver();
			  		System.out.println("[UDP-LISTENER] Message from [" + groupName + "]");
			  		String signedText = Translate.getTranslation(message, client.getLanguage());
			  		client.groupMessages.get(groupName).add(signedText);
			  		client.getGui().updateChats(groupName);
			  		client.getGui().unreadMessages(groupName);
		  		}
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch(ParseException e){
			e.printStackTrace();
		}
		
	}
	

}

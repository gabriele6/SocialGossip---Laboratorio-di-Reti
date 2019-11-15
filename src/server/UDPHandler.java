package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.*;

public class UDPHandler implements Runnable{
  	private SocialManager manager;
  	private DatagramSocket socket;
  	private String jmessage;
  
  	public UDPHandler(SocialManager manager, DatagramSocket socket, String jmessage){
  		this.manager = manager;
  		this.socket = socket;
  		this.jmessage = jmessage;
    }
  
  	@Override
  	public void run(){
  		try{
	  		InetAddress multicastAddress = InetAddress.getByName(manager.settings.MULTICAST_ADDRESS);
	  		//server has received a packet from a client, so it has to forward it to the multicast group
	  		JSONParser parser = new JSONParser();
	  		JSONObject jobj = (JSONObject) parser.parse(jmessage);
	  		Message message = new Message(jobj);
	  		if(message.getType() == Message.GROUPMSG){
	  			String sender = message.getSender();
	  			String groupName = message.getReceiver();
	  			System.out.println("[UDP-HANDLER] " + sender + " sent message to " + groupName);
	  			Group group = manager.getGroupDatabase().getGroup(groupName);
	  			//check if other users are online
	  			int counter = 0;
	  			if(group != null){
	  				group.mutex.lock();
	  				for(String name : group.getMembers()){
	  					if(name.compareTo(sender) != 0 && manager.getUserDatabase().isOnline(name))
	  						counter++;
	  				}
	  				group.mutex.unlock();
	  				//at least someone is online, so the server forwards the UDP packet to the group
	  				if(counter > 0){
	  					int groupPort = group.getPort();
  			  			DatagramPacket multiPacket = new DatagramPacket(
  								jmessage.getBytes("UTF-8"), 0,
  								jmessage.getBytes("UTF-8").length,
  								multicastAddress, groupPort);
  			  			this.socket.send(multiPacket);
  			  			System.out.println("[UDP-HANDLER] Packet sent: " + jobj.get("data"));
	  				}else{ //noone is online, so the server notifies the client
	  					try{
	  						manager.getUserDatabase().getUser(sender).getStub().nobodyOnline(groupName);
	  					}catch(RemoteException e){}
	  				}
	  			}
	  		//sender decided to delete the group, so the message will be forwarded to all the group members, 
	  		//so that they can stop their UDPListener for this group
	  		}else if(message.getType() == Message.DELETEGROUP){
	  			String sender = message.getSender();
	  			String groupName = message.getReceiver();
	  			System.out.println("[UDP-HANDLER] " + sender + " is killing " + groupName);
	  			int groupPort = manager.getGroupDatabase().getGroup(groupName).getPort();
	  			DatagramPacket multiPacket = new DatagramPacket(
						jmessage.getBytes("UTF-8"), 0,
						jmessage.getBytes("UTF-8").length,
						multicastAddress, groupPort);
	  			this.socket.send(multiPacket);
	  			System.out.println("[UDP-HANDLER] Packet sent: " + jobj.get("data"));
	  		}
		}catch(IOException e){
			e.printStackTrace();
		}catch(ParseException e){
			e.printStackTrace();
		}
		
      	
    }

}

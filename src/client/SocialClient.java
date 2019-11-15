package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.*;

public class SocialClient extends RemoteObject 
	implements SocialClientInterface{
	private static final long serialVersionUID = 1L;
	
	public Settings settings;
	private ClientGui gui = null;
	private String username;
	private String password;
	private String language;
	public TCPConnection tcp;
	//private UDPConnection udp;
	SocialClientInterface stub;
	SocialManagerInterface manager;
	ExecutorService executorTCP; 
	TCPListener tcplistener;
	NIOListener niolistener;
	ExecutorService executorUDP;
	ExecutorService executorNIO;
	public ConcurrentHashMap<String,Boolean> friends; //<Friend,Status>
	public ConcurrentHashMap<String,Integer> groups; //<Group,Port>
	public ConcurrentHashMap<String,String> groupRole; //<Group,Role>
	public ConcurrentHashMap<String,ArrayList<String>> friendMessages;
	public ConcurrentHashMap<String,ArrayList<String>> groupMessages;
	
	public SocialClient(String username, String password, String language){
		try{
			this.username = username;
			this.password = password;
			this.language = language;
			this.settings = new Settings();
			this.friends = new ConcurrentHashMap<String,Boolean>();
			this.groups = new ConcurrentHashMap<String,Integer>();
			this.groupRole = new ConcurrentHashMap<String,String>();
          	this.friendMessages = new ConcurrentHashMap<String,ArrayList<String>>();
			this.groupMessages = new ConcurrentHashMap<String,ArrayList<String>>();
			//preparing RMI
			stub = (SocialClientInterface) UnicastRemoteObject.exportObject(this,0);
			manager = (SocialManagerInterface) LocateRegistry.getRegistry(this.settings.REGISTRY_PORT).lookup(this.settings.OBJECT_NAME);
		}catch(RemoteException e){
			System.out.println("ERROR: something happened with the client." + e.getMessage());
		}catch(NotBoundException e){
			System.out.println("ERROR: Unbound. " + e.getMessage());
		}
	}
	
	// ---------- LOGIN/LOGOUT ------------
	/** Logs in the user to Social Gossip using an RMI connection with the server.
	 * @param gui client's Graphic User Interface
	 * @return true if the user logged in successfully, false otherwise.
	 */
	public boolean login(ClientGui gui){
		try{
			//starting listeners
			this.executorTCP = Executors.newSingleThreadExecutor();
			this.executorUDP = Executors.newCachedThreadPool();
			this.executorNIO = Executors.newSingleThreadExecutor();
			this.tcplistener = new TCPListener(this);
			this.executorTCP.submit(this.tcplistener);
			this.tcp = new TCPConnection(this.tcplistener.getAddress(), this.settings.TCP_PORT);
			this.niolistener = new NIOListener(this);
			this.executorNIO.submit(this.niolistener);
			
			if(this.manager.login(this.username, this.password, this.language, this, 
					this.tcplistener.getAddress(), this.tcplistener.getListenerPort())){
				this.assignGui(gui);
				System.out.println("[CLIENT-" + this.username.toUpperCase() + "] I'm " + this.username + "! (tcp-listener: " + this.tcplistener.getMyIp() + "/" + this.tcplistener.getListenerPort() + ")");
				this.getFriends();
				this.getAllGroups();
				//subscribing to all joined groups messages
				for(String group : this.groupRole.keySet()){
					if(this.groupRole.get(group).compareTo("(admin)") == 0 
							|| this.groupRole.get(group).compareTo("(member)") == 0){
						UDPListener listener = new UDPListener(this, this.groups.get(group));
						executorUDP.submit(listener);
					}
				}
				return true;
			}
			this.executorTCP.shutdown();
			this.executorUDP.shutdown();
			this.executorNIO.shutdown();
			return false;
		}catch(RemoteException e){
			System.out.println("ERROR: can't reach the server." + e.getMessage());
			return false;
		}
	}
	
	/** Registers the user to Social Gossip using an RMI connection.
	 * @return true if the user was successfully registered, false otherwise.
	 */
	public boolean register(){
		try{
			return this.manager.register(this.username, this.password, this.language);
		}catch(RemoteException e){
			System.out.println("ERROR: can't reach the server." + e.getMessage());
			return false;
		}
	}
	
	/** Logs out the user from Social Gossip using an RMI connection.
	 * @return true if the user was successfully logged out, false otherwise.
	 */
	public boolean logout(){
		try{
			this.manager.logout(this.username);
			UnicastRemoteObject.unexportObject(this, true);
			this.assignGui(null);
			this.executorTCP.shutdown();
			this.executorUDP.shutdown();
			this.executorNIO.shutdown();
			return true;
		}catch(NoSuchObjectException e){
			System.out.println("Could not unexport: " + e.getMessage());
		}catch(RemoteException e){
			System.out.println("LOGOUT RemoteException: " + e.getMessage());
		}
		return false;
	}
	
	// ---------- ACTIONS ----------
	/** Sends a TCP request to the server to retreive a list of users or group with a name containing the selected substring.
	 * @param name selected substring 
	 * @return a String[] containing the selected users and groups.
	 */
	public String[] searchFor(String name){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.SEARCH, this.username, name);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreplyList = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message replyList = new Message(jreplyList);
			JSONParser parser = new JSONParser();
			JSONArray replyArray = (JSONArray) parser.parse(replyList.getData());
			String[] result = new String[replyArray.size()]; 
			int i = 0;
			while(i<replyArray.size()){
				result[i] = (String) replyArray.get(i);
				i++;
			}
			return result;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(ParseException e){
			e.printStackTrace();
		}
		return null;
	}
	
	/** Sends a TCP request to the server to retreive user's friends.
	 * @return true if the list has been retreived successfully, false otherwise.
	 */
	public synchronized boolean getFriends(){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.FRIENDLIST, this.username);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreplyFriends = (JSONObject) tcp.getInput().readObject();
			JSONObject jreplyStats = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message messageFriends = new Message(jreplyFriends);
			Message messageStats = new Message(jreplyStats);
			JSONParser parser = new JSONParser();
			JSONArray friendsArray = (JSONArray) parser.parse(messageFriends.getData());
			JSONArray statsArray = (JSONArray) parser.parse(messageStats.getData());
			Iterator<?> i = friendsArray.iterator();
			Iterator<?> j = statsArray.iterator();
			while(i.hasNext() && j.hasNext()){
				String name = (String) i.next();
				Boolean status = (Boolean) j.next();
				this.friends.put(name, status);
				ArrayList<String> messages = new ArrayList<String>();
				this.friendMessages.put(name, messages);
			}
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(ParseException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a TCP request to the server to add a new user to the friend list.
	 * @param friend name of the friend that the user wants to add
	 * @return true if the user has been added to the friend list, false otherwise.
	 */
	public boolean addFriend(String friend){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.ADDFRIEND, this.username, friend);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreply = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message reply = new Message(jreply);
			if(reply.getType() == Message.ACK)
				return true;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a TCP request to the server to retreive the groups created on Social Gossip.
	 * @return true if the request has been completed, false otherwise.
	 */
	public synchronized boolean getAllGroups(){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.ALLGROUPS, this.username);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreplyGroups = (JSONObject) tcp.getInput().readObject();
			JSONObject jreplyPorts = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message replyGroups = new Message(jreplyGroups);
			Message replyPorts = new Message(jreplyPorts);
			JSONParser parser = new JSONParser();
			JSONArray groupsArray = (JSONArray) parser.parse(replyGroups.getData());
			JSONArray portsArray = (JSONArray) parser.parse(replyPorts.getData());
			//clearing the old version of the array
			this.groups = new ConcurrentHashMap<String,Integer>();
			Iterator<?> i = groupsArray.iterator();
			Iterator<?> j = portsArray.iterator();
			while(i.hasNext() && j.hasNext()){
				String groupName = (String) i.next();
				Long groupPort = (Long) j.next();
				String groupRole = new String("(none)");
				//check the tag and remove it, inserting in the special HashMap (groupRole)
				if(groupName.contains("(admin)") || groupName.contains("(member)")){
					String[] split = groupName.split(" ");
					groupRole = split[0];
					groupName = split[1];
				}
				this.groups.put(groupName, groupPort.intValue());
				this.groupRole.put(groupName, groupRole);
				//adding GroupMessages Array
				ArrayList<String> messages = new ArrayList<String>();
				this.groupMessages.put(groupName, messages);
			}
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(ParseException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a TCP request to the server to create a new group. The user that has requested this operation will be the group's administrator.
	 * @param groupName name of the new group
	 * @return true if the group has been successfully created, false otherwise.
	 */
	public boolean createGroup(String groupName){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.NEWGROUP, this.username, groupName);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreply = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message reply = new Message(jreply);
			if(reply.getType() == Message.ACK)
				return true;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a TCP request to the server to retreive the members of a group.
	 * @param groupName name of the selected group
	 * @return an ArrayList containing all group members.
	 */
	public ArrayList<String> getMembers(String groupName){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.MEMBERS, this.username, groupName);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreply = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message reply = new Message(jreply);
			ArrayList<String> members = new ArrayList<String>();
			if(reply.getType() == Message.ACK){
				JSONParser parser = new JSONParser();
				JSONArray jarray = (JSONArray) parser.parse(reply.getData()) ;
				Iterator<?> i = jarray.iterator();
				while(i.hasNext()){
					String member = (String) i.next();
					members.add(member);
				}
			}
			return members;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(ParseException e){
			e.printStackTrace();
		}
		return null;
	}
	
	/** Sends a TCP request to the server to join a selected group.
	 * @param groupName nameof the selected group
	 * @return true if the user has been successfully added to the group, false otherwise.
	 */
	public boolean joinGroup(String groupName){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.JOIN, this.username, groupName);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreply = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message reply = new Message(jreply);
			if(reply.getType() == Message.ACK)
				return true;
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a TCP request to the server to remove an existent group from Social Gossip. The operation can be done only by the group's administrator.
	 * @param groupName name of the group that has to be deleted
	 * @return true if the group has been deleted successfully, false otherwise.
	 */
	public boolean deleteGroup(String groupName){
		try{
			//open tcp connection and send request
			tcp.openConnection();
			Message msg = new Message(Message.DELETEGROUP, this.username, groupName);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//get reply and close connection
			JSONObject jreply = (JSONObject) tcp.getInput().readObject();
			tcp.closeConnection();
			//get data from reply
			Message reply = new Message(jreply);
			if(reply.getType() == Message.ACK){
				try(DatagramSocket socket = new DatagramSocket()){
					System.out.println("[CLIENT] Preparing delete packet.");
					Message message = new Message(Message.DELETEGROUP, this.username, this.language, groupName, 0, "Deleting group.");
					//InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
					InetAddress address = InetAddress.getLocalHost();
					String jmessage = message.toJson().toJSONString();
					//building packet
					DatagramPacket packet = new DatagramPacket(
							jmessage.getBytes("UTF-8"), 0,
							jmessage.getBytes("UTF-8").length,
							address, this.settings.UDP_PORT);
					//sending to server 
					socket.send(packet);
					return true;
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a message to a friend, using a TCP connection with the server.
	 * @param receiver name of the friend 
	 * @param text the message that has to be sent to the friend
	 * @return true if the message was successfully sent, false otherwise.
	 */
	public boolean sendMessage(String receiver, String text){
      	try{
      		tcp.openConnection();
      		String signedText = new String("[" + this.username + "]: " + text);
      		Message msg = new Message(Message.MSG, this.username, this.language, receiver, 0, text);
            tcp.getOutput().writeObject(msg.toJson());
            tcp.getOutput().flush();
            //waiting for the server to ACK
            JSONObject jreply = (JSONObject) tcp.getInput().readObject();
            tcp.closeConnection();
            //get data from the reply
            Message reply = new Message(jreply);
            if(reply.getType() == Message.ACK){
                //message sent, adding to the list of the gui
            	ArrayList<String> messages = this.friendMessages.get(receiver);
            	messages.add(signedText);
                return true;
            }
        }catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
      return false;
    }
	
	/** Sends a TCP request to the server, so that it can inform a friend that the user wants to transfer a file.
	 * @param receiver name of the friend 
	 * @param fileName name of the file that the user wants to send
	 * @return true if the request reached the receiver, false otherwise.
	 */
	public boolean requestSendFile(String receiver, String fileName){
        try{
        	//create a new thread as a server (sender) for the file 
        	tcp.openConnection();
        	Message msg = new Message(Message.FILE, this.username, this.language, receiver, this.niolistener.getPort(), fileName);
			tcp.getOutput().writeObject(msg.toJson());
			tcp.getOutput().flush();
			//waiting for the server to ACK
	        JSONObject jreply = (JSONObject) tcp.getInput().readObject();
	        tcp.closeConnection();
	        Message reply = new Message(jreply);
	        //check if ack or nack
	        if(reply.getType() == Message.ACK){
	        	System.out.println("[CLIENT-FILE] Sent message: " + msg.toJson());
	        	return true;
	        }
        }catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** Sends a UDP message to the server, that will forward it to the selected group using multicast. 
	 * @param group name of the group
	 * @param text the message that has to be sent to the group
	 * @return true if the message was successfully sent, false otherwise.
	 */
	public boolean sendGroupMessage(String group, String text){
		try(DatagramSocket socket = new DatagramSocket()){
			System.out.println("[CLIENT] Preparing packet.");
			Message message = new Message(Message.GROUPMSG, this.username, this.language, group, 0, text);
			InetAddress address = InetAddress.getLocalHost();
			String jmessage = message.toJson().toJSONString();
			//building packet
			DatagramPacket packet = new DatagramPacket(
					jmessage.getBytes("UTF-8"), 0,
					jmessage.getBytes("UTF-8").length,
					address, this.settings.UDP_PORT);
			//sending to server 
			socket.send(packet);
			System.out.println("[CLIENT] Datagram sent!");
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
	
	// ----------- AUX ----------
	/** Assigns a new Graphic User Interface to the client.
	 * @param gui the GUI 
	 */
	public void assignGui(ClientGui gui) throws RemoteException{
		this.gui = gui;
	}
	
	/** Gets clien's Graphic User Interface.
	 * @return the GUI 
	 */
	public ClientGui getGui(){
		return this.gui;
	}
	
	/** Gets the username associated with the client.
	 * @return the username
	 */
	public String getUsername(){
		return this.username;
	}
	
	// ---------- NOTIFICATION ----------
	/** Prints a notification on client's GUI
	 * @param message a string containing the message
	 */
	public void sendNotification(String message) throws RemoteException{
		getGui().printNotification(message);
	}
	
	/** Notifies the user that a new friend has been added.
	 * @param name name of the new friend
	 */
	public void friendAdded(String name) throws RemoteException{
		Boolean status = new Boolean(true);
		this.friends.put(name, status);
		this.friendMessages.put(name, new ArrayList<String>());
		this.getGui().updateFriends();
	}
	
	/** Notifies the user that one of its friends changed its status.
	 * @param name name of the friend
	 * @param status new status
	 */
	public void statusChanged(String name, Boolean status) throws RemoteException{
		System.out.println("[CLIENT-" + this.username.toUpperCase() + "] received statusChange(" + name +"," + status.toString() + ")");
		this.friends.put(name, status);
		this.getGui().updateFriends();
	}
	
	/** Notifies the user that it has successfully joined a new group.
	 * @param name name of the group
	 * @param port port used by the group
	 */
	public void groupJoined(String name, int port) throws RemoteException{
		UDPListener udplistener = new UDPListener(this, port);
		this.executorUDP.submit(udplistener);
		this.getAllGroups();
		this.getGui().updateGroups();
	}
	
	/** Notifies the user that it has successfully created a new group.
	 * @param name name of the group
	 * @param port port used by the group
	 */
	public void groupCreated(String name, int port) throws RemoteException{
		UDPListener udplistener = new UDPListener(this, port);
		this.executorUDP.submit(udplistener);
		this.getAllGroups();
		this.getGui().updateGroups();
	}
	
	/** Notifies the user that it has successfully deleted a new group.
	 * @param name name of the group
	 */
	public void groupDeleted(String name) throws RemoteException{
		this.groups.remove(name);
		this.groupRole.remove(name);
		this.getGui().updateGroups();
		this.getGui().printNotification("Group " + name + " has been deleted!");
	}
	
	/** Notifies the user that it is the only online user in the selected group.
	 * @param name name of the group
	 */
	public void nobodyOnline(String groupName){
		this.getGui().printNotification("Noone can hear you!");
	}
	
	/** Gets the language used by the client.
	 * @return the language 
	 */
	public String getLanguage(){
		return this.language;
	}
	
	/** Checks if the client is still online, using RMI.
	 */
	public void youAlive(){}

}

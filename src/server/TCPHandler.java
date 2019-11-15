package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.*;

public class TCPHandler implements Runnable{
	private SocialManager manager;
	private Socket client;
	
	public TCPHandler(SocialManager manager, Socket client){
		this.manager = manager;
		this.client = client;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(){
		try(ObjectInputStream input = new ObjectInputStream(client.getInputStream());
				ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
				){
			//reading request
			JSONObject jmessage = (JSONObject) input.readObject();
			Message message = new Message(jmessage);
			switch(message.getType()){
			
				case Message.SEARCH:{
					String senderName = message.getSender();
					String name = message.getData();
					System.out.println("[TCP-HANDLER] " + senderName + " requested searchFor(" + name + ")");
					ArrayList<String> users = manager.getUserDatabase().searchUsers(name);
					ArrayList<String> groups = manager.getGroupDatabase().searchGroups(name);
					//unifying results and adding a tag for each element (user or group)
					JSONArray jresult = new JSONArray();
					//removing sender and his friends from the list
					users.remove(senderName);
					User sender = manager.getUserDatabase().getUser(senderName);
					for(String user : sender.getFriends())
						users.remove(user);
					for(String user : users)
						jresult.add("(user) " + user);
					for(String group : groups)
						jresult.add("(group) " + group);
					//sending back result
					Message reply = new Message(Message.ACK, null, jresult.toJSONString());
					output.writeObject(reply.toJson());
					output.flush();
					break;
				}
			
				case Message.FRIENDLIST:{
					System.out.println("[TCP-HANDLER] " + message.getSender() + " requested friendlist.");
					CopyOnWriteArrayList<String> friends = manager.getUserDatabase().getUser(message.getSender()).getFriends();
					JSONArray friendsArray = new JSONArray();
					JSONArray statsArray = new JSONArray();
					for(String name : friends){
						Boolean status = manager.getUserDatabase().isOnline(name);
						friendsArray.add(name);
						statsArray.add(status);
					}
					//sending both friendName and friendStatus to the client
					Message replyFriends = new Message(Message.ACK, null, friendsArray.toJSONString());
					output.writeObject(replyFriends.toJson());
					output.flush();
					Message replyStats = new Message(Message.ACK, null, statsArray.toJSONString());
					output.writeObject(replyStats.toJson());
					output.flush();
					break;
				}
					
				case Message.ADDFRIEND:{
              		System.out.println("[TCP-HANDLER] " + message.getSender() + " requested addFriend[" + message.getData() +"].");
              		String info = null;
              		String friendName = (String) message.getData();
              		User sender = manager.getUserDatabase().getUser(message.getSender());
              		User friend = manager.getUserDatabase().getUser(friendName);
              		if(friend != null && friend.isOnline()
              				&& friend.getUsername().compareTo(sender.getUsername())!=0){ //can't add yourself
              			//add sender as receiver's friend
              			friend.mutex.lock();
                      	boolean result = friend.addFriend(message.getSender());
                      	friend.mutex.unlock();
              			//add receiver as sender's friend
                      	if(result){
                      		sender.mutex.lock();
                          	result = sender.addFriend(friendName);
                          	sender.mutex.unlock();
                          	if(result){
                              	//build reply message
                              	Message ack = new Message(Message.ACK, null, info);
                                output.writeObject(ack.toJson());
                              	output.flush();
                              	//send RMI notifications to both the sender and the receiver and update their friend lists
                              	try{
                              		sender.getStub().friendAdded(friendName);
                              		sender.getStub().sendNotification(friendName + " added to your friends!");
                                  	friend.getStub().friendAdded(message.getSender());
                                  	friend.getStub().sendNotification(message.getSender() + " added you!");
                              	}catch(RemoteException e){}
                                break;
                          	}
                          	info = new String("[TCP-HANDLER] Can't add ["+message.getSender()+"] to [" +friendName+ "]'s friends.");
                        }
                      	info = new String("[TCP-HANDLER] Can't add ["+friendName+"] to [" +message.getSender()+ "]'s friends.");
                    }
                    //sending error message
                    Message nack = new Message(Message.NACK, null, info);
              		output.writeObject(nack.toJson());
                    output.flush();
              		break;
				}
              		
				case Message.ALLGROUPS:{
					String sender = message.getSender();
					System.out.println("[TCP-HANDLER] " + sender + " requested all groups.");
					//get a copy of the arrays
					HashMap<String,Integer> allGroups = manager.getGroupDatabase().getGroups();
					JSONArray groupsArray = new JSONArray();
					JSONArray portsArray = new JSONArray();
					//allGroups contains: <(role) groupName, groupPort>
					for(String group : allGroups.keySet()){
						portsArray.add(allGroups.get(group));
						//if sender is group's admin
						if(manager.getGroupDatabase().isAdmin(sender,group))
							groupsArray.add("(admin) " + group);
						//if sender is group's member
						else if(manager.getGroupDatabase().isMember(sender,group))
							groupsArray.add("(member) " + group);
						//if sender hasn't joined the group yet
						else
							groupsArray.add(group);
					}
					Message replyGroups = new Message(Message.ACK, null, groupsArray.toJSONString());
					output.writeObject(replyGroups.toJson());
					output.flush();
					Message replyPorts = new Message(Message.ACK, null, portsArray.toJSONString());
					output.writeObject(replyPorts.toJson());
					output.flush();
					break;
				}
				
				case Message.NEWGROUP:{
					String sender = message.getSender();
					String group = message.getData();
					System.out.println("[TCP-HANDLER] " + sender + " requested newGroup(" + group + ")");
					Boolean created = false;
					//getting locks on both DBs so that none can add elements to them
					this.manager.getUserDatabase().mutex.lock();
					this.manager.getGroupDatabase().mutex.lock();
					if(!this.manager.getUserDatabase().isRegistered(group))
						created = manager.getGroupDatabase().addGroup(sender,group);
					this.manager.getGroupDatabase().mutex.unlock();
					this.manager.getUserDatabase().mutex.unlock();
					//sending back reply and notifying with RMI 
					Message reply;
					if(created){
						reply = new Message(Message.ACK, null);
						User user = manager.getUserDatabase().getUser(sender);
						int groupPort = manager.getGroupDatabase().getGroup(group).getPort();
						user.getStub().groupJoined(group, groupPort);
					}else
						reply = new Message(Message.NACK, null);
					output.writeObject(reply.toJson());
					output.flush();
					break;
				}
				
				case Message.MEMBERS:{
					String sender = message.getSender();
					String groupName = message.getData();
					System.out.println("[TCP-HANDLER] " + sender + " requested getMembers(" + groupName + ")");
					Group group = this.manager.getGroupDatabase().getGroup(groupName); 
					Message reply = new Message(Message.NACK, null);
					//if the group exists, sends and ACK message containing the requested list
					if(group != null){
						CopyOnWriteArrayList<String> members = group.getMembers();
						JSONArray replyArray = new JSONArray();
						for(String user : members)
							replyArray.add(user);
						reply = new Message(Message.ACK, sender, replyArray.toJSONString());
					}
					output.writeObject(reply.toJson());
					output.flush();
					break;
				}
				
				case Message.JOIN:{
					String sender = message.getSender();
					String groupName = message.getData();
					Group group = manager.getGroupDatabase().getGroup(groupName);
					System.out.println("[TCP-HANDLER] " + sender + " requested joinGroup(" + groupName + ")");
					boolean joined = manager.getGroupDatabase().addMember(groupName, sender);
					Message reply;
					//sending back reply and notifying with RMI 
					if(joined){
						reply = new Message(Message.ACK, null);
						User user = manager.getUserDatabase().getUser(sender);
						int groupPort = group.getPort();
						user.getStub().groupJoined(groupName, groupPort);
						System.out.println("[TCP-HANDLER] " + sender + " joined " + groupName);
					}else
						reply = new Message(Message.NACK, null);
					output.writeObject(reply.toJson());
					output.flush();
					break;
				}
				
				case Message.DELETEGROUP:{
					String sender = message.getSender();
					String groupName = message.getData();
					System.out.println("[TCP-HANDLER] " + sender + " requested deleteGroup(" + groupName + ")");
					Message reply;
					Group group = this.manager.getGroupDatabase().getGroup(groupName);
					//check if the user is the admin of the group
					if(group != null && group.getAdmin().compareTo(sender)==0){
						reply = new Message(Message.ACK, null);
						output.writeObject(reply.toJson());
						output.flush();
						
						CopyOnWriteArrayList<String> members = group.getMembers();
						for(String member : members){
							User user = this.manager.getUserDatabase().getUser(member);
							try{
								user.getStub().groupDeleted(groupName);
							}catch(RemoteException e){
								System.out.println("[TCP-HANDLER] User " + member + " is offline.");
								user.setStatus(false);
							}
						}
						manager.getGroupDatabase().deleteGroup(sender, groupName);
					}else{
						reply = new Message(Message.NACK, null);
						output.writeObject(reply.toJson());
						output.flush();
					}
					break;
				}
				
				case Message.MSG:{
              		System.out.println("[TCP-HANDLER] " + message.getSender() + " requested newMessage[" + message.getReceiver() +"].");
              		String info = null;
              		if(message.getData() != null){
                		//opening connection with receiver
              			User receiver = this.manager.getUserDatabase().getUser(message.getReceiver());
              			try(Socket forward = new Socket(InetAddress.getLocalHost(),receiver.getListenerPort());
              					ObjectOutputStream forwardOutput = new ObjectOutputStream(forward.getOutputStream());
              					ObjectInputStream forwardInput = new ObjectInputStream(forward.getInputStream());
              					){
              				//connected successfully, so I send an ack to sender
                  			Message ack = new Message(Message.ACK, null, info);
                  			output.writeObject(ack.toJson());
                  			output.flush();
              				//send the message to the receiver's messageListener
              				forwardOutput.writeObject(message.toJson());
              				forwardOutput.flush();
              			}catch(IOException e){
              				System.out.println("[TCP-HANDLER] ERROR Can't send message (IOException)" + e.getMessage());
              				info = new String("[TCP-HANDLER] Can't send message!");
                        	Message nack = new Message(Message.NACK, null, info);
                        	output.writeObject(nack.toJson());
                  			output.flush();
                  			break;
              			}
                    }else{
                    	info = new String("[TCP-HANDLER] Can't send null message!");
                    	Message nack = new Message(Message.NACK, null, info);
                    	output.writeObject(nack.toJson());
              			output.flush();
                    }
                  	break;
				}
              		
				case Message.FILE:{
					System.out.println("[TCP-HANDLER] " + message.getSender() + " " + message.getPort() + " requested newFile[" + message.getReceiver() +"].");
              		String receiverName = message.getReceiver();
              		User receiver = this.manager.getUserDatabase().getUser(receiverName);
              		int port = receiver.getListenerPort();
              		Message reply;
              		if(receiver.isOnline()){
              			//forwarding request to receiver
              			TCPConnection receiverConnection = new TCPConnection(receiver.getAddress(), port);
          				receiverConnection.openConnection();
          				//check if receiver is reachable 
          				if(!receiverConnection.isOpen()){
          					reply = new Message(Message.NACK, receiverName);
                  			output.writeObject(reply.toJson());
                  			output.flush();
          				}else{
          					String senderAddress = client.getInetAddress().toString();
          					String fileName = message.getData();
          					//sending sender's address and file's name as a payload 
          					String payload = new String(senderAddress + " " + fileName);
          					Message requestSend = new Message(Message.FILE, message.getSender(), null, receiverName, message.getPort(),payload);
          					receiverConnection.getOutput().writeObject(requestSend.toJson());
          					receiverConnection.getOutput().flush();
          					//receive ACK from the receiver or nothing at all (wait 10s, then throw exception)
          					receiverConnection.getSocket().setSoTimeout(10*1000);
          					try{
          						JSONObject jreceiverReply = (JSONObject) receiverConnection.getInput().readObject();
              					Message receiverReply = new Message(jreceiverReply);
              					if(receiverReply.getType() == Message.ACK)
              						reply = new Message(Message.ACK, receiverName);
              					else
              						reply = new Message(Message.NACK, receiverName);
          					}catch(SocketTimeoutException e){ //timeout
          						reply = new Message(Message.NACK, receiverName);
          					}
          					output.writeObject(reply.toJson());
          					output.flush();
          				}
              		}else{
              			reply = new Message(Message.NACK, receiverName);
              			output.writeObject(reply.toJson());
      					output.flush();
              		}
					break;
				}
				
				default:{
					break;
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		
	}

}

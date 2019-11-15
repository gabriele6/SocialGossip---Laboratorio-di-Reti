package server;

import java.io.IOException;
import java.net.MulticastSocket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Group{
  	private String groupName;
	private String admin;	
 	private CopyOnWriteArrayList<String> members;
 	private int port;
 	ReentrantLock mutex = new ReentrantLock();
 	
  	public Group(String admin, String name){
    	this.admin = admin;
    	this.groupName = name;
      	this.members = new CopyOnWriteArrayList<String>();
      	addMember(admin);
      	MulticastSocket socket;
		try{
			socket = new MulticastSocket();
			this.port = socket.getLocalPort();
		}catch(IOException e){
			e.printStackTrace();
		}
		
  	}
  	
  	/** Gets the group name.
	 * @return A String containing the group name.
	 */
  	public String getName(){
    	return this.groupName;
  	}
  	
  	/** Gets the group Administrator.
	 * @return A String containing the group Administrator.
	 */
  	public String getAdmin(){
  		return this.admin;
    }
  	
  	/** Gets the group members.
	 * @return A CopyOnWriteArrayList containing the group members.
	 */
  	public CopyOnWriteArrayList<String> getMembers(){
  		return this.members;
    }
  
  	/** Checks if an user is member of the group.
  	 * @param name
	 * @return true if name is member, false otherwise.
	 */
  	public boolean isMember(String name){
    	return this.members.contains(name);
    }
	
  	/** Adds an user to the group.
  	 * @param name
	 * @return true if name has beeen added to the group, false otherwise.
	 */
  	public synchronized boolean addMember(String name){
  		if(name!=null)
  			return this.members.addIfAbsent(name);
  		return false;
  	}
  	
  	/** Gets the Multicast UDP port the group is registered on.
	 * @return An int containing the group port.
	 */
  	public int getPort(){
  		return this.port;
  	}
  	
}

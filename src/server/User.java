package server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.net.InetAddress;

import common.*;

public class User{
	private String username;
	private String password;
	private String language;
	private boolean status;
	private SocialClientInterface userStub;
	private InetAddress address;
	private int listenerPort;
	private CopyOnWriteArrayList<String> friends;
	ReentrantLock mutex = new ReentrantLock();
	
	public User(String username, String password, String language){
		this.username = username;
		this.password = password;
		this.language = language;
		this.userStub = null;
		this.status = false;
		this.friends = new CopyOnWriteArrayList<String>();
	}
	
	/** Gets the username from the user.
	 * @return a String containing the username.
	 */
	public String getUsername(){
		return this.username;
	}
	
	/** Gets the hashed password from the user.
	 * @return a String containing the password.
	 */
	public String getPassword(){
		return this.password;
	}
	
	/** Gets the language of the user.
	 * @return a String containing the language.
	 */
	public String getLanguage(){
		return this.language;
	}
	
	/** Gets the RMI stub from the user.
	 * @return a SocialClientInterface containing the user stub.
	 */
	public SocialClientInterface getStub(){
		return this.userStub;
	}
	
	/** Sets the RMI stub.
	 * @param stub the user's stub
	 * @return a String containing the username.
	 */
	public synchronized void setStub(SocialClientInterface stub){
		this.userStub = stub;
	}
	
	/** Gets the current address.
	 * @return an InetAddress containing the user address.
	 */
	public InetAddress getAddress(){
		return this.address;
	}
	
	/** Sets the user address.
	 * @param address InetAddress containing the new address.
	 */
	public synchronized void setAddress(InetAddress address){
		this.address = address;
	}
	
	/** Gets the TCP port of the user.
	 * @return an int containing the TCP port.
	 */
	public int getListenerPort(){
		return this.listenerPort;
	}
	
	/** Sets the TCP port of the user.
	 * @param port
	 */
	public synchronized void setListenerPort(int port){
		this.listenerPort = port;
	}
	
	/** Checks if the user is currently logged in.
	 * @return true if user is logged in, false otherwise.
	 */
	public boolean isOnline(){
		return this.status;
	}
	
	/** Sets the user status.
	 * @param value contains the value of the new status (true if online, false if offline)
	 */
	public synchronized void setStatus(boolean value){
		this.status = value;
	}
	
	/** Gets the friendlist of the user.
	 * @return a CopyOnWriteArrayList containing the friendlist.
	 */
	public CopyOnWriteArrayList<String> getFriends(){
		return this.friends;
	}
	
	/** Sets the friendlist of the user.
	 * @param friends an array containing the friendlist.
	 */
	public synchronized void setFriends(List<String> friends){
		this.friends = (CopyOnWriteArrayList<String>) friends;
	}
	
	/** Adds the username to the user's friendlist.
	 * @param friend a String containing the username of the user to add to the friendlist.
	 * @return true if the friend is added, false otherwise.
	 */
	public synchronized boolean addFriend(String friend){
		//check if you're adding yourself or you are already friends
		if(this.username.compareTo(friend)!=0 && !this.friends.contains(friend))
			return this.friends.add(friend);
		return false;
	}
}

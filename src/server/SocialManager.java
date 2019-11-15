package server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.*;

public class SocialManager extends RemoteObject
	implements SocialManagerInterface{
	private static final long serialVersionUID = 1L;
	
	public Settings settings;
	private UserDatabase users;
	private GroupDatabase groups;
	public ExecutorService keeper;
	
	public SocialManager(){
		settings = new Settings();
		this.users = new UserDatabase();
		this.groups = new GroupDatabase();
		KeepAlive keepalive = new KeepAlive(this);
		this.keeper = Executors.newSingleThreadExecutor();
		this.keeper.submit(keepalive);
		
		System.out.println("[USER-DB] dim: " + this.users.getDatabase().size());
		System.out.println("[GROUP-DB] dim: " + this.groups.getDatabase().size());
	}

	@Override
	public boolean register(String username, String password, String language)
			throws RemoteException {
		String hashedPassword = password;
		//applying SHA-256 to the password
		MessageDigest digest;
		try{
			digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			hashedPassword = new String(convertBytes(hash));
		}catch(NoSuchAlgorithmException e){
			System.out.println("[MANAGER] ERROR applying SHA-256 (NoSuchAlgorithm)" + e.getMessage());;
			return false;
		}
		this.getUserDatabase().mutex.lock();
		this.getGroupDatabase().mutex.lock();
		if(!this.getGroupDatabase().existsGroup(username)){
			if(users.addUser(username, hashedPassword, language)){
				System.out.println("[MANAGER] New user: " + username);
				this.getGroupDatabase().mutex.unlock();
				this.getUserDatabase().mutex.unlock();
				return true;
			}
		}
		this.getGroupDatabase().mutex.unlock();
		this.getUserDatabase().mutex.unlock();
		return false;
	}

	@Override
	public boolean login(String username, String password, String language, 
			SocialClientInterface client, 
			InetAddress address, int listenerPort) 
			throws RemoteException {
		String hashedPassword = password;
		//applying SHA-256 to the password
		MessageDigest digest;
		try{
			digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			hashedPassword = convertBytes(hash);
		}catch(NoSuchAlgorithmException e){
			System.out.println("[MANAGER] ERROR applying SHA-256 (NoSuchAlgorithm)" + e.getMessage());;
			return false;
		}
		if(users.isRegistered(username) && users.checkPassword(username, hashedPassword)){
			if(users.isOnline(username))
				return false;
			User user = users.getUser(username);
			user.mutex.lock();
			user.setStatus(true);
			user.setStub(client);
			user.setAddress(address);
			user.setListenerPort(listenerPort);
			System.out.println("[MANAGER] Logged in: " + username + "!");
			for(String friend : user.getFriends()){
				if(users.isOnline(friend))
					try{
						users.getUser(friend).getStub().statusChanged(username, true);
					}catch(RemoteException e){}
			}
			user.mutex.unlock();
			return true;
		}
		return false;
	}

	@Override
	public boolean logout(String username) throws RemoteException {
		if(users.isOnline(username)){
			User user = users.getUser(username);
			user.mutex.lock();
			user.setStatus(false);
			user.setStub(null);
			System.out.println("[MANAGER] Logged out: " + username);
			for(String friend : user.getFriends()){
				if(users.isOnline(friend))
					try{
						users.getUser(friend).getStub().statusChanged(username, false);
					}catch(RemoteException e){}
			}
			user.mutex.unlock();
			return true;
		}
		return false;
	}
	
	//converts from byte[] to String
    private static String convertBytes(byte[] array) {
        StringBuffer result = new StringBuffer();
        for(byte b : array) 
        	result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
    
    public UserDatabase getUserDatabase(){
    	return this.users;
    }
    
    public GroupDatabase getGroupDatabase(){
    	return this.groups;
    }

}

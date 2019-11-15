package common;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SocialManagerInterface extends Remote{

	/** Registers the user to Social Gossip.
	 * @param username
	 * @param password
	 * @param language
	 * @return true if the user was successfully registered, false otherwise.
	 * @throws RemoteException
	 */
	public boolean register(String username, String password, String language)
		throws RemoteException;
	
	/** Logs in the user to Social Gossip.
	 * @param username
	 * @param password
	 * @param language
	 * @param client the RMI stub of the client
	 * @param address the IP address of the client
	 * @param listenerPort	the port on which the client waits for TCP requests
	 * @return true if the user successfully logged in, false otherwise.
	 * @throws RemoteException
	 */
	public boolean login(String username, String password, String language, 
			SocialClientInterface client, 
			InetAddress address, int listenerPort)
		throws RemoteException;
	
	/** Logs the user out.
	 * @param username
	 * @throws RemoteException
	 */
	public boolean logout(String username)
		throws RemoteException;

	
}

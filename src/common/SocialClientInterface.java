package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SocialClientInterface extends Remote{
	
	/** Send a notification to the client, so that its GUI can print the message.
	 * @param message message to be displayed by the client's GUI
	 * @throws RemoteException
	 */
	public void sendNotification(String message)
		throws RemoteException;
	
	/** Sends a notification to the client when the server successfully created the friendship relationship. 
	 * @param name user that has been added to Friends
	 * @throws RemoteException
	 */
	public void friendAdded(String name)
		throws RemoteException;
	
	/** Sends a notification to the client when one oh its friends logs in/out.
	 * @param name user that changed its status.
	 * @param status true if user logged in, false otherwise.
	 * @throws RemoteException
	 */
	public void statusChanged(String name, Boolean status)
		throws RemoteException;
	
	/** Sends a notification to the client when the server successfully created the group. 
	 * @param name name of the created group
	 * @throws RemoteException
	 */
	public void groupJoined(String name, int port)
		throws RemoteException;
	
	/** Sends a notification to the client when a joined group has been deleted. 
	 * @param name name of the deleted group
	 * @throws RemoteException
	 */
	public void groupDeleted(String name)
		throws RemoteException;
	
	/** Sends a notification to the client when a he tries to send a message to a group
	 * but noone is online. 
	 * @param name name of the group
	 * @throws RemoteException
	 */
	public void nobodyOnline(String name)
		throws RemoteException;
	
	/** Method used by the server to test if the client is still running. 
	 * @throws RemoteException
	 */
	public void youAlive()
		throws RemoteException;
	
	

}

package server;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class KeepAlive implements Runnable{
	private static final int SLEEP_TIME = 30*1000;
	private SocialManager manager;
	
	public KeepAlive(SocialManager manager){
		this.manager = manager;
	}

	@Override
	public void run(){
		ConcurrentHashMap<String, User> users = this.manager.getUserDatabase().getDatabase();
		int counter = 0;
		while(true){
			System.out.println("[KEEPER] KeepAlive running. (iter:" + counter +")");
			for(String name : users.keySet()){
				User user = users.get(name);
				user.mutex.lock();
				if(user.isOnline()){
					try{
						user.getStub().youAlive();
					}catch(RemoteException e){
						user.setStatus(false);
						user.setStub(null);
						System.out.println("[KEEPER] Logged out: " + name);
						for(String friend : user.getFriends()){
							if(this.manager.getUserDatabase().isOnline(friend))
								try{
									users.get(friend).getStub().statusChanged(name, false);
								}catch(RemoteException e2){}
						}
					}
				}
				user.mutex.unlock();
			}
			try{
				Thread.sleep(SLEEP_TIME);
				counter++;
				//saving databases every 3 iterations
				if(counter % 3 == 0){
					manager.getUserDatabase().saveDatabase();
					manager.getGroupDatabase().saveDatabase();
					System.out.println("[KEEPER] Database saved. (iter:" + counter +")");
					counter = 0;
				}
			}catch(InterruptedException e){
				System.out.println("[KEEPER] Keeper can't sleep! (InterruptedException)" + e.getMessage());
			}
		}
	}
}

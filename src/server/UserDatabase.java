package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class UserDatabase{
	public static final String USERDB = "./users.db";
	
	private ConcurrentHashMap<String,User> database; //session-based
	private JSONArray jsonDatabase; //file-based
	private File file = null;
	ReentrantLock mutex = new ReentrantLock();
	
	public UserDatabase(){
		this.database = new ConcurrentHashMap<String,User>();
		this.jsonDatabase = new JSONArray();
		this.file = new File(USERDB);
		if(file.exists())
			this.loadDatabase();
	}
	
	// ---------- USERS ACTIONS ----------
	/** Checks if the user is registered.
	 * @param username
	 * @return true if user is registered, false otherwise.
	 */
	public boolean isRegistered(String username){
		if(database.containsKey(username))
			return true;
		else
			return false;
	}
	
	/** Adds the user to the database.
	 * @param username
	 * @param password
	 * @param language
	 * @return true if the user was added, false otherwise.
	 */
	public synchronized boolean addUser(String username, String password, String language){
		if(!isRegistered(username)){
			User user = new User(username, password, language);
			database.put(username, user);
			return true;
		}else
			return false;
	}
	
	/** Gets the user from the database
	 * @param username
	 * @return an User if it is registered in the database, null otherwise.
	 */
	public User getUser(String username){
		if(isRegistered(username))
			return database.get(username);
		else
			return null;
	}
	
	/** checks if the inserted password is correct for the user.
	 * @param username
	 * @param password
	 * @return true if password is correct, false otherwise.
	 */
	public boolean checkPassword(String username, String password){
		if(database.get(username).getPassword().compareTo(password)==0)
			return true;
		else
			return false;
	}
	
	/** Checks if the user is currently online.
	 * @param user
	 * @return true if user is online, false otherwise.
	 */
	public boolean isOnline(String user){
		if(getUser(user) != null)
			return getUser(user).isOnline();
		else
			return false;
	}
	
	/** Gets a subset of Database, including only users with the substring name in their username.
	 * @param name
	 * @return an ArrayList<String> containing the subset.
	 */	
	public ArrayList<String> searchUsers(String name){
		ArrayList<String> users = new ArrayList<String>();
		for(String key : this.database.keySet()){
			if(key.contains(name))
				users.add(key);
		}
		return users;
	}
	
	// ---------- USERDB ACTIONS ----------
	/** Returns the users database.
	 * @return a ConcurrentHashMap containing the user database.
	 */
	public ConcurrentHashMap<String,User> getDatabase(){
		return this.database;
	}
	
	/** Saves the database on a JSON file.
	 */
	@SuppressWarnings("unchecked")
	public synchronized void saveDatabase(){
		try{
        	this.file.delete();
			if(this.file.createNewFile()){
				FileWriter writer = new FileWriter(this.file);
				this.jsonDatabase.clear();
				for(String key : this.database.keySet()){
					User user = this.database.get(key);
					JSONObject juser = toJson(user);
					jsonDatabase.add(juser);
				}
				writer.write(jsonDatabase.toJSONString());
				writer.flush();
				writer.close();
			}
		}catch(IOException e){
			System.out.println("[USERDB] ERROR Saving Database (IOException): " + e.getMessage());
		}
	}
	
	/** Loads the database from a JSON file.
	 */
	public void loadDatabase(){
		JSONParser parser = new JSONParser();
		try(FileReader reader = new FileReader(this.file)){
			JSONArray array = (JSONArray) parser.parse(reader);
			this.jsonDatabase = array;
			Iterator<?> i = array.iterator();
			while(i.hasNext()){
				JSONObject juser = (JSONObject) i.next();
				User user = fromJson(juser);
				if(!this.database.containsKey(user.getUsername())) {
					this.database.put(user.getUsername(), user);
					System.out.println("[USERDB] Loaded " + user.getUsername() + ".");
				}
			}
		}catch(FileNotFoundException e){
			System.out.println("[USERDB] ERROR Loading Database (File Not Found)");
		}catch(IOException e){
			System.out.println("[USERDB] ERROR Loading Database (IOException)");
		}catch(ParseException e){
			System.out.println("[USERDB] ERROR Loading Database (ParseException)");
		}
	}
	
	// ---------- JSON STUFF ----------
	@SuppressWarnings("unchecked")
	private JSONObject toJson(User user){
		JSONObject jUser = new JSONObject();
		jUser.put("username", user.getUsername());
		jUser.put("password", user.getPassword());
		jUser.put("language", user.getLanguage());
		jUser.put("friends", user.getFriends());
		return jUser;
	}
	
	private User fromJson(JSONObject jUser){
		String username = (String) jUser.get("username");
		String password = (String) jUser.get("password");
		String language = (String) jUser.get("language");
		JSONArray jFriends = (JSONArray) jUser.get("friends");
		List<String> friends = new CopyOnWriteArrayList<String>();
		Iterator<?> i = jFriends.iterator();
		while(i.hasNext()){
			String name = i.next().toString();
			friends.add(name);
		}
		User user = new User(username, password, language);
		user.setFriends(friends);
		return user;
	}

}

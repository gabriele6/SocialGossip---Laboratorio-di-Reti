package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GroupDatabase{
  	public static final String GROUPDB = "./groups.db";
  
	private ConcurrentHashMap<String,Group> database; //session-based
	private JSONArray jsonDatabase; //file-based
	private File file = null;
	ReentrantLock mutex = new ReentrantLock();

	
	public GroupDatabase(){
		this.database = new ConcurrentHashMap<String,Group>();
		this.jsonDatabase = new JSONArray();
		this.file = new File(GROUPDB);
		if(file.exists())
			this.loadDatabase();
	}
  
	/** Checks if the group currently exists on the database.
	 * @param groupName
	 */
  	public boolean existsGroup(String groupName){
      	return database.containsKey(groupName);
    }
  	
  	/** Gets the group from the group database.
	 * @param name the name of the group to get
	 * @return the wanted Group (null if not existing).
	 */
  	public Group getGroup(String name){
  		return this.database.get(name);
  	}
  
  	/** Adds a member to a group.
	 * @param groupName
	 * @param user
	 * @return true if the user has been added to the group, false otherwise.
	 */
  	public boolean addMember(String groupName, String user){
    	Group group = database.get(groupName);
      	if(group != null){
      		group.mutex.lock();
      		boolean result = group.addMember(user);
      		group.mutex.unlock();
      		return result;
      	}
      	return false;
    }
  
  	/** Adds the group to the database.
  	 * @param admin the name of the group Administrator
	 * @param groupName
	 * @return true if group has been added, false otherwise.
	 */
  	public synchronized boolean addGroup(String admin, String groupName){
      	// controlla se esiste un gruppo con lo stesso nome	
  		this.mutex.lock();
      	if(!existsGroup(groupName)){
  			Group group = new Group(admin, groupName);
          	database.put(groupName, group);
          	this.mutex.unlock();
          	//TODO: use a thread 
          	return true;
      	}
      	this.mutex.unlock();
      	return false;
    }
  
  	/** Deletes the group from the database.
  	 * @param admin the name of the group Administrator
	 * @param groupName
	 * @return true if group has been deleted, false otherwise.
	 */
  	public synchronized boolean deleteGroup(String admin, String groupName){
      	//controlla se gruppo esiste
  		this.mutex.lock();
      	Group group = database.get(groupName);
      	if(group != null){
          	if(group.getAdmin().compareTo(admin) == 0){
            	this.database.remove(groupName);
            	this.mutex.unlock();
            	return true;
            }
      	}
      	this.mutex.unlock();
      	return false;
    }
  	
  	/** Checks if an user is the group Administrator.
  	 * @param user
	 * @param groupName
	 * @return true if user is Admin, false otherwise.
	 */
  	public boolean isAdmin(String user, String groupName){
  		Group group = this.database.get(groupName); 
  		if(group != null){
  			if(group.getAdmin().compareTo(user) == 0)
  				return true;
  		}
  		return false;
  	}
  	
  	/** Checks if the user is a member of the group.
  	 * @param user
	 * @param groupName
	 * @return true if user is member, false otherwise.
	 */
  	public boolean isMember(String user, String groupName){
  		Group group = this.database.get(groupName); 
  		if(group != null){
  			if(group.isMember(user))
  				return true;
  		}
  		return false;
  	}
  	
  	/** Gets the groups containing a specific substring in their name.
  	 * @param name substring to search
	 * @return ArrayList containing the groups with that substring in their name.
	 */
  	public ArrayList<String> searchGroups(String name){
  		ArrayList<String> groups = new ArrayList<String>();
  		for(String key : this.database.keySet()){
  			if(key.contains(name))
  				groups.add(key);
  		}
  		return groups;
  	}
  	
  	/** Gets the group database.
	 * @return a ConcurrentHashMap of the database.
	 */
  	public ConcurrentHashMap<String,Group> getDatabase(){
		return this.database;
	}
  	
  	/** Gets a copy the group database, containing the ports.
	 * @return an HashMap containing the groups
	 */
  	public HashMap<String,Integer> getGroups(){
  		HashMap<String,Integer> groups = new HashMap<String,Integer>();
  		for(String key : this.database.keySet())
  			groups.put(key, this.database.get(key).getPort());
  		return groups;
  	}
  	
  	// ----------- FILE STUFF ----------
  	/** Saves the database on a JSON file on disk.
	 */
  	@SuppressWarnings("unchecked")
	public void saveDatabase(){
      	try{
        	this.file.delete();
          	this.file.createNewFile();
          	FileWriter writer = new FileWriter(this.file);
          	this.jsonDatabase.clear();
          	for(String key : this.database.keySet()){
            	Group group = this.database.get(key);
              	JSONObject jgroup = toJson(group);
              	jsonDatabase.add(jgroup);
            }
          	writer.write(jsonDatabase.toJSONString());
          	writer.flush();
          	writer.close();
        }catch(IOException e){
			System.out.println("[GROUP-DB] ERROR Saving GroupDatabase IOException." + e.getMessage());
			e.printStackTrace();
		}
    }
  
  	/** Loads the database from a JSON file on the disk.
	 */
  	public void loadDatabase(){
  		JSONParser parser = new JSONParser();
      	try(FileReader reader = new FileReader(this.file)){
			JSONArray array = (JSONArray) parser.parse(reader);
          	this.jsonDatabase = array;
          	Iterator<?> i = array.iterator();
          	while(i.hasNext()){
            	JSONObject jgroup = (JSONObject) i.next();
              	Group group = fromJson(jgroup);
              	if(!existsGroup(group.getName())){
              		database.put(group.getName(), group);
                  	System.out.println("[GROUP-DB] Loaded " + group.getName());
                }
            }
        }catch(FileNotFoundException e){
			System.out.println("[GROUP-DB] ERROR Loading Database (File Not Found)" + e.getMessage());
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("[GROUP-DB] ERROR Loading Database (IOException)" + e.getMessage());
			e.printStackTrace();
		}catch(ParseException e){
			System.out.println("[GROUP-DB] ERROR Loading Database (ParseException)" + e.getMessage());
			e.printStackTrace();
		}
    }
  
  	// ---------- JSON STUFF ----------
  	@SuppressWarnings("unchecked")
	public JSONObject toJson(Group group){
    	JSONObject jgroup = new JSONObject();
      	jgroup.put("name", 	group.getName());
      	jgroup.put("admin", group.getAdmin());
      	jgroup.put("members", group.getMembers());
      	return jgroup;
    }
  
  	public Group fromJson(JSONObject jgroup){
  		String groupName = (String) jgroup.get("name");
      	String admin = (String) jgroup.get("admin");
      	JSONArray jmembers	= (JSONArray) jgroup.get("members");
      	Group group = new Group(admin, groupName);
      	Iterator<?> i = jmembers.iterator();
      	while(i.hasNext()){
      		String name = i.next().toString();
          	group.addMember(name);
        }
      	return group;
    }
  
}

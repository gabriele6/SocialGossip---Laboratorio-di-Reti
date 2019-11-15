package common;

import org.json.simple.JSONObject;


public class Message{
	public static final int FRIENDLIST 	= 0;
	public static final int GROUPLIST 	= 1;
	public static final int ALLGROUPS 	= 2;
	public static final int MSG 		= 3;
	public static final int GROUPMSG 	= 4;
	public static final int FILE 		= 5;
	public static final int SEARCH 		= 6;
	public static final int ADDFRIEND 	= 7;
	public static final int	MEMBERS		= 8;
	public static final int JOIN 		= 9;
	public static final int NEWGROUP 	= 10;
	public static final int DELETEGROUP = 11;
	public static final int ALIVE 		= 12;
	public static final int ACK 		= 13;
	public static final int NACK 		= 14;

	private long 	type;
	private String 	sender;
	private String 	senderLanguage;
	private String 	receiver;
	private long 	port;
	private String 	data;
	
	public Message(long type, String sender){
		this.type 			= type;
		this.sender 		= sender;
		this.senderLanguage = null;
		this.receiver 		= null;
		this.port 			= 0;
		this.data 			= null;
	}
	
	public Message(long type, String sender, String data){
		this.type 			= type;
		this.sender 		= sender;
		this.senderLanguage = null;
		this.receiver 		= null;
		this.port 			= 0;
		this.data 			= data;
	}
	
	
	public Message(long type, String sender, String language, 
			String receiver, long port, String data){
		this.type 			= type;
		this.sender 		= sender;
		this.senderLanguage = language;
		this.receiver 		= receiver;
		this.port 			= port;
		this.data 			= data;
	}
	
	public Message(JSONObject jmessage){
		this.type 			= (long) 	jmessage.get("type");
		this.sender 		= (String) 	jmessage.get("sender");
		this.senderLanguage = (String) 	jmessage.get("senderLanguage");
		this.receiver 		= (String) 	jmessage.get("receiver");
		this.port 			= (long) 	jmessage.get("port");
		this.data 			= (String) 	jmessage.get("data");
	}
	
	// ---------- JSON -----------
	@SuppressWarnings("unchecked")
	public JSONObject toJson(){
		JSONObject jmessage = new JSONObject();
		jmessage.put("type", 			this.type);
		jmessage.put("sender", 			this.sender);
		jmessage.put("senderLanguage", 	this.senderLanguage);
		jmessage.put("receiver", 		this.receiver);
		jmessage.put("port", 			this.port);
		jmessage.put("data", 			this.data);
		return jmessage;
	}
	
	// ---------- AUX ----------
	/** Gets the type of the message.
	 * @return an int rappresenting the type. 
	 */
	public int getType(){
		return (int) this.type;
	}
	
	/** Gets the sender of the message.
	 * @return a String containing the sender.
	 */
	public String getSender(){
		return this.sender;
	}
	
	/** Gets the receiver of the message.
	 * @return a String containing the receiver. 
	 */
	public String getReceiver(){
		return this.receiver;
	}
	
	/** Gets the port of the message. 
	 * @return a long containing the port used by the sender. 
	 */
	public long getPort(){
		return this.port;
	}
	
	/** Gets the payload of the message.
	 * @return a JSON String containing the payload. 
	 */
	public String getData(){
		return this.data;
	}

	/** Gets the language used by the sender of the message.
	 * @return a String containing the language. 
	 */
	public String getLanguage(){
		return this.senderLanguage;
	}
	
}

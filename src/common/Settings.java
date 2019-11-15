package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Settings {
	public int REGISTRY_PORT;
	public int TCP_PORT;
	public int UDP_PORT;
	public int UDP_LENGTH;
	public String MULTICAST_ADDRESS;
	public String OBJECT_NAME;
	public int PACKET_SIZE;
	private final String CONFIG_FILE = "./config.txt";
	
	public Settings(){
		File file = new File(this.CONFIG_FILE);
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader(file));
			//REGISTRY_PORT
			reader.readLine();
			REGISTRY_PORT = Integer.parseInt(reader.readLine());
			//TCP_PORT
			reader.readLine();
			TCP_PORT = Integer.parseInt(reader.readLine());
			//UDP_PORT
			reader.readLine();
			UDP_PORT = Integer.parseInt(reader.readLine());
			//UDP_LENGTH
			reader.readLine();
			UDP_LENGTH = Integer.parseInt(reader.readLine());
			//MULTICAST_ADDRESS
			reader.readLine();
			MULTICAST_ADDRESS = reader.readLine();
			//OBJECT_NAME
			reader.readLine();
			OBJECT_NAME = reader.readLine();
			//PACKET_SIZE
			reader.readLine();
			PACKET_SIZE = Integer.parseInt(reader.readLine());
			
		}catch(FileNotFoundException e){
			System.out.println("[SETTINGS] Error reading server configuration. (FileNotFound)");
		}catch(IOException e){
			System.out.println("[SETTINGS] Error reading server configuration. (IOException)");
		}finally{
			if(reader != null){
				try{
					reader.close();
				}catch(IOException e){
					System.out.println("[SETTINGS] Error reading server configuration. (IOException)");
				}
			}
		}
	}

}

package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import common.*;

public class Translate {
	
	/** Translates the message to another language.
	 * @param message the message that has to be translated
	 * @param an ISO 639-2 rappresentation of the language.
	 * @return a String containing the translated version of the message. 
	 */
	public static String getTranslation(Message message, String toLanguage){
		String sender = message.getSender();
		String fromLanguage = message.getLanguage();
		String text = message.getData();
		String signedText = null;
		//if the two languages are different, translate
		if(toLanguage.compareTo(fromLanguage)!=0){
			URL url;
		    InputStream is = null;
		    BufferedReader br;
		    String line;
		    try{
		    	//connecting to the external service
		    	String encodedMessage = URLEncoder.encode(text, "UTF-8");
		        url = new URL("https://api.mymemory.translated.net/get?q=" + encodedMessage + "&langpair=" + fromLanguage + "|" + toLanguage);
		        is = url.openStream();
		        br = new BufferedReader(new InputStreamReader(is));
		        //saving reply
		        String msg = new String();
		        StringBuilder response = new StringBuilder(); 
		        while((line = br.readLine()) != null) {
		        	response.append(line);
		        	msg.concat(line);
		        }
		        msg = response.toString();
		        JSONParser parser = new JSONParser();
		  		JSONObject jobj = (JSONObject) parser.parse(msg);
		  		JSONObject data = (JSONObject) jobj.get("responseData");
		  		String translation = text;
		  		String encodedTranslation = (String) data.get("translatedText");
		  		if(encodedTranslation != null)
		  			translation = URLDecoder.decode(encodedTranslation, "UTF-8");
		  		//when there's no translation for the text
		        signedText = new String("[" + sender + "]: " + translation);
		    }catch(MalformedURLException e){
		        return text;
		    }catch(IOException e){
		    	return text;
		    }catch(ParseException e){
		    	return text;
			}finally{
		        try{
		            if(is != null) 
		            	is.close();
		        }catch(IOException e){
		        	return text;
		        }
		    }				    
	    }else //same language
			signedText = new String("[" + sender + "]: " + text);
		return signedText;
	}
}

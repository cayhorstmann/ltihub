package models;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import play.Logger;
import play.mvc.Http;

public class Util {
	public static String getStackTrace(Throwable t) {
		StringWriter out = new StringWriter();
		t.printStackTrace(new PrintWriter(out));
		return out.toString();
	}
	
    public static String paramsToString(Map<String, String[]> params) {
    	StringBuilder result = new StringBuilder();
    	result.append("{");
    	for (String key : params.keySet()) {
    		if (result.length() > 1) result.append(", ");
    		result.append(key); 
    		result.append("=");
    		result.append(Arrays.toString(params.get(key)));
    	}
    	result.append("}");
    	return result.toString();
    }
    
    public static boolean validate(Http.Request request) {
    	Map<String, String[]> postParams = request.body().asFormUrlEncoded();
    	Set<Map.Entry<String, String>> entries = new HashSet<>();
	 	for (Map.Entry<String, String[]> entry : postParams.entrySet()) 
	 		for (String s : entry.getValue())
	 			entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), s));
	 	String url = "https://" + request.host() + request.uri();
	 	for (Map.Entry<String, String> entry : getParams(url).entrySet())
	 		entries.add(entry);
	 	int n = url.lastIndexOf("?"); if (n >= 0) url = url.substring(0, n); 
	 	OAuthMessage oam = new OAuthMessage("POST", url, entries);
        OAuthConsumer cons = new OAuthConsumer(null, "fred", "fred", null); // TODO
        OAuthValidator oav = new SimpleOAuthValidator();
        OAuthAccessor acc = new OAuthAccessor(cons);
	 	//TODO: For JSON payload, need to check the body hash https://www.programcreek.com/java-api-examples/index.php?api=net.oauth.signature.OAuthSignatureMethod
	    try {
          oav.validateMessage(oam, acc);
          return true;
        } catch (Exception e) {
        	Logger.info("Did not validate: " + e.getLocalizedMessage());
    	 	Logger.info("url: " + url);
  	 	    Logger.info("entries: " + entries);
            return false;
        }
    }
    
	/**
	 * Yields a map of query parameters in a HTTP URI
	 * @param url the HTTP URL
	 * @return the map of query parameters or an empty map if there are none
	 * For example, if uri is http://fred.com?name=wilma&passw%C3%B6rd=c%26d%3De
	 * then the result is { "name" -> "wilma", "passwörd" -> "c&d=e" }
	 */
	public static Map<String, String> getParams(String url)
	{		
		// https://www.talisman.org/~erlkonig/misc/lunatech%5Ewhat-every-webdev-must-know-about-url-encoding/
		Map<String, String> params = new HashMap<>();
		String rawQuery;
		try {
			rawQuery = new URI(url).getRawQuery();
			if (rawQuery != null) {
				for (String kvpair : rawQuery.split("&"))
				{
					int n = kvpair.indexOf("=");
					params.put(
						URLDecoder.decode(kvpair.substring(0, n), "UTF-8"), 
						URLDecoder.decode(kvpair.substring(n + 1), "UTF-8"));
				}
			}
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is supported
		} catch (URISyntaxException e1) {
			// Return empty map
		}
		return params;
	}
}

package models;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import io.ebean.Ebean;

import java.io.ByteArrayOutputStream;

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
	
	public static boolean isInstructor(String role) {
		return role != null && (role.contains("Faculty") || role.contains("TeachingAssistant") || role.contains("Instructor"));
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0 || str.trim().equals("null");		
	}
	
    public static String paramsToString(Map<String, String[]> params) {
    	if (params == null) return "null";
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
    
    public static String getParam(Map<String, String[]> params, String key) {
		String[] values = params.get(key);
		if (values == null || values.length == 0) return null;
		else return values[0];
	}
    
    public static boolean validate(Http.Request request) {
    	final String OAUTH_KEY_PARAMETER = "oauth_consumer_key";
    	
    	Map<String, String[]> postParams = request.body().asFormUrlEncoded();
    	if (postParams == null) return false;
    	Set<Map.Entry<String, String>> entries = new HashSet<>();
	 	for (Map.Entry<String, String[]> entry : postParams.entrySet()) 
	 		for (String s : entry.getValue())
	 			entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), s));
	 	String url = "https://" + request.host() + request.uri();
	 	String key = getParam(postParams, OAUTH_KEY_PARAMETER);
	 	for (Map.Entry<String, String> entry : getParams(url).entrySet())
	 		entries.add(entry);
	 	int n = url.lastIndexOf("?"); 
	 	if (n >= 0) url = url.substring(0, n);
	 	OAuthMessage oam = new OAuthMessage("POST", url, entries);
        OAuthConsumer cons = new OAuthConsumer(null, key, getSharedSecret(key), null); 
        OAuthValidator oav = new SimpleOAuthValidator();
        OAuthAccessor acc = new OAuthAccessor(cons);
        
        try {
	      oav.validateMessage(oam, acc);
          return true;
        } catch (Exception e) {
        	Logger.info("Did not validate: " + e.getLocalizedMessage() + "\nurl: " + url + "\nentries: " + entries);
            return false;
        }
    }
    
    /*
    public static boolean validate(Http.Request request) {
    	// Useful background: https://dev.twitter.com/oauth/overview/creating-signatures
    	// Fixes broken code http://grepcode.com/file/repo1.maven.org/maven2/org.imsglobal/basiclti-util/1.1.1/org/imsglobal/lti/launch/LtiOauthVerifier.java?av=f
	 	// For JSON payload, would need to check the body hash 
        // https://www.programcreek.com/java-api-examples/index.php?api=net.oauth.signature.OAuthSignatureMethod
    	
    	final String OAUTH_KEY_PARAMETER = "oauth_consumer_key";
    	
    	Map<String, String[]> postParams = request.body().asFormUrlEncoded();
    	if (postParams == null) return false;
    	Set<Map.Entry<String, String>> entries = new HashSet<>();
	 	for (Map.Entry<String, String[]> entry : postParams.entrySet()) 
	 		for (String s : entry.getValue())
	 			entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), s));
	 	String url = "https://" + request.host() + request.uri();
	 	String key = getParam(postParams, OAUTH_KEY_PARAMETER);
	 	for (Map.Entry<String, String> entry : getParams(url).entrySet())
	 		entries.add(entry);
	 	int n = url.lastIndexOf("?"); if (n >= 0) url = url.substring(0, n); 
	 	OAuthMessage oam = new OAuthMessage("POST", url, entries);
        OAuthConsumer cons = new OAuthConsumer(null, key, "fred", null); // TODO
        OAuthValidator oav = new SimpleOAuthValidator();
        OAuthAccessor acc = new OAuthAccessor(cons);
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
    */
    
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
	
	public static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(in, out);
		out.close();
		return out.toByteArray();
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		final int BLOCKSIZE = 1024;
		byte[] bytes = new byte[BLOCKSIZE];
		int len;
		while ((len = in.read(bytes)) != -1) out.write(bytes, 0, len);
	}
	
	public static String httpPost(String urlString, Map<String, String> postData) {
		StringBuilder result = new StringBuilder();
		try {
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			try (Writer out = new OutputStreamWriter(
					connection.getOutputStream(), StandardCharsets.UTF_8)) {
				boolean first = true;
				for (Map.Entry<String, String> entry : postData.entrySet()) {
					if (first) first = false;
					else out.write("&");
					out.write(URLEncoder.encode(entry.getKey(), "UTF-8"));
					out.write("=");
					out.write(URLEncoder.encode(entry.getValue(), "UTF-8"));
				}
			}
			int response = connection.getResponseCode();
			result.append(response);
			result.append("\n");
			try (Scanner in = new Scanner(connection.getInputStream(), "UTF-8")) {
				while (in.hasNextLine()) {
					result.append(in.nextLine());
					result.append("\n");
				}
			}
			catch (IOException e) {
			    InputStream err = connection.getErrorStream();
			    if (err == null) throw e;
			    try (Scanner in = new Scanner(err, "UTF-8")) {
			        result.append(in.nextLine());
			        result.append("\n");
			    }
			}			
		} catch (Throwable ex) {
			result.append(Util.getStackTrace(ex));
		}
		return result.toString();		
	}
	
	public static String getSharedSecret(String oauthConsumerKey) {
		Oauth oauth = Ebean.find(Oauth.class)
				.where().eq("oauth_consumer_key", oauthConsumerKey).findOne();
		String sharedSecret = "";
		if (oauth != null) sharedSecret = oauth.sharedSecret;
		Logger.info("shared secret for " + oauthConsumerKey + " is " + sharedSecret);
		return sharedSecret;
	}
}

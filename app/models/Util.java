package models;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

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
}

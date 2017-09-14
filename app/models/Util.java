package models;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {
	public static String getStackTrace(Throwable t) {
		StringWriter out = new StringWriter();
		t.printStackTrace(new PrintWriter(out));
		return out.toString();
	}
}

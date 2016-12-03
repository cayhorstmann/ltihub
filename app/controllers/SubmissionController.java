package controllers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import play.*;
import play.data.*;
import play.libs.Json;
import static play.data.Form.*;
import java.net.*;
import java.io.*;
import play.mvc.BodyParser;                     
import play.libs.Json.*;   
import play.libs.Jsonp;                     
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static play.libs.Json.toJson;
import play.Logger;
import play.mvc.*;
import models.*;
import play.mvc.Http.RequestBody;
import views.html.*;

public class SubmissionController extends Controller {
	
	public Result addSubmission() {
		RequestBody body = request().body();
		System.out.println(body.asText());
		System.out.println("Result is received" );
		System.out.println("Received score is:" + request().getQueryString("score"));
		System.out.println("Received solution is:" + request().getQueryString("file"));
		String callback = request().getQueryString("callback");
		ObjectNode result = Json.newObject();
		result.put("received", true);
		result.put("score", request().getQueryString("score"));
		if (callback == null)
			return ok(result.asText());
		else
			return ok(Jsonp.jsonp(callback, result));	
	}
}


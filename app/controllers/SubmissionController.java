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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static play.libs.Json.toJson;
import play.Logger;
import play.mvc.*;
import models.*;

import views.html.*;

public class SubmissionController extends Controller {
	
	@BodyParser.Of(BodyParser.Json.class)
	public Result addSubmission() {
		System.out.println("Result is received" );
        JsonNode json = request().body().asJson();
        System.out.println("Result is" + json.toString());
        return ok("submission saved");
	}
}


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
import views.html.*;

public class SubmissionController extends Controller {
	
   public Result addSubmission(Long problemID, Long assignmentID, Long userID) {	
	Logger.info("Result is received" );
        JsonNode json = request().body().asJson();
	if(json == null)
		return badRequest("Expecting Json data");

	String score = json.findPath("score").textValue();

	Logger.info("Received score is:" + score);
	String[] scores = score.split("/");
	
	Logger.info("AssignmentID is: " + assignmentID);
	Logger.info("UserID is: " + userID);

	Problem problem = Problem.find.byId(problemID);
	Logger.info("Problem is: " + problem);
		
	List<Submission> submissions = Submission.find.where().eq("canvasAssignmentId",assignmentID).eq("studentId",userID).findList();
	Logger.info(submissions.toString());
	
	Submission submission = new Submission();
	submission.setcanvasAssignmentId(assignmentID);
	submission.setStudentId(userID);
	submission.setCorrect(Long.parseLong(scores[0]));
	if(scores.length >1)
		submission.setMaxScore(Long.parseLong(scores[1]));
	else
		submission.setMaxScore(0L);
	submission.setProblem(problem);
	problem.getSubmissions().add(submission);
	submission.save();
	
	Logger.info("New score is added and the value is: "+ score);
  //      String callback = request().getQueryString("callback");
//		ObjectNode result = Json.newObject();
//		result.put("received", true);
//		result.put("score", request().getQueryString("score"));
//		if (callback == null)
//			return ok(result.asText());
//		else
//			return ok(Jsonp.jsonp(callback, result));	
       return ok("Score is saved");	
     }

	public Result addSubmissions(Long assignmentID){
	   JsonNode jsonPayload = request().body().asJson();
           Logger.info("json from client = {}", jsonPayload);

	   Logger.info("AssignmentID is: " + assignmentID);
	   Http.Cookie userIdCookie = request().cookie("custom_canvas_user_id");
	   Long userId = Long.parseLong(userIdCookie.value());
	   Logger.info("UserID is: " + userId);
           Iterator<JsonNode> nodeIterator = jsonPayload.elements();
		
        while (nodeIterator.hasNext()) {
            JsonNode exercise = nodeIterator.next();
	    if(exercise.has("activity")){
	    Problem problem = Problem.find.where().like("url", "%"+exercise.get("activity").asText()+"%").findList().get(0);
	    List<Submission> submissions = Submission.find.where().eq("canvasAssignmentId",assignmentID).eq("studentId",userId).findList();
	    Logger.info("Submission is: " + submissions);
	    
            Submission submission = new Submission();
	    submission.setcanvasAssignmentId(assignmentID);
	    submission.setProblem(problem);
	    submission.setStudentId(userId);
	    submission.setActivity(exercise.get("activity").asText());
	    submission.setCorrect(exercise.get("correct").asLong());
	    submission.setMaxScore(exercise.get("maxscore").asLong());
	    submission.save();
	    
	}
	 }
	return ok();
}
}


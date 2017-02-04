package controllers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import play.*;
import play.data.*;
import play.libs.Json;
import static play.data.Form.*;

import java.net.*;
import java.io.*;

import play.Logger;
import play.mvc.*;
import models.*;
import views.html.*;


public class HomeController extends Controller {
     
    public Result index() throws UnsupportedEncodingException{    
 	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
    	for(String key: postParams.keySet())
    		Logger.info(key + " - " + Arrays.toString(postParams.get(key)));
	
	if (postParams.get("lis_outcome_service_url") == null || postParams.get("lis_result_sourcedid") == null) {
          	flash("warning", "");
	}
	else{
//		Logger.info("OutcomeService URL is: " + postParams.get("lis_outcome_service_url")[0]);
		response().setCookie(new Http.Cookie("lis_outcome_service_url", postParams.get("lis_outcome_service_url")[0],
                 null, null, null, false, false));
//		Logger.info("Result sourcedId is: " +postParams.get("lis_result_sourcedid")[0]);
	response().setCookie(new Http.Cookie("lis_result_sourcedid", URLEncoder.encode(postParams.get("lis_result_sourcedid")[0],"UTF-8"),
                 null, null, null, false, false));
	}

	if(postParams.get("custom_canvas_user_id")==null){
		String userid = postParams.get("user_id")[0];
		Logger.info("User ID from ilearn is: " + userid);
		response().setCookie(new Http.Cookie("custom_canvas_user_id", postParams.get("user_id")[0],null, null, null, false, false));
	}else
	response().setCookie(new Http.Cookie("custom_canvas_user_id", postParams.get("custom_canvas_user_id")[0],null, null, null, false, false));
	//String url = controllers.routes.HomeController.getAssignment().url()
          //      + "?id=" + URLEncoder.encode(request().getQueryString("id"), "UTF-8");
     	response().setCookie(new Http.Cookie("launch_presentation_return_url", postParams.get("launch_presentation_return_url")[0],
                    null, null, null, false, false));
	String contextID = postParams.get("context_id")[0];
	String resourceLinkID = postParams.get("resource_link_id")[0];
	String toolConsumerInstanceGuID = postParams.get("tool_consumer_instance_guid")[0];
	String role = postParams.get("roles")[0];
	Logger.info("Role is: " + role);

	return redirect(controllers.routes.HomeController.getAssignment(contextID, resourceLinkID, toolConsumerInstanceGuID, role));	
     	//return redirect(url);
 	}
	
	//Method to save the created assignment
	public Result addAssignment(String contextID, String resourceLinkID, String toolConsumerInstanceGuID) {        
		DynamicForm bindedForm = Form.form().bindFromRequest();
   		String problemlist = bindedForm.get("url");
	
		Assignment assignment = new Assignment();
		assignment.setContextId(contextID);
		assignment.setResourceLinkId(resourceLinkID);
		assignment.setToolConsumerInstanceGuId(toolConsumerInstanceGuID);
		assignment.save();
 	        Logger.info(problemlist);
		if(null != problemlist|| !problemlist.equals("")) {
			String [] problemArr = problemlist.split(","); 
			for(String problemstr : problemArr) {
				if(null != problemstr && !problemstr.equals("")) {
					Problem problem = new Problem();
					problem.setProblemUrl(problemstr);
					problem.setAssignment(assignment);
					assignment.getProblems().add(problem);
					problem.save();
				}	
			}
		}
		List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).findList();
	
		Http.Cookie launchReturnUrlCookie = request().cookie("launch_presentation_return_url");
		String returnUrl = launchReturnUrlCookie.value();
	
		return ok(showassignment.render(returnUrl,assignment,problems, getPrefix()));
	}
	
	//Get Assignment Method
	public Result getAssignment(String contextID, String resourceLinkID, String toolConsumerInstanceGuID, String role){
	if(Assignment.find.where().eq("contextId",contextID).eq("resourceLinkId",resourceLinkID).eq("toolConsumerInstanceGuId",toolConsumerInstanceGuID).findList().size()==0 && (role.contains("Faculty")|| role.contains("TeachingAssistant") || role.contains("Instructor")))
		return ok(create_exercise.render(contextID, resourceLinkID, toolConsumerInstanceGuID));
	Assignment assignment = Assignment.find.where().eq("contextId",contextID).eq("resourceLinkId", resourceLinkID).eq("toolConsumerInstanceGuId",toolConsumerInstanceGuID).findList().get(0);
	List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).findList();
	if(assignment != null && (role.contains("Faculty") || role.contains("TeachingAssistant") || role.contains("Instructor")))
		return ok(editAssignment.render(assignment, problems));
        
	Logger.info("user ID value from cookie is: " + request().cookie("custom_canvas_user_id").value());
	Http.Cookie userIdCookie = request().cookie("custom_canvas_user_id");
	String userId = userIdCookie.value();
	Logger.info("UserID is: " + userId);
	List<Submission> submissions = new ArrayList<Submission>();
	for(Problem problem: problems){
	List<Submission> submissionsAll = Submission.find.where().eq("problem.problemId",problem.problemId).eq("assignmentId",assignment.assignmentId).eq("studentId",userId).findList();
	Logger.info("Submission list is: " + submissionsAll);
	int correctForThisProblem = 0;
	int maxscoreForThisProblem = 0;
        if(submissionsAll.size()!=0){
		for(Submission s: submissionsAll){
		if(s.getMaxScore()>0)
			maxscoreForThisProblem = (s.getMaxScore()).intValue();
		if(s.getCorrect()> correctForThisProblem)
			correctForThisProblem = (s.getCorrect()).intValue();
		}
	Submission submission = Submission.find.where().eq("problem.problemId", problem.problemId).eq("assignmentId", assignment.assignmentId).eq("studentId",userId).eq("correct",correctForThisProblem).findList().get(0);
	submissions.add(submission);			
	}}
        Logger.info("Submission list is: " + submissions);
	Logger.info("Problems list is: " + problems);
        if(submissions.size()==0)
	return ok(finalAssignment.render(problems,assignment.assignmentId, userId, getPrefix()));

	return ok(finalAssignmentWithSubmission.render(problems,submissions, assignment.assignmentId, userId, getPrefix()));
}	
	
	public String getPrefix() { 
		String prefix = System.getProperty("play.http.context");
		Logger.info("Prefix is: " + prefix);
		if (prefix == null) return ""; else return prefix;
	}

	public Result saveEditedAssignment(Long assignment) {
        
	      DynamicForm bindedForm = Form.form().bindFromRequest();
              String problemlist = bindedForm.get("url");
	      Assignment assignment1 = Assignment.find.byId(assignment);
              Logger.info(problemlist);
		if(null != problemlist|| !problemlist.equals("")) {
			String [] problemArr = problemlist.split(","); 
			for(String problemstr : problemArr) {
			    if(null != problemstr && !problemstr.equals("")) {
				Problem problem = new Problem();
				problem.setProblemUrl(problemstr);
				problem.setAssignment(assignment1);
				assignment1.getProblems().add(problem);
				problem.save();
			   }	
			}
		}
	     List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
    	System.out.println(problems);
	    Http.Cookie launchReturnUrlCookie = request().cookie("launch_presentation_return_url");
	    String returnUrl = launchReturnUrlCookie.value();
	Logger.info("ReturnURL is: " + returnUrl);
        return ok(showassignment.render(returnUrl,assignment1,problems, getPrefix()));
	}
    

	public Result showEditPage(Long assignment) {
		Assignment assignment1 = Assignment.find.byId(assignment);
		
        	List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
        	
        	return ok(editAssignment.render(assignment1, problems));    
    }

}


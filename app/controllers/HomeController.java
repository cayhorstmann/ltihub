package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
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
	private static String getParam(Map<String, String[]> params, String key) {
		String[] values = params.get(key);
		if (values == null || values.length == 0) return null;
		else return values[0];
	}
	
	private static boolean isInstructor(String role) {
		return role.contains("Faculty")|| role.contains("TeachingAssistant") || role.contains("Instructor");
	}
	
	private static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0 || str.trim().equals("null");		
	}
	
	private static String getStackTrace(Throwable t) {
		StringWriter out = new StringWriter();
		t.printStackTrace(new PrintWriter(out));
		return out.toString();
	}	

	private static String httpPost(String urlString, Map<String, String> postData) {
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
			result.append(getStackTrace(ex));
		}
		return result.toString();		
	}


    public Result config() throws UnknownHostException {
        Http.Request request = request();
        return ok(views.xml.lti_config.render()).as("application/xml");
    }     
    
    public Result index() throws UnsupportedEncodingException {    
	 	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	Logger.info("HomeController.index: ");
    	for (String key : postParams.keySet())
    		Logger.info(key + ": " + Arrays.toString(postParams.get(key)));

    	String lisOutcomeServiceURL = getParam(postParams, "lis_outcome_service_url");
    	String lisResultSourcedID = getParam(postParams, "lis_result_sourcedid");
		if (lisOutcomeServiceURL == null || lisResultSourcedID == null) {
          	flash("warning", "");
		}
		else { // TODO: No cookies	
			response().setCookie(new Http.Cookie("lis_outcome_service_url", lisOutcomeServiceURL,
		                 null, null, null, false, false));
			response().setCookie(new Http.Cookie("lis_result_sourcedid", URLEncoder.encode(lisResultSourcedID,"UTF-8"),
		                 null, null, null, false, false));
		}
		String userID = getParam(postParams, "custom_canvas_user_id"); 
		if (userID == null) userID = getParam(postParams, "user_id");
		Logger.info("User ID: " + userID);
		response().setCookie(new Http.Cookie("user_id", userID, null, null, null, false, false));  // TODO: No cookies

		// response().setCookie(new Http.Cookie("launch_presentation_return_url", postParams.get("launch_presentation_return_url")[0],
	    //                null, null, null, false, false));
		String contextID = getParam(postParams, "context_id");
		String resourceLinkID = getParam(postParams, "resource_link_id");
		String toolConsumerInstanceGuID = getParam(postParams, "tool_consumer_instance_guid");
		String role = getParam(postParams, "roles");
		String launchPresentationReturnURL = getParam(postParams, "launch_presentation_return_url");
	    String assignmentId = request().getQueryString("id");
		
		if (assignmentId == null && isInstructor(role))
			return ok(create_exercise.render(contextID, resourceLinkID, toolConsumerInstanceGuID, launchPresentationReturnURL));
		else
			return redirect(controllers.routes.HomeController.getAssignment(role, Long.parseLong(assignmentId)));		     	
 	}

	public Result createAssignment() {		
         return ok(create_exercise_outside_LMS.render());
    }
		
	// This method gets called when an assignment has been created with create_exercise.scala.html.
	public Result addAssignment() {        
       DynamicForm bindedForm = Form.form().bindFromRequest();
       String problemlist = bindedForm.get("url");
       
       Assignment assignment = new Assignment();
       assignment.setContextId(bindedForm.get("context_id"));
       assignment.setResourceLinkId(bindedForm.get("resource_link_id"));
       assignment.setToolConsumerInstanceGuId(bindedForm.get("tool_consumer_id"));
       
       String duration = bindedForm.get("duration");
       if(duration.equals(""))
    	   assignment.setDuration(0L);
       else
    	   assignment.setDuration(Long.parseLong(duration));
       assignment.save();
       Logger.info(problemlist);
   
       if (!isEmpty(problemlist)) {
		   for (String problemstr : problemlist.split("\\s+")) {
			   if (!isEmpty(problemstr)) {
				   Problem problem = new Problem();
				   problem.setProblemUrl(problemstr);
				   problem.setAssignment(assignment);
				   assignment.getProblems().add(problem);
				   problem.save();
			   }	
		   }
       }

       String launchPresentationReturnURL = bindedForm.get("launch_presentation_return_url");
       List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).orderBy("problemId").findList();
	   String assignmentURL = (request().secure() ? "https://" : "http://" ) 
			   + request().host() + getPrefix() + "/assignment?id=" + assignment.getAssignmentId();
	
	
       return ok(showassignment.render(launchPresentationReturnURL, problems, assignmentURL));
    }
	
	public Result addAssignmentOutsideLMS() {        
	      DynamicForm bindedForm = Form.form().bindFromRequest();
   	      String problemlist = bindedForm.get("url");
	      String key = bindedForm.get("key");
	      String secret = bindedForm.get("secret");
	      String duration = bindedForm.get("duration");

	      if(key.equals("fred") && secret.equals("fred")){
	      Assignment assignment = new Assignment();
	      assignment.setDuration(Long.parseLong(duration));
	      assignment.save();

 	      Logger.info(problemlist);
	      if(null != problemlist|| !problemlist.equals("")) {
		     String [] problemArr = problemlist.split("\n"); 
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
	List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).orderBy("problemId").findList();
	return ok(showassignmentOutsideLMS.render(assignment,problems, getPrefix()));
	}
	else
		return ok("Secret or key doesn't match.");
}
	
	//Get Assignment Method
	public Result getAssignment(String role, Long assignmentId){
        Logger.info("getAssignment() Commencing...");
        Logger.info("Role is: " + role);
        Logger.info("Assignment_id is: " + assignmentId);

		Assignment assignment = Assignment.find.byId(assignmentId);

		if(assignment != null && (role.contains("Faculty")|| role.contains("TeachingAssistant") || role.contains("Instructor"))){
			List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).orderBy("problemId").findList();
			return ok(showAssignmentInstructorView.render(problems,assignmentId, "Teacher", getPrefix()));
		}

		List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignmentId).orderBy("problemId").findList();
		Http.Cookie userIdCookie = request().cookie("user_id");
		String userId = userIdCookie.value();
		Logger.info("UserID is: " + userId);
		List<Submission> submissions = new ArrayList<>();
		for(Problem problem: problems){
			List<Submission> submissionsAll = Submission.find.where().eq("problem.problemId",problem.problemId).eq("assignmentId",assignmentId).eq("studentId",userId).findList();

			int correctForThisProblem = 0;
			int maxscoreForThisProblem = 0;
			if(submissionsAll.size()!=0){
				for(Submission s: submissionsAll){
					if(s.getMaxScore()>0)
						maxscoreForThisProblem = (s.getMaxScore()).intValue();
					if(s.getCorrect()> correctForThisProblem)
						correctForThisProblem = (s.getCorrect()).intValue();
				}

				Submission submission = Submission.find.where().eq("problem.problemId", problem.problemId).eq("assignmentId", assignmentId).eq("studentId",userId).eq("correct",correctForThisProblem).findList().get(0);
				submissions.add(submission);
			}
		}
        Logger.info("Submissions list is: " + submissions);
        Logger.info("Problems list is: " + problems);
		Long duration = assignment.getDuration();
		Logger.info("Duration is: " + duration);
        if(submissions.size()==0) {
			if(duration == 0)
				return ok(finalAssignment.render(problems,assignmentId, userId, getPrefix()));
			else
			    return ok(timedAssignmentWelcomeView.render(problems, assignmentId, duration));
		}
		else
		{
            if(duration != 0)
                return ok("This was a timed assignment and you have already tried it once. Please look at the grade book to see your grades");
            else {
				System.out.println("");
				return ok(finalAssignmentWithSubmission.render(problems, submissions, assignmentId, userId, getPrefix()));
			}
		}
    }

	public Result showTimedAssignment(Long assignmentId, Long duration) {
		Assignment assignment = Assignment.find.byId(assignmentId);

        List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignmentId).orderBy("problemId").findList();
        Http.Cookie userIdCookie = request().cookie("user_id");
        String userId = userIdCookie.value();
        Logger.info("UserID is: " + userId);

        return ok(timedFinalAssignment.render(problems, assignmentId, userId, getPrefix(), duration));
}


	public String getPrefix() { 
		String prefix = System.getProperty("play.http.context");
		if (prefix == null) return "/"; 
		else {
			if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
			if (!prefix.startsWith("/")) prefix = "/" + prefix;
			return prefix;
		}
	}

	public Result deleteProblem(Long assignmentId, Long problemID) {
  		Problem.delete(problemID);
		Assignment assignment = Assignment.find.byId(assignmentId);
		List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).orderBy("problemId").findList();
  		return ok(editAssignment.render(assignment, problems));
	}
	
	public Result saveEditedAssignment(Long assignment) {
	    DynamicForm bindedForm = Form.form().bindFromRequest();
	    String problemlist = bindedForm.get("url");
	    Assignment assignment1 = Assignment.find.byId(assignment);
	    Logger.info(problemlist);
	    if(null != problemlist|| !problemlist.equals("")) {
	        String [] problemArr = problemlist.split("\n");
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
	    List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).orderBy("problemId").findList();

	    Http.Cookie launchReturnUrlCookie = request().cookie("launch_presentation_return_url");
	    String returnUrl = launchReturnUrlCookie.value();
	    Logger.info("ReturnURL is: " + returnUrl);
	    //TODO: Check
	    String assignmentURL = (request().secure() ? "https://" : "http://" ) 
				   + request().host() + getPrefix() + "/assignment?id=" + assignment;
        return ok(showassignment.render(returnUrl,problems, assignmentURL);
	}

	public Result showEditPage(Long assignment) {
		Assignment assignment1 = Assignment.find.byId(assignment);
		
        	List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).orderBy("problemId").findList();
        	
        	return ok(editAssignment.render(assignment1, problems));    
    }

}


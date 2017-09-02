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
import java.util.stream.Collectors;

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
		return role != null && (role.contains("Faculty") || role.contains("TeachingAssistant") || role.contains("Instructor"));
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
		if (isEmpty(userID)) return badRequest("No user id");
		Logger.info("User ID: " + userID);

		String contextID = getParam(postParams, "context_id");
		String resourceLinkID = getParam(postParams, "resource_link_id");
		String toolConsumerInstanceGuID = getParam(postParams, "tool_consumer_instance_guid");
		String role = getParam(postParams, "roles");
		String launchPresentationReturnURL = getParam(postParams, "launch_presentation_return_url");
	    String assignmentId = request().getQueryString("id");
		
		if (assignmentId == null && isInstructor(role))
			return ok(create_exercise.render(contextID, resourceLinkID, toolConsumerInstanceGuID, launchPresentationReturnURL));
		else // TODO: Why redirect???
			return redirect(controllers.routes.HomeController.getAssignment(role, Long.parseLong(assignmentId), userID));		     	
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
   
        addNewProblemsFromFormSubmission(problemlist, assignment);

       String launchPresentationReturnURL = bindedForm.get("launch_presentation_return_url");
        List<Problem> problems = assignment.getProblems();
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

		if(key.equals("fred") && secret.equals("fred")){ // TODO
		    Assignment assignment = new Assignment();
		    assignment.setDuration(Long.parseLong(duration));
		    assignment.save();
	
            addNewProblemsFromFormSubmission(problemlist, assignment);
            List<Problem> problems = assignment.getProblems();
			return ok(showassignmentOutsideLMS.render(assignment,problems, getPrefix()));
		}
		else
			return ok("Secret or key doesn't match.");
	}
	
	public Result getAssignment(String role, Long assignmentId, String userId){
        Logger.info("getAssignment. Role: " + role + " Assignment_id: " + assignmentId + " UserID: " + userId);

		Assignment assignment = Assignment.find.byId(assignmentId);

		if(assignment != null && isInstructor(role)){
			List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment.assignmentId).orderBy("problemId").findList();
			return ok(showAssignmentInstructorView.render(problems,assignmentId, "Teacher", getPrefix()));
		}

        // Maps each problemId to the submission with the most correct for that problem for the given user ID
        Map<Long, Submission> problemIdToSubmissionWithMostCorrect = new HashMap<>();
        Long duration = assignment.getDuration();
        List<Problem> problems = assignment.getProblems();

		for(Problem problem: problems){
            Optional<Submission> submissionStream = problem.getSubmissions().stream()
                    .filter((submission) -> (submission.getStudentId().equals(userId)))
                    .max((submission1, submission2) ->
                            (submission1.getCorrect().compareTo(submission2.getCorrect())));

            if (submissionStream.isPresent())
                problemIdToSubmissionWithMostCorrect.put(
                        problem.getProblemId(), submissionStream.get());
				}

        if (duration > 0 && problemIdToSubmissionWithMostCorrect.isEmpty())
            return ok(timedAssignmentWelcomeView.render(problems, assignmentId, userId, duration));
		else
            return ok(combinedAssignment.render(getPrefix(), assignmentId, userId, duration, isInstructor(role),
                    problems, problemIdToSubmissionWithMostCorrect));
    }

	public Result showTimedAssignment(Long assignmentId, String userId, Long duration) {
		Assignment assignment = Assignment.find.byId(assignmentId);
        List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignmentId).orderBy("problemId").findList();
        Logger.info("showTimedAssignment. UserID is: " + userId);
        return ok(timedFinalAssignment.render(problems, assignmentId, userId, getPrefix(), duration));
	}

	/**
	 * Gets the prefix for this version of LTIHub. These are used because all instances share the same
	 * load balancer so that we only need one SSL certificate.
	 * @return the prefix (such as /lti or /ltitest)
	 */	
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
	
    public Result saveEditedAssignment(Long assignmentId) {
	    DynamicForm bindedForm = Form.form().bindFromRequest();
        String problemUrls = bindedForm.get("url");
        Assignment assignment = Assignment.find.byId(assignmentId);
        Logger.info("New Problem Submission URLs: " + problemUrls);
        addNewProblemsFromFormSubmission(problemUrls, assignment);
        List<Problem> problems = assignment.getProblems();

	    Http.Cookie launchReturnUrlCookie = request().cookie("launch_presentation_return_url");
	    String returnUrl = launchReturnUrlCookie.value();
	    Logger.info("ReturnURL is: " + returnUrl);
        //TODO: Check
	    String assignmentURL = (request().secure() ? "https://" : "http://" ) 
                + request().host() + getPrefix() + "/assignment?id=" + assignmentId;
        return ok(showassignment.render(returnUrl,problems, assignmentURL));
	}

	public Result showEditPage(Long assignment) {
		Assignment assignment1 = Assignment.find.byId(assignment);
		
        	List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).orderBy("problemId").findList();
        	
        	return ok(editAssignment.render(assignment1, problems));    
    }


    private void addNewProblemsFromFormSubmission(String newProblemFormSubmission, Assignment assignment) {
        if(newProblemFormSubmission != null && !newProblemFormSubmission.trim().isEmpty()) {
            String[] newProblemUrls = newProblemFormSubmission.split("\\s+");
            for(String newProblemUrl: newProblemUrls) {
                if(!newProblemUrl.trim().isEmpty()) {
                    addNewProblem(newProblemUrl.trim(), assignment);
                }
            }
        }
}

    /**
     * Adds a new problem to the database with the given problemUrl on the given assignment.
     * @param problemUrl the URL that links to the problem
     * @param assignment the assignment where this problem belongs
     */
    private void addNewProblem(String problemUrl, Assignment assignment) {
        Problem problem = new Problem();
        problem.setProblemUrl(problemUrl);
        problem.setAssignment(assignment);
        assignment.getProblems().add(problem);
        problem.save();
    }


}

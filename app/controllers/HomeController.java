package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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

import javax.inject.Inject;

import com.avaje.ebean.Ebean;

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
        String host = request().host() + getPrefix();
        return ok(views.xml.lti_config.render(host)).as("application/xml");
    }     
    
    public Result index() throws UnsupportedEncodingException {    
	 	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	if (postParams == null)
	 		return badRequest("Post params missing." + "\n" +
								"Request body: " + request().body().asText());

	 	Logger.info("HomeController.index: ");
    	for (String key : postParams.keySet())
    		Logger.info(key + ": " + Arrays.toString(postParams.get(key)));

    	String lisOutcomeServiceURL = getParam(postParams, "lis_outcome_service_url");
    	String lisResultSourcedID = getParam(postParams, "lis_result_sourcedid");

    	String userID = getParam(postParams, "custom_canvas_user_id"); 
		if (userID == null) userID = getParam(postParams, "user_id");
		if (isEmpty(userID)) return badRequest("No user id");
		Logger.info("User ID: " + userID);

		String contextID = getParam(postParams, "context_id");
		String resourceLinkID = getParam(postParams, "resource_link_id");
		String toolConsumerInstanceGuID = getParam(postParams, "tool_consumer_instance_guid");
		String role = getParam(postParams, "roles");
		String launchPresentationReturnURL = getParam(postParams, "launch_presentation_return_url");
	    String assignmentID = request().getQueryString("id");
	    if (assignmentID == null) { 
	    	List<Assignment> assignments = Ebean.find(Assignment.class)
	    			.where()
	    			.eq("context_id", contextID)
	    			.eq("resource_link_id", resourceLinkID)
	    			.findList();
	    	Logger.info("id empty, looking for assignment with context_id " + contextID + ", resource_link_id " + resourceLinkID + ", found " + assignments.size());
	    	if (assignments.size() == 1) assignmentID = "" + assignments.get(0).getAssignmentId();
	    }
		
	    boolean instructor = isInstructor(role); 
	    
		if (assignmentID == null) {
			if (instructor)		
				return ok(create_exercise.render(contextID, resourceLinkID, toolConsumerInstanceGuID, launchPresentationReturnURL));
			else 
				return badRequest("No assignment id and no assignment with context_id " + contextID + ", resource_link_id " + resourceLinkID + "");
		}
		
		if (isEmpty(lisOutcomeServiceURL)) {
          	return badRequest("lis_outcome_service_url missing.");
		} else if (!instructor && isEmpty(lisResultSourcedID)) {
			return badRequest("lis_result_sourcedid missing.");
		} else { // TODO: No cookies
			response().setCookie(new Http.Cookie("lis_outcome_service_url", lisOutcomeServiceURL,
		                 null, null, null, false, false));
			if (lisResultSourcedID != null) response().setCookie(new Http.Cookie("lis_result_sourcedid", URLEncoder.encode(lisResultSourcedID,"UTF-8"),
		                 null, null, null, false, false));
		}

		return getAssignment(role, Long.parseLong(assignmentID), userID, lisOutcomeServiceURL, lisResultSourcedID);
 	}

	public Result createAssignment() {		
         return ok(create_exercise_outside_LMS.render());
    }
		
	/**
	 * Yields a map of query parameters in a HTTP URI
	 * @param url the HTTP URL
	 * @return the map of query parameters or an empty map if there are none
	 * For example, if uri is http://fred.com?name=wilma&passw%C3%B6rd=c%26d%3De
	 * then the result is { "name" -> "wilma", "passwörd" -> "c&d=e" }
	 */
	private static Map<String, String> getParams(String url)
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
	
	// This method gets called when an assignment has been created with create_exercise.scala.html.
	public Result addAssignment() {
		Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	
		String problemlist = getParam(postParams, "url");
       
		Assignment assignment = new Assignment();
		assignment.setContextId(getParam(postParams, "context_id"));
		assignment.setResourceLinkId(getParam(postParams, "resource_link_id"));
		assignment.setToolConsumerInstanceGuId(getParam(postParams, "tool_consumer_id"));
       
		String duration = getParam(postParams, "duration");
		if(duration.equals(""))
    	   assignment.setDuration(0L);
		else
    	   assignment.setDuration(Long.parseLong(duration));
		assignment.save();
		Logger.info(problemlist);
   
        addNewProblemsFromFormSubmission(problemlist, assignment);

        String launchPresentationReturnURL = getParam(postParams, "launch_presentation_return_url");
        List<Problem> problems = assignment.getProblems();
        String assignmentURL = "https://" + request().host() + getPrefix() + "/assignment?id=" + assignment.getAssignmentId();
        return ok(showassignment.render(launchPresentationReturnURL, 
    		   getParams(launchPresentationReturnURL), problems, assignmentURL));
    }
	
	public Result addAssignmentOutsideLMS() {        
	    Map<String, String[]> postParams = request().body().asFormUrlEncoded();
		String problemlist = getParam(postParams, "url");
		String key = getParam(postParams, "key");
		String secret = getParam(postParams, "secret");
		String duration = getParam(postParams, "duration");

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
	
	private Result getAssignment(String role, Long assignmentId, String userId, String lisOutcomeServiceURL, String lisResultSourcedID){
		Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	
		Assignment assignment = Ebean.find(Assignment.class, assignmentId);

        // Maps each problemId to the submission with the most correct for that problem for the given user ID
        Map<Long, Submission> problemIdToSubmissionWithMostCorrect = new HashMap<>();

        Long duration = assignment.getDuration();
        List<Problem> problems = assignment.getProblems();

        if(!isInstructor(role)) {
        	for(Problem problem: problems){
        		Optional<Submission> submissionStream = problem.getSubmissions().stream()
                    .filter((submission) -> (submission.getStudentId().equals(userId)))
                    .max((submission1, submission2) ->
                            (submission1.getCorrect().compareTo(submission2.getCorrect())));

        		if (submissionStream.isPresent())
        			problemIdToSubmissionWithMostCorrect.put(
                        problem.getProblemId(), submissionStream.get());
        	}
		}

        return ok(combinedAssignment.render(getPrefix(), assignmentId, userId, duration, isInstructor(role),
            problems, problemIdToSubmissionWithMostCorrect, lisOutcomeServiceURL, lisResultSourcedID));
    }

	public Result getSubmissionViewer(Long assignmentId, String role) {

		Assignment assignment = Ebean.find(Assignment.class, assignmentId);
		List<Problem> problems = assignment.getProblems();

		if (!isInstructor(role))
			return badRequest("Error, user is not authorized to view submissions.");

		return ok(studentSumbissionsViewer.render(assignmentId, problems));
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
		Ebean.delete(Ebean.find(Problem.class, problemID));
		Assignment assignment = Ebean.find(Assignment.class, assignmentId); // TODO: Doesn't the ORM do that?
		List<Problem> problems = Ebean.find(Problem.class)
			.where()
			.eq("assignment.assignmentId",assignment.assignmentId)
			.orderBy("problemId")
			.findList();
  		return ok(editAssignment.render(assignment, problems));
	}
	
    public Result saveEditedAssignment(Long assignmentId) {
	    Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	String problemUrls = getParam(postParams, "url");
        Assignment assignment = Ebean.find(Assignment.class, assignmentId);
        Logger.info("New Problem Submission URLs: " + problemUrls);
        addNewProblemsFromFormSubmission(problemUrls, assignment);
        List<Problem> problems = assignment.getProblems();

	    Http.Cookie launchReturnUrlCookie = request().cookie("launch_presentation_return_url");
	    String returnUrl = launchReturnUrlCookie.value();
	    Logger.info("ReturnURL is: " + returnUrl);
        //TODO: Check
	    String assignmentURL = "https://" + request().host() + getPrefix() + "/assignment?id=" + assignmentId;
        return ok(showassignment.render(returnUrl, getParams(returnUrl), problems, assignmentURL));
	}

	public Result showEditPage(Long assignment) {
		Assignment assignment1 = Ebean.find(Assignment.class, assignment);
		
        	List<Problem> problems = Ebean.find(Problem.class) // TODO: Doesn't the ORM do that?
        			.fetch("assignment")
        			.where()
        			.eq("assignment.assignmentId",assignment1.assignmentId)
        			.orderBy("problemId")
        			.findList();
        	
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

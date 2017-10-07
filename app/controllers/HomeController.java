package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Assignment;
import models.Problem;
import models.Util;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.combinedAssignment;
import views.html.create_exercise;
import views.html.create_exercise_outside_LMS;
import views.html.editAssignment;
import views.html.showassignment;
import views.html.showassignmentOutsideLMS;
import views.html.studentSubmissionsViewer;

import com.avaje.ebean.Ebean;

public class HomeController extends Controller {
    public Result config() throws UnknownHostException {
        String host = request().host() + getPrefix();
        return ok(views.xml.lti_config.render(host)).as("application/xml");
    }     
    
    public Result index() throws UnsupportedEncodingException {    
	 	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	Logger.info("HomeController.index: " + Util.paramsToString(postParams));
	 	if (!Util.validate(request())) {
	 		session().clear();
	 		return badRequest("Failed OAuth validation");
	 	}	 	
	 	
    	String lisOutcomeServiceURL = Util.getParam(postParams, "lis_outcome_service_url");
    	String lisResultSourcedID = Util.getParam(postParams, "lis_result_sourcedid");

    	String userID = Util.getParam(postParams, "custom_canvas_user_id");  // TODO: Add server ID to user ID
		if (userID == null) userID = Util.getParam(postParams, "user_id");
		if (Util.isEmpty(userID)) return badRequest("No user id");
		session().put("user", userID);

		String contextID = Util.getParam(postParams, "context_id");
		String resourceLinkID = Util.getParam(postParams, "resource_link_id");
		String toolConsumerInstanceGuID = Util.getParam(postParams, "tool_consumer_instance_guid");
		String role = Util.getParam(postParams, "roles");
		String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
	    String assignmentID = request().getQueryString("id");
	    if (assignmentID == null) { 
	    	List<Assignment> assignments = Ebean.find(Assignment.class)
	    			.where()
	    			.eq("context_id", contextID)
	    			.eq("resource_link_id", resourceLinkID)
	    			.findList();
	    	if (assignments.size() == 1) assignmentID = "" + assignments.get(0).getAssignmentId();
	    }
		
	    boolean instructor = Util.isInstructor(role); 
	    
		if (assignmentID == null) {
			if (instructor)		
				return ok(create_exercise.render(contextID, resourceLinkID, toolConsumerInstanceGuID, launchPresentationReturnURL));
			else {
				String result = "No assignment id and no assignment with context_id " + contextID + ", resource_link_id " + resourceLinkID;
				Logger.info(result);
				return badRequest(result);
			}
		}
		
		if (Util.isEmpty(lisOutcomeServiceURL)) {
          	return badRequest("lis_outcome_service_url missing.");
		} else if (!instructor && Util.isEmpty(lisResultSourcedID)) {
			return badRequest("lis_result_sourcedid missing.");
		}  else { // TODO: Eliminate 
			response().setCookie(new Http.Cookie("lis_outcome_service_url", lisOutcomeServiceURL,
		                 null, null, null, false, false));
			if (lisResultSourcedID != null) response().setCookie(new Http.Cookie("lis_result_sourcedid", URLEncoder.encode(lisResultSourcedID,"UTF-8"),
		                 null, null, null, false, false));
		}
		Long assignmentId = Long.parseLong(assignmentID); 

		Assignment assignment = Ebean.find(Assignment.class, assignmentId);
		Long duration = assignment.getDuration();
		return ok(combinedAssignment.render(getPrefix(), assignmentId, userID, duration, Util.isInstructor(role),
		    lisOutcomeServiceURL, lisResultSourcedID));
 	}

	// This method gets called when an assignment has been created with create_exercise.scala.html.
	public Result addAssignment() {
		Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	
		String problemlist = Util.getParam(postParams, "url");
       
		Assignment assignment = new Assignment();
		assignment.setContextId(Util.getParam(postParams, "context_id"));
		assignment.setResourceLinkId(Util.getParam(postParams, "resource_link_id"));
		assignment.setToolConsumerInstanceGuId(Util.getParam(postParams, "tool_consumer_id"));
       
		String duration = Util.getParam(postParams, "duration");
		if(duration.equals(""))
    	   assignment.setDuration(0L);
		else
    	   assignment.setDuration(Long.parseLong(duration));
		assignment.save();
   
        addNewProblemsFromFormSubmission(problemlist, assignment);

        String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
        List<Problem> problems = assignment.getProblems();
        String assignmentURL = "https://" + request().host() + getPrefix() + "/assignment?id=" + assignment.getAssignmentId();
        return ok(showassignment.render(launchPresentationReturnURL, 
    		   Util.getParams(launchPresentationReturnURL), problems, assignmentURL));
    }
	
	public Result createAssignmentOutsideLMS() {		
        return ok(create_exercise_outside_LMS.render());
   }
		
	public Result addAssignmentOutsideLMS() {        
	    Map<String, String[]> postParams = request().body().asFormUrlEncoded();
		String problemlist = Util.getParam(postParams, "url");
		String key = Util.getParam(postParams, "key");
		String secret = Util.getParam(postParams, "secret");
		String duration = Util.getParam(postParams, "duration");
		if (duration == null) duration = "0";

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
	
	@Security.Authenticated(Secured.class)
	public Result getSubmissionViewer(Long assignmentId) {

		Assignment assignment = Ebean.find(Assignment.class, assignmentId);
		List<Problem> problems = assignment.getProblems();

		return ok(studentSubmissionsViewer.render(assignmentId, problems));
		//TODO: Make it viewable by student
		//TODO: This is getting more complicated because different students have different problems
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
	 	String problemUrls = Util.getParam(postParams, "url");
        Assignment assignment = Ebean.find(Assignment.class, assignmentId);
        addNewProblemsFromFormSubmission(problemUrls, assignment);
        List<Problem> problems = assignment.getProblems();

	    Http.Cookie launchReturnUrlCookie = request().cookie("launch_presentation_return_url"); // TODO: Eliminate
	    String returnUrl = launchReturnUrlCookie.value();
	    String assignmentURL = "https://" + request().host() + getPrefix() + "/assignment?id=" + assignmentId;
        return ok(showassignment.render(returnUrl, Util.getParams(returnUrl), problems, assignmentURL));
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

	/*
	 * Format for problems:
	 * https://... [x%] [ym]
	 * ...
	 * ---
	 * https://... [x%] [ym]
	 * ...
	 */
	
    private void addNewProblemsFromFormSubmission(String newProblemFormSubmission, Assignment assignment) {
        if(newProblemFormSubmission != null && !newProblemFormSubmission.trim().isEmpty()) {
        	String[] groups = newProblemFormSubmission.split("\\s+-{3,}\\s+");
        	for (int problemGroup = 0; problemGroup < groups.length; problemGroup++) {
	            String[] lines = groups[problemGroup].split("\\n+");
	            for(String line: lines) {
	            	String problemUrl = null;
	            	Double weight = null;
	            	Integer duration = null;
	            	//TODO: Error checking/reporting
	            	for (String token: line.split("\\s+")) {
	            		if (token.startsWith("https")) problemUrl = token;
	            		else if (token.endsWith("%")) weight = 0.01 * Double.parseDouble(token.substring(0, token.length() - 1));
	            		else if (token.endsWith("m")) duration = Integer.parseInt(token.substring(0, token.length() - 1));
	            		else throw new IllegalArgumentException("Bad token: " + token);
	            	}	            	
	            	Problem problem = new Problem(assignment, problemUrl, problemGroup, weight, duration);
	                assignment.getProblems().add(problem);
	                problem.save();
	            }
        	}
        }
    }
}
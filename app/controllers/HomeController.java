package controllers;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import io.ebean.Ebean;
import models.Assignment;
import models.Problem;
import models.Util;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.combinedAssignment;
import views.html.createAssignment;
import views.html.showAssignment;
import views.html.studentSubmissionsViewer;

/*
 * For Canvas, a problem must be created through a separate pathway than /assignment
 * because the context_id and resource_link_id are all the same until Canvas has received
 * a callback from the launch presentation return URL.
 * 
 * In Moodle, there is no UI (or at least, I don't know of one) to customize an assignment.
 * There, we create a blank assignment with the given context_id and resource_link_id (which
 * are different for each link). The instructor then needs to visit that assignment 
 * and customize it.    
 */
	    
public class HomeController extends Controller {
    public Result config() throws UnknownHostException {
        String host = request().host() + getPrefix();
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
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
    	String lisResultSourcedId = Util.getParam(postParams, "lis_result_sourcedid");
    	String oauthConsumerKey = Util.getParam(postParams, "oauth_consumer_key");
    	
    	String userId = Util.getParam(postParams, "user_id");
		if (Util.isEmpty(userId)) return badRequest("No user id");
		session().put("user", userId);

		String contextId = Util.getParam(postParams, "context_id");
		String resourceLinkId = Util.getParam(postParams, "resource_link_id");
		String toolConsumerId = Util.getParam(postParams, "tool_consumer_instance_guid");
		String role = Util.getParam(postParams, "roles");
		String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
	    String assignmentIdString = request().getQueryString("id");
	    long assignmentId = assignmentIdString == null ? -1 : Long.parseLong(assignmentIdString);
	    if (assignmentId == -1) {  
	    	List<Assignment> assignments = Ebean.find(Assignment.class)
	    			.where()
	    			.eq("toolConsumerId", toolConsumerId)
	    			.eq("contextId", contextId)
	    			.eq("resourceLinkId", resourceLinkId)
	    			.findList();
	    	if (assignments.size() == 1) assignmentId = assignments.get(0).id;
	    	if (assignments.size() > 1) { 
				String result = "No assignment id and multiple assignments with context_id " + contextId + ", resource_link_id " + resourceLinkId;
				Logger.info(result);
				return badRequest(result);
	    	}
	    }
		
	    boolean isInstructor = Util.isInstructor(role); 
	    
		if (assignmentId == -1) {
			if (isInstructor)		
				return ok(createAssignment.render(contextId, resourceLinkId, 
						toolConsumerId, launchPresentationReturnURL));
			else {
				String result = "No assignment id and no assignment with context_id " + contextId + ", resource_link_id " + resourceLinkId;
				Logger.info(result);
				return badRequest(result);
			}
		}
		
		if (Util.isEmpty(lisOutcomeServiceURL)) {
          	return badRequest("lis_outcome_service_url missing.");
		} else if (!isInstructor && Util.isEmpty(lisResultSourcedId)) {
			return badRequest("lis_result_sourcedid missing.");
		}
		Assignment assignment = Ebean.find(Assignment.class, assignmentId);
		
		return ok(combinedAssignment.render(getPrefix(), assignmentId, userId, 
			toolConsumerId, contextId,  
			assignment.duration, isInstructor,
		    lisOutcomeServiceURL, lisResultSourcedId, oauthConsumerKey));
 	}

    /*
     * Called from Canvas and potentially other LMS with a "resource selection" interface
     */
    public Result createAssignment() throws UnsupportedEncodingException {    
	 	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	Logger.info("HomeController.createAssignment: " + Util.paramsToString(postParams));
	 	if (!Util.validate(request())) {
	 		session().clear();
	 		return badRequest("Failed OAuth validation");
	 	}	 	
	 	
		String role = Util.getParam(postParams, "roles");
		if (!Util.isInstructor(role)) 
			return badRequest("Instructor role is required to create an assignment.");
    	String userId = Util.getParam(postParams, "user_id");
		if (Util.isEmpty(userId)) 
			return badRequest("No user id");
		session().put("user", userId);

		String contextId = Util.getParam(postParams, "context_id");
		String resourceLinkId = Util.getParam(postParams, "resource_link_id");
		String toolConsumerId = Util.getParam(postParams, "tool_consumer_instance_guid");
		String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
		return ok(createAssignment.render(contextId, resourceLinkId, 
			toolConsumerId, launchPresentationReturnURL));			
 	}
        
	// This method gets called when an assignment has been created with createAssignment.scala.html.
	public Result addAssignment() {
		Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	 	
		String problemlist = Util.getParam(postParams, "url");
       
		Assignment assignment = new Assignment();
		assignment.contextId = Util.getParam(postParams, "context_id");
		assignment.resourceLinkId = Util.getParam(postParams, "resource_link_id");
		assignment.toolConsumerId = Util.getParam(postParams, "tool_consumer_id");
       
		String duration = Util.getParam(postParams, "duration");
		if(duration.equals(""))
    	   assignment.duration = 0;
		else
    	   assignment.duration = Integer.parseInt(duration);
		assignment.save();
		try {
			addNewProblemsFromFormSubmission(problemlist, assignment);
		} catch (Exception ex) {
			return badRequest(ex.getMessage());
		}

        String launchPresentationReturnURL = Util.getParam(postParams, "launch_presentation_return_url");
        List<Problem> problems = assignment.getProblems();
        String assignmentURL = "https://" + request().host() + getPrefix() + "/assignment?id=" + assignment.id;
        return ok(showAssignment.render(launchPresentationReturnURL, 
    		   Util.getParams(launchPresentationReturnURL), problems, assignmentURL));
    }
	
	
	@Security.Authenticated(Secured.class)
	public Result getSubmissionViewer(Long assignmentId) {

		Assignment assignment = Ebean.find(Assignment.class, assignmentId);
		List<Problem> problems = assignment.getProblems();

		return ok(studentSubmissionsViewer.render(assignmentId, problems));
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

	/*
	 * Format for problems:
	 * https://... [x%] 
	 * ...
	 * ---
	 * https://... [x%] 
	 * ...
	 */
	
    private void addNewProblemsFromFormSubmission(String newProblemFormSubmission, Assignment assignment) {
        if(newProblemFormSubmission != null && !newProblemFormSubmission.trim().isEmpty()) {
        	String[] groups = newProblemFormSubmission.split("\\s+-{3,}\\s+");
        	for (int problemGroup = 0; problemGroup < groups.length; problemGroup++) {
	            String[] lines = groups[problemGroup].split("\\n+");
	            if (lines.length == 0) throw new IllegalArgumentException("No problems given");
	            String[] problemUrls = new String[lines.length];
	            Double[] weights = new Double[lines.length];	            
	            for (int i = 0; i < lines.length; i++) {
	            	for (String token: lines[i].split("\\s+")) {
	            		if (token.startsWith("https")) problemUrls[i] = token;
	            		else if (token.startsWith("http")) problemUrls[i] = "https" + token.substring(4);
	            		else if (token.endsWith("%")) weights[i] = 0.01 * Double.parseDouble(token.substring(0, token.length() - 1));
	            		else throw new IllegalArgumentException("Bad token: " + token);
	            	}	            	
	            }
	            double weightSum = 0;
	            int noWeights = 0;
	            for (int i = 0; i < lines.length; i++) {
	            	if (weights[i] == null) {
	            		noWeights++;
	            	} else {
	            		if (weights[i] < 0) throw new IllegalArgumentException("Bad weight: " + 100 * weights[i]);
	            		else weightSum += weights[i];
	            	} 
	            }
	            if (noWeights > 0) {
	            	if (weightSum > 1) {
	            		throw new IllegalArgumentException("Sum of weights > 100%");
	            	}
	            	double defaultWeight = (1 - weightSum) / noWeights;
	            	for (int i = 0; i < lines.length; i++)
	            		if (weights[i] == null) weights[i] = defaultWeight;
	            } else if (weightSum > 1) {
	            	for (int i = 0; i < lines.length; i++)
	            		weights[i] /= weightSum;
	            }
	            		
	            for (int i = 0; i < lines.length; i++) {
	            	Problem problem = new Problem(assignment, problemUrls[i], problemGroup, weights[i]);
	            	assignment.getProblems().add(problem);
	            	problem.save();
	            }
        	}
        }
    }
}

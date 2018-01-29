package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.ebean.Ebean;
import models.Assignment;
import models.AssignmentWork;
import models.Meyer;
import models.Problem;
import models.ProblemWork;
import models.Submission;
import models.Util;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class SubmissionController extends Controller {

    // Save problem submission that is sent back from combinedAssignment
    public Result addSubmission() {
        JsonNode params = request().body().asJson();
        if (params == null)
            return badRequest("SubmissionController.addSubmission: Expected Json data, received " + request());

        Logger.info("SubmissionController.addSubmission: params=" + Json.stringify(params));

        try {
        	Problem problem = Ebean.find(Problem.class, params.get("problemId").asLong(0L));
        	String userId = params.get("userId").textValue();
        	String toolConsumerId = params.get("toolConsumerId").textValue();
        	String contextId = params.get("contextId").textValue();
        	String state = params.get("state").textValue();
        	double score = params.get("score").asDouble(); 
        	long clientStamp = params.get("clientStamp").asLong(0L);
            Map<String, Object> responseMap = new HashMap<>();
        	long now = new Date().getTime();
        	long endDate;
            Assignment assignment = problem.assignment;
            if (assignment.duration == 0) endDate = Long.MAX_VALUE;
            else {
	    		AssignmentWork awork = Ebean.find(AssignmentWork.class)
	    	            .where()
	    	                .eq("problem.id", problem.id)
	    	                .eq("studentId", userId)
	    	                .eq("toolConsumerId", toolConsumerId)
	    	                .eq("contextId", contextId)
	    	                .findOne();
	    		int GRACE_PERIOD = 2; // in minutes
	    		endDate = awork.startTime.getTime() + 1000 * 60 * (assignment.duration + GRACE_PERIOD);
            }
    		
        	Ebean.execute(() -> {
        		ProblemWork work = Ebean.find(ProblemWork.class)
        	            .where()
        	                .eq("problem.id", problem.id)
        	                .eq("studentId", userId)
        	                .eq("toolConsumerId", toolConsumerId)
        	                .eq("contextId", contextId)
        	                .findOne();
        		if (work.clientStamp < clientStamp && now < endDate) {
        			String previousState = work.state == null ? "" : work.state;
        			String script = Meyer.shortestEditScript(previousState, state);
        			work.state = state;
        			work.highestScore = Math.max(work.highestScore, score);
        			work.clientStamp = clientStamp;
        			Submission submission = new Submission();
        			submission.studentId = userId;
        			submission.toolConsumerId = toolConsumerId;
        			submission.contextId = contextId;
        			submission.problem = problem;
        			submission.score = score;
        			submission.script = script;
        			submission.previous = work.lastDetail;
        			submission.save();
        			work.lastDetail = submission;
        			work.save();
        		}
        		if (work.lastDetail != null)
        			responseMap.put("submittedAt", work.lastDetail.submittedAt);
                responseMap.put("highestScore", work.highestScore);        		
        	});        	
            return ok(Json.toJson(responseMap));
        } catch (Exception ex) {
            Logger.info(Util.getStackTrace(ex));
            return badRequest("Received problem content: " + Json.stringify(params) + "\n" +
                    "Exception message: " + ex.getMessage());
        }
    }
}
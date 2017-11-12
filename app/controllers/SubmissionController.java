package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;

import models.Assignment;
import models.Problem;
import models.Submission;
import models.Util;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class SubmissionController extends Controller {

    // Method to save problem submission that is sent back from codecheck server
	// TODO: add params to JSON
    public Result addSubmission() {
        JsonNode params = request().body().asJson();
        if (params == null)
            return badRequest("SubmissionController.addSubmission: Expected Json data, received " + request());

        Logger.info("SubmissionController.addSubmission: params=" + Json.stringify(params));

        try {

            /*
             The script that will change the previously submitted state to the current state.

             The format for the script is a series of delete instructions followed by a space and a
             series of insertion instructions.

             A deletion instruction follows this fomat: (indexInResult,numberOfDeletionsAfterIndex|)
             An insertion instruction follows this format: (indexInResult,lengthOfInsertion,insertion)

             For example: to change "Hello world!" to "Hi world, I am a computer.":
                "11,1|1,4| 1,1,i8,18,, I am a computer."
              */
        	Long assignmentID = params.get("assignmentId").asLong(0L); 
            Problem problem = Ebean.find(Problem.class, params.get("problemId").asLong(0L));
        	String userID = params.get("userId").textValue();
        	String stateEditScript = params.get("stateEditScript").textValue();

            Submission submission = new Submission();

            submission.setAssignmentId(assignmentID);
            submission.setStudentId(userID);
            submission.setContent(stateEditScript);
            submission.setPrevious(params.get("previous").longValue());
            submission.setProblem(problem);
            submission.setCorrect(params.get("correct").asLong(0L));
            submission.setMaxScore(params.get("maxscore").asLong(0L));

            submission.save();
            problem.getSubmissions().add(submission);
            long endTime = getEndTime(problem, userID);            
            List<Submission> submissions = Ebean.find(Submission.class)
        		.select("correct, maxscore, submittedAt")
        		.where()
        		.eq("problem.problemId", problem.getProblemId())
        		.eq("studentId", userID)
        		.findList();
            double highestScore = 0;
            for (Submission s : submissions) {
            	long submittedAt = s.getSubmittedAt().getTime();
            	if (submittedAt < endTime && s.maxscore > 0) highestScore = Math.max(highestScore,  s.correct * 1.0 / s.maxscore);
            }
            
            long now = System.currentTimeMillis();
            long timeRemaining = endTime == Long.MAX_VALUE ? -1 : now < endTime ? endTime - now : 0;  

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("submissionId", submission.getSubmissionId());
            responseMap.put("submittedAt", submission.getSubmittedAt());
            responseMap.put("highestScore", highestScore);
            responseMap.put("timeRemaining", timeRemaining);

            Logger.info("Saved submission: " + responseMap.toString());
            return ok(Json.toJson(responseMap));
        } catch (Exception ex) {
            Logger.info(Util.getStackTrace(ex));
            return badRequest("Received problem content: " + Json.stringify(params) + "\n" +
                    "Exception message: " + ex.getMessage());
        }
    }
        
    public static long getEndTime(Problem p, String studentId) {
    	Long assignmentDuration = p.getAssignment().getDuration();    
    	long assignmentEndTime = Long.MAX_VALUE;
    	long problemEndTime = Long.MAX_VALUE;
    	if (assignmentDuration != null & assignmentDuration > 0) {
    		String query = "select min(submitted_at) as starttime from submission where student_id = :sid and assignment_id = :aid";
        	Date assignmentStartDate = Ebean.createSqlQuery(query)
        			.setParameter("aid", p.getAssignment().getAssignmentId())
        			.setParameter("sid", studentId)
        			.findUnique()
        			.getDate("starttime");
    		assignmentEndTime = (assignmentStartDate == null ? System.currentTimeMillis() : assignmentStartDate.getTime())  
    				+ assignmentDuration * 60 * 1000;
    	}
    	if (p.getDuration() > 0) {
        	String query = "select min(submitted_at) as starttime from submission where student_id = :sid and problem_problem_id = :pid";
        	Date problemStartDate = Ebean.createSqlQuery(query)
        			.setParameter("pid", p.getProblemId())
        			.setParameter("sid", studentId)
        			.findUnique()
        			.getDate("starttime");
        	problemEndTime = (problemStartDate == null ? System.currentTimeMillis() : problemStartDate.getTime()) 
        			+ p.getDuration() * 60 * 1000;     		
    	}
    	return Math.min(assignmentEndTime, problemEndTime);
    }    
}


package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;

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
        	Long assignmentID = params.get("assignmentId").asLong(0L); //TODO: Why needed? 
        	String userID = params.get("userId").textValue();
        	String stateEditScript = params.get("stateEditScript").textValue();
            JsonNode previousIdNode = params.get("previousSubmissionId");
            String previousId = previousIdNode == null ? "" : Long.toString(previousIdNode.longValue());
            //TODO: Shouldn't be a string
            Problem problem = Ebean.find(Problem.class, params.get("problemId").asLong(0L));

            Submission submission = new Submission();

            submission.setAssignmentId(assignmentID);
            submission.setStudentId(userID);
            submission.setContent(stateEditScript);
            submission.setPrevious(previousId);
            submission.setProblem(problem);
            submission.setCorrect(params.get("correct").asLong(0L));
            submission.setMaxScore(params.get("maxscore").asLong(0L));

            submission.save();
            problem.getSubmissions().add(submission);
            
            List<Submission> submissions = Ebean.find(Submission.class)
        		.select("correct, maxscore")
        		.where()
        		.eq("problem.problemId", problem.getProblemId())
        		.eq("studentId", userID)
        		.findList();
            double highestScore = 0;
            for (Submission s : submissions) {
            	if (s.maxscore > 0) highestScore = Math.max(highestScore,  s.correct * 1.0 / s.maxscore);
            }

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("submissionId", submission.getSubmissionId());
            responseMap.put("submittedAt", submission.getSubmittedAt());
            responseMap.put("highestScore", highestScore);
            //TODO: shouldn't be a string
            String previousString = submission.getPrevious();
            Long previous = null;
            if (previousString != null) previous = Long.parseLong(previousString.trim());
            responseMap.put("previous", previous);

            Logger.info("Saved submission: " + responseMap.toString());
            return ok(Json.toJson(responseMap));
        } catch (Exception ex) {
            Logger.info(Util.getStackTrace(ex));
            return badRequest("Received problem content: " + Json.stringify(params) + "\n" +
                    "Exception message: " + ex.getMessage());
        }
    }
}


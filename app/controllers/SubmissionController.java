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
    public Result addSubmission(Long assignmentID, String userID) {
        Logger.info("SubmissionController.addSubmission AssignmentID: " + assignmentID + " UserID: " + userID);

        JsonNode problemContent = request().body().asJson();
        if (problemContent == null)
            return badRequest("Expected Json data. Received: " + request());

        Logger.info("params: " + Json.stringify(problemContent));

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
            String stateEditScript = problemContent.get("stateEditScript").textValue();
            JsonNode previousHashNode = problemContent.get("previousSubmissionId");
            String previousHash = previousHashNode == null ? "" : previousHashNode.textValue();

            Problem problem = Ebean.find(Problem.class, problemContent.get("problemId").asLong(-1L));
            JsonNode score = problemContent.get("score");

            Submission submission = new Submission();

            submission.setAssignmentId(assignmentID);
            submission.setStudentId(userID);
            submission.setContent(stateEditScript);
            submission.setPrevious(previousHash);
            submission.setProblem(problem);
            submission.setCorrect(score.get("correct").asLong(0L));
            submission.setMaxScore(score.get("maxscore").asLong(0L));

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
            responseMap.put("correct", highestScore);
            responseMap.put("maxscore", 1.0);
            responseMap.put("content", submission.getContent());
            responseMap.put("previous", submission.getPrevious());

            Logger.info("Saved submission: " + responseMap.toString());
            return ok(Json.toJson(responseMap));
        } catch (Exception ex) {
            Logger.info(Util.getStackTrace(ex));
            return badRequest("Received problem content: " + Json.stringify(problemContent) + "\n" +
                    "Exception message: " + ex.getMessage());
        }
    }
}


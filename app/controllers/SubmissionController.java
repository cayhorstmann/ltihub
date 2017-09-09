package controllers;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;

import models.Problem;
import models.Submission;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class SubmissionController extends Controller {

    // Method to save problem submission that is sent back from codecheck server
	// TODO: add params to JSON
    public Result addSubmission(Long assignmentID, String userID) {
        Logger.info("SubmissionController.addSubmission");

        JsonNode problemContent = request().body().asJson();
        if (problemContent == null)
            return badRequest("Expected Json data. Received: " + request());

        Logger.info("params: " + Json.stringify(problemContent));

        try {
            Logger.info("AssignmentID: " + assignmentID + " UserID: " + userID);

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
            String previousHash = problemContent.get("previousHash").textValue();

            Problem problem = Ebean.find(Problem.class, problemContent.get("problemId").asLong(-1L));
            Logger.info("Problem: " + problem.getProblemId());

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

            String response = "Submission " + submission.getSubmissionId() + " saved at " + submission.getSubmittedAt();
            Logger.info(response);

            return ok(response);
        } catch (Exception ex) {
            Logger.error("Submission failed.");
            Logger.error("Received problem content: " + Json.stringify(problemContent));
            ex.printStackTrace();
            return badRequest("Received problem content: " + Json.stringify(problemContent) + "\n" +
                    "Exception message: " + ex.getMessage());
        }
    }
}


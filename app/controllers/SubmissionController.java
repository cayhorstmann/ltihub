package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Problem;
import models.Submission;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class SubmissionController extends Controller {

    // Method to save problem submission that is sent back from codecheck server
    public Result addSubmission(Long assignmentID, String userID) {
        Logger.info("Result is received");

        JsonNode json = request().body().asJson();
        if (json == null)
            return badRequest("Expected Json data. Received: " + request());

        Logger.info("Received file: " + Json.stringify(json));

        try {
            Logger.info("addSubmission. AssignmentID: " + assignmentID + " UserID: " + userID);

            /*
             The script that will change the previously submitted state to the current state.

             The format for the script is a series of delete instructions followed by a space and a
             series of insertion instructions.

             A deletion instruction follows this fomat: (indexInResult,numberOfDeletionsAfterIndex|)
             An insertion instruction follows this format: (indexInResult,lengthOfInsertion,insertion)

             For example: to change "Hello world!" to "Hi world, I am a computer.":
                "11,1|1,4| 1,1,i8,18,, I am a computer."
              */
            String stateEditScript = Json.stringify(json.get("stateEditScript"));

            Problem problem = Problem.find.byId(json.get("problemId").asLong(-1L));
            Logger.info("Problem: " + problem.getProblemId());

            JsonNode score = json.get("score");
            Logger.info("Score: " + Json.stringify(score));


            Submission submission = new Submission();

            submission.setAssignmentId(assignmentID);
            submission.setStudentId(userID);
            submission.setContent(stateEditScript);

            if (score.get("correct") != null && score.get("maxscore") != null) {
                submission.setCorrect(score.get("correct").asLong(0L));
                submission.setMaxScore(score.get("maxscore").asLong(0L));
            } else {
                submission.setCorrect(0L);
                submission.setMaxScore(0L);
            }

            submission.setProblem(problem);

            submission.save();

            Logger.info("Submission " + submission.getSubmissionId() + " saved.");

            problem.getSubmissions().add(submission);

            return ok("Submission is saved");
        } catch (Exception ex) {
            Logger.error("Submission failed: " + ex.getMessage());
            return badRequest("Received request: " + request() + "\n" +
                    "Exception: " + ex.getMessage());
        }
    }

    //Method to save interactive Exercise score
//    public Result addSubmission(Long assignmentID) {
//        JsonNode jsonPayload = request().body().asJson();
//        Logger.info("json from client = {}", jsonPayload);
//
//        Logger.info("AssignmentID is: " + assignmentID);
//        Http.Cookie userIdCookie = request().cookie("user_id");
//        String userId = userIdCookie.value();
//        Logger.info("UserID is: " + userId);
//        Iterator<JsonNode> nodeIterator = jsonPayload.elements();
//
//        while (nodeIterator.hasNext()) {
//            JsonNode exercise = nodeIterator.next();
//            if (exercise.has("activity")) {
//                Problem problem = Problem.find.where().eq("assignment.assignmentId", assignmentID).like("url", "%" + exercise.get("activity").asText() + "%").findList().get(0);
//
//                Submission submission = new Submission();
//                submission.setAssignmentId(assignmentID);
//                submission.setProblem(problem);
//                submission.setStudentId(userId);
//                submission.setActivity(exercise.get("activity").asText());
//                submission.setCorrect(exercise.get("correct").asLong());
//                submission.setMaxScore(exercise.get("maxscore").asLong());
//                submission.save();
//            }
//        }
//        return ok();
//    }
}


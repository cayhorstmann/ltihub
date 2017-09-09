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
        Logger.info("SubmissionController.addSubmission");

        JsonNode problemContent = request().body().asJson();
        if (problemContent == null)
            return badRequest("Expected Json data. Received: " + request());

        Logger.info("Received file: " + Json.stringify(problemContent));

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
            String stateEditScript = Json.stringify(problemContent.get("stateEditScript"));
            Logger.info("stateEditScript " + stateEditScript);
            String previousHash = Json.stringify(problemContent.get("previousHash"));
            Logger.info("previousHash " + previousHash);

            Problem problem = Problem.find.byId(problemContent.get("problemId").asLong(-1L));
            Logger.info("Problem: " + problem.getProblemId());

            JsonNode score = problemContent.get("score");
            Logger.info("Score: " + Json.stringify(score));


            Submission submission = new Submission();

            submission.setAssignmentId(assignmentID);
            submission.setStudentId(userID);
            submission.setContent(stateEditScript);
            submission.setPrevious(previousHash);
            submission.setProblem(problem);

            if (score.get("correct") != null && score.get("maxscore") != null) {
                submission.setCorrect(score.get("correct").asLong(0L));
                submission.setMaxScore(score.get("maxscore").asLong(0L));
            } else {
                submission.setCorrect(0L);
                submission.setMaxScore(0L);
            }


            submission.save();

            Logger.info("Submission " + submission.getSubmissionId() + " saved.");

            problem.getSubmissions().add(submission);

            return ok("Submission is saved");
        } catch (Exception ex) {
            Logger.error("Submission failed.");
            Logger.error("Received problem content: " + Json.stringify(problemContent));
            ex.printStackTrace();
            return badRequest("Received problem content: " + Json.stringify(problemContent) + "\n" +
                    "Exception message: " + ex.getMessage());
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


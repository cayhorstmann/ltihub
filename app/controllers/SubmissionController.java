package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Problem;
import models.Submission;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Iterator;
import java.util.List;

public class SubmissionController extends Controller {

    // Method to save the codecheck submission that is sent back from codecheck server
    public Result addSubmissions(Long assignmentID) {
        Logger.info("Result is received");

        JsonNode json = request().body().asJson();
        if (json == null)
            return badRequest("Expecting Json data");

        Logger.info("Received file: " + Json.stringify(json));

        Logger.info("AssignmentID: " + assignmentID);

        Http.Cookie userIdCookie = request().cookie("user_id");
        String userID = userIdCookie.value();
        Logger.info("UserID: " + userID);

        Iterator<JsonNode> problemsContentsIter = json.elements();
        while (problemsContentsIter.hasNext()) {
            JsonNode problemContent = problemsContentsIter.next();
            Logger.info("Problem Content: " + Json.stringify(problemContent));

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
            Logger.info("Received state edit script is: " + stateEditScript);

            Problem problem = Problem.find.byId(problemContent.get("problemId").asLong(-1L));
            Logger.info("Problem: " + problem);

            JsonNode score = problemContent.get("score");
            Logger.info("Score: " + Json.stringify(score));


            Submission submission = new Submission();

            submission.setAssignmentId(assignmentID);
            submission.setStudentId(userID);
            submission.setContent(stateEditScript);
            submission.setCorrect(score.get("correct").asLong(0L));
            submission.setMaxScore(score.get("maxscore").asLong(0L));
            submission.setProblem(problem);

            submission.save();

            problem.getSubmissions().add(submission);
        }

        return ok("Submission is saved");
    }

    //Method to save interactive Exercise score
//    public Result addSubmissions(Long assignmentID) {
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


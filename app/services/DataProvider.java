package services;


import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;

import models.Assignment;
import models.Problem;
import models.Submission;
import play.libs.Json;
import play.mvc.Result;

import java.util.*;

import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

/**
 * Has methods meant to provide data to pages that request it
 */
public class DataProvider {

    /**
     * Provides the user ids of the users that have made submissions to any of the problems on the given assignment
     * @param assignmentId the id of the assignment to collect the user ids of
     * @return a stringified json object with the user ids of the users that have made submissions
     * to any of the problems on the given assignment
     */
    public Result getUserIdsForAssignment(Long assignmentId) {
        Assignment assignment = Ebean.find(Assignment.class, assignmentId);
        if (assignment == null)
            return badRequest("Assignment not found. ID Given: " + assignmentId);

        Set<String> userIds = new TreeSet<>(); // TODO: Make DB query
        for (Problem problem: assignment.getProblems()) {
            for (Submission submission: problem.getSubmissions()) {
                userIds.add(submission.getStudentId());
            }
        }

        JsonNode userIdsJSON = Json.toJson(userIds);

        return ok(Json.stringify(userIdsJSON));
    }

    /**
     * Provides contents from all of the submissions for a given problem id and user id
     * @param problemId the id of the problem to find the submissions for
     * @param studentId the id of the user to find the submissions for
     * @return a stringified json object with contents from all of the submissions for a given problem id and user id
     */
    public Result getSubmissionContent(Long problemId, String studentId) {
        List<Submission> submissions = Ebean.find(Submission.class)
        		.select("content")
        		.where()
        		.eq("problem.problemId", problemId)
        		.eq("studentId", studentId)
        		.orderBy("submissionId")
        		.findList();

        StringBuilder result = new StringBuilder("[");
        for (Submission submission: submissions) {
            String content = submission.getContent();
            if (content != null && content.length() != 0 && !"\"\"".equals(content)) {
            	if (result.length() > 1) result.append(",");            
            	result.append(content != null && content.startsWith("\"") ? content : Json.toJson(content));
            }
        }
        result.append("]");
        
        return ok(result.toString());
    }
}

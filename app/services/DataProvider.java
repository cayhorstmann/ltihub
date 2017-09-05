package services;


import com.fasterxml.jackson.databind.JsonNode;
import models.Assignment;
import models.Problem;
import models.Submission;
import play.Logger;
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
        Assignment assignment = Assignment.find.byId(assignmentId);
        if (assignment == null)
            return badRequest("Assignment not found. ID Given: " + assignmentId);

        Set<String> userIds = new TreeSet<>();
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
        Problem problem = Problem.find.byId(problemId);
        if (problem == null)
            return badRequest("Problem not found. ID Given: " + problemId);

        List<Submission> submissions = problem.getSubmissions();
        submissions.sort(
                (s1, s2) -> (s1.getSubmissionId().compareTo(s2.getSubmissionId()))
        );

        Collection<String> contents = new LinkedList<>();
        for (Submission submission: submissions) {
            if (studentId.equals(submission.getStudentId())) {
                contents.add(submission.getContent());
            }
        }

        return ok(contents.toString());
    }
}

package services;


import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import models.Assignment;
import models.Problem;
import models.Submission;
import play.libs.Json;
import play.mvc.Result;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Has methods meant to provide data to pages that request it
 */
public class DataProvider {

    /**
     * Provides the problems of the assignment with the given assignment ID
     * @param assignmentId the ID of the assignment to get the problems for
     * @return a JSON array containing the problems of the assignment with the given ID as JSON objects with the
     * properties:
     * <code>
     *     problemId, problemUrl
     * </code>
     */
    public Result getProblems(Long assignmentId) {
        Assignment assignment = Ebean.find(Assignment.class, assignmentId);
        if (assignment == null)
            return badRequest("Assignment not found. ID Given: " + assignmentId);

        List<JsonNode> problemsJsonList = new ArrayList<>();
        List<Problem> problems = new ArrayList<>(assignment.getProblems()); // TODO: Query with orderby
        problems.sort((p, q) -> Long.compare(p.getProblemId(), q.getProblemId()));
        for (Problem problem: problems) {
            Map<String, Object> problemValues = new HashMap<>();
            problemValues.put("problemId", problem.getProblemId());
            problemValues.put("problemUrl", problem.getProblemUrl());

            problemsJsonList.add(Json.toJson(problemValues));
        }

        return ok(Json.toJson(problemsJsonList));
    }

    /**
     * Provides the user ids of the users that have made submissions to any of the problems on the given assignment
     * @param assignmentId the id of the assignment to collect the user ids of
     * @return a JSON array containing the user ids of the users that have made submission to any of the problems on
     * the assignment with the given ID
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

        return ok(Json.toJson(userIds));
    }

    /**
     * Provides all of the submissions for a given problem id and user id
     * @param problemId the id of the problem to find the submissions for
     * @param studentId the id of the user to find the submissions for
     * @return a JSON array containing all of the submissions for the problem with the given problem ID as
     * JSON objects with the properties:
     * <code>
     *     submissionId, submittedAt, correct, maxscore, content, previous
     * </code>
     */
    public Result getSubmissions(Long problemId, String studentId) {
        List<Submission> submissions = Ebean.find(Submission.class)
                .select("submissionId, submittedAt, correct, maxscore, content, previous")
                .where()
                .eq("problem.problemId", problemId)
                .eq("studentId", studentId)
                .orderBy("submissionId")
                .findList();

        List<JsonNode> submissionsJsonList = new ArrayList<>();
        for (Submission submission: submissions) {
            Map<String, Object> submissionValues = new HashMap<>();
            submissionValues.put("submissionId", submission.getSubmissionId());
            submissionValues.put("submittedAt", submission.getSubmittedAt());
            submissionValues.put("correct", submission.getCorrect());
            submissionValues.put("maxscore", submission.getMaxScore());
            submissionValues.put("previous", submission.getPrevious());

            // Some values are stringifed strings, but the client is expecting just the strings themselves
            submissionValues.put("content",
                    submission.getContent().startsWith("\"") ?
                            Json.parse(submission.getContent()) : submission.getContent()
            );

            submissionsJsonList.add(Json.toJson(submissionValues));
        }

        return ok(Json.toJson(submissionsJsonList));
    }

    /**
     * Provides the time of the first submission to the assignment with the given assignment ID
     * by the student with the given student ID
     * @param assignmentId the ID of the assignment to get the first submission time of
     * @param studentId the ID of the student to find the first submission time for
     * @return the time of the first submission to the assignment with the given assignment ID
     * by the student with the given student ID
     */
    public Result getStartTimeInMilliseconds(Long assignmentId, String studentId) {
    	String query = "select min(submitted_at) as starttime from submission where student_id = :sid and assignment_id = :aid";
    	Date startTime = Ebean.createSqlQuery(query)
    			.setParameter("aid", assignmentId)
    			.setParameter("sid", studentId)
    			.findUnique()
    			.getDate("starttime");

        if (startTime == null) {
        	startTime = new Date();
            // TODO: Save start time? 
        }
        return ok(Json.toJson(startTime.getTime()));
    }
}

package controllers;

import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import models.Assignment;
import models.Problem;
import models.AssignmentWork;
import models.ProblemWork;
import models.Submission;
import models.Util;
import play.libs.Json;
import play.mvc.Result;
import io.ebean.Ebean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    public Result getProblems(Long assignmentId, String userId, String toolConsumerId, String contextId, String role) {
        Assignment assignment = Ebean.find(Assignment.class, assignmentId);
        if (assignment == null)
            return badRequest("No assignment with ID " + assignmentId);

        List<JsonNode> problemsJsonList = new ArrayList<>();
        List<Problem> problems = new ArrayList<>(); // TODO: Query with orderby
        if (Util.isInstructor(role)) 
        	problems.addAll(assignment.getProblems());
    	else {
    		// Is this the first attempt of this assignment?
    		List<AssignmentWork> queryResult = Ebean.find(AssignmentWork.class)
                    .where()
                    .eq("assignment.id", assignmentId)
                    .eq("studentId", userId)
                    .eq("toolConsumerId", toolConsumerId)
                    .eq("contextId", contextId)
                    .findList(); // TODO: Is there a find optional?
    		AssignmentWork work;
    		if (queryResult.size() == 0) {
    			work = new AssignmentWork();
    			work.assignment = assignment;
    			work.studentId = userId;
    			work.toolConsumerId = toolConsumerId;
    			work.contextId = contextId;
    			int ngroups = 0;
    			for (Problem p : assignment.getProblems())
    				ngroups = Math.max(ngroups, p.getProblemGroup());
    			ngroups++;
    			work.problemGroup = Math.abs(userId.hashCode()) % ngroups;
    			work.save();
    		} else {
    			work = queryResult.get(0);
    		}
    		    		
			for (Problem p : assignment.getProblems())
				if (p.getProblemGroup() == work.problemGroup) problems.add(p);
    	}
        problems.sort(Comparator.comparing(Problem::getProblemGroup).thenComparing(Problem::getId));
        for (Problem problem: problems) {
            Map<String, Object> problemValues = new HashMap<>();
            problemValues.put("id", problem.getId());
            problemValues.put("url", problem.getProblemUrl());
            problemValues.put("group", problem.getProblemGroup());
            problemValues.put("weight", problem.getWeight());            
            
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
    	//TODO: This needs to be users and tool consumer ID, or it needs to get the tool consumer id
        Assignment assignment = Ebean.find(Assignment.class, assignmentId);
        if (assignment == null)
            return badRequest("Assignment not found. ID Given: " + assignmentId);

        Set<String> userIds = new TreeSet<>();
        List<AssignmentWork> queryResult = Ebean.find(AssignmentWork.class)
                .where()
                .eq("assignment.id", assignmentId)
                .findList();      
        
        for (AssignmentWork work : queryResult) {
            userIds.add(work.studentId);
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
                .where()
                .eq("problem.id", problemId)
                .eq("studentId", studentId)
                .orderBy("submissionId")
                .findList();

        List<JsonNode> submissionsJsonList = new ArrayList<>();
        for (Submission submission: submissions) {
            Map<String, Object> submissionValues = new HashMap<>();
            submissionValues.put("submissionId", submission.id);
            submissionValues.put("submittedAt", submission.submittedAt);
            submissionValues.put("score", submission.score);
          	submissionValues.put("previous", submission.previous.id);

            // TODO: Some values are stringifed strings, but the client is expecting just the strings themselves
            submissionValues.put("content",
                    submission.script.startsWith("\"") ?
                            Json.parse(submission.script) : submission.script
            );

            submissionsJsonList.add(Json.toJson(submissionValues));
        }

        return ok(Json.toJson(submissionsJsonList));
    }

    public Result getWork(long problemId, String studentId, 
    		String toolConsumerId, String contextId) {
        Problem problem = Ebean.find(Problem.class, problemId);
        if (problem == null)
            return badRequest("No problem with ID " + problemId);

		List<ProblemWork> queryResult = Ebean.find(ProblemWork.class)
            .where()
                .eq("problem.id", problemId)
                .eq("studentId", studentId)
                .eq("toolConsumerId", toolConsumerId)
                .eq("contextId", contextId)
                .findList(); // TODO: Is there a find optional?
		ProblemWork work;
		if (queryResult.size() == 0) {
			work = new ProblemWork();
			work.problem = problem;
			work.studentId = studentId;
			work.toolConsumerId = toolConsumerId;
			work.contextId = contextId;
			work.save();
		} else {
			work = queryResult.get(0);
		}
        Map<String, Object> problemValues = new HashMap<>();
        problemValues.put("state", work.state);
        problemValues.put("score", work.highestScore);
        problemValues.put("submittedAt", work.lastDetail == null ? null : work.lastDetail.submittedAt);

        return ok(Json.toJson(problemValues));
    }
    
    /**
     * Provides the time of the first submission to the assignment with the given assignment ID
     * by the student with the given student ID
     */
    public Result getStartTimeInMilliseconds(Long assignmentId, String studentId, String toolConsumerId, String contextId) {
    	
    	// Find the AssignmentWork and return the start time
    	// If none, set start time to current server time--it'll be created soon
    	List<AssignmentWork> queryResult = Ebean.find(AssignmentWork.class)
                .where()
                .eq("assignment.id", assignmentId)
                .eq("studentId", studentId)
                .eq("toolConsumerId", toolConsumerId)
                .eq("contextId", contextId)
                .findList(); // TODO: Is there a find optional?
    	Date startTime;
    	if (queryResult.size() > 0) {
    		AssignmentWork work = queryResult.get(0);
    		startTime = work.startTime;
    	}
    	else 
    		startTime = new Date();
    		    	
    	Date now = new Date();
        ObjectNode result = Json.newObject();
        result.put("start", startTime.getTime());
        result.put("current", now.getTime());
        return ok(Json.toJson(result));
    }
}
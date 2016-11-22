package controllers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import play.*;
import play.data.*;
import play.libs.Json;
import static play.data.Form.*;
import java.net.*;
import java.io.*;

import play.Logger;
import play.mvc.*;
import models.*;

import views.html.*;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {
	 
    public Result index() {
        return ok("Your new application is ready");
    }
	
	public Result createAssignment() {
        return ok(create_exercise.render());
    }
	
	public Result addAssignment() {
        
	DynamicForm bindedForm = Form.form().bindFromRequest();
        String problemlist = bindedForm.get("url");
	Assignment assignment = new Assignment();
	assignment.save();
        System.out.println(problemlist);
	if(null != problemlist|| !problemlist.equals("")) {
		String [] problemArr = problemlist.split(","); 
		for(String problemstr : problemArr) {
			if(null != problemstr && !problemstr.equals("")) {
				Problem problem = new Problem();
				problem.setProblemUrl(problemstr);
				problem.setAssignment(assignment);
				assignment.getProblems().add(problem);
				problem.save();
			}	
		}
		assignment.save();
	}
	return redirect(routes.HomeController.showAssignment(assignment.getAssignmentId()));

	}
	
	public Result saveEditedAssignment(Long assignment) {
        
		DynamicForm bindedForm = Form.form().bindFromRequest();
        	String problemlist = bindedForm.get("url");
		Assignment assignment1 = Assignment.find.byId(assignment);
		//assignment.save();
        	System.out.println(problemlist);
		if(null != problemlist|| !problemlist.equals("")) {
			String [] problemArr = problemlist.split(","); 
			for(String problemstr : problemArr) {
			    if(null != problemstr && !problemstr.equals("")) {
				Problem problem = new Problem();
				problem.setProblemUrl(problemstr);
				problem.setAssignment(assignment1);
				assignment1.getProblems().add(problem);
				problem.save();
			   }	
			}
			assignment1.save();
		}
		List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
        System.out.println(problems);
        return ok(finalAssignment.render(problems));

}
    /**
     * GET method
     */
    public Result showAssignment(Long assignment) {
	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	Assignment assignment1 = Assignment.find.byId(assignment);
	System.out.println(assignment1.getAssignmentId());
        List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
        System.out.println(problems);
        return ok(showassignment.render(assignment1,problems));    
    }

    public Result showEditPage(Long assignment) {
	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
	Assignment assignment1 = Assignment.find.byId(assignment);
	System.out.println(assignment1.getAssignmentId());
        List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
        System.out.println(problems);
        return ok(editAssignment.render(assignment1, problems));    
    }
	
    public Result embedAssignment(Long assignment) {
	Map<String, String[]> postParams = request().body().asFormUrlEncoded();
       	Assignment assignment1 = Assignment.find.byId(assignment);
	System.out.println(assignment1.getAssignmentId());
        List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
        System.out.println(problems);
        return ok(finalAssignment.render(problems));    
    }
	
    public Result login(){
	return ok(loginPage.render());
    }
	
    public Result checkStudentValidity(){
	//System.out.println("Id is: " + Integer.parseInt(form().bindFromRequest().get("uId")));
	//System.out.println("Assignmentid is: " + form().bindFromRequest().get("aId"));

	if(((Integer.parseInt(form().bindFromRequest().get("uId")))== 1234567) &&((form().bindFromRequest().get("uname")).equals("test"))){
		Long ID = Long.parseLong(form().bindFromRequest().get("aId"));
		//System.out.println("Id is: " + ID);

		Assignment assignment1 = Assignment.find.byId(ID);
		if(assignment1 !=null)
			return redirect(routes.HomeController.embedAssignment(assignment1.getAssignmentId()));
		else 
			return ok("Assignment doesn't exist");			
	}
	return ok("Invalid values");
    }
}


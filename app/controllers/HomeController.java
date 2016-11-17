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

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
	 
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
	//return ok(Json.toJson(assignment.getProblems()));
	return redirect(routes.HomeController.showAssignment(assignment.getAssignmentId()));

   }
	
	/**
     * POST method
     */
    public Result setupExercises() throws UnsupportedEncodingException {
        Map<String, String[]> postParams = request().body().asFormUrlEncoded();

        if (postParams.get("lis_outcome_service_url") == null || postParams.get("lis_result_sourcedid") == null) {
            flash("warning", "");
        } else {
            response().setCookie(new Http.Cookie("lis_outcome_service_url", postParams.get("lis_outcome_service_url")[0],
                    null, null, null, false, false));
            response().setCookie(new Http.Cookie("lis_result_sourcedid", postParams.get("lis_result_sourcedid")[0],
                    null, null, null, false, false));
        }
       // String url = controllers.routes.HomeController.assignment().url();
                //+ "?urls=" + URLEncoder.encode(request().getQueryString("urls"), "UTF-8");
       // Logger.info(url);
        //return redirect(url);
		return ok("Done!");
    }

    /**
     * GET method
     */
    public Result showAssignment(Long assignment) {
	Assignment assignment1 = Assignment.find.byId(assignment);
	System.out.println(assignment1.getAssignmentId());
        List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignment1.assignmentId).findList();
        System.out.println(problems);
        return ok(showassignment.render(problems));
        
    }
}


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
import static play.data.Form.*;
import java.net.*;
import net.htmlparser.jericho.*;
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
	 return ok(index.render());
    }

    public Result addAssignment() {
                System.out.println(Form.form().bindFromRequest());
		DynamicForm bindedForm = Form.form().bindFromRequest();
                String problemlist = bindedForm.get("problems");
                System.out.println(problemlist);
		Assignment assignment = new Assignment();
		assignment.title = bindedForm.get("title");
		assignment.insert();
		if(null != problemlist) {
			Assignment.addProblems(assignment, problemlist);
		}
		return ok("Assignment created");

	}
}

package controllers;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class LtiConfigController extends Controller {

    public Result configuration() throws UnknownHostException {
        Http.Request request = request();

        String domain = controllers.routes.HomeController.index().absoluteURL(request, true);
        //String launchUrl = controllers.routes.HomeController.setupExercises().absoluteURL(request, true);
        String resourceSelectionUrl = controllers.routes.HomeController.createAssignment().absoluteURL(request, true);

        return ok(views.xml.ltiConfig
                .render()).as("application/xml");
    }
}

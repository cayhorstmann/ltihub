package controllers;

import play.Logger;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {

    @Override
    public String getUsername(Context ctx) {
    	// Logger.info("Secured.getUsername: " + ctx.session().get("user"));
        return ctx.session().get("user");
    }

    @Override
    public Result onUnauthorized(Context ctx) {
        return badRequest("Not logged in");
    }
}
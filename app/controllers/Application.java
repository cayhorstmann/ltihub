package controllers;

import java.time.Instant;

import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {
	public Result health() {
		return ok("LTIHub " + Instant.now());
	}
}
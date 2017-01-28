package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import play.Logger;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
//import play.utils.*;
import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.*;
import java.io.*;
import models.*;

public class GradeSubmitterController extends Controller {

	private final WSClient ws;

	@Inject
	public GradeSubmitterController(WSClient ws) {
		this.ws = ws;
	}

	public Result submitGradeToCanvas(Long assignmentID, Long userID) throws UnsupportedEncodingException {
        Http.Cookie outcomeServiceUrlCookie = request().cookie("lis_outcome_service_url");
        Http.Cookie sourcedIdCookie = request().cookie("lis_result_sourcedid");
      	if (outcomeServiceUrlCookie == null) {
            Logger.info("lis_outcome_service_url cookie not found.");
           return badRequest();
        }
        if (sourcedIdCookie == null) {
            Logger.info("lis_result_sourcedid cookie not found.");
            return badRequest();
       }
      
    String outcomeServiceUrl = outcomeServiceUrlCookie.value();
//    String sourcedId = sourcedIdCookie.value();
 String sourcedId = URLDecoder.decode(sourcedIdCookie.value(),"UTF-8");

	//String assignmentId = assignmentIdCookie.value();
//	Long assignmentID = Long.parseLong(assignmentId);
//	Long userId = Long.parseLong(userIdCookie.value());
        if (outcomeServiceUrl == null
                || outcomeServiceUrl.equals("")
                || sourcedId == null
                || sourcedId.equals("")) {
            return badRequest();
        }

        Logger.info("lis_outcome_service_url = {}", outcomeServiceUrl);
        Logger.info("lis_result_sourcedid = {}", sourcedId);
	
	int correct = 0;
	int maxscore = 0;
	double score = 0.0;

	// TODO: Find a way to weigh the problems. The instructor would
	// need to assign the weights because we don't know the weight of an unattempted
	// problem.
	List<Problem> problems = Problem.find.fetch("assignment").where().eq("assignment.assignmentId",assignmentID).findList();
    for (Problem problem: problems) {
	   List<Submission> submissions = Submission.find.where().eq("problem.problemId",problem.problemId).eq("canvasAssignmentId",assignmentID).eq("studentId",userID).findList();
	   int correctForThisProblem = 0;
	   double maxscoreForThisProblem = 0.0;
	   for (Submission s: submissions) {
		   double maxScore = (s.getMaxScore()).intValue();
		   if (maxScore > 0)
			   maxscoreForThisProblem = Math.max(maxscoreForThisProblem, 
					   (s.getCorrect()).intValue() / maxScore);        
	   }
	   score += maxscoreForThisProblem;
    }
    if (problems.size() > 0)
       score = score / problems.size();
	Logger.info("Score is: " + score);

        Logger.info(views.xml.scorepassback.render(sourcedId, score).toString());

        try {
            passbackGradeToCanvas(outcomeServiceUrl, views.xml.scorepassback.render(sourcedId, score).toString(),
                    "fred", "fred");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
        } catch (OAuthExpectationFailedException e) {
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
        }
        return ok("Grade saved in gradebook. Please check your grades.");
    }

	/**
	 * Pass back the grade to Canvas. If the <code>xml</code> is set up with
	 * fetch URL string, then also pass back the URL to the codecheck report.
	 * 
	 * @param gradePassbackURL
	 *            the grade passback URL from the LTI launch
	 * @param xml
	 *            the data to send off, with the sourcedId, score, and possibly
	 *            the fetchURL
	 * @param oauthKey
	 *            the oauth consumer key
	 * @param oauthSecret
	 *            the oauth secret key
	 */
	public static void passbackGradeToCanvas(String gradePassbackURL,
			String xml, String oauthKey, String oauthSecret)
			throws URISyntaxException, IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		// Create an oauth consumer in order to sign the grade that will be
		// sent.
		DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(oauthKey,
				oauthSecret);

		consumer.setTokenWithSecret("", "");

		// This is the URL that we will send the grade to so it can go back to
		// Canvas
		URL url = new URL(gradePassbackURL);

		// This is the part where we send the HTTP request
		HttpURLConnection request = (HttpURLConnection) url.openConnection();

		// Set http request to POST
		request.setRequestMethod("POST");
		request.setDoOutput(true);

		// Set the content type to accept xml
		request.setRequestProperty("Content-Type", "application/xml");

		// Set the content-length to be the length of the xml
		request.setRequestProperty("Content-Length",
				Integer.toString(xml.length()));

		// Sign the request per the oauth 1.0 spec
		consumer.sign(request); // Throws OAuthMessageSignerException,
								// OAuthExpectationFailedException,
								// OAuthCommunicationException
		xml = xml.replace("&quot;", "\"");
		Logger.info("XML is: {}", xml);
		// POST the xml to the grade passback url
		request.getOutputStream().write(xml.getBytes("UTF8"));

		// send the request
		request.connect();

		try {
			request.getInputStream();
		} catch (Exception e) {

			int responseCode = request.getResponseCode();
			Logger.info("GET Response Code : " + responseCode);

			InputStream in = request.getErrorStream();
			// String encoding = request.getContentEncoding();
			// encoding = encoding == null ? "UTF-8" : encoding;
			String body = org.apache.commons.io.IOUtils.toString(in);
			Logger.info(body);
		}/*
		 * StringBuilder response = new StringBuilder(); try (Scanner in = new
		 * Scanner(request.getInputStream())) { while (in.hasNextLine()) {
		 * response.append(in.nextLine()); response.append("\n"); }
		 * Logger.info("Response is:" + response); } catch (IOException e) {
		 * InputStream err = request.getErrorStream(); if (err == null) throw e;
		 * try (Scanner in = new Scanner(err)) { response.append(in.nextLine());
		 * response.append("\n"); } }
		 */
	}

}

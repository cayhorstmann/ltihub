package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.imsglobal.pox.IMSPOXRequest;

import models.Assignment;
import models.Submission;
import models.Util;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpParameters;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;

public class GradeSubmitterController extends Controller {
	public Result submitGradeToLMS() throws UnsupportedEncodingException {
        JsonNode params = request().body().asJson();
        if (params == null) {
        	String result = "Expected JSON data. Received: " + request();
        	Logger.info("GradeSubmitterController.submitGradeToLMS " + result);
            return badRequest(result);
        }
		Logger.info("GradeSubmitterController.submitGradeToLMS params: " + Json.stringify(params));

        long assignmentID = params.get("assignment").asLong();
        String userID = params.get("user").asText();
        String outcomeServiceUrl = params.get("lis_outcome_service_url").asText();
		String sourcedId = params.get("lis_result_sourcedid").asText();
		
        if (outcomeServiceUrl == null || outcomeServiceUrl.equals("")
                || sourcedId == null || sourcedId.equals("")) {
        	String result = "Missing lis_outcome_service_url or lis_result_sourcedid";
        	Logger.info(result);
            return badRequest(result);
        }
	
		double score = 0.0;
			
		// TODO: Allow the instructor to assign a weight for each problem
	    List<Submission> submissionsForAssignment = Ebean.find(Submission.class)
		   .select("correct, maxscore, submittedAt")
		   .fetch("problem", "problemId")
		   .where()
		   .eq("assignmentId", assignmentID)
		   .eq("studentId", userID)
		   .findList();
	    Logger.info("submissionsForAssignment=" + submissionsForAssignment);
	
	    Assignment assignment = Ebean.find(Assignment.class, assignmentID);
		long duration = assignment.duration != null && assignment.duration > 0 ? assignment.duration : 0L;
		Date endTime = null;
		if (duration > 0) {
			Date startTime = null;
			for (Submission s : submissionsForAssignment) 
				if (startTime == null || s.getSubmittedAt().compareTo(startTime) < 0)
					startTime = s.getSubmittedAt();
			endTime = Date.from(startTime.toInstant().plusSeconds(duration * 60 + 30)); // 30 second fudge
		}
		
	    Map<Long, Double> maxScores = new HashMap<Long, Double>();
		for (Submission s : submissionsForAssignment) {
			long problemId = s.getProblem().getProblemId();
			double maxScore = s.getMaxScore();
			if (maxScore > 0 && (endTime == null || s.getSubmittedAt().compareTo(endTime) <= 0))
				maxScores.put(problemId, Math.max(s.getCorrect() / maxScore,
						maxScores.getOrDefault(problemId, 0.0)));
		}
		for (double s : maxScores.values()) score += s;
		if (maxScores.size() > 0) score /= maxScores.size();
		
        try {
    		String xmlString1 = "<?xml version = \"1.0\" encoding = \"UTF-8\"?> <imsx_POXEnvelopeRequest xmlns = \"http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0\"> <imsx_POXHeader> <imsx_POXRequestHeaderInfo> <imsx_version>V1.0</imsx_version> <imsx_messageIdentifier>12341234</imsx_messageIdentifier> </imsx_POXRequestHeaderInfo> </imsx_POXHeader> <imsx_POXBody> <replaceResultRequest> <resultRecord> <sourcedGUID> <sourcedId>";
    		String xmlString2 = "</sourcedId> </sourcedGUID> <result> <resultScore> <language>en</language> <textString>";
    		String xmlString3 = "</textString> </resultScore> </result> </resultRecord> </replaceResultRequest> </imsx_POXBody> </imsx_POXEnvelopeRequest>";        	
    		String xmlString = // views.xml.scorepassback.render(sourcedId, score).toString()
    				xmlString1 + sourcedId + xmlString2 + score + xmlString3;        	
    		// xmlString = xml.replace("&quot;","\"");
    			
            passbackGradeToLMS(outcomeServiceUrl, xmlString, "fred", "fred"); // TODO
    		
    		// IMSPOXRequest.sendReplaceResult(outcomeServiceUrl, "fred", "fred", sourcedId, "" + score);

        } catch (Exception e) {
    		Logger.info("score: " + score);        
            Logger.info(Util.getStackTrace(e));
            return badRequest(e.getMessage());
        }
        String result = "Grade saved in gradebook. You achieved " + (int) Math.round(100 * score) + "% of the total score.";
        Logger.info(result);
        return ok(result);
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
	 * @throws NoSuchAlgorithmException 
	 */
	public static void passbackGradeToLMS(String gradePassbackURL,
			String xml, String oauthKey, String oauthSecret)
			throws URISyntaxException, IOException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, NoSuchAlgorithmException {
		// Create an oauth consumer in order to sign the grade that will be sent.
		DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(oauthKey, oauthSecret);

		consumer.setTokenWithSecret(null, null);

		// This is the URL that we will send the grade to so it can go back to
		// Canvas
		URL url = new URL(gradePassbackURL);

		// This is the part where we send the HTTP request
		HttpsURLConnection request = (HttpsURLConnection) url.openConnection();

		// Set http request to POST
		request.setRequestMethod("POST");

		// Set the content type to accept xml
		request.setRequestProperty("Content-Type", "application/xml");
		//request.setRequestProperty("Authorization", "OAuth"); // Needed for Moodle???
		
		// Set the content-length to be the length of the xml
		byte[] xmlBytes = xml.getBytes("UTF-8"); 
		request.setRequestProperty("Content-Length",
				Integer.toString(xmlBytes.length));
		// https://stackoverflow.com/questions/28204736/how-can-i-send-oauth-body-hash-using-signpost
		MessageDigest md = MessageDigest.getInstance("SHA1");
		String bodyHash = Base64.getEncoder().encodeToString(md.digest(xmlBytes));
		HttpParameters params = new HttpParameters();
        params.put("oauth_body_hash", URLEncoder.encode(bodyHash, "UTF-8"));
        //params.put("realm", gradePassbackURL); // http://zewaren.net/site/?q=node/123
        consumer.setAdditionalParameters(params);
        
		consumer.sign(request); // Throws OAuthMessageSignerException,
				// OAuthExpectationFailedException,
				// OAuthCommunicationException		
		// Logger.info("Request after signing: {}", consumer.getRequestParameters());
		// Logger.info("XML: {}", xml);


		// POST the xml to the grade passback url
		request.setDoOutput(true);
		OutputStream out = request.getOutputStream();
		out.write(xmlBytes);
		out.close();

		// request.connect();
		Logger.info(request.getResponseCode() + " " + request.getResponseMessage());
		try {
			InputStream in = request.getInputStream();
			String body = new String(Util.readAllBytes(in), "UTF-8");
			Logger.info("Response body received from LMS: " + body);
		} catch (Exception e) {			
			InputStream in = request.getErrorStream();
			String body = new String(Util.readAllBytes(in), "UTF-8");
			Logger.info("Response error received from LMS: " + body);
		}
	}

}

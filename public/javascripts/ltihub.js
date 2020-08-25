// TODO: Do something better with the nanosecond time API
var clientStampCounter = 0
function getClientStamp() {
	clientStampCounter++;
	return new Date().getTime() + clientStampCounter;
}

var problems = null;
// Load the iframes for the problems of the assignment
function loadProblems() {
	const url = assignment.submissionURL
	const xhr = new XMLHttpRequest()
	xhr.responseType = 'json'
	xhr.addEventListener('load', event => {
		const problems = xhr.response;
		const buttonDiv = document.createElement('div')
		buttonDiv.id = 'buttons'
		document.body.appendChild(buttonDiv)
		
		for (let i = 0; i < problems.length; i++) {
			// TODO: Maybe put everything into a div?
			// TODO: For instructors, show the group

			/*const highestScoreEl = document.createElement('div');
			highestScoreEl.id = "highestScore-" + problems[i].id;
			highestScoreEl.className = "highestScore message";

			document.body.appendChild(highestScoreEl); // TODO: ???
			*/
			const problemIframe = document.createElement('iframe');
			problemIframe.id = problems[i].id;
			problemIframe.className = 'exercise-iframe';
			problemIframe.src = problems[i].url;
			document.body.appendChild(problemIframe);

			problemIframe.addEventListener('load', function() {
				const message = { query: 'docHeight', id: problems[i].id };
				problemIframe.contentWindow.postMessage(message, '*');
				// CSH restoreStateOfProblem(problems[i].id);
			})
			problemIframe.style.display = i === 0 ? 'block' : 'none'
			const button = document.createElement('button')
			button.id = 'button-' + problems[i].id
			button.className = 'exercise-button'
			buttonDiv.appendChild(button)
			button.textContent = "" + (i + 1) // TODO: Add %age
			button.addEventListener('click', event => {
				for (const frame of document.getElementsByClassName('exercise-iframe'))
					if (frame !== problemIframe)
						frame.style.display = 'none'
				problemIframe.style.display = 'block'
				const message = { query: 'docHeight', id: problems[i].id };
				problemIframe.contentWindow.postMessage(message, '*');		
				for (const btn of document.getElementsByClassName('exercise-button'))
					if (btn !== button)
						btn.classList.remove('active')
				button.classList.add('active')	
			})
			if (i === 0) button.classList.add('active')	
			
		}
	})
	xhr.addEventListener('error', event => {
		console.log("Error getting problems from " + url);
	})
	xhr.open('GET', url)
	xhr.send()
}

// Response from messages to iframe
function receiveMessage(event) {
	if (event.data.request) { // It's a response
		console.log('received ' + JSON.stringify(event.data)); // TODO
		if (event.data.request.query === 'docHeight') {
			const newHeight = event.data.docHeight;
			document.getElementById(event.data.request.id).style.height = newHeight + 'px'
		}
		else if (event.data.request.query === 'getContent') {
			const problemId = event.data.request.problemId;
			const score = event.data.score;
			const state = event.data.state;
			sendScoreAndState(problemId, score, state);
		}
	} else { // It's a request
		if (event.data.query === 'retrieve') {
			for (const frame of document.getElementsByClassName('exercise-iframe')) {
				if (frame.contentWindow === event.source)
					restoreStateOfProblem(frame.id, event.data.request)
			}
		}
	}
}

window.addEventListener("message", receiveMessage, false);

window.onload = loadProblems;

// Score and state handlers

/*
 Periodically calls the function to request the updated states and scores of problems,
 that way if the score or the states changed, then we can report those changes to the server.
*/
var updateStatesInterval = setInterval(updateStatesFromProblems, 60 * 1000);

var problemIdToContent = {};

function restoreStateOfProblemId(problemId, request) {
	if (assignment.isInstructor) {
		const iframe = document.getElementById('' + problemId);
		const state = undefined
		if (request === undefined) {
			iframe.contentWindow.postMessage({ query: 'restoreState', state }, '*');
		} else {
			iframe.contentWindow.postMessage({ request, param: { state } }, '*');
		}
		return
	}

	const url = 'assignment.prefix/getWork/' + problemId + '/assignment.userId/assignment.toolConsumerId/assignment.contextId'
	const xhr = new XMLHttpRequest()
	xhr.responseType = 'json'

	xhr.addEventListener('load', event => {
		const result = xhr.response
		problemIdToContent[problemId] = {
			score: result.score,
			state: result.state
		}
		if (result.state) { // If the state was empty, then don't try to restore it
			var iframe = document.getElementById('' + problemId);
			const state = JSON.parse(result.state)
			if (request === undefined) {
				iframe.contentWindow.postMessage({ query: 'restoreState', state }, '*');
			} else {
				iframe.contentWindow.postMessage({ request, param: { state } }, '*');
			}
		}
		updateHighestScoreDisplay(problemId, result.submittedAt, result.score);
	})
	xhr.addEventListener('error', event => {
		console.log("Error getting problem contents from " + url);
		// Initializes the problem content so that it can be saved
		problemIdToContent[problemId] = {
			score: 0,
			state: "",
		}
	})
	xhr.open('GET', url)
	xhr.send()
}


// Tell the iframes to report their state back.
function updateStatesFromProblems() {
	for (const iframe of document.getElementsByClassName('exercise-iframe')) {
		iframe.contentWindow.postMessage({ query: 'getContent', problemId: iframe.id }, '*');
	}
}

// If the state or the score changed since the last submission or this is a timed problem, then send the new state and score to the server
function sendScoreAndState(problemId, score, state) {
	if (!state) return
	if (!problemIdToContent.hasOwnProperty(problemId)) // Haven't yet received first state
		return;
	// TODO: v2 won't have maxscore but just the normalized score
	const previousScore = problemIdToContent[problemId].score;
	const currentCorrect = score.errors ? Math.max(score.correct - score.errors, 0) : score.correct;
	const currentScore = score && score.correct && score.maxscore ?
		currentCorrect / score.maxscore : 0.0;

	const previousStateString = problemIdToContent[problemId].state;
	const currentStateString = state ? JSON.stringify(state) : "";

	if (currentScore !== previousScore || previousStateString !== currentStateString) {
		const data = {
			problemId: problemId,
			score: currentScore,
			state: currentStateString,
			assignmentId: assignment.assignmentId,
			userId: assignment.userId,
			toolConsumerId: assignment.toolConsumerId,
			contextId: assignment.contextId,
			clientStamp: getClientStamp()
		}

		const url = 'assignment.prefix/addSubmission'

		const xhr = new XMLHttpRequest()
		xhr.responseType = 'json'
		xhr.addEventListener('load', event => {
			const result = xhr.response
			problemIdToContent[problemId] = {
				score: currentScore,
				state: currentStateString,
			}

			updateHighestScoreDisplay(problemId, result.submittedAt, result.highestScore);
		})

		xhr.addEventListener('error', event => {
			console.log("Error saving work: " + JSON.stringify(data) + "\n" +
				"Problem ID: " + problemId + "\n" +
				"User ID: " + assignment.userId + "\n" +
				"Score: " + JSON.stringify(score) + "\n" +
				"Previous state: " + previousStateString + "\n" +
				"Current state: " + currentStateString + "\n");
		})
		xhr.open('POST', url)
		xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
		xhr.send(JSON.stringify(data))
	}
}

function formatTime(time) {
	if (time <= 0) return "0 seconds";
	else if (time >= 60 * 1000) return Math.round(time / (60 * 1000)) + " minutes";
	else return Math.round(time / 1000) + " seconds";
}

// Update the highest score element to display the score from the given submission
function updateHighestScoreDisplay(problemId, submittedAt, highestScore) {
	/*
	var highestScoreEl = document.getElementById('highestScore-' + problemId);
	highestScoreEl.innerHTML = (submittedAt ? "<p>Submission saved at: " + new Date(submittedAt) : "") +
		"</p><p>Highest recorded score: <b>" + (highestScore * 100.0).toFixed(2) + "%</b></p>"
		*/
    const button = document.getElementById('button-' + problemId);
    const text = button.textContent
    const index = text.indexOf(' (')
	if (index < 0) index = text.length
	button.textContent = text.substring(0, index) + ' (' + (highestScore * 100.0).toFixed(2) + ')' 
    button.title = 'Submission saved at: ' + new Date(submittedAt)
}


// Save work and report score
function submitGrades() {
	updateStatesFromProblems();
	setTimeout(function() {
		document.getElementById("response").style.display = "none";
		const data = {
			assignmentId: assignment.assignmentId,
			userId: assignment.userId,
			toolConsumerId: assignment.toolConsumerId,
			contextId: assignment.contextId,
			lisOutcomeServiceUrl: assignment.lisOutcomeServiceURL,
			lisResultSourcedId: assignment.lisResultSourcedId,
			oauthConsumerKey: assignment.oauthConsumerKey
		};
		const url = assignment.prefix / sendScore
		const xhr = new XMLHttpRequest()
		xhr.addEventListener('load', event => {
			const response = document.getElementById("response");
			response.innerHTML = xhr.responseText;
			response.style.display = "block";
		})
		xhr.addEventListener('error', event => {
			console.log("Error submitting grades: " + JSON.stringify(data) + "\n" +
				"Assignment ID: " + assignment.assignmentId + "\nUser ID: " + assignment.userId);
		})
		xhr.open('POST', url)
		xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8');
		xhr.send(JSON.stringify(data))
	}, 1000)
}


// Timed assignment specific code 
const durationInMilliseconds = assignment.duration * 60 * 1000;
var endTimeInMilliseconds;
var timeLeftInterval;
var alertedTimeIsUp = false;

function initializeTimer() {
	var timerEl = document.createElement('div');
	timerEl.id = 'timer';
	timerEl.className = 'rightSidebar';
	document.body.appendChild(timerEl);
	var currentTimeOnClient = new Date().getTime();

	const url = assignment.startTimeURL
	const xhr = new XMLHttpRequest()
	xhr.responseType = 'json'

	xhr.addEventListener('load', event => {
		const result = xhr.response
		var timeSpent = result.current - result.start
		var timeLeft = durationInMilliseconds - timeSpent
		endTimeInMilliseconds = currentTimeOnClient + timeLeft;
		updateTimeLeftDisplay();
		timeLeftInterval = setInterval(updateTimeLeftDisplay, 1000);
	})
	xhr.addEventListener('error', event => {
		console.log("Error getting start time from " + url);
	})
	xhr.open('GET', url)
	xhr.send()
}

function updateTimeLeftDisplay() {
	var timerEl = document.getElementById('timer');
	var timeLeftObj = new Date();
	timeLeftObj.setTime(endTimeInMilliseconds - timeLeftObj.getTime());

	// Time is up, submit assignment
	if (timeLeftObj.getTime() <= 0) {
		if (!alertedTimeIsUp) {
			alertedTimeIsUp = true;
			submitGrades();
			deleteProblems();
			if (timerEl)
				timerEl.remove();

			if (timeLeftInterval)
				clearInterval(timeLeftInterval);
			alert("You used up your time, your work has been saved, and your assignment has been graded");
		}
	} else {
		/*
		All hours and days will be put into minutes
		i.e. two hours and 30 minutes will turn into 150 minutes
		If seconds is less than 10, then we add a 0 in front
		 */
		const timeLeftMin = Math.floor((timeLeftObj.getTime() / 1000) / 60);
		const timeLeftSec = ((timeLeftObj.getSeconds() < 10) ? "0" : "") + timeLeftObj.getSeconds();

		timerEl.innerHTML = "Time Left: " + timeLeftMin + ":" + timeLeftSec;
	}
}

// Delete the iframes of the problems in the assignment
function deleteProblems() {
	var highestScoreEls = [...document.getElementsByClassName('highestScore')]
	var problemIframes = [...document.getElementsByClassName('exercise-iframe')]

	for (var i = 0; i < highestScoreEls.length; i++)
		highestScoreEls[i].remove()

	for (var i = 0; i < problemIframes.length; i++)
		problemIframes[i].remove()

	clearInterval(updateStatesInterval);
}

if (assignment.duration > 0)
	document.addEventListener('DOMContentLoaded', initializeTimer);




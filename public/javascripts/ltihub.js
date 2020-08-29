// TODO: Do something better with the nanosecond time API
var clientStampCounter = 0
function getClientStamp() {
	clientStampCounter++;
	return new Date().getTime() + clientStampCounter;
}

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
			const iframe = document.createElement('iframe');
			iframe.id = problems[i].id;
			iframe.className = 'exercise-iframe';
			iframe.src = problems[i].url;
			document.body.appendChild(iframe);
			iframe.style.display = i === 0 ? 'block' : 'none'
			const button = document.createElement('button')
			button.id = 'button-' + problems[i].id
			button.className = 'exercise-button'
			buttonDiv.appendChild(button)
			button.textContent = "" + (i + 1) // TODO: Add %age
			button.addEventListener('click', event => {
				for (const f of document.getElementsByClassName('exercise-iframe'))
					if (f !== iframe) f.style.display = 'none'
				iframe.style.display = 'block'
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

function sendingIframe(event) {
	for (const f of document.getElementsByClassName('exercise-iframe'))
    if (f.contentWindow === event.source) return f
  return undefined
}
	
// Response from messages to iframe
function receiveMessage(event) {
	console.log('received ', { event }); // TODO
	if (event.data.request) { // Receiving a response to our request, but we make no requests any more
    return 
	} else { // Receiving request from iframe
    let iframe = sendingIframe(event)    
		if (event.data.query === 'docHeight') {
			const newHeight = event.data.param.docHeight;
			if (newHeight > 50) // TODO: Eliminate fudge in codecheck
				iframe.style.height = newHeight + 'px'
		}
		else if (event.data.query === 'retrieve') {
			restoreStateOfProblem(iframe.id, event.data)			
		}
		else if (event.data.query === 'send') 
		  sendScoreAndState(iframe.id, event.data.param.score, event.data.param.state, event.data)
	}
}

// TODO: style
window.addEventListener("message", receiveMessage, false);

window.onload = loadProblems;

// Score and state handlers

function restoreStateOfProblem(problemId, request) {
  const iframe = document.getElementById('' + problemId);

	if (assignment.isInstructor) {
		iframe.contentWindow.postMessage({ request }, '*'); // TODO: param undefined, or should there be param.state???
		return
	}

  // TODO: For consistency, let Play make URL
	const url = assignment.prefix + '/getWork/' + problemId + '/' + assignment.userId + '/' + assignment.toolConsumerId + '/' + assignment.contextId
	const xhr = new XMLHttpRequest()
	xhr.responseType = 'json'

	xhr.addEventListener('load', event => {
		const result = xhr.response
		// TODO: This should come as JSON from the server
		const state = typeof result.state === 'string' || result.state instanceof String ? JSON.parse(result.state) : result.state 
		iframe.contentWindow.postMessage({ request, param: state }, '*');
		updateScoreDisplay(problemId, result.score, result.submittedAt);
	})
	xhr.addEventListener('error', event => {
		console.log("Error getting problem contents from " + url);
		// Initializes the problem content so that it can be saved
	})
	xhr.open('GET', url)
	xhr.send()
}

function sendScoreAndState(problemId, score, state, request) {
	if (!state) { // TODO: Needed???
		iframe.contentWindow.postMessage({ request }, '*');
    return
	}
  const iframe = document.getElementById('' + problemId);
	const data = {
		problemId,
		score,
		state,
		assignmentId: assignment.assignmentId,
		userId: assignment.userId,
		toolConsumerId: assignment.toolConsumerId,
		contextId: assignment.contextId,
		clientStamp: getClientStamp()
	}
  // TODO: For consistency, let Play make URL
	const url = assignment.prefix + '/addSubmission'

	const xhr = new XMLHttpRequest()
	xhr.responseType = 'json'
	xhr.addEventListener('load', event => {
		const result = xhr.response
    iframe.contentWindow.postMessage({ request, param: result }, '*');
    // TODO: We don't want the highest score anymore
		updateScoreDisplay(problemId, result.highestScore, result.submittedAt); 		
	})

	xhr.addEventListener('error', event => {
		const error = "Error saving work: " + JSON.stringify(data) + "\n" +
      "Problem ID: " + problemId + "\n" +
      "User ID: " + assignment.userId + "\n" +
      "Score: " + JSON.stringify(score) + "\n" +
      "Previous state: " + previousStateString + "\n" +
      "Current state: " + currentStateString + "\n"
    iframe.contentWindow.postMessage({ request, error }, '*');
		console.log(error);
	})
	xhr.open('POST', url)
	xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
	xhr.send(JSON.stringify(data))
}

function formatTime(time) {
	if (time <= 0) return "0 seconds";
	else if (time >= 60 * 1000) return Math.round(time / (60 * 1000)) + " minutes";
	else return Math.round(time / 1000) + " seconds";
}

function updateScoreDisplay(problemId, score, submittedAt) {
  const button = document.getElementById('button-' + problemId);
  const text = button.textContent
  let index = text.indexOf(' (')
	if (index < 0) index = text.length
	button.textContent = text.substring(0, index) + ' (' + (Math.round(score * 100.0)).toFixed(0) + '%)'
	if (submittedAt !== undefined) 
    button.title = 'Submission saved at: ' + new Date(submittedAt)
}


// Report score
function submitGrades() {
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
	// TODO: For consistency, let Play make URL
	const url = assignment.prefix + '/sendScore'
	const xhr = new XMLHttpRequest()
	xhr.addEventListener('load', event => {
		const result = xhr.response    
		const responseElement = document.getElementById("response")		
		responseElement.innerHTML = "Score saved in gradebook. You achieved " + Math.round(100 * result.score) + "% of the total score."
		responseElement.style.display = "block"
	})
	xhr.addEventListener('error', event => {
		console.log("Error submitting grade: " + JSON.stringify(data) + "\n" +
			"Assignment ID: " + assignment.assignmentId + "\nUser ID: " + assignment.userId);
	})
	xhr.responseType = 'json'
	xhr.open('POST', url)
	xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8');
	xhr.send(JSON.stringify(data))
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
// TODO: Do we really want to do that? 
function deleteProblems() {
	// TODO: Buttons???
	//var highestScoreEls = [...document.getElementsByClassName('highestScore')]
	var iframes = [...document.getElementsByClassName('exercise-iframe')]

	//for (var i = 0; i < highestScoreEls.length; i++)
	//	highestScoreEls[i].remove()

	for (var i = 0; i < iframes.length; i++)
		iframes[i].remove()

	clearInterval(updateStatesInterval);
}

if (assignment.duration > 0)
	document.addEventListener('DOMContentLoaded', initializeTimer);




// Included with LTIHub problem files constructed by makeproblems
// https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage

// TODO: Put under version control

/*

  Change to scheme where EPUB.Education.send and EPUB.Education.retrieve send messages to parent.

  Let's do EPUB.Education.retrieve first. That should produce the exact same as the current restoreState message as the response, together with a nonce to find the callback.

  Using the old API for now, not

  https://w3c.github.io/publ-cg/education/epub-education.html#javascript-apis

  Need to keep the legacy protocol alive until current LTIHub replaced

  Protocol:

  data
  query      (request from parent to child) 'docHeight', 'getContent', 'restoreState' (LTIHub v1)                                 
  (request from child to parent) 'docHeight', 'send', 'retrieve' (LTIHub v2)
  
  id         (docHeight request from child to parent) the id of the element requesting which height it should have
  (docHeight request from parent to child) the id of the element that is queried for its actual height

  version    (docHeight request from parent to child, to set up
  EPUB in version 2)

  state      (restoreState request from parent to child)
  state      (getContent response from child to parent) 
  
  nonce      (request from child to parent) a nonce to be returned with
  the response, for non-void requests ('retrieve')

  request    (response) the request

  docHeight  (docHeight request, response from child to parent) 

  problemId  (getContent request from parent to child) the id of the element that is queried for its score and state

  score      (getContent response from child to parent) 

  param      (request or response) parameter object, currently only used for 'retrieve'

  horstmann_vitalsource (which is part of horstmann_all_min) defines

  horstmann_config.score_change_listener 
  horstmann_config.retrieve_state

  This should be moved to horstmann_common if not VS specific

  -> EPUB.Education.retrieve 
  -> postMessage retrieve
  -> message retrieve
  -> callback from nonceMap
  


*/


const nonceMap = {} // TODO namespace

// https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript

function generateUUID() { // Public Domain/MIT
  var d = new Date().getTime() 
  var d2 = (performance && performance.now && (performance.now() * 1000)) || 0 // Time in microseconds since page-load or 0 if unsupported
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    let r = Math.random() * 16 // random number between 0 and 16
    if(d > 0) { // Use timestamp until depleted
      r = (d + r) % 16 | 0
      d = Math.floor(d / 16)
    } else { // Use microseconds since page-load if supported
      r = (d2 + r) % 16 | 0
      d2 = Math.floor(d2/16)
    }
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
  })
}

function setupEPUB(version, restoredState) {
  if (!('EPUB' in window))
    window.EPUB = {}
  if (!('Education' in window.EPUB)) {
    window.EPUB.Education = version === 1
      ? {
        send: () => {},
        retrieve: (request, callback) => {
          callback({ data: [ { data: restoredState } ] })
          const docHeight = document.body.children[0].scrollHeight
          document.body.style.height = docHeight + 'px'
        }
      }
    : {
      send: () => {}, // TODO
      retrieve: (request, callback) => {          
        // Make nonce and register callback
        const nonce = generateUUID()
        nonceMap[nonce] = callback
        // Pass request and nonce to parent
        const data = { query: 'retrieve', param: request, nonce }
        console.log('Posting to parent', data)
        window.parent.postMessage(data, '*' )
        // TODO: What when no response on retrieve message?
        // Invoke nonceMap with null after timeout?
      }
    }
  }
}

//if (window.parent !== window.top)
{ // iframe

  const interactiveElements = [...document.querySelectorAll('div, ol')].
        filter(e => {
          const ty = e.tagName
          const cl = e.getAttribute('class')
          return cl && (ty === 'div' && cl.indexOf('horstmann_') == 0 || ty === 'ol' && (cl.indexOf('multiple-choice') == 0 || cl.indexOf('horstmann_ma') == 0))
          })    
  const element = interactiveElements[0]

  
  window.addEventListener('load', event => {  
    const resizeObserver = new ResizeObserver(entries => {
      const docHeight = document.body.children[0].scrollHeight
      document.body.style.height = docHeight + 'px'
      const data = { query: 'docHeight', param: { docHeight } }
      console.log('Posting to parent', data)
      window.parent.postMessage(data, '*' )
      /*
      for (const entry of entries) {
        const docHeight = entry.contentRect.height
        const data = { query: 'docHeight', param: { docHeight } }
        console.log('Posting to parent', data)
        window.parent.postMessage(data, '*' )
      }
*/
    })

    resizeObserver.observe(document.body.children[0])  
  })

  window.addEventListener("message", event => {
    
    const origin = event.origin || event.originalEvent.origin;
    // For Chrome, the origin property is in the event.originalEvent object.
    // TODO: Filter origin?

    console.log('Received from parent', event.data)
    if ('request' in event.data) { // It's a response
      const request = event.data.request    
      if (request.query === 'retrieve') {
        const problemId = request.param.filters[0].activityIds[0]
        // TODO Old VitalSource API
        nonceMap[request.nonce](event.data.response)
        delete nonceMap[request.nonce]
      }
      // Handle other responses  
    } else { // It's a request   
      let response = {}

      if (event.data.query === 'docHeight') {
        if (event.data.version) setupEPUB(event.data.version) // LTIHub v2
        // TODO--Following codecheck.js for now
        const body = document.body
        const html = document.documentElement;
        const fudge = 50;
        const docHeight = document.body.children[0].scrollHeight
        document.body.style.height = docHeight + 'px'
        // TODO document.body.style.overflow = 'hidden' 
        response = { docHeight }
      }
      else {
        if (event.data.query === 'getContent') { // LTIHub v1
          const docHeight = document.body.children[0].scrollHeight
          document.body.style.height = docHeight + 'px'
          const id = element.closest('li').id
          const score = { correct: Math.min(element.correct, element.maxscore), errors: element.errors, maxscore: element.maxscore, activity: id }
          response = { score: score, state: element.state } 
        }
        if (event.data.query === 'restoreState') { // LTIHub v1        
          setupEPUB(1, event.data.state)
          // TODO: e.restoreState is ancient legacy, remove from horstmann_common once this is done
          // if (element.restoreState) element.restoreState(event.data.state)
        }
      }
      response.request = event.data
      console.log('Posting to parent', response)
      event.source.postMessage(response, '*' )
    }
  }, false)
}

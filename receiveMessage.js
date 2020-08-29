/*

  Protocol:

data.
  query      (request from parent to child) 'docHeight', 'getContent', 'restoreState' (LTIHub v1)                                 
             (request from child to parent) 'docHeight', 'send', 'retrieve' (LTIHub v2)
  request    (response) the request

  
  state      (restoreState request from parent to child)
  state      (getContent response from child to parent) 
  
  nonce      (request from child to parent) a nonce to be returned with
  the response, for non-void requests ('retrieve')

  docHeight  (docHeight request, response from child to parent) 

  score      (getContent response from child to parent) 

  param      (request or response) parameter object, currently only used for 'retrieve'

*/

if (window.parent !== window.top) 
{ // iframe

  if (!('EPUB' in window))
    window.EPUB = {}
  if (!('Education' in window.EPUB)) {
    window.EPUB.Education = {
      nonceMap: {},
      retrieveCallback: undefined, // LTIHub v1
      retrieve: (request, callback) => {
        window.EPUB.Education.retrieveCallback = callback // LTIHub v1
        // Make nonce and register callback
        const nonce = generateUUID()
        window.EPUB.Education.nonceMap[nonce] = callback
        // Pass request and nonce to parent
        // TODO: Change from VitalSource format
        const qid = request.filters[0].activityIds[0]
        const param = { qid }
        const data = { query: 'retrieve', param, nonce }
        console.log('Posting to parent', data)
        window.parent.postMessage(data, '*' )
        // TODO: What when no response on retrieve message?
        // Invoke nonceMap with null after timeout?
        // If so, ensure that retrieveCallback doesn't happen twice
      },
      send: (request, callback) => {
        const nonce = generateUUID()
        window.EPUB.Education.nonceMap[nonce] = callback
        // TODO: Change from VitalSource format
        const param = { state: request.data[0].state.data, score: request.data[0].results[0].score }
        const data = { query: 'send', param, nonce }
        console.log('Posting to parent', data)
        window.parent.postMessage(data, '*' )
      },
    }
  }

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
  
  let element = undefined
  
  window.addEventListener('load', event => {
    const interactiveElements = [...document.querySelectorAll('div, ol')].
          filter(e => {
            const ty = e.tagName
            const cl = e.getAttribute('class')
            return cl && (ty === 'div' && cl.indexOf('horstmann_') == 0 || ty === 'ol' && (cl.indexOf('multiple-choice') == 0 || cl.indexOf('horstmann_ma') == 0))
          })    
    element = interactiveElements[0]
    
    document.body.style.height = '100%'
    document.body.style.overflow = 'hidden' 
    const resizeObserver = new ResizeObserver(entries => {
      if (window.EPUB.Education.version !== 1) { // TODO
        const docHeight = document.documentElement.scrollHeight 
        const data = { query: 'docHeight', param: { docHeight } }
        console.log('Posting to parent', data)
        window.parent.postMessage(data, '*' )
      }
    })
    /* 
       Weirdly, when listening to document.body or 
       document.documentElement, the document height keeps
       getting increased
    */    
    resizeObserver.observe(document.body.children[0])
  })

  window.addEventListener("message", event => {    
    console.log('Received from parent', event.data)
    if ('request' in event.data) { // It's a response
      const request = event.data.request    
      if (request.query === 'retrieve') { // LTIHub v2        
        const state = event.data.param
        // TODO Old VitalSource API
        // let state = response.data[0].data
        const arg = { data: [ { data: state } ] }
        window.EPUB.Education.nonceMap[request.nonce](arg)
        delete window.EPUB.Education[request.nonce]
      }
      // Handle other responses  
    } else { // It's a request   
      if (event.data.query === 'docHeight') { // LTIHub v1
        const body = document.body
        const html = document.documentElement;
        const docHeight = document.body.children[0].scrollHeight
        document.body.style.height = docHeight + 'px'
        document.body.style.overflow = 'hidden' 
        let response = { request: event.data, docHeight }
        console.log('Posting to parent', response)
        event.source.postMessage(response, '*' )
        window.EPUB.Education.send = () => {}
        window.EPUB.Education.version = 1
        if (window.EPUB.Education.retrieveCallback !== undefined) {
          // It is possible that the parent never sends restoreState
          setTimeout(() => {
            if (window.EPUB.Education.retrieveCallback !== undefined) {
              window.EPUB.Education.retrieve = (request, callback) => {
                callback({ data: [ { data: null } ] })
                const docHeight = document.body.children[0].scrollHeight
                document.body.style.height = docHeight + 'px'
              }
              window.EPUB.Education.retrieve(null, window.EPUB.Education.retrieveCallback)
              delete window.EPUB.Education.retrieveCallback
            }
          }, 5000)
        }
      }
      else if (event.data.query === 'getContent') { // LTIHub v1
        const docHeight = document.body.children[0].scrollHeight
        document.body.style.height = docHeight + 'px'
        const id = element.closest('li').id
        const score = { correct: Math.min(element.correct, element.maxscore), errors: element.errors, maxscore: element.maxscore, activity: id }
        let response = { request: event.data, score: score, state: element.state }
        console.log('Posting to parent', response)
        event.source.postMessage(response, '*' )          
      } else if (event.data.query === 'restoreState') { // LTIHub v1
        const restoredState = event.data.state        
        window.EPUB.Education.retrieve = (request, callback) => {
          callback({ data: [ { data: restoredState } ] })
          const docHeight = document.body.children[0].scrollHeight
          document.body.style.height = docHeight + 'px'
        }                  
        /*
          It is possible that the element already made a
          retrieve request (which goes unanswered by the parent). 
        */
        if (window.EPUB.Education.retrieveCallback !== undefined) {
          window.EPUB.Education.retrieve(undefined, window.EPUB.Education.retrieveCallback)
          delete window.EPUB.Education.retrieveCallback
        }        
      }
    }
  }, false)
}

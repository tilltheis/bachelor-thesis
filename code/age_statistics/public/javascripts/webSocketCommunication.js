var globalWebSocket;

function useWebSockets() {
  var form = document.forms[0];
  var input = document.getElementById("ageInput");

  // prevent form submission
  form.onsubmit = function() { return false; };

  // webSocketUrl is defined in index.scala.html
  var ws = new WebSocket(webSocketUrl);

  ws.onopen = function() {
    form.onsubmit = function() {
      ws.send(input.value)
      form.reset();
      return false; // prevent submission
    }
  };

  // don't alter form.onsubmit here because it can be called after dontUseWebSockets()
  // ws.onclose = function() {
  //   form.onsubmit = function() { return false; };
  // };

  ws.onmessage = function(event) {
    console.log("ws.onmessage");
    var age = parseInt(event.data, 10);
    chart.increment(age);
    chart.update();
  };


  globalWebSocket = ws;
}


function dontUseWebSockets() {
  if (globalWebSocket.readyState !== WebSocket.CLOSED) {
    console.log("globalWebSocket.close");
    globalWebSocket.close();
  }
  globalWebSocket = undefined;

  document.forms[0].onsubmit = null;
}
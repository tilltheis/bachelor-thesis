var globalEventSource;

function useServerSentEvents() {
  var form = document.forms[0];
  var input = document.getElementById("ageInput");

  form.onsubmit = function() {
    var params = "age=" + input.value;
    var request = new XMLHttpRequest();
    request.open("POST", "/");
    request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    request.send(params);

    form.reset();
    return false; // prevent submission
  }

  // eventSourceUrl is defined in index.scala.html
  var eventSource = new EventSource(eventSourceUrl);

  eventSource.onmessage = function(event) {
    console.log("eventSource.onmessage");
    var age = parseInt(event.data, 10);
    chart.increment(age);
    chart.update();
  };


  globalEventSource = eventSource;
}

function dontUseServerSentEvents() {
  if (globalEventSource.readyState !== EventSource.CLOSED) {
    console.log("globalEventSource.close");
    globalEventSource.close();
  }
  globalEventSource = undefined;

  document.forms[0].onsubmit = null;
}
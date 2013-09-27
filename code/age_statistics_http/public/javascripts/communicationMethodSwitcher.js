var lastUsedCommunicationMethod = "Http";

function enableCommunicationMethod(method, userString) {
  var indicatorField =
    document.getElementById("currentlyUsedCommunicationMethod");

  document.getElementById("use" + method).onclick = function(e) {
    e.preventDefault();
    window["dontUse" + lastUsedCommunicationMethod]();
    window["use" + method]();
    indicatorField.innerHTML = userString;
    lastUsedCommunicationMethod = method;
  };
}

enableCommunicationMethod("Http", "HTTP");
enableCommunicationMethod("WebSockets", "Web Sockets");
enableCommunicationMethod("ServerSentEvents", "Server Sent Events");
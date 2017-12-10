// Blocking requests are intended
var httpRequest;
window.onload = onLoad;

function onLoad() {
  showEntries();
  showLog();
}
function showEntries() {
  httpRequest = new XMLHttpRequest();
  httpRequest.onreadystatechange = wrapRequestCallback(function () {
    renderEntries(JSON.parse(httpRequest.responseText));
  });
  httpRequest.open('GET', '/entries', false);
  httpRequest.send();
}

function renderEntries(jsonArray) {
  var table = document.getElementById("entities-table")

  for (i = 0; i < jsonArray.length; i++) {
    var entry = jsonArray[i];
    var tr = table.insertRow(table.rows.length - 2);

    var cell0 = tr.insertCell(0);
    cell0.innerHTML = entry["alias"];

    var cell1 = tr.insertCell(1);
    cell1.innerHTML = entry["url"];

    var cell2 = tr.insertCell(2);
    cell2.innerHTML = '<button class="js-remove" data=' + entry["alias"] +' onclick="return removeClick(event);">Remove</button>'
  }
}

function addClick(event) {
  var aliasEl = document.getElementsByClassName("js-add-alias")[0]
  var urlEl = document.getElementsByClassName("js-add-url")[0]
  var alias = aliasEl.value
  var url = urlEl.value
  httpRequest = new XMLHttpRequest();
  httpRequest.onreadystatechange = wrapRequestCallback(function () {
    aliasEl.value = '';
    urlEl.value = '';
    window.location.reload();
  });
  httpRequest.open('POST', '/entries/' + alias + '?url=' + url, false);
  httpRequest.send();
}

function removeClick(event) {
  var alias = event.target.getAttribute("data")
  if (window.confirm('Remove "' + alias + '"?')) { 
    httpRequest = new XMLHttpRequest();
    httpRequest.onreadystatechange = wrapRequestCallback(function () {
      window.location.reload();
    });
    httpRequest.open('DELETE', '/entries/' + alias, false);
    httpRequest.send();
  }
}

function showLog() {
  httpRequest = new XMLHttpRequest();
  httpRequest.onreadystatechange = wrapRequestCallback(function () {
    window.location.reload();
  });
  httpRequest.open('DELETE', '/entries/' + alias, false);
  httpRequest.send();
}

function showLog() {
  httpRequest = new XMLHttpRequest();
  httpRequest.onreadystatechange = wrapRequestCallback(function () {
    renderLog(httpRequest.responseText);
  });
  httpRequest.open('GET', '/log', false);
  httpRequest.send();
}

function renderLog(text) {
  var logArea = document.getElementById("log-area");
  logArea.value = text;
  logArea.scrollTop = logArea.scrollHeight;
}

//
// Helpers
//

function wrapRequestCallback(callback) {
  return function() {
    if (httpRequest.readyState === XMLHttpRequest.DONE) {
      if (httpRequest.status === 200) {
        callback();
      } else {
        showError();
      }
    }
  };
}

function showError() {
  var errorDetailsString = 'There was a problem with the request';
  try {
    var responseText = httpRequest.responseText;
    var responseJson = JSON.parse(responseText);
    errorDetailsString += ':\n' + responseJson['message']
  } catch(ex) {}
  alert(errorDetailsString);
}

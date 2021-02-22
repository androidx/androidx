/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
allFramesObject.onmessage = function(event) {
  document.getElementById('reply_message').innerText = event.data;
}
function sendMessage(e) {
  e.preventDefault();
  const message = document.getElementById("send_message").value;
  allFramesObject.postMessage(message);
}
function checkForObject(objectName, elementId) {
  var verb = window.hasOwnProperty(objectName) ? "do" : "don't";
  var element = document.getElementById(elementId);
  element.innerText = `I ${verb} have access to ${objectName}`;
}
function detectObjects(event) {
  checkForObject('restrictedObject', 'restricted_object_result');
  checkForObject('allFramesObject', 'all_frames_object_result');
}
window.addEventListener("DOMContentLoaded", detectObjects);

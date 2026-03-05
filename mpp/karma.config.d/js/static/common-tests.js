/*
 * Copyright 2024 The Android Open Source Project
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

window.addEventListener("error", (message, source, lineno, colno, error) => {
    console.log(`[web] error message: ${message} \n`);
    console.log(`[web] error source: ${source} \n`);
    console.log(`[web] error lineno: ${lineno} \n`);
    console.log(`[web] error colno: ${colno} \n`);
    console.log(`[web] error error: ${error} \n`);

    return true;
});


window.addEventListener("unhandledrejection", (event) => {
    try {
        console.log(`[web] unhandled Promise rejection ${event.reason} \n`);
    } catch (e) {
        console.log('[web] failed to retrieve Promise rejection reason', e.message, '\n');
    }
});

window.addEventListener("rejectionhandled", (event) => {
        try {
            console.log(`[web] handled Promise rejection ${event.reason} \n`);
        } catch (e) {
            console.log('[web] failed to retrieve Promise rejection reason', e.message, '\n');
        }
    }, false
);

const awaitAnimationFrame = () => new Promise(resolve => requestAnimationFrame(resolve));

beforeEach(async function() {
    // Wait for skiko.wasm to be ready before each test
    await awaitAnimationFrame;

    // This is the part of mocha configuration which guarantees that DOM elements are recreated for each test
    const canvasAppContainer = document.createElement("div");
    canvasAppContainer.setAttribute("id", "canvasApp");
    document.body.replaceChildren(canvasAppContainer);
});
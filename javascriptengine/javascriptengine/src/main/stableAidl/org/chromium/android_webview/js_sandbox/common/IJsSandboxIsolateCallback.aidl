/*
 * Copyright 2022 The Android Open Source Project
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

package org.chromium.android_webview.js_sandbox.common;

/**
 * Used to communicate the result of the JavaScript evaluation from the
 * sandbox to the embedding app.
 * DEPRECATED INTERFACE! Do not add methods or constants into this file.
 * @hide
 */
oneway interface IJsSandboxIsolateCallback {
    // An exception was thrown during the JS evaluation.
    const int JS_EVALUATION_ERROR = 0;
    // The evaluation failed and the isolate crashed due to running out of heap memory.
    const int MEMORY_LIMIT_EXCEEDED = 1;

    void reportResult(String result) = 0;

    // errorType is one of the error constants above.
    void reportError(int errorType, String error) = 1;
}

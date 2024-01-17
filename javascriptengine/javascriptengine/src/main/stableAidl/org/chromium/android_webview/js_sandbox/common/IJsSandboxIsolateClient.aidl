/*
 * Copyright 2023 The Android Open Source Project
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
 * Callbacks for isolate events, not specific to evaluations.
 * @hide
 */
interface IJsSandboxIsolateClient {
    // These crash codes may be generated on either the client or service side.

    // The isolate terminated for an unknown reason.
    const int TERMINATE_UNKNOWN_ERROR = 1;
    // The sandbox died.
    //
    // This is typically generated client-side as the service may die before it gets a chance to
    // send a message to the client.
    const int TERMINATE_SANDBOX_DEAD = 2;
    // The isolate exceeded its heap size limit.
    const int TERMINATE_MEMORY_LIMIT_EXCEEDED = 3;

    /**
     * Informs the client that the isolate should now be considered terminated.
     *
     * @param status  A status code describing the reason for the termination. Must be one of the
     *                constants beginning "TERMINATE_".
     * @param message Unstructured information about the termination. May be null.
     */
    void onTerminated(int status, String message) = 1;
}

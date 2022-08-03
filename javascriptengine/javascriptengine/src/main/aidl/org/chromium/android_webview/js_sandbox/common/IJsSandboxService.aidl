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
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolate;

/**
 * Used by the embedding app to execute JavaScript in a sandboxed environment.
 * @hide
 */
interface IJsSandboxService {
    IJsSandboxIsolate createIsolate() = 0;

    /**
     * Feature flag indicating that closing an isolate will terminate its
     * execution as soon as possible, instead of allowing previously-requested
     * executions to run to completion first.
     */
    const String ISOLATE_TERMINATION = "ISOLATE_TERMINATION";

    /**
     * This feature flag is a combination of three sub-features:
     * - If evaluateJavascript() returns a promise, we wait for the promise
     *   to resolve and then return the resolved value.
     * - Supports Java API provideNamedData() and JS API
     *   android.consumeNamedDataAsArrayBuffer().
     * - WebAssembly.compile() API is supported. Wasm can be compiled from
     *   an array buffer.
     */
    const String WASM_FROM_ARRAY_BUFFER = "WASM_FROM_ARRAY_BUFFER";

    /**
     * @return A list of feature names supported by this implementation.
     */
    List<String> getSupportedFeatures() = 1;
}

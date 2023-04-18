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

import android.content.res.AssetFileDescriptor;

/**
 * Used to communicate the result of the JavaScript evaluation from the
 * sandbox to the embedding app.
 * This interface is not marked 'oneway' like IJsSandboxIsolateCallback and should be preferred for
 * ordering correctness.
 * @hide
 */
interface IJsSandboxIsolateSyncCallback {
    // An exception was thrown during the JS evaluation.
    const int JS_EVALUATION_ERROR = 0;
    // The evaluation failed and the isolate crashed due to running out of heap memory.
    const int MEMORY_LIMIT_EXCEEDED = 1;

    /**
     * @param afd      input AssetFileDescriptor containing the return value of JS evaluation
     */
    void reportResultWithFd(in AssetFileDescriptor afd) = 2;

    /**
     * @param errorType denotes the type of error. Should be one of the constants in this file
     * @param afd       input AssetFileDescriptor containing the returned error of the JS evaluation
     */
    void reportErrorWithFd(int errorType, in AssetFileDescriptor afd) = 3;
}

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

import android.content.res.AssetFileDescriptor;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxConsoleCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateCallback;
import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateSyncCallback;
import org.chromium.android_webview.js_sandbox.common.IMessagePort;

/**
 * Used by the embedding app to execute JavaScript in a sandboxed environment.
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IJsSandboxIsolate {
    /**
     * @param code the JavaScript code
     * to be evaluated in the sandbox.
     * @param callback used to pass the information back to the embedding app
     * from the sandbox.
     */
    void evaluateJavascript(String code, in IJsSandboxIsolateCallback callback) = 0;

    /**
     * Stop the execution of the Isolate as soon as possible and destroy it.
     */
    void close() = 1;

    /**
     * Provides the data represented by afd such that it can be
     * retrieved in the JS code by calling `consumeNamedDataAs*(name)` APIs.
     * @param name   the id used to refer to the data in JS.
     * @param afd    input AssetFileDescriptor which will be read to retrieve data.
     * @return     true if data with the given name can be retrieved
     *             in JS code, else false.
     */
    boolean provideNamedData(String name, in AssetFileDescriptor afd) = 2;

    /**
     * @param afd      input AssetFileDescriptor containing the JavaScript code to be evaluated
     * @param callback used to pass the information back to the embedding app
     *                 from the sandbox.
     */
    void evaluateJavascriptWithFd(in AssetFileDescriptor afd, in IJsSandboxIsolateSyncCallback callback)
        = 3;

    /**
     * Set or unset a console callback to receive console messages from the isolate.
     * @param callback The callback to receive messages, or null to unset.
     */
    void setConsoleCallback(IJsSandboxConsoleCallback callback) = 4;

    /**
     * Provide a named MessagePort to the isolate, and returns the remote port.
     *
     * The passed and returned ports can be used to transfer messages between the host app and the isolate.
     *
     * Can be called multiple times to provide multiple ports.
     * Cannot pass a port with an existing name for a given isolate.
     *
     * @param name of the message port in the JavaScript context.
     * @param port to be entangled with the port in the JavaScript context.
     * @return the remote port.
     */
    IMessagePort provideMessagePort(
        in String name,
        in IMessagePort port) = 5;
}

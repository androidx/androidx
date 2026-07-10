/*
 * Copyright 2025 The Android Open Source Project
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

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IMessagePort {
    /**
     * Send string directly through Binder.
     *
     * Used for <64KiB strings.
     */
    void sendString(in String string) = 1;

    /**
     * Send string through a file descriptor.
     *
     * Must be used for >=64KiB strings.
     * The string is encoded as UTF-8 format when sent over the file descriptor.
     * The length is measured in bytes, and must be between 0 and Integer.MAX_VALUE inclusive.
     * A new file descriptor is sent for each message.
     */
    void sendStringOverFd(in AssetFileDescriptor afd) = 2;

    /**
     * Send byte array data directly through Binder.
     *
     * Used for <64KiB of data.
     */
    void sendArrayBuffer(in byte[] bytes) = 3;

    /**
     * Send byte array data through a file descriptor.
     *
     * Must be used for >=64KiB of data.
     * The length is measured in bytes and must be between 0 and Integer.MAX_VALUE inclusive.
     * A new file descriptor is sent for each message.
     */
    void sendArrayBufferOverFd(in AssetFileDescriptor afd) = 4;

    /**
     * Close the MessagePort.
     *
     * Notifies the other side of the closing event.
     * Called by the API or on isolate/sandbox close and death.
     */
    void close() = 5;
}

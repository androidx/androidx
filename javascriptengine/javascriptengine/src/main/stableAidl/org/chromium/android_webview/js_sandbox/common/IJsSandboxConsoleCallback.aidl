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
 * Used to relay console messages to the embedding app.
 * @hide
 */
interface IJsSandboxConsoleCallback {
    // These must be individual bits so that they can be trivially filtered using a bitmask.
    const int CONSOLE_MESSAGE_LEVEL_LOG = 1 << 0;
    const int CONSOLE_MESSAGE_LEVEL_DEBUG = 1 << 1;
    const int CONSOLE_MESSAGE_LEVEL_INFO = 1 << 2;
    const int CONSOLE_MESSAGE_LEVEL_ERROR = 1 << 3;
    const int CONSOLE_MESSAGE_LEVEL_WARNING = 1 << 4;

    /**
     * Notification of a console message.
     * @param contextGroupId Context group ID.
     * @param level The message (error/verbosity) level.
     * @param message The message body.
     * @param source The source file/expression where the message was generated.
     * @param line Line number of where the message was generated.
     * @param column Column number of where the message was generated.
     * @param trace Stack trace of where the message was generated, which may be null.
     */
    void consoleMessage(int contextGroupId, int level, String message, String source, int line,
            int column, String trace) = 0;

    /**
     * Notification of a console.clear()
     * @param contextGroupId context group ID.
     */
    void consoleClear(int contextGroupId) = 1;
}

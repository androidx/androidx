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

package androidx.appactions.interaction.capabilities.core.impl.utils;

import androidx.annotation.NonNull;

/**
 * Define the logger interface to hide specific logger implementations from the capability library.
 * This is needed because the capabilities library cannot depend on android deps directly.
 */
public interface CapabilityLogger {

    void log(@NonNull LogLevel logLevel, @NonNull String logTag, @NonNull String message);

    void log(
            @NonNull LogLevel logLevel,
            @NonNull String logTag,
            @NonNull String message,
            @NonNull Throwable throwable);

    /** Log levels to match the visibility of log errors from android.util.Log. */
    enum LogLevel {
        INFO,
        WARN,
        ERROR,
        DEBUG,
    }

}

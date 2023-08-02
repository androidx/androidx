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
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger.LogLevel;

/**
 * A singleton class to define the logger allows logger implementations to be defined outside of the
 * capabilities classes, where it is permitted to depend on Android Logger(android.util.Log).
 */
public final class LoggerInternal {

    @Nullable
    private static CapabilityLogger sDelegate = null;

    private LoggerInternal() {
    }

    public static void setLogger(@NonNull CapabilityLogger logger) {
        sDelegate = logger;
    }

    public static void log(
            @NonNull LogLevel logLevel, @NonNull String logTag, @NonNull String message) {
        if (sDelegate != null) {
            sDelegate.log(logLevel, logTag, message);
        }
    }

    public static void log(
            @NonNull LogLevel logLevel,
            @NonNull String logTag,
            @NonNull String message,
            @NonNull Throwable t) {
        if (sDelegate != null) {
            sDelegate.log(logLevel, logTag, message, t);
        }
    }
}

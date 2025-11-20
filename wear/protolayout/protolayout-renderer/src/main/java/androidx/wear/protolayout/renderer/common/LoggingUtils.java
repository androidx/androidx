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
package androidx.wear.protolayout.renderer.common;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Logger used for extensive logging. Note that all logs will contain the component name. */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public interface LoggingUtils {

    /** Logs message as debug if level is set or if the build type is not user. */
    default void logDOrNotUser(@NonNull String tag, @NonNull String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** Logs message as debug if level is set or if the build type is not user. */
    @FormatMethod
    default void logDOrNotUser(
            @NonNull String tag,
            @FormatString @NonNull String format,
            @Nullable Object @NonNull ... args) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** Logs message as warning. */
    default void logW(@NonNull String tag, @NonNull String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** Logs message as error. */
    default void logE(@NonNull String tag, @NonNull String message, @Nullable Throwable tr) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** LogD a formatted message. */
    void logD(@NonNull String tag, @NonNull String message);

    /** LogD a formatted message. */
    @FormatMethod
    void logD(@NonNull String tag, @FormatString @NonNull String format, Object @NonNull ... args);

    /**
     * Check whether debug logging is allowed or not for the given {@code tag}. This will allow
     * clients to skip building logs if it's not necessary.
     */
    boolean canLogD(@NonNull String tag);
}

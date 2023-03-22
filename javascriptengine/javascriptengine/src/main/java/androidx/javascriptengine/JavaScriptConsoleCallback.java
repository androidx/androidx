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

package androidx.javascriptengine;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxConsoleCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A JavaScriptConsoleCallback can be used to receive and process console messages and events which
 * originate from the isolate.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface JavaScriptConsoleCallback {
    /**
     * Representation of a console message, such as produced by console.log.
     */
    final class ConsoleMessage {
        /**
         * Console message (error) level
         */
        @IntDef({LEVEL_LOG, LEVEL_DEBUG, LEVEL_INFO, LEVEL_ERROR, LEVEL_WARNING})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Level {}
        public static final int LEVEL_LOG = IJsSandboxConsoleCallback.CONSOLE_MESSAGE_LEVEL_LOG;
        public static final int LEVEL_DEBUG = IJsSandboxConsoleCallback.CONSOLE_MESSAGE_LEVEL_DEBUG;
        public static final int LEVEL_INFO = IJsSandboxConsoleCallback.CONSOLE_MESSAGE_LEVEL_INFO;
        public static final int LEVEL_ERROR = IJsSandboxConsoleCallback.CONSOLE_MESSAGE_LEVEL_ERROR;
        public static final int LEVEL_WARNING =
                IJsSandboxConsoleCallback.CONSOLE_MESSAGE_LEVEL_WARNING;
        public static final int LEVEL_ALL =
                LEVEL_LOG | LEVEL_DEBUG | LEVEL_INFO | LEVEL_ERROR | LEVEL_WARNING;

        @Level
        private final int mLevel;
        @NonNull
        private final String mMessage;
        @NonNull
        private final String mSource;
        private final int mLine;
        private final int mColumn;
        @Nullable
        private final String mTrace;

        /**
         * Construct a new ConsoleMessage
         * @param level The message (error/verbosity) level.
         * @param message The message body.
         * @param source The source file/expression where the message was generated.
         * @param line Line number of where the message was generated.
         * @param column Column number of where the message was generated.
         * @param trace Stack trace of where the message was generated.
         */
        public ConsoleMessage(@Level int level, @NonNull String message, @NonNull String source,
                int line, int column, @Nullable String trace) {
            mLevel = level;
            mMessage = message;
            mSource = source;
            mLine = line;
            mColumn = column;
            mTrace = trace;
        }

        // Returns a single-character representation of the log level.
        @NonNull
        private String levelInitial() {
            switch (mLevel) {
                case LEVEL_LOG:
                    return "L";
                case LEVEL_DEBUG:
                    return "D";
                case LEVEL_INFO:
                    return "I";
                case LEVEL_ERROR:
                    return "E";
                case LEVEL_WARNING:
                    return "W";
                default:
                    return "?";
            }
        };

        /** Return the log level */
        @Level
        public int level() {
            return mLevel;
        }

        /** Return the message body */
        @NonNull
        public String message() {
            return mMessage;
        }

        /** Return the source file/expression name */
        @NonNull
        public String source() {
            return mSource;
        }

        /** Return the line number producing the message */
        public int line() {
            return mLine;
        }

        /** Return the column number producing the message */
        public int column() {
            return mColumn;
        }

        /** Return a stringified stack trace */
        @Nullable
        public String trace() {
            return mTrace;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(levelInitial())
                    .append(" ")
                    .append(mSource)
                    .append(":")
                    .append(mLine)
                    .append(":")
                    .append(mColumn)
                    .append(": ")
                    .append(mMessage)
                    .toString();
        }
    }

    /**
     * Called when a console message is produced by the isolate, such as through console.log().
     * <p>
     * Do not rely on console messages for the transfer of large volumes of data. Overly large
     * messages, stack traces, or source identifiers may be truncated.
     * <p>
     * The default implementation does nothing.
     */
    void onConsoleMessage(@NonNull ConsoleMessage message);

    /**
     * Called when the console should notionally be cleared, such as through console.clear().
     * <p>
     * The default implementation does nothing.
     */
    default void onConsoleClear() {}
};

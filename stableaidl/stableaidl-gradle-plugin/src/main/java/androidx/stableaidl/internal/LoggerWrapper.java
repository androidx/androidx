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

package androidx.stableaidl.internal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.MergingException;
import com.android.utils.ILogger;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Implementation of Android's {@link ILogger} over Gradle's {@link Logger}.
 *
 * Note that this maps info to the default user-visible lifecycle.
 *
 * Cloned from <code>com.android.build.gradle.internal.LoggerWrapper</code>.
 */
public class LoggerWrapper implements ILogger {

    // Mapping from ILogger method call to gradle log level.
    private static final LogLevel ILOGGER_ERROR = LogLevel.ERROR;
    private static final LogLevel ILOGGER_WARNING = LogLevel.WARN;
    private static final LogLevel ILOGGER_QUIET = LogLevel.QUIET;
    private static final LogLevel ILOGGER_LIFECYCLE = LogLevel.LIFECYCLE;
    private static final LogLevel ILOGGER_INFO = LogLevel.INFO;
    private static final LogLevel ILOGGER_VERBOSE = LogLevel.INFO;

    private final Logger logger;

    @NonNull
    public static LoggerWrapper getLogger(@NonNull Class<?> klass) {
        return new LoggerWrapper(Logging.getLogger(klass));
    }

    public LoggerWrapper(@NonNull Logger logger) {
        this.logger = logger;
    }

    @Override
    public void error(@Nullable Throwable throwable, @Nullable String s, Object... objects) {
        if (throwable instanceof MergingException) {
            // MergingExceptions have a known cause: they aren't internal errors, they
            // are errors in the user's code, so a full exception is not helpful (and
            // these exceptions should include a pointer to the user's error right in
            // the message).
            //
            // Furthermore, these exceptions are already caught by the MergeResources
            // and MergeAsset tasks, so don't duplicate the output
            return;
        }

        if (!logger.isEnabled(ILOGGER_ERROR)) {
            return;
        }

        if (s == null) {
            s = "[no message defined]";
        } else if (objects != null && objects.length > 0) {
            s = String.format(s, objects);
        }

        if (throwable == null) {
            logger.log(ILOGGER_ERROR, s);

        } else {
            logger.log(ILOGGER_ERROR, s, throwable);
        }
    }

    @Override
    public void warning(@NonNull String s, Object... objects) {
        log(ILOGGER_WARNING, s, objects);
    }

    @Override
    public void quiet(@NonNull String s, Object... objects) {
        log(ILOGGER_QUIET, s, objects);
    }

    @Override
    public void lifecycle(@NonNull String s, Object... objects) {
        log(ILOGGER_LIFECYCLE, s, objects);
    }

    @Override
    public void info(@NonNull String s, Object... objects) {
        log(ILOGGER_INFO, s, objects);
    }

    @Override
    public void verbose(@NonNull String s, Object... objects) {
        log(ILOGGER_VERBOSE, s, objects);
    }

    private void log(@NonNull LogLevel logLevel, @NonNull String s, @Nullable Object[] objects){
        if (!logger.isEnabled(logLevel)) {
            return;
        }
        if (objects == null || objects.length == 0) {
            logger.log(logLevel, s);

        } else {
            logger.log(logLevel, String.format(s, objects));
        }
    }

    /**
     * Return a {@link Supplier} for an instance of {@link ILogger} for the given class c.
     *
     * @param c the class' used to provide a logger name
     * @return the {@link Supplier} for a logger instance.
     */
    public static Supplier<ILogger> supplierFor(Class<?> c) {
        return new LoggerSupplier(c);
    }

    private static class LoggerSupplier implements Supplier<ILogger>, Serializable {

        private final Class<?> clazz;
        private ILogger logger = null;

        private LoggerSupplier(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public synchronized ILogger get() {
            if (logger == null) {
                logger = new LoggerWrapper(Logging.getLogger(clazz));
            }
            return logger;
        }
    }
}

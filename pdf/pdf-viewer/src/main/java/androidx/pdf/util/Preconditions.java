/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Simple parameter checking.
 *
 * <p>Subset of functions from {@code com.google.common.base.Preconditions}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class Preconditions {
    private Preconditions() {
    }

    /**
     * Check if the value is not null.
     */
    public static <T> T checkNotNull(T parameter) {
        return checkNotNull(parameter, null);
    }

    /**
     * Check if the value is not null.
     */
    @CanIgnoreReturnValue
    public static <T> T checkNotNull(T parameter, @Nullable String message) {
        if (parameter == null) {
            throw new NullPointerException(message);
        }
        return parameter;
    }

    /**
     * Check if the state is true otherwise throws the string exception.
     */
    public static void checkState(boolean state) {
        if (!state) {
            throw new IllegalStateException();
        }
    }

    /**
     * Check if the state is true otherwise throws the string exception.
     */
    public static void checkState(boolean state, @NonNull String message) {
        if (!state) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Check if the argument is true otherwise throws the string exception.
     */
    public static void checkArgument(boolean state, @NonNull String message)
            throws IllegalArgumentException {
        if (!state) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Check if the process runs on Ui thread.
     */
    public static void checkRunOnUIThread() {
        Preconditions.checkState(
                Looper.getMainLooper().getThread() == Thread.currentThread(),
                "Error - not running on the UI thread.");
    }

    /**
     * Check if the process runs on Ui thread.
     */
    public static void checkNotRunOnUIThread() {
        Preconditions.checkState(Looper.getMainLooper().getThread() != Thread.currentThread(),
                "Error - running on the UI thread.");
    }
}

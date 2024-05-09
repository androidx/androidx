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

package androidx.appsearch.util;

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utilities for handling exceptions.
 */
// This file has different behavior in Framework as compared to JetPack (like it is okay to rethrow
// exception and log instead of rethrowing from SystemServer in case of RemoteException). This file
// is not synced to Framework, as it maintains its own environment specific copy.
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ExceptionUtil {
    private static final String TAG = "AppSearchExceptionUtil";

    /**
     * {@link RuntimeException} will be rethrown if {@link #isItOkayToRethrowException()} returns
     * true.
     */
    public static void handleException(@NonNull Exception e) {
        if (isItOkayToRethrowException() && e instanceof RuntimeException) {
            rethrowRuntimeException((RuntimeException) e);
        }
    }

    /** Returns whether it is OK to rethrow exceptions from this entrypoint. */
    private static boolean isItOkayToRethrowException() {
        return true;
    }

    /** Rethrow exception from SystemServer in Framework code. */
    public static void handleRemoteException(@NonNull RemoteException e) {
        Log.w(TAG, "Unable to make a call to AppSearchManagerService!", e);
    }

    /**
     * A helper method to rethrow {@link RuntimeException}.
     *
     * <p>We use this to enforce exception type and assure the compiler/linter that the exception is
     * indeed {@link RuntimeException} and can be rethrown safely.
     */
    private static void rethrowRuntimeException(RuntimeException e) {
        throw e;
    }

    private ExceptionUtil() {}
}

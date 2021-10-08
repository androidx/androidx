/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.tracing;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * This is a helper class that handles {@link android.os.Trace} functionality in API >= 29.
 * <p>
 * This class is being defined separately to avoid class verification failures.
 * For more information read https://chromium.googlesource
 * .com/chromium/src/build/+/refs/heads/master/android/docs/class_verification_failures
 * .md#understanding-the-reason-for-the-failure
 */
@RequiresApi(29)
final class TraceApi29Impl {

    private TraceApi29Impl() {
        // Does nothing
    }

    /**
     * Checks whether or not tracing is currently enabled.
     */
    @DoNotInline
    public static boolean isEnabled() {
        return android.os.Trace.isEnabled();
    }

    /**
     * Writes a trace message to indicate that a given section of code has
     * begun. Must be followed by a call to {@link #endAsyncSection(String, int)} with the same
     * methodName and cookie.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events
     */
    public static void beginAsyncSection(@NonNull String methodName, int cookie) {
        android.os.Trace.beginAsyncSection(methodName, cookie);
    }

    /**
     * Writes a trace message to indicate that the current method has ended.
     * Must be called exactly once for each call to {@link #beginAsyncSection(String, int)}
     * using the same name and cookie.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events
     */
    public static void endAsyncSection(@NonNull String methodName, int cookie) {
        android.os.Trace.endAsyncSection(methodName, cookie);
    }

    /**
     * Writes trace message to indicate the value of a given counter.
     *
     * @param counterName  The counter name to appear in the trace.
     * @param counterValue The counter value.
     */
    public static void setCounter(@NonNull String counterName, int counterValue) {
        android.os.Trace.setCounter(counterName, counterValue);
    }
}

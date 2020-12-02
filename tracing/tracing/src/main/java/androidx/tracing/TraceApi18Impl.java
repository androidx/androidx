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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * This is a helper class that handles {@link android.os.Trace} functionality in API >= 18.
 * <p>
 * This class is being defined separately to avoid class verification failures.
 * For more information read https://chromium.googlesource
 * .com/chromium/src/build/+/refs/heads/master/android/docs/class_verification_failures
 * .md#understanding-the-reason-for-the-failure
 */
@RequiresApi(18)
final class TraceApi18Impl {

    private TraceApi18Impl() {
        // Does nothing
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun. This call must
     * be followed by a corresponding call to {@link #endSection()} on the same thread.
     *
     * <p class="note"> At this time the vertical bar character '|', newline character '\n', and
     * null character '\0' are used internally by the tracing mechanism.  If sectionName contains
     * these characters they will be replaced with a space character in the trace.
     *
     * @param label The name of the code section to appear in the trace.  This may be at
     *              most 127 Unicode code units long.
     */
    public static void beginSection(@NonNull String label) {
        android.os.Trace.beginSection(label);
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended. This call must
     * be preceded by a corresponding call to {@link #beginSection(String)}. Calling this method
     * will mark the end of the most recently begun section of code, so care must be taken to
     * ensure that beginSection / endSection pairs are properly nested and called from the same
     * thread.
     */
    public static void endSection() {
        android.os.Trace.endSection();
    }
}

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

package androidx.tracing

import androidx.tracing.apple.OSLogger
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
public actual object Trace {
    private val logger = OSLogger()

    /**
     * Writes a trace message to indicate that a given section of code has begun.
     *
     * This call must be followed by a corresponding call to [endSection] on the same thread.
     *
     * At this time the vertical bar character '|', newline character '\n', and null character '\0'
     * are used internally by the tracing mechanism. If sectionName contains these characters they
     * will be replaced with a space character in the trace.
     *
     * @param label The name of the code section to appear in the trace.
     */
    public actual fun beginSection(label: String) {
        logger.beginSection(label)
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended.
     *
     * This call must be preceded by a corresponding call to [beginSection]. Calling this method
     * will mark the end of the most recently begun section of code, so care must be taken to ensure
     * that beginSection / endSection pairs are properly nested and called from the same thread.
     */
    public actual fun endSection() {
        logger.endSection()
    }
}

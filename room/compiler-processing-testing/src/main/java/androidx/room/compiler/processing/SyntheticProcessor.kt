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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.RecordingXMessager
import androidx.room.compiler.processing.util.XTestInvocation

/**
 * Common interface for SyntheticProcessors that we create for testing.
 */
internal interface SyntheticProcessor {
    /**
     * List of invocations that was sent to the test code.
     *
     * The test code can register assertions on the compilation result, which is why we need this
     * list (to run assertions after compilation).
     */
    val invocationInstances: List<XTestInvocation>

    /**
     * The recorder for messages where we'll grab the diagnostics.
     */
    val messageWatcher: RecordingXMessager

    /**
     * Should return any assertion error that happened during processing.
     *
     * When assertions fail, we don't fail the compilation to keep the stack trace, instead,
     * dispatch them afterwards.
     */
    fun getProcessingException(): Throwable?
}

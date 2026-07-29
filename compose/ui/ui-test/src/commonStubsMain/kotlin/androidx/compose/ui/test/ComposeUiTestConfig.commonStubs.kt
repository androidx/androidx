/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.InputMode
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline
import kotlin.time.Duration

@Immutable
public actual class ComposeUiTestConfig
public actual constructor(
    public actual val effectContext: CoroutineContext,
    public actual val runTestContext: CoroutineContext,
    public actual val testTimeout: Duration,
    public actual val inputMode: InputMode,
    public actual val failurePolicy: TestFailurePolicy,
) {
    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public actual constructor(
        effectContext: CoroutineContext,
        runTestContext: CoroutineContext,
        testTimeout: Duration,
        inputMode: InputMode,
    ) : this(
        effectContext = effectContext,
        runTestContext = runTestContext,
        testTimeout = testTimeout,
        inputMode = inputMode,
        failurePolicy = TestFailurePolicy(),
    )
}

@Immutable
public actual class TestFailurePolicy
public actual constructor(
    public actual val screenshotCaptureMode: CaptureMode,
    public actual val uiHierarchyCaptureMode: CaptureMode,
    public actual val failureHandlers: List<TestFailureHandler>,
) {
    @JvmInline
    public actual value class CaptureMode private actual constructor(private val value: Int) {
        public actual companion object {
            public actual val Unspecified: CaptureMode = CaptureMode(0)
            public actual val Enabled: CaptureMode = CaptureMode(1)
            public actual val Disabled: CaptureMode = CaptureMode(2)
        }
    }
}

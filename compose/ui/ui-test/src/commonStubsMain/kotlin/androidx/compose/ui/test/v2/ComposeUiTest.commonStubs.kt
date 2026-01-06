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

package androidx.compose.ui.test.v2

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.implementedInJetBrainsFork
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.test.TestResult

@ExperimentalTestApi
actual fun runComposeUiTest(
    effectContext: CoroutineContext,
    runTestContext: CoroutineContext,
    testTimeout: Duration,
    block: suspend ComposeUiTest.() -> Unit,
): TestResult = implementedInJetBrainsFork()

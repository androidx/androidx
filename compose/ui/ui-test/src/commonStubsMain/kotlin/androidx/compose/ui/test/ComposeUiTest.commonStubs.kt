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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import kotlin.coroutines.CoroutineContext

@ExperimentalTestApi
actual fun runComposeUiTest(
    effectContext: CoroutineContext,
    block: ComposeUiTest.() -> Unit
): Unit = implementedInJetBrainsFork()

@ExperimentalTestApi
actual sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    actual val density: Density
    actual val mainClock: MainTestClock

    actual fun <T> runOnUiThread(action: () -> T): T

    actual fun <T> runOnIdle(action: () -> T): T

    actual fun waitForIdle()

    actual suspend fun awaitIdle()

    actual fun waitUntil(
        conditionDescription: String?,
        timeoutMillis: Long,
        condition: () -> Boolean
    )

    actual fun registerIdlingResource(idlingResource: IdlingResource)

    actual fun unregisterIdlingResource(idlingResource: IdlingResource)

    actual fun setContent(composable: @Composable () -> Unit)

    actual fun enableAccessibilityChecks()

    actual fun disableAccessibilityChecks()
}

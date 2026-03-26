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

import android.os.Build
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * Whether or not this test is running on Robolectric.
 *
 * The implementation of this check is widely used but not officially supported and should therefore
 * stay internal.
 */
internal val HasRobolectricFingerprint
    get() = Build.FINGERPRINT.lowercase() == "robolectric"

/**
 * Idling strategy for use with Robolectric.
 *
 * When running on Robolectric, the following things are different:
 * 1. IdlingResources are not queried. We drive Compose from the ComposeIdlingResource, so we need
 *    to do that manually here.
 * 2. Draw passes don't happen. Compose performs most measure and layout passes during the draw
 *    pass, so we need to manually trigger an actual measure/layout pass when needed.
 * 3. Awaiting idleness must happen on the main thread. On Espresso it's exactly the other way
 *    around, so we need to invert our thread checks.
 *
 * Note that we explicitly don't install our [IdlingResourceRegistry] into Espresso even though it
 * would be a noop anyway: if at some point in the future they will be supported, our behavior would
 * silently change (potentially leading to breakages).
 */
internal class RobolectricIdlingStrategy(
    private val composeRootRegistry: ComposeRootRegistry,
    private val composeIdlingResource: ComposeIdlingResource,
    private val idlingResourceRegistry: IdlingResourceRegistry,
) : IdlingStrategy {
    override val canSynchronizeOnUiThread: Boolean = true

    /*
     * On Robolectric, Espresso.onIdle() needs to be called from the main thread; so use
     * Dispatchers.Main. Use `.immediate` in case we're already on the main thread.
     */
    override val synchronizationContext: CoroutineContext
        get() = Dispatchers.Main.immediate

    override fun runUntilIdle() {
        val policy = IdlingPolicies.getMasterIdlingPolicy()
        val timeoutMillis = policy.idleTimeoutUnit.toMillis(policy.idleTimeout)
        runOnUiThread {
            // Use Java's clock, Android's clock is mocked
            val start = System.currentTimeMillis()
            var iteration = 0
            // Draining the Espresso message queue might trigger Compose state changes,
            // and fast-forwarding Compose might post new tasks back to the Espresso queue.
            // To ensure the system has truly stabilized, we require two consecutive passes
            // where absolutely no work is requested or performed.
            var isIdle = false
            do {
                // Check if we hit the timeout
                if (System.currentTimeMillis() - start >= timeoutMillis) {
                    val diagnosticInfo = idlingResourceRegistry.getDiagnosticMessageIfBusy()
                    val errorMessage = buildString {
                        appendLine(
                            "Compose did not get idle after $iteration attempts in ${policy.idleTimeout} ${policy.idleTimeoutUnit}."
                        )

                        if (!diagnosticInfo.isNullOrEmpty()) {
                            appendLine()
                            appendLine(diagnosticInfo)
                        }

                        appendLine()
                        appendLine(
                            "1. Check your measure/layout lambdas for infinite composition loops."
                        )
                        appendLine(
                            "2. Verify that the 'busy' resources listed above are not deadlocked."
                        )
                        appendLine(
                            "3. Increase Espresso's master idling policy if a longer timeout is required."
                        )
                    }

                    throw AppNotIdleException.create(emptyList(), errorMessage)
                }
                iteration++
                // Track state from the previous iteration
                val wasIdle = isIdle
                // Run Espresso.onIdle() to drain the main message queue
                runEspressoOnIdle()
                // Check if we need a measure/layout pass
                requestLayoutIfNeeded()
                // Evaluate idleness for both compose and registered resources
                isIdle = composeIdlingResource.isIdleNow && idlingResourceRegistry.isIdleNow
                // Loop continues if we are currently busy, or if we just became idle
                // and need one more pass to confirm (wasIdle == false).
            } while (!isIdle || !wasIdle)
        }
    }

    /**
     * Calls [requestLayout][android.view.View.requestLayout] on all compose hosts that are awaiting
     * a measure/layout pass, because the draw pass that it is normally awaiting never happens on
     * Robolectric.
     */
    private fun requestLayoutIfNeeded(): Boolean {
        val composeRoots = composeRootRegistry.getRegisteredComposeRoots()
        return composeRoots
            .filter { it.shouldWaitForMeasureAndLayout }
            .onEach { it.view.requestLayout() }
            .isNotEmpty()
    }
}

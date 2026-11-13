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

package androidx.camera.integration.uiwidgets.viewpager

import android.graphics.Rect
import android.view.View
import androidx.annotation.IdRes
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

internal const val ACTION_IDLE_TIMEOUT: Long = 10_000
private const val POLL_INTERVAL_MS: Long = 50

internal fun waitUntilViewDisplayed(
    scenario: ActivityScenario<out BaseActivity>,
    @IdRes viewId: Int,
    timeoutMs: Long = ACTION_IDLE_TIMEOUT,
) = runBlocking {
    val displayed =
        withTimeoutOrNull(timeoutMs) {
            while (!isViewDisplayed(scenario, viewId)) {
                delay(POLL_INTERVAL_MS)
            }
            true
        } ?: false
    assertWithMessage("Timed out waiting for view ($viewId) to be displayed")
        .that(displayed)
        .isTrue()
}

private fun isViewDisplayed(
    scenario: ActivityScenario<out BaseActivity>,
    @IdRes viewId: Int,
): Boolean {
    var isVisible = false
    scenario.onActivity { activity ->
        val view = activity.findViewById<View>(viewId)
        isVisible = view != null && view.isShown && view.getGlobalVisibleRect(Rect())
    }
    return isVisible
}

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

package androidx.privacysandbox.ui.client.view

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Class for calculating signals related to the presentation of a [SandboxedSdkView].
 *
 * This class also schedules the collection of signals to ensure that signals are not sent too
 * frequently.
 */
internal class SandboxedSdkViewSignalMeasurer(
    val view: SandboxedSdkView,
    private val session: SandboxedUiAdapter.Session,
    private val clock: Clock = Clock { SystemClock.uptimeMillis() }
) {
    private companion object {
        private const val MIN_SIGNAL_LATENCY_MS = 200
    }

    internal fun interface Clock {
        fun uptimeMillis(): Long
    }

    private val windowLocation = IntArray(2)
    private var onScreenGeometry = Rect()
    private var containerWidthPx = 0
    private var containerHeightPx = 0
    private var opacityHint = 1.0f
    private var lastTimeSentSignals: Long = clock.uptimeMillis()
    private var scheduledTask: Runnable? = null

    /**
     * Updates the [SandboxedSdkViewUiInfo] that represents the view if there is no task already
     * scheduled and the time since the last time signals were sent is at least the minimum
     * acceptable latency.
     *
     * TODO(b/333853853): Use concurrency constructs instead.
     */
    fun maybeSendSignals() {
        if (scheduledTask != null) {
            return
        }

        if ((clock.uptimeMillis() - lastTimeSentSignals) < MIN_SIGNAL_LATENCY_MS) {
            val delayToNextSend =
                MIN_SIGNAL_LATENCY_MS - (clock.uptimeMillis() - lastTimeSentSignals)
            scheduledTask = Runnable {
                scheduledTask = null
                maybeSendSignals()
            }
            view.postDelayed(scheduledTask, delayToNextSend)
        } else {
            updateUiContainerInfo()
            session.notifyUiChanged(
                SandboxedSdkViewUiInfo.toBundle(
                    SandboxedSdkViewUiInfo(
                        containerWidthPx,
                        containerHeightPx,
                        onScreenGeometry,
                        opacityHint
                    )
                )
            )
            lastTimeSentSignals = clock.uptimeMillis()
        }
    }

    /** Removes the pending UI update [Runnable] from the message queue, if one exists. */
    fun dropPendingUpdates() {
        scheduledTask?.let { view.removeCallbacks(it) }
        scheduledTask = null
    }

    /** Updates the [SandboxedSdkViewUiInfo] that represents the state of the view. */
    private fun updateUiContainerInfo() {
        if (view.windowVisibility == View.VISIBLE) {
            val isVisible = view.getGlobalVisibleRect(onScreenGeometry)
            if (!isVisible) {
                onScreenGeometry.set(-1, -1, -1, -1)
            } else {
                view.getLocationOnScreen(windowLocation)
                onScreenGeometry.offset(-windowLocation[0], -windowLocation[1])
                onScreenGeometry.intersect(0, 0, view.width, view.height)
            }
        } else {
            onScreenGeometry.set(-1, -1, -1, -1)
        }
        containerHeightPx = view.height
        containerWidthPx = view.width
        opacityHint = view.alpha
    }
}

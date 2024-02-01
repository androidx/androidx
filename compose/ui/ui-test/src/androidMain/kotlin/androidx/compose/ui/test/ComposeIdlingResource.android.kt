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

import android.view.View
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ViewRootForTest
import kotlin.math.max

/**
 * Provides an idle check to be registered into Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule].
 */
internal class ComposeIdlingResource(
    private val composeRootRegistry: ComposeRootRegistry,
    private val clock: MainTestClockImpl,
    private val mainRecomposer: Recomposer
) : IdlingResource {

    private var hadAwaitersOnMainClock = false
    private var hadSnapshotChanges = false
    private var hadRecomposerChanges = false
    private var hadPendingSetContent = false
    private var hadPendingMeasureLayout = false

    /**
     * Tries to get Compose to idle by advancing frames until Compose has no more work to do, then
     * returns true iff Compose was "idle" at the time when the getter was called – i.e. there was
     * no Compose work that required advancing frames, and there are no pending `setContent` calls
     * and no pending measure/layout passes.
     *
     * It returns false if frames were advanced, even if Compose is technically idle after advancing
     * them because of Robolectric. When running tests in Robolectric, the return value of this
     * property is used to determine whether to drain the main queue again. If Compose did any work
     * at all, that work may have posted additional work to the main queue, so we need to drain it
     * again just to make sure.
     *
     * So this implementation treats the "now" in the property name as the time just before the
     * property was called, not the time just before it returns.
     */
    override val isIdleNow: Boolean
        get() {
            fun shouldPumpTime(): Boolean {
                hadAwaitersOnMainClock = clock.hasAwaiters
                hadSnapshotChanges = Snapshot.current.hasPendingChanges()
                hadRecomposerChanges = mainRecomposer.hasPendingWork

                val needsRecompose = hadAwaitersOnMainClock || hadSnapshotChanges ||
                    hadRecomposerChanges
                return clock.autoAdvance && needsRecompose
            }

            var i = 0
            while (i < 100 && shouldPumpTime()) {
                clock.advanceTimeByFrame()
                ++i
            }
            val composeDidWork = i > 0

            // pending set content needs all created compose roots,
            // because by definition they will not be in resumed state
            hadPendingSetContent =
                composeRootRegistry.getCreatedComposeRoots().any { it.isBusyAttaching }

            val composeRoots = composeRootRegistry.getRegisteredComposeRoots()
            hadPendingMeasureLayout = composeRoots.any { it.shouldWaitForMeasureAndLayout }

            return !composeDidWork &&
                !hadPendingSetContent &&
                !hadPendingMeasureLayout
        }

    override fun getDiagnosticMessageIfBusy(): String? {
        val wasBusy = hadSnapshotChanges || hadRecomposerChanges || hadAwaitersOnMainClock ||
            hadPendingSetContent || hadPendingMeasureLayout

        if (!wasBusy) {
            return null
        }

        val busyReasons = mutableListOf<String>()
        val busyRecomposing = hadSnapshotChanges || hadRecomposerChanges || hadAwaitersOnMainClock
        if (busyRecomposing) {
            busyReasons.add("pending recompositions")
        }
        if (hadPendingSetContent) {
            busyReasons.add("pending setContent")
        }
        if (hadPendingMeasureLayout) {
            busyReasons.add("pending measure/layout")
        }

        var message = "${javaClass.simpleName} is busy due to ${busyReasons.joinToString(", ")}.\n"
        if (busyRecomposing) {
            message += "- Note: Timeout on pending recomposition means that there are most likely" +
                " infinite re-compositions happening in the tested code.\n"
            message += "- Debug: hadRecomposerChanges = $hadRecomposerChanges, "
            message += "hadSnapshotChanges = $hadSnapshotChanges, "
            message += "hadAwaitersOnMainClock = $hadAwaitersOnMainClock"
        }
        return message
    }
}

private val ViewRootForTest.isBusyAttaching: Boolean
    get() {
        // If the rootView has a parent, it is the ViewRootImpl, which is set in
        // windowManager.addView(). If the rootView doesn't have a parent, the view hasn't been
        // attached to a window yet, or is removed again.
        return view.rootView.parent != null && !view.isAttachedToWindow
    }

/**
 * Whether or not we should wait until this root has done a measure/layout pass. Not necessarily
 * the same as if the root has a pending measure/layout pass, e.g. if the pending measure/layout
 * pass will never happen because the containing View is GONE anyway.
 */
internal val ViewRootForTest.shouldWaitForMeasureAndLayout: Boolean
    get() {
        // Should wait if: 1) there actually is a pending measure/layout
        return hasPendingMeasureOrLayout &&
            // 2) the containing View is not GONE
            !view.isEffectivelyGone &&
            // 3) the containing View is not INVISIBLE while trying to do the
            // measure/layout pass by only invalidating the containing View
            !(view.isEffectivelyInvisible && !view.isLayoutRequested)
    }

private val View.isEffectivelyGone: Boolean
    get() {
        return effectiveVisibility == View.GONE
    }

private val View.isEffectivelyInvisible: Boolean
    get() {
        return effectiveVisibility == View.INVISIBLE
    }

/**
 * Return the effective visibility of the View, which accounts for the visibility of ancestors.
 * If the view or any of its ancestors is GONE, this view is GONE. Otherwise, if this view or any
 * of its ancestors is INVISIBLE, this view is INVISIBLE. Otherwise, this view is VISIBLE.
 */
private val View.effectiveVisibility: Int
    get() {
        // Visibility values increase as they express "less visible", so the effective visibility
        // is the maximum of all visibility values from this view to the root.
        return when (visibility) {
            View.GONE -> View.GONE
            else -> max(visibility, (parent as? View)?.effectiveVisibility ?: View.VISIBLE)
        }
    }

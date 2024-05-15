/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density

/**
 * The marker interface to be implemented by the root backing the composition.
 * To be used in tests.
 */
interface RootForTest {
    /**
     * Current device density.
     */
    val density: Density

    /**
     * Semantics owner for this root. Manages all the semantics nodes.
     */
    val semanticsOwner: SemanticsOwner

    /**
     * The service handling text input.
     */
    @Deprecated("Use PlatformTextInputModifierNode instead.")
    val textInputService: TextInputService

    /**
     * Send this [KeyEvent] to the focused component in this [Owner].
     *
     * @return true if the event was consumed. False otherwise.
     */
    fun sendKeyEvent(keyEvent: KeyEvent): Boolean

    /**
     * Force accessibility to be enabled for testing.
     *
     * @param enable force enable accessibility if true.
     */
    @ExperimentalComposeUiApi
    fun forceAccessibilityForTesting(enable: Boolean) { }

    /**
     * Set the time interval between sending accessibility events in milliseconds.
     *
     * This is the delay before dispatching a recurring accessibility event in milliseconds. It
     * delays the loop that sends events to the accessibility and content capture framework
     * in batches. A recurring event will be sent at most once during the
     * [intervalMillis] timeframe. The default time delay is 100 milliseconds.
     */
    @ExperimentalComposeUiApi
    fun setAccessibilityEventBatchIntervalMillis(intervalMillis: Long) { }

    /**
     * Requests another layout (measure + placement) pass be performed for any nodes that need it.
     * This doesn't force anything to be remeasured that wouldn't be if `requestLayout` were called.
     * However, unlike `requestLayout`, it doesn't merely _schedule_ another layout pass to be
     * performed, it actually performs it synchronously.
     *
     * This method is used in UI tests to perform layout in between frames when pumping frames as
     * fast as possible (i.e. without waiting for the choreographer to schedule them) in order to
     * get to idle, e.g. during a `waitForIdle` call.
     */
    @ExperimentalComposeUiApi
    fun measureAndLayoutForTest() {}
}

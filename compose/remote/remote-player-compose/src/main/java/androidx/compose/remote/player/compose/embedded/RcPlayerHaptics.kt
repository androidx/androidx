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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.view.View
import androidx.compose.remote.core.CoreDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

private val HapticTable =
    intArrayOf(
        HapticFeedbackConstantsCompat.NO_HAPTICS,
        HapticFeedbackConstantsCompat.LONG_PRESS,
        HapticFeedbackConstantsCompat.VIRTUAL_KEY,
        HapticFeedbackConstantsCompat.KEYBOARD_TAP,
        HapticFeedbackConstantsCompat.CLOCK_TICK,
        HapticFeedbackConstantsCompat.CONTEXT_CLICK,
        HapticFeedbackConstantsCompat.KEYBOARD_PRESS,
        HapticFeedbackConstantsCompat.KEYBOARD_RELEASE,
        HapticFeedbackConstantsCompat.VIRTUAL_KEY_RELEASE,
        HapticFeedbackConstantsCompat.TEXT_HANDLE_MOVE,
        HapticFeedbackConstantsCompat.GESTURE_START,
        HapticFeedbackConstantsCompat.GESTURE_END,
        HapticFeedbackConstantsCompat.CONFIRM,
        HapticFeedbackConstantsCompat.REJECT,
        HapticFeedbackConstantsCompat.TOGGLE_ON,
        HapticFeedbackConstantsCompat.TOGGLE_OFF,
        HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE,
        HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_DEACTIVATE,
        HapticFeedbackConstantsCompat.DRAG_START,
        HapticFeedbackConstantsCompat.SEGMENT_TICK,
        HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK,
    )

/**
 * Maps a document haptic type (as carried by `HapticFeedback`) to the platform feedback constant
 * for [ViewCompat.performHapticFeedback], matching the View player's `HapticSupport` table.
 */
internal fun hapticFeedbackConstant(type: Int): Int =
    HapticTable[Math.floorMod(type, HapticTable.size)]

/**
 * Creates the haptic engine for a player. Replaceable in tests via [LocalRcHapticEngineFactory].
 */
internal fun interface RcHapticEngineFactory {
    fun create(view: View): CoreDocument.HapticEngine
}

/** Performs document haptics on the host view, with the View player's type mapping. */
internal val DefaultRcHapticEngineFactory = RcHapticEngineFactory { view ->
    CoreDocument.HapticEngine { type ->
        ViewCompat.performHapticFeedback(view, hapticFeedbackConstant(type))
    }
}

internal val LocalRcHapticEngineFactory = staticCompositionLocalOf { DefaultRcHapticEngineFactory }

/**
 * Installs a haptic engine on [document] while the player is composed, the embedded equivalent of
 * the View player's `HapticSupport.setupHaptics`.
 */
@Composable
internal fun RcPlayerHapticEffect(document: CoreDocument) {
    val view = LocalView.current
    val factory = LocalRcHapticEngineFactory.current
    DisposableEffect(document, view, factory) {
        document.setHapticEngine(factory.create(view))
        onDispose { document.setHapticEngine {} }
    }
}

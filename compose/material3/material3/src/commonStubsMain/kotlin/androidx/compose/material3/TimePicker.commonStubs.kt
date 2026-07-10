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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@ReadOnlyComposable
@Composable
internal actual fun defaultTimePickerLayoutType(): TimePickerLayoutType {
    implementedInJetBrainsFork()
}

@Composable
internal actual fun rememberTimeInputErrorHandler(
    isTouchExplorationEnabled: Boolean
): TimeInputErrorHandler {
    val haptics = LocalHapticFeedback.current

    return remember(haptics) { TimeInputErrorHandlerImpl(haptics = haptics) }
}

private class TimeInputErrorHandlerImpl(private val haptics: HapticFeedback) :
    TimeInputErrorHandler {

    override fun onError() {
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
    }
}

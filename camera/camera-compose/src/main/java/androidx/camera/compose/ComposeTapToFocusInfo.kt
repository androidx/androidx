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

package androidx.camera.compose

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.camera.viewfinder.core.FocusMeteringIntState
import androidx.camera.viewfinder.core.FocusMeteringState
import androidx.camera.viewfinder.core.TapToFocusInfo
import androidx.compose.ui.geometry.Offset

/**
 * Information about a tap-to-focus action for Compose components.
 *
 * This class provides the result of a focus and metering operation triggered by a tap gesture,
 * along with the coordinate of the tap in the Compose coordinate system.
 *
 * Possible focus states:
 * - [FocusMeteringState.FOCUS_METERING_STARTED]: The focus and metering operation has started.
 * - [FocusMeteringState.FOCUS_METERING_FOCUSED]: The focus and metering operation completed
 *   successfully and the camera is focused.
 * - [FocusMeteringState.FOCUS_METERING_NOT_FOCUSED]: The focus and metering operation completed
 *   successfully but the camera is still unfocused.
 * - [FocusMeteringState.FOCUS_METERING_FAILED]: The focus and metering operation failed to
 *   complete.
 *
 * @property status The state of the focus and metering action. Possible values are defined in
 *   [FocusMeteringState].
 * @property tapCoordinate The tap coordinate in Compose's local coordinate system ([Offset]).
 */
@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeTapToFocusInfo(
    @get:FocusMeteringIntState override val status: Int,
    public val tapCoordinate: Offset,
) : TapToFocusInfo

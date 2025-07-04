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

package androidx.camera.view

import android.graphics.PointF
import androidx.camera.core.CameraControl

/**
 * Describes the focus state and tap location that [CameraController] uses for tap-to-focus feature
 * corresponding to the [PreviewView] touch events.
 *
 * @property focusState The focus state of the camera.
 * @property tapPoint The `PreviewView` co-ordinates of user's touch event corresponding to this
 *   focusing event. This can be null when a focusing event is not due to the user's touch event.
 *   For example, `FocusOnTap(TAP_TO_FOCUS_NOT_STARTED, null)` is used when there is no focusing
 *   event ongoing e.g. during initialization or after a focus event has been completed.
 * @see CameraController
 * @see CameraController.getTapToFocusInfoState
 * @see CameraControl.startFocusAndMetering
 */
public class TapToFocusInfo(public val focusState: Int, public val tapPoint: PointF?)

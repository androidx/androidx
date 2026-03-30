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

package androidx.camera.compose

import androidx.annotation.RestrictTo
import androidx.camera.core.FocusMeteringAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** State of [CameraXViewfinder]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public interface CameraXViewfinderState {
    /** Input/Config: Enablement for pinch-to-zoom. */
    public var isPinchToZoomEnabled: Boolean

    /** Input/Config: Enablement for tap-to-focus. */
    public var isTapToFocusEnabled: Boolean

    /** Input/Config: Duration in milliseconds for tap-to-focus to auto-cancel. */
    public var tapToFocusAutoCancelDurationMillis: Long
}

/** Creates a [CameraXViewfinderState] that is remembered across recompositions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberCameraXViewfinderState(): CameraXViewfinderState {
    return remember { CameraXViewfinderStateImpl() }
}

internal class CameraXViewfinderStateImpl : CameraXViewfinderState {
    override var isPinchToZoomEnabled: Boolean by mutableStateOf(false)
    override var isTapToFocusEnabled: Boolean by mutableStateOf(false)
    override var tapToFocusAutoCancelDurationMillis: Long by
        mutableLongStateOf(FocusMeteringAction.DEFAULT_AUTO_CANCEL_DURATION_MILLIS)
}

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

package androidx.camera.viewfinder.core

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Result of a focus and metering action. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FocusMeteringResult {
    /** Returns the state of the focus and metering action. */
    @get:FocusMeteringIntState public val focusMeteringState: Int
}

/** State constants for focus and metering actions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FocusMeteringState {
    public companion object {
        /** A focus and metering action has started but not completed. */
        public const val FOCUS_METERING_STARTED: Int = 1

        /** The focus and metering action was completed successfully and the camera is focused. */
        public const val FOCUS_METERING_FOCUSED: Int = 2

        /**
         * The focus and metering action was completed successfully but the camera is still
         * unfocused.
         */
        public const val FOCUS_METERING_NOT_FOCUSED: Int = 3

        /** The focus and metering action failed to complete. */
        public const val FOCUS_METERING_FAILED: Int = 4
    }
}

/** Annotation for the integer focus and metering state. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    FocusMeteringState.FOCUS_METERING_STARTED,
    FocusMeteringState.FOCUS_METERING_FOCUSED,
    FocusMeteringState.FOCUS_METERING_NOT_FOCUSED,
    FocusMeteringState.FOCUS_METERING_FAILED,
)
public annotation class FocusMeteringIntState

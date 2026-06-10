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

/** State constants for focus actions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object FocusState {
    /** The focus state is inactive or unknown. */
    public const val INACTIVE: Int = 0

    /** A focus action has started but not completed. */
    public const val STARTED: Int = 1

    /** The focus action was completed successfully and the camera is focused. */
    public const val FOCUSED: Int = 2

    /** The focus action was completed successfully but the camera is still unfocused. */
    public const val NOT_FOCUSED: Int = 3

    /** The focus action failed to complete. */
    public const val FAILED: Int = 4
}

/** Annotation for the integer focus state. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY,
)
@IntDef(
    FocusState.INACTIVE,
    FocusState.STARTED,
    FocusState.FOCUSED,
    FocusState.NOT_FOCUSED,
    FocusState.FAILED,
)
public annotation class FocusStateValue

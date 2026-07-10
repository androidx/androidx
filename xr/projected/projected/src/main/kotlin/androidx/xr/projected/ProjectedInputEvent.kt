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

package androidx.xr.projected

import androidx.annotation.RestrictTo
import androidx.collection.MutableIntObjectMap
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.util.Objects

/**
 * Represents an input event for a projected device.
 *
 * @property inputAction The action of this input event.
 */
@ExperimentalProjectedApi
public class ProjectedInputEvent
internal constructor(public val inputAction: ProjectedInputAction) {

    /** Supported Projected input actions. */
    public class ProjectedInputAction
    private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val code: Int) {
        /**
         * Sent to apps from the Projected device when the user requests to toggle the app camera.
         * Apps should use the signal to start / stop the camera if relevant.
         *
         * Only Activities running in foreground and in the resumed state can receive this signal.
         */
        public companion object {
            @JvmField public val TOGGLE_APP_CAMERA: ProjectedInputAction = ProjectedInputAction(0)

            private val valueMap =
                MutableIntObjectMap<ProjectedInputAction>().apply {
                    put(TOGGLE_APP_CAMERA.code, TOGGLE_APP_CAMERA)
                }

            /**
             * Returns the [ProjectedInputAction] constant associated with the given code.
             *
             * @param code The integer value to look up.
             * @return The corresponding [ProjectedInputAction] constant.
             * @throws IllegalArgumentException if no such constant exists.
             */
            internal fun fromCode(code: Int): ProjectedInputAction {
                return valueMap[code]
                    ?: throw IllegalArgumentException("No ProjectedInputAction with code '$code'")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectedInputEvent

        if (inputAction != other.inputAction) return false

        return true
    }

    override fun hashCode(): Int = Objects.hash(inputAction)

    override fun toString(): String = "ProjectedInputEvent(inputAction=$inputAction)"
}

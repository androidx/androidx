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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/**
 * Represents the field of view of a camera.
 *
 * @property angleLeft The angle in radians of the left edge of the field of view.
 * @property angleRight The angle in radians of the right edge of the field of view.
 * @property angleUp The angle in radians of the top edge of the field of view.
 * @property angleDown The angle in radians of the bottom edge of the field of view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FieldOfView
constructor(
    public val angleLeft: Float,
    public val angleRight: Float,
    public val angleUp: Float,
    public val angleDown: Float,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldOfView) return false

        if (angleLeft != other.angleLeft) return false
        if (angleRight != other.angleRight) return false
        if (angleUp != other.angleUp) return false
        if (angleDown != other.angleDown) return false

        return true
    }

    override fun hashCode(): Int {
        var result = angleLeft.hashCode()
        result = 31 * result + angleRight.hashCode()
        result = 31 * result + angleUp.hashCode()
        result = 31 * result + angleDown.hashCode()
        return result
    }

    override fun toString(): String =
        "Fov{\n\tangleLeft=$angleLeft\n\tangleRight=$angleRight\n\tangleUp=$angleUp\n\tangleDown=$angleDown\n}"

    @JvmOverloads
    public fun copy(
        angleLeft: Float = this.angleLeft,
        angleRight: Float = this.angleRight,
        angleUp: Float = this.angleUp,
        angleDown: Float = this.angleDown,
    ): FieldOfView {
        return FieldOfView(angleLeft, angleRight, angleUp, angleDown)
    }
}

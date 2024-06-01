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

package androidx.tv.material3

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines the shadow for a TV component.
 *
 * @param elevationColor [Color] to be applied on the shadow
 * @param elevation defines how strong should be the shadow. Larger its value, further the shadow
 *   goes from the center of the component.
 */
@Immutable
class Glow(val elevationColor: Color, val elevation: Dp) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Glow

        if (elevationColor != other.elevationColor) return false
        if (elevation != other.elevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elevationColor.hashCode()
        result = 31 * result + elevation.hashCode()
        return result
    }

    override fun toString(): String {
        return "Glow(elevationColor=$elevationColor, elevation=$elevation)"
    }

    fun copy(glowColor: Color? = null, glowElevation: Dp? = null): Glow =
        Glow(
            elevationColor = glowColor ?: this.elevationColor,
            elevation = glowElevation ?: this.elevation
        )

    companion object {
        /**
         * Signifies the absence of a glow in TV Components. Use this if you do not want to display
         * a glow indication in any of the Leanback TV Components.
         */
        val None = Glow(elevationColor = Color.Transparent, elevation = 0.dp)
    }
}

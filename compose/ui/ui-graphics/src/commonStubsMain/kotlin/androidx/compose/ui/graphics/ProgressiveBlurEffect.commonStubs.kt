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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.unit.Density

@Immutable
internal class StubProgressiveBlurEffect(
    private val radius: BlurRadiusSpec,
    private val size: Size,
    private val density: Density,
    private val edgeTreatment: TileMode,
    private val hasStructuralEquality: Boolean = true,
) : RenderEffect() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StubProgressiveBlurEffect) return false

        // A shader-based radius wraps mutable user-owned shader state (uniform updates).
        if (!hasStructuralEquality || !other.hasStructuralEquality) return false

        if (radius != other.radius) return false
        if (size != other.size) return false
        if (density != other.density) return false
        if (edgeTreatment != other.edgeTreatment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = radius.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + density.hashCode()
        result = 31 * result + edgeTreatment.hashCode()
        return result
    }

    override fun toString(): String {
        return "ProgressiveBlurEffect(radius=$radius, size=$size, density=$density, " +
            "edgeTreatment=$edgeTreatment)"
    }
}

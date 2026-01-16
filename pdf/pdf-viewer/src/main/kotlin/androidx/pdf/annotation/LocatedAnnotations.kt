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

package androidx.pdf.annotation

import androidx.annotation.RestrictTo

/**
 * Represents the result of a successful hit test on PDF annotations.
 *
 * @property x The x-coordinate of the touch event in view coordinates.
 * @property y The y-coordinate of the touch event in view coordinates.
 * @property annotations The list of [KeyedPdfAnnotation] objects found at the (x, y) location,
 *   typically ordered by visual stacking order (Z-index) (top-bottom).
 */
// TODO: Revisit the class parameters based on requirements.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LocatedAnnotations(
    public val x: Float,
    public val y: Float,
    public val annotations: List<KeyedPdfAnnotation>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocatedAnnotations) return false

        if (x != other.x) return false
        if (y != other.y) return false

        if (annotations != other.annotations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + annotations.hashCode()
        return result
    }
}

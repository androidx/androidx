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

package androidx.glance.adaptive.core.templates

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo

/**
 * Displays an icon asset inside a Glance Adaptive template component.
 *
 * Create an instance with [TemplateIcon.fromResource]. Host renderers tint monochrome vector
 * drawables to match the active surface theme.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TemplateIcon
private constructor(
    /** Android drawable resource identifier backing this icon. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @get:DrawableRes public val resId: Int
) {
    init {
        require(resId != 0) { "Icon resource ID must not be 0." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemplateIcon) return false
        return resId == other.resId
    }

    override fun hashCode(): Int = resId

    override fun toString(): String = "TemplateIcon(resId=$resId)"

    public companion object {
        /**
         * Creates a [TemplateIcon] backed by an Android drawable resource.
         *
         * @param resId non-zero drawable resource ID (for example, `R.drawable.ic_steps`)
         * @return [TemplateIcon] referencing [resId]
         * @throws IllegalArgumentException if [resId] is `0`
         */
        public fun fromResource(@DrawableRes resId: Int): TemplateIcon = TemplateIcon(resId)
    }
}

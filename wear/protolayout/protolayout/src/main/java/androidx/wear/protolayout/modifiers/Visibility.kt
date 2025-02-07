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

package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.wear.protolayout.TypeBuilders.BoolProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.expression.RequiresSchemaVersion

/**
 * Adds a modifier to specify the visibility of the element. A hidden element still consume space in
 * the layout, but will not render any contents, nor will any of its children render any contents.
 *
 * Note that hidden elements won't receive input events.
 *
 * @param staticVisibility The static value for visibility. This value will be used if
 *   [dynamicVisibility] is null, or if can't be resolved.
 * @param dynamicVisibility The dynamic value for visibility. This can be used to change the
 *   visibility of the element dynamically (without changing the layout definition).
 */
@RequiresSchemaVersion(major = 1, minor = 300)
@ProtoLayoutExperimental
fun LayoutModifier.visibility(
    staticVisibility: Boolean,
    dynamicVisibility: DynamicBool? = null
): LayoutModifier = this then BaseVisibilityElement(staticVisibility, dynamicVisibility)

@RequiresSchemaVersion(major = 1, minor = 300)
internal class BaseVisibilityElement(
    val visibility: Boolean,
    val dynamicVisibility: DynamicBool? = null
) : LayoutModifier.Element {
    @SuppressLint("ProtoLayoutMinSchema")
    fun mergeTo(initial: BoolProp.Builder?): BoolProp.Builder =
        (initial ?: BoolProp.Builder(visibility)).apply {
            dynamicVisibility?.let { setDynamicValue(it) }
        }
}

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
import androidx.annotation.FloatRange
import androidx.wear.protolayout.TypeBuilders.FloatProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.RequiresSchemaVersion

/**
 * Adds a modifier to specify the opacity of the element with a value from 0 to 1, where 0 means the
 * element is completely transparent and 1 means the element is completely opaque.
 *
 * @param staticValue The static value for opacity. This value will be used if [dynamicValue] is
 *   null, or if can't be resolved.
 * @param dynamicValue The dynamic value for opacity. This can be used to change the opacity of the
 *   element dynamically (without changing the layout definition). To create a smooth transition for
 *   the dynamic change, you can use one of [DynamicFloat.animate] methods.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.opacity(
    @FloatRange(from = 0.0, to = 1.0) staticValue: Float,
    dynamicValue: DynamicFloat? = null
): LayoutModifier = this then BaseOpacityElement(staticValue, dynamicValue)

@RequiresSchemaVersion(major = 1, minor = 400)
internal class BaseOpacityElement(val staticValue: Float, val dynamicValue: DynamicFloat? = null) :
    LayoutModifier.Element {
    @SuppressLint("ProtoLayoutMinSchema")
    fun mergeTo(initial: FloatProp.Builder?): FloatProp.Builder =
        (initial ?: FloatProp.Builder(staticValue)).apply {
            dynamicValue?.let { setDynamicValue(it) }
        }
}

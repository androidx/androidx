/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

/**
 * A [CurvedModifier] that provides data to the parent layout.
 * The parent data is commonly used to inform the parent how the child Layout should be measured
 * and positioned.
 *
 * @param modifyParentData provides a parentData, given the parentData provided through the
 * modifier's chain.
 */
public fun CurvedModifier.parentDataModifier(
    modifyParentData: (Any?) -> Any?
) = this.then { child -> ParentDataWrapper(child, modifyParentData) }

/**
 * Size the element's proportional to its [weight] relative to other weighted sibling
 * elements in the container (this will be the height in a [curvedColumn] and the width in a
 * [curvedRow]). The parent will divide the space remaining after measuring unweighted child
 * elements and distribute it according to this weight.
 *
 * Example usage:
 * @sample androidx.wear.compose.foundation.samples.CurvedWeight
 *
 * @param weight The proportional size to give to this element, as related to the total of
 * all weighted siblings. Must be positive.
 */
public fun CurvedModifier.weight(
    /* @FloatRange(from = 0f, fromInclusive = false) */
    weight: Float
) = parentDataModifier { parentData ->
    require(weight > 0f) { "Weights must be positive." }
    ((parentData as? CurvedScopeParentData) ?: CurvedScopeParentData()).also {
        it.weight = weight
    }
}

internal class ParentDataWrapper(
    child: CurvedChild,
    val modifyParentData: (Any?) -> Any?
) : BaseCurvedChildWrapper(child) {
    override fun computeParentData(): Any? = modifyParentData(wrapped.computeParentData())
}

internal data class CurvedScopeParentData(var weight: Float = 0f)

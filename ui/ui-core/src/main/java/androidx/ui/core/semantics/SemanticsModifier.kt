/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.semantics

import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.semantics.SemanticsPropertyReceiver

/**
 * A [Modifier.Element] that adds semantics key/value for use in testing,
 * accessibility, and similar use cases.
 */
interface SemanticsModifier : Modifier.Element {
    /**
     * The unique id of this semantics.  Should be generated from SemanticsNode.generateNewId().
     */
    val id: Int

    /**
     * If true, then the semantics modifier applies to the first layout node below it in the tree,
     * not the composable the modifier is applied to.  This is for use by legacy non-modifier-style
     * semantics and is planned to be removed (with the behavior 'false' made universal).
     */
    val applyToChildLayoutNode: Boolean

    /**
     * The SemanticsConfiguration holds substantive data, especially a list of key/value pairs
     * such as (label -> "buttonName").
     */
    val semanticsConfiguration: SemanticsConfiguration
}

internal class SemanticsModifierCore(
    override val id: Int,
    override val applyToChildLayoutNode: Boolean,
    mergeAllDescendants: Boolean,
    properties: (SemanticsPropertyReceiver.() -> Unit)?
) : SemanticsModifier {
    override val semanticsConfiguration: SemanticsConfiguration =
        SemanticsConfiguration().also {
            it.isMergingSemanticsOfDescendants = mergeAllDescendants

            properties?.invoke(it)
        }
}

/**
 * Add semantics key/value for use in testing, accessibility, and similar use cases.
 *
 * @param applyToChildLayoutNode If true, then the semantics modifier applies to the first layout
 * node below it in the tree, not the composable the modifier is applied to.  This is for use by
 * legacy non-modifier-style semantics and is planned to be removed (with the behavior 'false'
 * made universal).
 * @param mergeAllDescendants Whether the semantic information provided by the owning component and
 * all of its descendants should be treated as one logical entity.
 * @param properties properties to add to the semantics. [SemanticsPropertyReceiver] will be
 * provided in the scope to allow access for common properties and its values.
 */
fun Modifier.semantics(
    applyToChildLayoutNode: Boolean = false,
    mergeAllDescendants: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit)? = null
): Modifier = composed {
    val id = remember { SemanticsNode.generateNewId() }
    SemanticsModifierCore(id, applyToChildLayoutNode, mergeAllDescendants, properties)
}

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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
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
     * The SemanticsConfiguration holds substantive data, especially a list of key/value pairs
     * such as (label -> "buttonName").
     */
    val semanticsConfiguration: SemanticsConfiguration
}

internal class SemanticsModifierCore(
    override val id: Int,
    container: Boolean,
    mergeAllDescendants: Boolean,
    properties: (SemanticsPropertyReceiver.() -> Unit)?
) : SemanticsModifier {
    override val semanticsConfiguration: SemanticsConfiguration =
        SemanticsConfiguration().also {
            require(!mergeAllDescendants || container) {
                "Attempting to set mergeAllDescendants to true on a configuration" +
                        " that is not a semantic boundary (container must be true)"
            }

            it.isSemanticBoundary = container
            it.isMergingSemanticsOfDescendants = mergeAllDescendants

            properties?.invoke(it)
        }
}

@Deprecated("This modifier is for use in the initial stages of the semantics modifier transition." +
    " It should not be used directly.")
@Composable
fun Modifier.semanticsCore(
    container: Boolean = false,
    mergeAllDescendants: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit)? = null
): SemanticsModifier {
    val id = remember { SemanticsNode.generateNewId() }

    return SemanticsModifierCore(id, container, mergeAllDescendants, properties)
}

/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.node

import androidx.xr.compose.subspace.layout.SubspaceModifier

/**
 * Modifier elements manage an instance of a particular [SubspaceModifier.Node] implementation. A
 * given [SubspaceModifier.Node] implementation can only be used when a
 * [SubspaceModifierNodeElement], which creates and updates that implementation, is applied to a
 * layout.
 *
 * A [SubspaceModifierNodeElement] should be very lightweight, and do little more than hold the
 * information necessary to create and maintain an instance of the associated
 * [SubspaceModifier.Node] type.
 *
 * @param N The type of node that this element creates and updates.
 */
public abstract class SubspaceModifierNodeElement<N : SubspaceModifier.Node> : SubspaceModifier {
    /**
     * This will be called the first time the modifier is applied to the layout and it should
     * construct and return the corresponding [SubspaceModifier.Node] instance.
     */
    public abstract fun create(): N

    /**
     * Called when a modifier is applied to a layout whose inputs have changed from the previous
     * application. This function will have the current node instance passed in as a parameter, and
     * it is expected that the node will be brought up to date.
     */
    public abstract fun update(node: N)

    /**
     * Require hashCode() to be implemented. Using a data class is sufficient. Singletons and
     * modifiers with no parameters may implement this function by returning an arbitrary constant.
     */
    public abstract override fun hashCode(): Int

    /**
     * Require equals() to be implemented. Using a data class is sufficient. Singletons may
     * implement this function with referential equality (`this === other`). Modifiers with no
     * inputs may implement this function by checking the type of the other object.
     */
    public abstract override fun equals(other: Any?): Boolean
}

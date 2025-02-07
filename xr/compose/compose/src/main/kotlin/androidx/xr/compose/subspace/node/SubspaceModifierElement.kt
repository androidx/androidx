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
 * An abstract class for [SubspaceModifier] that creates and updates [SubspaceModifier.Node]
 * instances.
 *
 * @param N The type of node that this element creates and updates.
 */
public abstract class SubspaceModifierElement<N : SubspaceModifier.Node> : SubspaceModifier {
    /**
     * Creates a new node of type [SubspaceModifier.Node].
     *
     * @return A new node of type [SubspaceModifier.Node].
     */
    public abstract fun create(): N

    /**
     * Updates the given [SubspaceModifier.Node] with the current state of this element.
     *
     * @param node The [SubspaceModifier.Node] to update.
     */
    public abstract fun update(node: N)

    /**
     * Returns a hash code value for this element.
     *
     * @return A hash code value for this element.
     */
    public abstract override fun hashCode(): Int

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param other The other object to compare to.
     * @return `true` if this object is the same as the [other] argument; `false` otherwise.
     */
    public abstract override fun equals(other: Any?): Boolean
}

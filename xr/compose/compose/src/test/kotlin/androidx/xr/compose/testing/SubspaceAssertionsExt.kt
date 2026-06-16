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

package androidx.xr.compose.testing

import androidx.xr.scenecore.Entity
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * Asserts that the Entity associated with the current Subspace layout node is a descendant of the
 * [expectedAncestor] Entity.
 *
 * @param expectedAncestor the ancestor entity that is expected to be found in the current
 *   hierarchy.
 * @throws AssertionError if no entity is found or the expected ancestor is not in the current
 *   entities' hierarchy.
 */
// TODO(b/510822215): Deprecate assertEntityIsDescendantOf in favor of superior semantics matchers
@CanIgnoreReturnValue
public fun SubspaceSemanticsNodeInteraction.assertEntityIsDescendantOf(
    expectedAncestor: Entity
): SubspaceSemanticsNodeInteraction {
    val entity =
        fetchSemanticsNode().semanticsEntity
            ?: throw AssertionError("Did not find an associated entity for $this.")

    var current: Entity? = entity
    while (current != null) {
        if (current == expectedAncestor) {
            return this // Found the ancestor
        }
        current = current.parent
    }
    throw AssertionError(
        "Entity $entity of $this is not a descendant of the expected ancestor $expectedAncestor."
    )
}

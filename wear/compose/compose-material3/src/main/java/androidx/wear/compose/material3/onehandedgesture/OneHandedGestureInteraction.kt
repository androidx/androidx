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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.foundation.interaction.Interaction

// An interface, not a sealed class, to allow adding new types here in a safe way
// (and not break exhaustive when clauses)
/**
 * An interaction related to one-handed gesture events.
 *
 * @see androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
 * @see Indicate
 */
public interface OneHandedGestureInteraction : Interaction {
    /**
     * An interaction representing an indication event, used to display a visual indicator for the
     * given gesture action.
     *
     * @property action The specific [GestureAction] to be visually represented.
     * @property key The unique identifier associated with this gesture instance.
     */
    public class Indicate(public val action: GestureAction, public val key: String) :
        OneHandedGestureInteraction {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other !is Indicate) return false

            if (action != other.action) return false
            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            var result = action.hashCode()
            result = 31 * result + key.hashCode()
            return result
        }
    }
}

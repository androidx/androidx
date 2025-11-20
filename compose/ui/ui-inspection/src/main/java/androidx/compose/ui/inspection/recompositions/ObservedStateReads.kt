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

package androidx.compose.ui.inspection.recompositions

import androidx.collection.MutableIntSet
import androidx.collection.mutableIntSetOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList

/** A single recorded state read by a state variable for a composable that was recomposed. */
class StateReadRecord(
    // The value of the state variable.
    val value: Any?,
    // The hashCode of the state variable.
    val valueInstanceHash: Int,
    // True, if this state variable was invalidated before the recomposition happened.
    val invalidated: Boolean,
    // The exception used for the purpose of holding the stack trace of the state read.
    // We store Exception instead of the stack trace directly to limit the memory impact of
    // storing many strings.
    val trace: Exception,
)

/** Invalidations and state reads for a composable that was recomposed once. */
data class ObservedStateReads(
    // The [StateRead]s recorded for this recomposition.
    val reads: MutableList<StateReadRecord> = mutableListOf(),
    // The valueInstance (hashCode of state variable) that were invalidated.
    private var invalidations: MutableIntSet? = null,
) {
    fun addStateRead(value: Any?, trace: Exception) {
        // Use [Snapshot.withoutReadObservation] to avoid another callback when we read
        // the value.
        val currentValue =
            Snapshot.withoutReadObservation {
                (value as? MutableState<*>)?.value
                    ?: (value as? SnapshotStateList<*>)?.toList()?.toList()
            }
        val valueInstanceHash = System.identityHashCode(value)
        val invalidated = wasInvalidated(valueInstanceHash)
        reads.add(StateReadRecord(currentValue, valueInstanceHash, invalidated, trace))
    }

    fun addInvalidation(value: Any?) {
        val valueInstanceHash = value?.let { System.identityHashCode(it) } ?: return
        if (invalidations == null) {
            invalidations = mutableIntSetOf()
        }
        invalidations?.add(valueInstanceHash)
    }

    private fun wasInvalidated(valueInstance: Int): Boolean =
        invalidations?.contains(valueInstance) ?: false

    fun clearInvalidations() {
        invalidations = null
    }
}

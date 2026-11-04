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

import androidx.collection.IntSet
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.inspection.inspector.RawParameter

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

/**
 * Observations for a recomposition.
 *
 * @param reads the state reads for this recomposition
 * @param parameterChanges the parameter name and value that changed since the last recomposition.
 */
data class ObservedStateReads(
    private val reads: MutableList<StateReadRecord> = mutableListOf(),
    private var parameterChanges: List<RawParameter> = emptyList(),
) {
    val readSize: Int
        get() = reads.size

    val hasParameterChanges: Boolean
        get() = parameterChanges.isNotEmpty()

    fun toObservedReadResult(recomposition: Int) =
        ObservedReadResult(recomposition, reads, parameterChanges)

    /**
     * Add a state read.
     *
     * @param value the state variable read
     * @param trace the stack trace of the read
     * @param invalidations the identityHashCode of the state variables that invalidated this
     *   recomposition
     */
    fun addStateRead(value: Any?, trace: Exception, invalidations: IntSet?) {
        // Use [Snapshot.withoutReadObservation] to avoid another callback when we read
        // the value.
        val currentValue = Snapshot.withoutReadObservation {
            (value as? MutableState<*>)?.value
                // Note: SnapshotStateList.toList() returns the internal persistent list of the
                // current snapshot. Copy it to a regular list with the stdlib toList().
                ?: (value as? SnapshotStateList<*>)?.toList()?.toList()
        }
        val valueInstanceHash = System.identityHashCode(value)
        val invalidated = invalidations?.contains(valueInstanceHash) ?: false
        reads.add(StateReadRecord(currentValue, valueInstanceHash, invalidated, trace))
    }

    fun addParameterChanges(parameterChanges: List<RawParameter>) {
        this.parameterChanges = parameterChanges
    }
}

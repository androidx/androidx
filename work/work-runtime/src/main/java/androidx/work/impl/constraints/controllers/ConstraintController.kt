/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.controllers

import androidx.work.impl.constraints.ConstraintListener
import androidx.work.impl.constraints.trackers.ConstraintTracker
import androidx.work.impl.model.WorkSpec

/**
 * A controller for a particular constraint.
 *
 * @param <T> the constraint data type managed by this controller.
 */
abstract class ConstraintController<T> internal constructor(
    private val tracker: ConstraintTracker<T>
) : ConstraintListener<T> {
    /**
     * A callback for when a constraint changes.
     */
    interface OnConstraintUpdatedCallback {
        /**
         * Called when a constraint is met.
         *
         * @param workSpecs A list of [WorkSpec] IDs that may have become eligible to run
         */
        fun onConstraintMet(workSpecs: List<WorkSpec>)

        /**
         * Called when a constraint is not met.
         *
         * @param workSpecs A list of [WorkSpec] IDs that have become ineligible to run
         */
        fun onConstraintNotMet(workSpecs: List<WorkSpec>)
    }

    private val matchingWorkSpecs = mutableListOf<WorkSpec>()
    private val matchingWorkSpecIds = mutableListOf<String>()
    private var currentValue: T? = null

    /**
     * Sets the callback to inform when constraints change.  This callback is also triggered the
     * first time it is set.
     */
    var callback: OnConstraintUpdatedCallback? = null
        set(value) {
            if (field !== value) {
                field = value
                updateCallback(value, currentValue)
            }
        }

    abstract fun hasConstraint(workSpec: WorkSpec): Boolean
    abstract fun isConstrained(value: T): Boolean

    /**
     * Replaces the list of [WorkSpec]s to monitor constraints for.
     *
     * @param workSpecs A list of [WorkSpec]s to monitor constraints for
     */
    fun replace(workSpecs: Iterable<WorkSpec>) {
        matchingWorkSpecs.clear()
        matchingWorkSpecIds.clear()
        workSpecs.filterTo(matchingWorkSpecs) { hasConstraint(it) }
        matchingWorkSpecs.mapTo(matchingWorkSpecIds) { it.id }

        if (matchingWorkSpecs.isEmpty()) {
            tracker.removeListener(this)
        } else {
            tracker.addListener(this)
        }
        updateCallback(callback, currentValue)
    }

    /**
     * Clears all tracked [WorkSpec]s.
     */
    fun reset() {
        if (matchingWorkSpecs.isNotEmpty()) {
            matchingWorkSpecs.clear()
            tracker.removeListener(this)
        }
    }

    /**
     * Determines if a particular [WorkSpec] is constrained. It is constrained if it is
     * tracked by this controller, and the controller constraint was set, but not satisfied.
     *
     * @param workSpecId The ID of the [WorkSpec] to check if it is constrained.
     * @return `true` if the [WorkSpec] is considered constrained
     */
    fun isWorkSpecConstrained(workSpecId: String): Boolean {
        // TODO: unify `null` treatment here and in updateCallback, because
        // here it is considered as not constrained and but in updateCallback as constrained.
        val value = currentValue
        return (value != null && isConstrained(value) && workSpecId in matchingWorkSpecIds)
    }

    private fun updateCallback(callback: OnConstraintUpdatedCallback?, currentValue: T?) {
        // We pass copies of references (callback, currentValue) to updateCallback because public
        // APIs on ConstraintController may be called from any thread, and onConstraintChanged() is
        // called from the main thread.
        if (matchingWorkSpecs.isEmpty() || callback == null) {
            return
        }
        if (currentValue == null || isConstrained(currentValue)) {
            callback.onConstraintNotMet(matchingWorkSpecs)
        } else {
            callback.onConstraintMet(matchingWorkSpecs)
        }
    }

    override fun onConstraintChanged(newValue: T) {
        currentValue = newValue
        updateCallback(callback, currentValue)
    }
}
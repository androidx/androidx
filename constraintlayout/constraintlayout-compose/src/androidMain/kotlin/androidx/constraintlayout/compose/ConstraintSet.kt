/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.layout.Measurable
import androidx.constraintlayout.core.state.Transition

@JvmDefaultWithCompatibility
/**
 * Immutable description of the constraints used to layout the children of a [ConstraintLayout].
 */
@Immutable
interface ConstraintSet {
    /**
     * Applies the [ConstraintSet] to a state.
     */
    fun applyTo(state: State, measurables: List<Measurable>)

    fun override(name: String, value: Float) = this
    fun applyTo(transition: Transition, type: Int) {
        // nothing here, used in MotionLayout
    }

    fun isDirty(measurables: List<Measurable>): Boolean = true
}

@JvmDefaultWithCompatibility
@Immutable
internal interface DerivedConstraintSet : ConstraintSet {
    /**
     * [ConstraintSet] that this instance will derive its constraints from.
     *
     * This means that the constraints from [extendFrom] will be applied before the constraints of
     * this [DerivedConstraintSet] instance.
     */
    val extendFrom: ConstraintSet?

    override fun applyTo(state: State, measurables: List<Measurable>) {
        extendFrom?.applyTo(state, measurables)
        applyToState(state)
    }

    /**
     * Inheritors should implement this function so that derived constraints are applied properly.
     */
    fun applyToState(state: State)
}
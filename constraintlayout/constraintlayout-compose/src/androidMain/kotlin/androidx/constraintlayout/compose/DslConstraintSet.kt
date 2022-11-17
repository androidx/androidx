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

/**
 * [ConstraintSet] implementation used in the kotlin DSL.
 */
@Immutable
internal class DslConstraintSet constructor(
    val description: ConstraintSetScope.() -> Unit,
    extendFrom: ConstraintSet? = null
) : DerivedConstraintSet {
    internal val scope: ConstraintSetScope =
        ConstraintSetScope(
            extendFrom = (extendFrom as? DslConstraintSet)?.scope?.containerObject
        ).apply(description)

    override val extendFrom: ConstraintSet? = null // Not needed

    override fun applyToState(state: State) {
        scope.applyTo(state)
    }

    override fun override(name: String, value: Float): ConstraintSet {
        // nothing yet
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other is DslConstraintSet) {
            return scope == other.scope
        }
        return false
    }

    override fun hashCode(): Int {
        return scope.hashCode()
    }
}
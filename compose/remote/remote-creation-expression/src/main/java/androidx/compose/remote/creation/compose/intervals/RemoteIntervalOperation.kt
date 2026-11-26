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

package androidx.compose.remote.creation.compose.intervals

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.BooleanOperationKey
import androidx.compose.remote.creation.compose.state.RemoteFloatOperationKey
import androidx.compose.remote.creation.compose.state.RemoteIntOperationKey
import androidx.compose.remote.creation.compose.state.RemoteOperation
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.creation.compose.state.StringOperationKey
import androidx.compose.remote.document.intervals.IntervalComparison

/**
 * Optional extension of [RemoteOperation] for custom external operations that participate in
 * interval analysis, condition refinement, and interval-guided expression simplification.
 *
 * Built-in `Remote*` operations stay decoupled from interval analysis and have their semantics
 * supplied externally by isolated `Remote*IntervalSemantics` handlers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteIntervalOperation : RemoteOperation {
    /**
     * Reports whether interval arithmetic on this operation can turn an unbounded float operand
     * (which may represent `NaN` or `Infinity` at runtime) into a bounded or half-bounded interval
     * even though the runtime operation may still yield `NaN` or non-finite values (for example,
     * `UNBOUNDED * 0f == [0, 0]`, `clamp`, `lerp`, `min`, `max`, `sin`, or `exp`).
     */
    public val requiresBoundedFloatOperands: Boolean
        get() = false

    /**
     * Relational [IntervalComparison] evaluated by this two-operand comparison operation, or `null`
     * if this operation is not a relational comparison.
     */
    public val comparisonCondition: IntervalComparison?
        get() = null

    /**
     * Relational [IntervalComparison] guarded by this 4-arg `selectIf*` operation, or `null` if
     * this operation is not a relational select.
     */
    public val selectCondition: IntervalComparison?
        get() = null

    /**
     * Domain ([RemoteValueKind]) in which this 4-arg `selectIf*` operation compares its first two
     * arguments ([args]). Defaults to [RemoteValueKind.OTHER] so cross-type selects do not silently
     * conflate their branch [resultKind] with their comparison domain.
     */
    public fun selectComparisonKind(args: List<RemoteStateCacheKey>): RemoteValueKind =
        RemoteValueKind.OTHER

    /** Reports whether this operation is a 3-arg boolean-condition `select(ifTrue, ifFalse)`. */
    public val isBooleanSelect: Boolean
        get() = false
}

/**
 * External interval-semantics adapter for a family of [RemoteOperation]s, allowing interval
 * analysis code to stay isolated from `Remote*` implementation files.
 */
internal interface RemoteOperationIntervalSemantics {
    fun requiresBoundedFloatOperands(op: Any): Boolean = false

    fun comparisonCondition(op: Any): IntervalComparison? = null

    fun selectCondition(op: Any): IntervalComparison? = null

    fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        RemoteValueKind.OTHER

    fun isBooleanSelect(op: Any): Boolean = false
}

internal fun intervalSemanticsFor(op: Any): RemoteOperationIntervalSemantics? =
    when (op) {
        is RemoteFloatOperationKey -> RemoteFloatIntervalSemantics
        is RemoteIntOperationKey -> RemoteIntIntervalSemantics
        is BooleanOperationKey -> RemoteBooleanIntervalSemantics
        is StringOperationKey -> RemoteStringIntervalSemantics
        is RemoteIntervalOperation -> CustomRemoteIntervalOperationSemantics
        else -> null
    }

private object CustomRemoteIntervalOperationSemantics : RemoteOperationIntervalSemantics {
    override fun requiresBoundedFloatOperands(op: Any): Boolean =
        (op as RemoteIntervalOperation).requiresBoundedFloatOperands

    override fun comparisonCondition(op: Any): IntervalComparison? =
        (op as RemoteIntervalOperation).comparisonCondition

    override fun selectCondition(op: Any): IntervalComparison? =
        (op as RemoteIntervalOperation).selectCondition

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        (op as RemoteIntervalOperation).selectComparisonKind(args)

    override fun isBooleanSelect(op: Any): Boolean = (op as RemoteIntervalOperation).isBooleanSelect
}

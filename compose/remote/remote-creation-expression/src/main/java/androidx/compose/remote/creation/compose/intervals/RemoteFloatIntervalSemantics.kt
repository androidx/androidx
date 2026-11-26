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

import androidx.compose.remote.creation.compose.state.RemoteFloatOperationKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.document.intervals.IntervalComparison

/** External interval semantics for [RemoteFloatOperationKey]. */
internal object RemoteFloatIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun requiresBoundedFloatOperands(op: Any): Boolean =
        when (op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.Times,
            RemoteFloatOperationKey.Div,
            RemoteFloatOperationKey.Rem,
            RemoteFloatOperationKey.Mad,
            RemoteFloatOperationKey.Clamp,
            RemoteFloatOperationKey.Lerp,
            RemoteFloatOperationKey.Min,
            RemoteFloatOperationKey.Max,
            RemoteFloatOperationKey.Abs,
            RemoteFloatOperationKey.Round,
            RemoteFloatOperationKey.Sin,
            RemoteFloatOperationKey.Cos,
            RemoteFloatOperationKey.Atan,
            RemoteFloatOperationKey.Atan2,
            RemoteFloatOperationKey.Sign,
            RemoteFloatOperationKey.CopySign,
            RemoteFloatOperationKey.Exp,
            RemoteFloatOperationKey.Cubic,
            RemoteFloatOperationKey.RandInRange -> true
            else -> false
        }

    override fun comparisonCondition(op: Any): IntervalComparison? =
        when (op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.CompareEQ -> IntervalComparison.EQ
            RemoteFloatOperationKey.CompareNE -> IntervalComparison.NEQ
            RemoteFloatOperationKey.CompareLT -> IntervalComparison.LT
            RemoteFloatOperationKey.CompareLE -> IntervalComparison.LTE
            RemoteFloatOperationKey.CompareGT -> IntervalComparison.GT
            RemoteFloatOperationKey.CompareGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectCondition(op: Any): IntervalComparison? =
        when (op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.SelectIfLT -> IntervalComparison.LT
            RemoteFloatOperationKey.SelectIfLE -> IntervalComparison.LTE
            RemoteFloatOperationKey.SelectIfGT -> IntervalComparison.GT
            RemoteFloatOperationKey.SelectIfGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        if (selectCondition(op) != null) RemoteValueKind.FLOAT else RemoteValueKind.OTHER
}

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

import androidx.compose.remote.creation.compose.state.RemoteIntOperationKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.document.intervals.IntervalComparison

/** External interval semantics for [RemoteIntOperationKey]. */
internal object RemoteIntIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun comparisonCondition(op: Any): IntervalComparison? =
        when (op as RemoteIntOperationKey) {
            RemoteIntOperationKey.CompareEQ -> IntervalComparison.EQ
            RemoteIntOperationKey.CompareNE -> IntervalComparison.NEQ
            RemoteIntOperationKey.CompareLT -> IntervalComparison.LT
            RemoteIntOperationKey.CompareLE -> IntervalComparison.LTE
            RemoteIntOperationKey.CompareGT -> IntervalComparison.GT
            RemoteIntOperationKey.CompareGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectCondition(op: Any): IntervalComparison? =
        when (op as RemoteIntOperationKey) {
            RemoteIntOperationKey.SelectIfLT -> IntervalComparison.LT
            RemoteIntOperationKey.SelectIfLE -> IntervalComparison.LTE
            RemoteIntOperationKey.SelectIfGT -> IntervalComparison.GT
            RemoteIntOperationKey.SelectIfGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        if (selectCondition(op) != null) RemoteValueKind.INT else RemoteValueKind.OTHER
}

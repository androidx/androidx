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

import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.creation.compose.state.StringOperationKey
import androidx.compose.remote.document.intervals.IntervalComparison

/** External interval semantics for [StringOperationKey]. */
internal object RemoteStringIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun selectCondition(op: Any): IntervalComparison? =
        when (op as StringOperationKey) {
            StringOperationKey.SelectIfLT -> IntervalComparison.LT
            StringOperationKey.SelectIfLE -> IntervalComparison.LTE
            StringOperationKey.SelectIfGT -> IntervalComparison.GT
            StringOperationKey.SelectIfGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind {
        val stringOp = op as StringOperationKey
        if (selectCondition(stringOp) == null || args.size < 2) return RemoteValueKind.OTHER
        val k0 = args[0].getValueKind()
        val k1 = args[1].getValueKind()
        return when {
            (k0.isIntEncoded || k1.isIntEncoded) &&
                k0 != RemoteValueKind.FLOAT &&
                k1 != RemoteValueKind.FLOAT -> RemoteValueKind.INT
            k0 == RemoteValueKind.FLOAT || k1 == RemoteValueKind.FLOAT -> RemoteValueKind.FLOAT
            else -> RemoteValueKind.OTHER
        }
    }
}

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

import androidx.compose.remote.creation.compose.state.FloatArrayOperationKey
import androidx.compose.remote.creation.compose.state.IntArrayOperationKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.StringArrayOperationKey
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.SlotInterval
import androidx.compose.remote.document.intervals.StringSummary

/** External interval semantics for [FloatArrayOperationKey]. */
internal object RemoteFloatArrayIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval {
        if (key.op != FloatArrayOperationKey.Get) return FloatInterval.UNBOUNDED
        val args = key.args
        if (args.size < 2) return FloatInterval.UNBOUNDED
        val arrayKey = args[0]
        val indexKey = args[1]
        val isIntIndex = indexKey.getValueKind().isIntEncoded
        val indexInt =
            if (isIntIndex) {
                evaluator.evalIntKey(indexKey)
            } else {
                SlotInterval.fromFloat(evaluator.evalFloatKey(indexKey)).toIntInterval()
            }
        if (arrayKey is RemoteStateIdKey) {
            val indexFloat =
                if (isIntIndex) indexInt.toFloatInterval() else evaluator.evalFloatKey(indexKey)
            return evaluator.environment.floatArrayElement(arrayKey.id, indexFloat)
        }
        if (arrayKey is RemoteOperationCacheKey && arrayKey.op == FloatArrayOperationKey.Create) {
            return evaluator.unionFloatArrayElements(arrayKey.args, indexInt)
        }
        return FloatInterval.UNBOUNDED
    }
}

/** External interval semantics for [IntArrayOperationKey]. */
internal object RemoteIntArrayIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval {
        if (key.op != IntArrayOperationKey.Get) return IntInterval.UNBOUNDED
        val args = key.args
        if (args.size < 2) return IntInterval.UNBOUNDED
        val arrayKey = args[0]
        val indexKey = args[1]
        val idxInterval =
            if (indexKey.getValueKind().isIntEncoded) {
                evaluator.evalIntKey(indexKey)
            } else {
                SlotInterval.fromFloat(evaluator.evalFloatKey(indexKey)).toIntInterval()
            }
        if (arrayKey is RemoteOperationCacheKey && arrayKey.op == IntArrayOperationKey.Create) {
            return evaluator.unionIntArrayElements(arrayKey.args, idxInterval)
        }
        return IntInterval.UNBOUNDED
    }
}

/** External interval semantics for [StringArrayOperationKey]. */
internal object RemoteStringArrayIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary {
        if (key.op != StringArrayOperationKey.Get) return StringSummary.UNKNOWN
        val args = key.args
        if (args.size < 2) return StringSummary.UNKNOWN
        val arrayKey = args[0]
        val idxInterval = evaluator.evalIntKey(args[1])
        if (arrayKey is RemoteOperationCacheKey && arrayKey.op == StringArrayOperationKey.Create) {
            return evaluator.unionStringArrayElements(arrayKey.args, idxInterval)
        }
        return StringSummary.UNKNOWN
    }
}

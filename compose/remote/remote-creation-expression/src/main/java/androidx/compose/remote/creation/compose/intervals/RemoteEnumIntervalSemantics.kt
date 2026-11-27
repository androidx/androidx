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

import androidx.compose.remote.creation.compose.state.EnumOperationKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.StringArrayOperationKey
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.StringSummary

/** External interval semantics for [EnumOperationKey]. */
internal object RemoteEnumIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval =
        when (key.op as EnumOperationKey) {
            EnumOperationKey.Ordinal,
            EnumOperationKey.RemoteEnum -> evaluator.evalIntKey(key).toFloatInterval()
            EnumOperationKey.ToString -> FloatInterval.UNBOUNDED
        }

    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval =
        when (key.op as EnumOperationKey) {
            EnumOperationKey.Ordinal,
            EnumOperationKey.RemoteEnum ->
                if (key.args.isNotEmpty()) {
                    evaluator.evalIntKey(key.args[0])
                } else {
                    IntInterval.UNBOUNDED
                }
            EnumOperationKey.ToString -> IntInterval.UNBOUNDED
        }

    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary {
        if (key.op != EnumOperationKey.ToString || key.args.size < 2) {
            return StringSummary.UNKNOWN
        }
        val idxInterval = evaluator.evalIntKey(key.args[0])
        val arrayKey = key.args[1]
        if (arrayKey is RemoteOperationCacheKey && arrayKey.op == StringArrayOperationKey.Create) {
            return evaluator.unionStringArrayElements(arrayKey.args, idxInterval)
        }
        return StringSummary.UNKNOWN
    }

    override fun extractIntAffine(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? =
        when (key.op as EnumOperationKey) {
            EnumOperationKey.Ordinal,
            EnumOperationKey.RemoteEnum ->
                key.args.firstOrNull()?.let { evaluator.extractIntAffineForm(it, depth + 1) }
            EnumOperationKey.ToString -> null
        }

    override fun unwrapAliasKey(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): RemoteStateCacheKey? =
        when (key.op as EnumOperationKey) {
            EnumOperationKey.Ordinal,
            EnumOperationKey.RemoteEnum -> key.args.firstOrNull()
            EnumOperationKey.ToString -> null
        }

    override fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? {
        val args = key.args
        return when (key.op as EnumOperationKey) {
            EnumOperationKey.Ordinal -> {
                if (!context.config.simplifyRangeMath) return null
                val wrapped = args.firstOrNull() as? RemoteOperationCacheKey
                if (
                    wrapped != null &&
                        wrapped.op == EnumOperationKey.RemoteEnum &&
                        wrapped.args.isNotEmpty()
                ) {
                    wrapped.args[0].getOrCreateValueOrNull()
                } else {
                    null
                }
            }
            EnumOperationKey.ToString -> {
                if (
                    !context.config.foldArrayGets ||
                        args.size < 2 ||
                        !context.hasBoundedFloatOperands(args[0])
                ) {
                    return null
                }
                val arrayKey = args[1] as? RemoteOperationCacheKey ?: return null
                if (arrayKey.op != StringArrayOperationKey.Create) return null
                val idxRange = context.evaluator.evalIntKey(args[0])
                if (idxRange.isPoint && idxRange.min in arrayKey.args.indices) {
                    arrayKey.args[idxRange.min].getOrCreateValueOrNull()
                } else {
                    null
                }
            }
            EnumOperationKey.RemoteEnum -> null
        }
    }
}

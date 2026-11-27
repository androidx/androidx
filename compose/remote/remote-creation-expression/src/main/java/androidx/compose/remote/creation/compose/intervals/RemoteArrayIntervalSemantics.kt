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
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteOperation
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.StringArrayOperationKey
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
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

    override fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? =
        simplifyLiteralArrayGet(
            key = key,
            context = context,
            getOp = FloatArrayOperationKey.Get,
            createOp = FloatArrayOperationKey.Create,
            allowFloatIndex = true,
            castElement = { it as? RemoteFloat },
            selectInt = { isOne, elem0, elem1 -> isOne.select(elem1, elem0) },
            selectFloat = { idxValue, elem0, elem1 -> selectIfLt(idxValue, 1f.rf, elem0, elem1) },
        )
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

    override fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? =
        simplifyLiteralArrayGet(
            key = key,
            context = context,
            getOp = IntArrayOperationKey.Get,
            createOp = IntArrayOperationKey.Create,
            allowFloatIndex = true,
            castElement = { it as? RemoteInt },
            selectInt = { isOne, elem0, elem1 -> isOne.select(elem1, elem0) },
            selectFloat = { idxValue, elem0, elem1 ->
                idxValue.isLessThan(1f.rf).select(elem0, elem1)
            },
        )
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

    override fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? =
        simplifyLiteralArrayGet(
            key = key,
            context = context,
            getOp = StringArrayOperationKey.Get,
            createOp = StringArrayOperationKey.Create,
            allowFloatIndex = false,
            castElement = { it as? RemoteString },
            selectInt = { isOne, elem0, elem1 -> isOne.select(elem1, elem0) },
        )
}

private fun <T : RemoteValue<*>> simplifyLiteralArrayGet(
    key: RemoteOperationCacheKey,
    context: IntervalOptimizationContext,
    getOp: RemoteOperation,
    createOp: RemoteOperation,
    allowFloatIndex: Boolean,
    castElement: (RemoteValue<*>) -> T?,
    selectInt: (RemoteBoolean, T, T) -> RemoteValue<*>,
    selectFloat: ((RemoteFloat, T, T) -> RemoteValue<*>)? = null,
): RemoteValue<*>? {
    if (key.op != getOp) return null
    val args = key.args
    if (args.size < 2 || !context.hasBoundedFloatOperands(args[1])) return null
    val arrayKey = args[0] as? RemoteOperationCacheKey ?: return null
    if (arrayKey.op != createOp) return null
    val elements = arrayKey.args
    val isIntIndex = !allowFloatIndex || args[1].getValueKind().isIntEncoded
    val rawFloatIndex = if (isIntIndex) null else context.evaluator.evalFloatKey(args[1])
    val idxInterval =
        if (isIntIndex) {
            context.evaluator.evalIntKey(args[1])
        } else {
            SlotInterval.fromFloat(rawFloatIndex!!).toIntInterval()
        }
    if (context.config.foldArrayGets && idxInterval.isPoint) {
        val idx = idxInterval.min
        if (idx in elements.indices) {
            return elements[idx].getOrCreateValueOrNull()
        }
    }
    if (
        context.config.lowerTwoElementArraysToSelect &&
            elements.size == 2 &&
            !idxInterval.isEmpty &&
            idxInterval.hasFiniteMin &&
            idxInterval.hasFiniteMax &&
            idxInterval.min >= 0 &&
            idxInterval.max <= 1
    ) {
        val elem0 = elements[0].getOrCreateValueOrNull()?.let(castElement) ?: return null
        val elem1 = elements[1].getOrCreateValueOrNull()?.let(castElement) ?: return null
        return when (val idxValue = args[1].getOrCreateValueOrNull()) {
            is RemoteInt -> selectInt(RemoteBoolean.fromRemoteInt(idxValue), elem0, elem1)
            is RemoteFloat -> {
                if (selectFloat == null) return null
                val fIdx = rawFloatIndex ?: context.evaluator.evalFloatKey(args[1])
                if (!fIdx.isEmpty && !fIdx.isUnbounded && fIdx.min >= 0f) {
                    selectFloat(idxValue, elem0, elem1)
                } else {
                    null
                }
            }
            else -> null
        }
    }
    return null
}

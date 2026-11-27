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

import androidx.compose.remote.creation.compose.state.ColorOperationKey
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.document.intervals.ColorInterval
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.StringAndColorIntervalOps

/** External interval semantics for [ColorOperationKey]. */
internal object RemoteColorIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval {
        if (key.op != ColorOperationKey.Component || key.args.size < 2) {
            return FloatInterval.UNBOUNDED
        }
        val component =
            ((key.args[1] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                ?: return FloatInterval.UNBOUNDED
        return StringAndColorIntervalOps.summarizeColorAttribute(
            evaluator.evalColorKey(key.args[0]),
            component,
        )
    }

    override fun evaluateColorInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): ColorInterval {
        val args = key.args
        return when (key.op as ColorOperationKey) {
            ColorOperationKey.FromArgb ->
                if (args.size >= 4) {
                    // FromArgb cacheKey stores arguments in (red, green, blue, alpha) order.
                    // ColorExpression.ARGB_MODE quantizes constant alpha to signed 16-bit fixed
                    // point with scale 1024: ((int) (alpha * 1024)) << 16, read back as / 1024f.
                    val constAlpha =
                        ((args[3] as? RemoteConstantCacheKey)?.value as? Number)?.toFloat()
                    val alphaInterval =
                        if (constAlpha != null && constAlpha.isFinite()) {
                            val quantized = (((constAlpha * 1024f).toInt() shl 16) shr 16) / 1024.0f
                            FloatInterval.point(quantized)
                        } else {
                            evaluator.evalFloatKey(args[3])
                        }
                    StringAndColorIntervalOps.summarizeColorArgb(
                        alpha = alphaInterval,
                        red = evaluator.evalFloatKey(args[0]),
                        green = evaluator.evalFloatKey(args[1]),
                        blue = evaluator.evalFloatKey(args[2]),
                    )
                } else {
                    ColorInterval.UNKNOWN
                }
            ColorOperationKey.FromHSV ->
                if (args.size >= 3) {
                    StringAndColorIntervalOps.summarizeColorHsv(
                        alpha = 255,
                        hue = evaluator.evalFloatKey(args[0]),
                        saturation = evaluator.evalFloatKey(args[1]),
                        value = evaluator.evalFloatKey(args[2]),
                    )
                } else {
                    ColorInterval.UNKNOWN
                }
            ColorOperationKey.FromAHSV ->
                if (args.size >= 4) {
                    val alphaInt =
                        ((args[0] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                            ?: return ColorInterval.UNKNOWN
                    StringAndColorIntervalOps.summarizeColorHsv(
                        alpha = alphaInt,
                        hue = evaluator.evalFloatKey(args[1]),
                        saturation = evaluator.evalFloatKey(args[2]),
                        value = evaluator.evalFloatKey(args[3]),
                    )
                } else {
                    ColorInterval.UNKNOWN
                }
            ColorOperationKey.Tween,
            ColorOperationKey.TweenInt ->
                if (args.size >= 3) {
                    StringAndColorIntervalOps.summarizeColorTween(
                        evaluator.evalColorKey(args[0]),
                        evaluator.evalColorKey(args[1]),
                        evaluator.evalFloatKey(args[2]),
                    )
                } else {
                    ColorInterval.UNKNOWN
                }
            ColorOperationKey.Multiply ->
                if (args.size >= 2) {
                    StringAndColorIntervalOps.summarizeColorMultiply(
                        evaluator.evalColorKey(args[0]),
                        evaluator.evalColorKey(args[1]),
                    )
                } else {
                    ColorInterval.UNKNOWN
                }
            ColorOperationKey.Component -> ColorInterval.UNKNOWN
        }
    }

    override fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? {
        if (
            (key.op != ColorOperationKey.Tween && key.op != ColorOperationKey.TweenInt) ||
                key.args.size < 3
        ) {
            return null
        }
        if (!context.config.pruneTernaries && !context.config.simplifyRangeMath) return null
        val args = key.args
        if (!context.hasBoundedFloatOperands(args[2])) return null
        val tRange = context.evaluator.evalFloatKey(args[2])
        fun resolveColorEndpoint(argKey: RemoteStateCacheKey): RemoteColor? =
            when (val resolved = argKey.getOrCreateValueOrNull()) {
                is RemoteColor -> resolved
                is RemoteInt -> resolved.constantValueOrNull?.let { RemoteColor(it) }
                else -> null
            }
        if (args[0] == args[1] && !tRange.isEmpty && !tRange.isUnbounded) {
            return resolveColorEndpoint(args[0])
        }
        if (!tRange.isPoint) return null
        return when (tRange.min) {
            0f -> resolveColorEndpoint(args[0])
            1f -> resolveColorEndpoint(args[1])
            else -> null
        }
    }
}

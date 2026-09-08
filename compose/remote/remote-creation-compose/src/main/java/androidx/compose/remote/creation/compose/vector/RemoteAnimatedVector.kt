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

package androidx.compose.remote.creation.compose.vector

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.easing.CubicEasing
import androidx.compose.remote.creation.RemoteComposeWriter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/** Procedural representation of an Animated Vector Drawable in Remote Compose. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteAnimatedVector(
    public val name: String,
    public val width: Float,
    public val height: Float,
    public val viewportWidth: Float,
    public val viewportHeight: Float,
    public val rootGroup: RemoteAnimatedVectorGroup,
    public val targets: List<RemoteAnimatedVectorTarget> = emptyList(),
) {
    public val totalDuration: Int = run {
        var maxDuration = 0
        for (i in targets.indices) {
            val animators = targets[i].animators
            for (j in animators.indices) {
                val d = animators[j].totalDuration
                if (d > maxDuration) {
                    maxDuration = d
                }
            }
        }
        maxDuration
    }

    public fun evaluate(progress: Float): RemoteVectorSnapshot {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val timeMs = (clampedProgress * totalDuration).toInt()

        // Build property override map: targetName -> (propertyName -> value)
        val overrides = mutableMapOf<String, MutableMap<String, Any?>>()
        for (i in targets.indices) {
            val target = targets[i]
            val targetOverrides = overrides.getOrPut(target.name) { mutableMapOf() }
            val animatorsByProp = mutableMapOf<String, MutableList<RemotePropertyAnimator>>()
            for (j in target.animators.indices) {
                val anim = target.animators[j]
                animatorsByProp.getOrPut(anim.propertyName) { mutableListOf() }.add(anim)
            }
            for ((prop, animList) in animatorsByProp) {
                if (animList.size == 1) {
                    val value = animList[0].evaluateAt(timeMs, clampedProgress)
                    if (value != null) {
                        targetOverrides[prop] = value
                    }
                } else {
                    var activeAnim = animList[0]
                    var foundActive = false
                    for (k in animList.indices) {
                        val anim = animList[k]
                        if (timeMs >= anim.startDelay) {
                            if (!foundActive || anim.startDelay >= activeAnim.startDelay) {
                                activeAnim = anim
                                foundActive = true
                            }
                        } else if (!foundActive && anim.startDelay < activeAnim.startDelay) {
                            activeAnim = anim
                        }
                    }
                    val value = activeAnim.evaluateAt(timeMs, clampedProgress)
                    if (value != null) {
                        targetOverrides[prop] = value
                    }
                }
            }
        }

        val evaluatedRoot = evaluateGroup(rootGroup, overrides)
        return RemoteVectorSnapshot(
            width = width,
            height = height,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            rootGroup = evaluatedRoot,
        )
    }

    public fun draw(writer: RemoteComposeWriter, progress: Float) {
        evaluate(progress).draw(writer)
    }

    private fun evaluateGroup(
        group: RemoteAnimatedVectorGroup,
        overrides: Map<String, Map<String, Any?>>,
    ): RemoteEvaluatedGroup {
        val groupOverrides = overrides[group.name] ?: emptyMap()

        val rotation = (groupOverrides["rotation"] as? Float) ?: group.rotation
        val pivotX = (groupOverrides["pivotX"] as? Float) ?: group.pivotX
        val pivotY = (groupOverrides["pivotY"] as? Float) ?: group.pivotY
        val scaleX = (groupOverrides["scaleX"] as? Float) ?: group.scaleX
        val scaleY = (groupOverrides["scaleY"] as? Float) ?: group.scaleY
        val translateX = (groupOverrides["translateX"] as? Float) ?: group.translationX
        val translateY = (groupOverrides["translateY"] as? Float) ?: group.translationY
        val clipPath =
            (if (group.clipPathName.isNotEmpty())
                overrides[group.clipPathName]?.get("pathData") as? String
            else null) ?: (groupOverrides["pathData"] as? String) ?: group.clipPathData

        val evaluatedChildren = ArrayList<RemoteEvaluatedNode>(group.children.size)
        for (i in group.children.indices) {
            val child = group.children[i]
            evaluatedChildren.add(
                when (child) {
                    is RemoteAnimatedVectorGroup -> evaluateGroup(child, overrides)
                    is RemoteAnimatedVectorPath -> evaluatePath(child, overrides)
                }
            )
        }

        return RemoteEvaluatedGroup(
            name = group.name,
            rotation = rotation,
            pivotX = pivotX,
            pivotY = pivotY,
            scaleX = scaleX,
            scaleY = scaleY,
            translationX = translateX,
            translationY = translateY,
            clipPathData = clipPath,
            children = evaluatedChildren,
        )
    }

    private fun evaluatePath(
        path: RemoteAnimatedVectorPath,
        overrides: Map<String, Map<String, Any?>>,
    ): RemoteEvaluatedPath {
        val pathOverrides = overrides[path.name] ?: emptyMap()

        val pathData = (pathOverrides["pathData"] as? String) ?: path.pathData
        val fillColor = (pathOverrides["fillColor"] as? Int) ?: path.fillColor
        val fillAlpha = (pathOverrides["fillAlpha"] as? Float) ?: path.fillAlpha
        val strokeColor = (pathOverrides["strokeColor"] as? Int) ?: path.strokeColor
        val strokeAlpha = (pathOverrides["strokeAlpha"] as? Float) ?: path.strokeAlpha
        val strokeWidth = (pathOverrides["strokeWidth"] as? Float) ?: path.strokeWidth
        val trimPathStart = (pathOverrides["trimPathStart"] as? Float) ?: path.trimPathStart
        val trimPathEnd = (pathOverrides["trimPathEnd"] as? Float) ?: path.trimPathEnd
        val trimPathOffset = (pathOverrides["trimPathOffset"] as? Float) ?: path.trimPathOffset

        return RemoteEvaluatedPath(
            name = path.name,
            pathData = pathData,
            fillColor = fillColor,
            fillAlpha = fillAlpha,
            strokeColor = strokeColor,
            strokeAlpha = strokeAlpha,
            strokeWidth = strokeWidth,
            strokeLineCap = path.strokeLineCap,
            strokeLineJoin = path.strokeLineJoin,
            strokeLineMiter = path.strokeLineMiter,
            trimPathStart = trimPathStart,
            trimPathEnd = trimPathEnd,
            trimPathOffset = trimPathOffset,
        )
    }

    public companion object {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RemoteAnimatedVectorNode {
    public val name: String
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteAnimatedVectorGroup(
    override val name: String = "",
    public val rotation: Float = 0f,
    public val pivotX: Float = 0f,
    public val pivotY: Float = 0f,
    public val scaleX: Float = 1f,
    public val scaleY: Float = 1f,
    public val translationX: Float = 0f,
    public val translationY: Float = 0f,
    public val clipPathName: String = "",
    public val clipPathData: String = "",
    public val children: List<RemoteAnimatedVectorNode> = emptyList(),
) : RemoteAnimatedVectorNode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteAnimatedVectorPath(
    override val name: String = "",
    public val pathData: String = "",
    public val fillColor: Int? = null,
    public val fillAlpha: Float = 1f,
    public val strokeColor: Int? = null,
    public val strokeAlpha: Float = 1f,
    public val strokeWidth: Float = 0f,
    public val strokeLineCap: Int = 0,
    public val strokeLineJoin: Int = 0,
    public val strokeLineMiter: Float = 4f,
    public val trimPathStart: Float = 0f,
    public val trimPathEnd: Float = 1f,
    public val trimPathOffset: Float = 0f,
) : RemoteAnimatedVectorNode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteAnimatedVectorTarget(
    public val name: String,
    public val animators: List<RemotePropertyAnimator> = emptyList(),
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemotePropertyAnimator(
    public val propertyName: String,
    public val duration: Int,
    public val startDelay: Int = 0,
    public val repeatCount: Int = 0,
    public val repeatMode: Int = REPEAT_MODE_RESTART,
    public val keyframes: List<RemoteKeyframe> = emptyList(),
    public val interpolator: RemoteInterpolator = RemoteInterpolator.Linear,
) {
    public val totalDuration: Int =
        if (repeatCount < 0) Int.MAX_VALUE else startDelay + duration * (repeatCount + 1)

    public fun evaluateAt(timeMs: Int, overallProgress: Float): Any? {
        if (keyframes.isEmpty()) return null
        if (keyframes.size == 1) return keyframes.first().value

        val effectiveDuration = if (duration > 0) duration else 1
        val elapsed = timeMs - startDelay
        val iterationFraction: Float
        if (elapsed < 0) {
            iterationFraction = 0f
        } else if (repeatCount >= 0 && elapsed >= duration * (repeatCount + 1)) {
            val isOddIteration = (repeatCount % 2 != 0)
            iterationFraction = if (repeatMode == REPEAT_MODE_REVERSE && isOddIteration) 0f else 1f
        } else {
            val iteration = elapsed / effectiveDuration
            var fraction = (elapsed % effectiveDuration).toFloat() / effectiveDuration.toFloat()
            if (repeatMode == REPEAT_MODE_REVERSE && iteration % 2 != 0) {
                fraction = 1f - fraction
            }
            iterationFraction = fraction
        }

        // Find adjacent keyframes
        val k0 = keyframes.first()
        val kLast = keyframes.last()
        if (iterationFraction <= k0.fraction) return k0.value
        if (iterationFraction >= kLast.fraction) return kLast.value

        var startKf = k0
        var endKf = kLast
        for (i in 0 until keyframes.size - 1) {
            if (
                iterationFraction >= keyframes[i].fraction &&
                    iterationFraction <= keyframes[i + 1].fraction
            ) {
                startKf = keyframes[i]
                endKf = keyframes[i + 1]
                break
            }
        }

        val range = endKf.fraction - startKf.fraction
        val segmentFraction = if (range > 0f) (iterationFraction - startKf.fraction) / range else 1f
        val easedFraction = endKf.interpolator.transform(segmentFraction)

        return interpolateValues(startKf.value, endKf.value, easedFraction)
    }

    private fun interpolateValues(start: Any, end: Any, fraction: Float): Any {
        if (start is Float && end is Float) {
            return start + (end - start) * fraction
        }
        if (start is Int && end is Int) {
            return interpolateColor(start, end, fraction)
        }
        if (start is String && end is String) {
            return interpolatePathData(start, end, fraction)
        }
        return if (fraction < 0.5f) start else end
    }

    public companion object {
        public const val REPEAT_MODE_RESTART: Int = 1
        public const val REPEAT_MODE_REVERSE: Int = 2
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteKeyframe(
    public val fraction: Float,
    public val value: Any,
    public val interpolator: RemoteInterpolator = RemoteInterpolator.Linear,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemoteInterpolator {
    public fun transform(fraction: Float): Float

    public companion object {
        public val Linear: RemoteInterpolator = RemoteInterpolator { it }
        public val AccelerateDecelerate: RemoteInterpolator = RemoteInterpolator {
            (cos((it + 1.0) * PI) / 2.0 + 0.5).toFloat()
        }
        public val FastOutSlowIn: RemoteInterpolator = CubicBezier(0.4f, 0.0f, 0.2f, 1.0f)
        public val FastOutLinearIn: RemoteInterpolator = CubicBezier(0.4f, 0.0f, 1.0f, 1.0f)
        public val LinearOutSlowIn: RemoteInterpolator = CubicBezier(0.0f, 0.0f, 0.2f, 1.0f)
    }

    public class Accelerate(private val factor: Float = 1.0f) : RemoteInterpolator {
        override fun transform(fraction: Float): Float {
            return if (factor == 1.0f) fraction * fraction
            else fraction.toDouble().pow(2.0 * factor).toFloat()
        }
    }

    public class Decelerate(private val factor: Float = 1.0f) : RemoteInterpolator {
        override fun transform(fraction: Float): Float {
            return if (factor == 1.0f) 1.0f - (1.0f - fraction) * (1.0f - fraction)
            else (1.0f - (1.0f - fraction).toDouble().pow(2.0 * factor)).toFloat()
        }
    }

    public class CubicBezier(
        public val x1: Float,
        public val y1: Float,
        public val x2: Float,
        public val y2: Float,
    ) : RemoteInterpolator {
        private val easing: CubicEasing =
            CubicEasing().apply {
                setup(x1, y1, x2, y2)
            }

        override fun transform(fraction: Float): Float {
            return easing.get(fraction)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteVectorSnapshot(
    public val width: Float,
    public val height: Float,
    public val viewportWidth: Float,
    public val viewportHeight: Float,
    public val rootGroup: RemoteEvaluatedGroup,
) {
    public fun draw(writer: RemoteComposeWriter) {
        val scaleX = if (viewportWidth > 0f) width / viewportWidth else 1f
        val scaleY = if (viewportHeight > 0f) height / viewportHeight else 1f
        writer.save()
        writer.scale(scaleX, scaleY)
        drawGroup(writer, rootGroup)
        writer.restore()
    }

    private fun drawGroup(writer: RemoteComposeWriter, group: RemoteEvaluatedGroup) {
        val hasTransform =
            group.translationX != 0f ||
                group.translationY != 0f ||
                group.rotation != 0f ||
                group.scaleX != 1f ||
                group.scaleY != 1f ||
                group.clipPathData.isNotEmpty()

        if (hasTransform) {
            writer.save()
            writer.translate(group.translationX, group.translationY)
            writer.rotate(group.rotation, group.pivotX, group.pivotY)
            writer.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
            if (group.clipPathData.isNotEmpty()) {
                val clipId = writer.addPathString(group.clipPathData)
                writer.addClipPath(clipId)
            }
        }

        for (i in group.children.indices) {
            when (val child = group.children[i]) {
                is RemoteEvaluatedGroup -> drawGroup(writer, child)
                is RemoteEvaluatedPath -> drawPath(writer, child)
            }
        }

        if (hasTransform) {
            writer.restore()
        }
    }

    private fun drawPath(writer: RemoteComposeWriter, path: RemoteEvaluatedPath) {
        if (path.pathData.isEmpty()) return
        val pathId = writer.addPathString(path.pathData)
        val isTrimmed =
            path.trimPathStart != 0f || path.trimPathEnd != 1f || path.trimPathOffset != 0f

        if (path.fillColor != null && path.fillAlpha > 0f) {
            val alpha = ((path.fillColor ushr 24) and 0xFF) / 255f * path.fillAlpha
            val argb =
                (path.fillColor and 0x00FFFFFF) or ((alpha * 255f).toInt().coerceIn(0, 255) shl 24)
            writer.rcPaint.setColor(argb)
            writer.rcPaint.setStyle(0) // FILL
            writer.rcPaint.commit()
            if (isTrimmed) {
                var start = (path.trimPathStart + path.trimPathOffset) % 1f
                var end = (path.trimPathEnd + path.trimPathOffset) % 1f
                if (start < 0f) start += 1f
                if (end < 0f) end += 1f
                writer.drawTweenPath(pathId, pathId, 0f, start, end)
            } else {
                writer.drawPath(pathId)
            }
        }

        if (path.strokeColor != null && path.strokeWidth > 0f && path.strokeAlpha > 0f) {
            val alpha = ((path.strokeColor ushr 24) and 0xFF) / 255f * path.strokeAlpha
            val argb =
                (path.strokeColor and 0x00FFFFFF) or
                    ((alpha * 255f).toInt().coerceIn(0, 255) shl 24)
            writer.rcPaint.setColor(argb)
            writer.rcPaint.setStyle(1) // STROKE
            writer.rcPaint.setStrokeWidth(path.strokeWidth)
            writer.rcPaint.setStrokeCap(path.strokeLineCap)
            writer.rcPaint.setStrokeJoin(path.strokeLineJoin)
            writer.rcPaint.commit()
            if (isTrimmed) {
                var start = (path.trimPathStart + path.trimPathOffset) % 1f
                var end = (path.trimPathEnd + path.trimPathOffset) % 1f
                if (start < 0f) start += 1f
                if (end < 0f) end += 1f
                writer.drawTweenPath(pathId, pathId, 0f, start, end)
            } else {
                writer.drawPath(pathId)
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RemoteEvaluatedNode {
    public val name: String
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteEvaluatedGroup(
    override val name: String = "",
    public val rotation: Float = 0f,
    public val pivotX: Float = 0f,
    public val pivotY: Float = 0f,
    public val scaleX: Float = 1f,
    public val scaleY: Float = 1f,
    public val translationX: Float = 0f,
    public val translationY: Float = 0f,
    public val clipPathData: String = "",
    public val children: List<RemoteEvaluatedNode> = emptyList(),
) : RemoteEvaluatedNode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteEvaluatedPath(
    override val name: String = "",
    public val pathData: String = "",
    public val fillColor: Int? = null,
    public val fillAlpha: Float = 1f,
    public val strokeColor: Int? = null,
    public val strokeAlpha: Float = 1f,
    public val strokeWidth: Float = 0f,
    public val strokeLineCap: Int = 0,
    public val strokeLineJoin: Int = 0,
    public val strokeLineMiter: Float = 4f,
    public val trimPathStart: Float = 0f,
    public val trimPathEnd: Float = 1f,
    public val trimPathOffset: Float = 0f,
) : RemoteEvaluatedNode

internal fun interpolateColor(from: Int, to: Int, fraction: Float): Int {
    return Utils.interpolateColor(from, to, fraction)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interpolatePathData(startPath: String, endPath: String, fraction: Float): String {
    val startTokens = tokenizePathData(startPath)
    val endTokens = tokenizePathData(endPath)
    if (startTokens.size != endTokens.size) {
        return if (fraction < 0.5f) startPath else endPath
    }

    val sb = StringBuilder()
    for (i in 0 until startTokens.size) {
        val st = startTokens[i]
        val et = endTokens[i]
        if (st.isNumber && et.isNumber) {
            val v = st.numberValue + (et.numberValue - st.numberValue) * fraction
            if (
                i > 0 && sb.isNotEmpty() && !sb.last().isWhitespace() && sb.last() != '-' && v >= 0
            ) {
                sb.append(' ')
            }
            sb.append(v)
        } else if (!st.isNumber && !et.isNumber && st.command == et.command) {
            if (i > 0 && sb.isNotEmpty() && !sb.last().isWhitespace()) {
                sb.append(' ')
            }
            sb.append(st.command)
        } else {
            return if (fraction < 0.5f) startPath else endPath
        }
    }
    return sb.toString()
}

private class PathToken(val isNumber: Boolean, val command: Char, val numberValue: Float)

private fun tokenizePathData(pathData: String): List<PathToken> {
    val tokens = mutableListOf<PathToken>()
    var i = 0
    val n = pathData.length
    while (i < n) {
        val c = pathData[i]
        if (c.isWhitespace() || c == ',') {
            i++
            continue
        }
        if (c.isLetter()) {
            tokens.add(PathToken(isNumber = false, command = c, numberValue = 0f))
            i++
        } else if (c == '-' || c == '+' || c == '.' || c.isDigit()) {
            val start = i
            var hasDot = c == '.'
            i++
            while (i < n) {
                val ch = pathData[i]
                if (ch.isDigit()) {
                    i++
                } else if (ch == '.' && !hasDot) {
                    hasDot = true
                    i++
                } else if ((ch == 'e' || ch == 'E') && i + 1 < n) {
                    i++
                    if (pathData[i] == '+' || pathData[i] == '-') i++
                } else {
                    break
                }
            }
            val numStr = pathData.substring(start, i)
            val num = numStr.toFloatOrNull() ?: 0f
            tokens.add(PathToken(isNumber = true, command = ' ', numberValue = num))
        } else {
            i++
        }
    }
    return tokens
}

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

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.cubicEasing
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * Calculates the default progress for an [animatedVector], which animates continuously based on
 * [RemoteContext.FLOAT_ANIMATION_TIME].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun defaultProgress(animatedVector: RemoteAnimatedVector): RemoteFloat {
    val durationMs = animatedVector.totalDuration
    if (durationMs <= 0) return 0f.rf
    val durationSec = durationMs / 1000f
    val animationTime = RemoteFloat(RemoteContext.FLOAT_ANIMATION_TIME)
    return (animationTime / durationSec.rf) % 1f.rf
}

/**
 * A [RemotePainter] that renders a [RemoteAnimatedVector] evaluated at a specific [progress]. By
 * default, [progress] animates continually based on [RemoteContext.FLOAT_ANIMATION_TIME].
 *
 * @param animatedVector The animated vector to render.
 * @param progress The animation progress. Defaults to a continuous animation based on animation
 *   time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteAnimatedVectorPainter(
    public val animatedVector: RemoteAnimatedVector,
    public val progress: RemoteFloat = defaultProgress(animatedVector),
) : RemotePainter() {

    internal val root: RemoteGroupComponent = animatedVector.toDynamicGroupComponent(progress)

    private val vector: RemoteVectorComponent =
        RemoteVectorComponent(root).apply {
            viewportSize =
                RemoteSize(animatedVector.viewportWidth.rf, animatedVector.viewportHeight.rf)
            name = animatedVector.name
        }

    override val intrinsicSize: RemoteSize =
        RemoteSize(animatedVector.width.rf, animatedVector.height.rf)

    override fun RemoteDrawScope.onDraw() {
        with(vector) {
            draw(null)
        }
    }
}

/**
 * Creates a [RemotePainter] to render a [RemoteAnimatedVector] at the given [progress]. Defaults to
 * animating continually based on [RemoteContext.FLOAT_ANIMATION_TIME].
 *
 * @param animatedVector The animated vector drawable to render.
 * @param progress The [RemoteFloat] progress to evaluate the animation at.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun painterRemoteAnimatedVector(
    animatedVector: RemoteAnimatedVector,
    progress: RemoteFloat = defaultProgress(animatedVector),
): RemotePainter {
    return RemoteAnimatedVectorPainter(animatedVector, progress)
}

/**
 * Creates a [RemotePainter] to render an XML Animated Vector Drawable resource at [progress].
 * Defaults to animating continually based on [RemoteContext.FLOAT_ANIMATION_TIME].
 *
 * @param context The Android context used to load the resource.
 * @param id The XML drawable resource ID.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun painterRemoteAnimatedVector(
    context: Context,
    @DrawableRes id: Int,
    progress: RemoteFloat = defaultProgress(RemoteAnimatedVector.fromXml(context.resources, id)),
): RemotePainter {
    val avd = RemoteAnimatedVector.fromXml(context.resources, id)
    return RemoteAnimatedVectorPainter(avd, progress)
}

/**
 * Remembers a [RemotePainter] for a [RemoteAnimatedVector] evaluated at the given [progress].
 * Defaults to animating continually based on [RemoteContext.FLOAT_ANIMATION_TIME].
 *
 * @param animatedVector The animated vector drawable to render.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteAnimatedVectorPainter(
    animatedVector: RemoteAnimatedVector,
    progress: RemoteFloat = defaultProgress(animatedVector),
): RemotePainter {
    return remember(animatedVector, progress) {
        RemoteAnimatedVectorPainter(animatedVector, progress)
    }
}

/**
 * Remembers a [RemotePainter] for an XML Animated Vector Drawable resource at the given [progress].
 * Defaults to animating continually based on [RemoteContext.FLOAT_ANIMATION_TIME].
 *
 * @param id The XML drawable resource ID.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteAnimatedVectorPainter(
    @DrawableRes id: Int,
    progress: RemoteFloat =
        defaultProgress(RemoteAnimatedVector.fromXml(LocalResources.current, id)),
): RemotePainter {
    val resources = LocalResources.current
    return remember(id, progress) {
        val avd = RemoteAnimatedVector.fromXml(resources, id)
        RemoteAnimatedVectorPainter(avd, progress)
    }
}

internal fun RemoteAnimatedVector.toDynamicGroupComponent(
    progress: RemoteFloat
): RemoteGroupComponent {
    val targetMap = HashMap<String, ArrayList<RemoteAnimatedVectorTarget>>()
    for (i in targets.indices) {
        val target = targets[i]
        var list = targetMap[target.name]
        if (list == null) {
            list = ArrayList()
            targetMap[target.name] = list
        }
        list.add(target)
    }
    return buildDynamicGroup(rootGroup, targetMap, totalDuration, progress)
}

private fun buildDynamicGroup(
    group: RemoteAnimatedVectorGroup,
    targetMap: Map<String, List<RemoteAnimatedVectorTarget>>,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteGroupComponent {
    val matchingTargets = targetMap[group.name]
    val animators = ArrayList<RemotePropertyAnimator>()
    if (matchingTargets != null) {
        for (i in matchingTargets.indices) {
            val tAnim = matchingTargets[i].animators
            for (j in tAnim.indices) {
                animators.add(tAnim[j])
            }
        }
    }

    val groupComponent =
        RemoteGroupComponent().apply {
            name = group.name
            rotation =
                evaluateDynamicFloat(animators, "rotation", group.rotation, totalDuration, progress)
            pivotX =
                evaluateDynamicFloat(animators, "pivotX", group.pivotX, totalDuration, progress)
            pivotY =
                evaluateDynamicFloat(animators, "pivotY", group.pivotY, totalDuration, progress)
            scaleX =
                evaluateDynamicFloat(animators, "scaleX", group.scaleX, totalDuration, progress)
            scaleY =
                evaluateDynamicFloat(animators, "scaleY", group.scaleY, totalDuration, progress)
            translationX =
                evaluateDynamicFloat(
                    animators,
                    "translateX",
                    group.translationX,
                    totalDuration,
                    progress,
                )
            translationY =
                evaluateDynamicFloat(
                    animators,
                    "translateY",
                    group.translationY,
                    totalDuration,
                    progress,
                )

            if (group.clipPathData.isNotEmpty()) {
                clipPathData =
                    PathParser().parsePathString(group.clipPathData).toNodes().toRemotePathNodes()
            }
        }

    for (index in group.children.indices) {
        val child = group.children[index]
        when (child) {
            is RemoteAnimatedVectorGroup -> {
                groupComponent.insertAt(
                    index,
                    buildDynamicGroup(child, targetMap, totalDuration, progress),
                )
            }
            is RemoteAnimatedVectorPath -> {
                groupComponent.insertAt(
                    index,
                    buildDynamicPath(child, targetMap, totalDuration, progress),
                )
            }
        }
    }
    return groupComponent
}

private class PathSegment(
    val startFraction: Float,
    val endFraction: Float,
    val cutoffFraction: Float,
    val startPath: String,
    val endPath: String,
    val interpolator: RemoteInterpolator,
)

private class RawSegment(
    val startFraction: Float,
    val endFraction: Float,
    val startPath: String,
    val endPath: String,
    val interpolator: RemoteInterpolator,
)

private fun extractPathSegments(
    sortedAnimators: List<RemotePropertyAnimator>,
    defaultPath: String,
    totalDuration: Int,
): List<PathSegment> {
    val rawSegments = ArrayList<RawSegment>()
    for (i in sortedAnimators.indices) {
        val anim = sortedAnimators[i]
        val kf = anim.keyframes
        if (kf.size <= 2) {
            val k0 =
                if (kf.isNotEmpty()) kf[0] else RemoteKeyframe(0f, defaultPath, anim.interpolator)
            val k1 = if (kf.size > 1) kf[1] else k0
            val startFrac =
                if (totalDuration > 0) {
                    (anim.startDelay + k0.fraction * anim.duration) / totalDuration
                } else 0f
            val endFrac =
                if (totalDuration > 0) {
                    (anim.startDelay + k1.fraction * anim.duration) / totalDuration
                } else 1f
            val startP = (k0.value as? String) ?: defaultPath
            val endP = (k1.value as? String) ?: startP
            rawSegments.add(RawSegment(startFrac, endFrac, startP, endP, k1.interpolator))
        } else {
            for (k in 0 until kf.size - 1) {
                val kStart = kf[k]
                val kEnd = kf[k + 1]
                val startFrac =
                    if (totalDuration > 0) {
                        (anim.startDelay + kStart.fraction * anim.duration) / totalDuration
                    } else 0f
                val endFrac =
                    if (totalDuration > 0) {
                        (anim.startDelay + kEnd.fraction * anim.duration) / totalDuration
                    } else 1f
                val startP = (kStart.value as? String) ?: defaultPath
                val endP = (kEnd.value as? String) ?: startP
                rawSegments.add(RawSegment(startFrac, endFrac, startP, endP, kEnd.interpolator))
            }
        }
    }

    if (rawSegments.isEmpty()) return emptyList()

    val segments = ArrayList<PathSegment>(rawSegments.size)
    val numRaw = rawSegments.size
    for (i in 0 until numRaw) {
        val raw = rawSegments[i]
        val cutoff =
            if (i < numRaw - 1) {
                rawSegments[i + 1].startFraction
            } else {
                raw.endFraction
            }
        segments.add(
            PathSegment(
                startFraction = raw.startFraction,
                endFraction = raw.endFraction,
                cutoffFraction = cutoff,
                startPath = raw.startPath,
                endPath = raw.endPath,
                interpolator = raw.interpolator,
            )
        )
    }
    return segments
}

private fun computeSegmentTween(seg: PathSegment, progress: RemoteFloat): RemoteFloat {
    val duration = seg.endFraction - seg.startFraction
    if (duration <= 0f) return 1f.rf
    val rawLocal = (progress - seg.startFraction.rf) / duration.rf
    val clamped = clamp(rawLocal, 0f, 1f)
    return transformInterpolator(seg.interpolator, clamped)
}

private fun transformInterpolator(
    interpolator: RemoteInterpolator,
    progress: RemoteFloat,
): RemoteFloat {
    return when (interpolator) {
        RemoteInterpolator.AccelerateDecelerate ->
            (1f.rf - cos(progress * (PI.toFloat()).rf)) / 2f.rf
        is RemoteInterpolator.Accelerate -> progress * progress
        is RemoteInterpolator.Decelerate -> 1f.rf - (1f.rf - progress) * (1f.rf - progress)
        is RemoteInterpolator.CubicBezier ->
            cubicEasing(
                interpolator.x1.rf,
                interpolator.y1.rf,
                interpolator.x2.rf,
                interpolator.y2.rf,
                progress,
            )
        else -> progress
    }
}

private fun buildDynamicPath(
    path: RemoteAnimatedVectorPath,
    targetMap: Map<String, List<RemoteAnimatedVectorTarget>>,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteVNode {
    val animators = ArrayList<RemotePropertyAnimator>()
    val targets = targetMap[path.name]
    if (targets != null) {
        for (i in targets.indices) {
            val tAnim = targets[i].animators
            for (j in tAnim.indices) {
                animators.add(tAnim[j])
            }
        }
    }

    val strokeAlphaFloat =
        evaluateDynamicFloat(
            animators,
            "strokeAlpha",
            path.strokeAlpha,
            totalDuration,
            progress,
        )
    val fillAlphaFloat =
        evaluateDynamicFloat(
            animators,
            "fillAlpha",
            path.fillAlpha,
            totalDuration,
            progress,
        )
    val strokeWidthFloat =
        evaluateDynamicFloat(
            animators,
            "strokeWidth",
            path.strokeWidth,
            totalDuration,
            progress,
        )
    val trimStart =
        evaluateDynamicFloat(
            animators,
            "trimPathStart",
            path.trimPathStart,
            totalDuration,
            progress,
        )
    val trimEnd =
        evaluateDynamicFloat(
            animators,
            "trimPathEnd",
            path.trimPathEnd,
            totalDuration,
            progress,
        )
    val trimOffset =
        evaluateDynamicFloat(
            animators,
            "trimPathOffset",
            path.trimPathOffset,
            totalDuration,
            progress,
        )
    val strokeColorDynamic =
        evaluateDynamicColor(
            animators,
            "strokeColor",
            path.strokeColor,
            totalDuration,
            progress,
        )
    val fillColorDynamic =
        evaluateDynamicColor(
            animators,
            "fillColor",
            path.fillColor,
            totalDuration,
            progress,
        )

    fun populateSharedAttrs(
        component: RemotePathComponent,
        fillAlphaOverride: RemoteFloat = fillAlphaFloat,
        strokeAlphaOverride: RemoteFloat = strokeAlphaFloat,
    ) {
        component.strokeAlpha = strokeAlphaOverride
        component.fillAlpha = fillAlphaOverride
        component.strokeLineWidth = strokeWidthFloat
        component.trimPathStart = trimStart
        component.trimPathEnd = trimEnd
        component.trimPathOffset = trimOffset
        component.strokeLineCap =
            when (path.strokeLineCap) {
                1 -> StrokeCap.Round
                2 -> StrokeCap.Square
                else -> StrokeCap.Butt
            }
        component.strokeLineJoin =
            when (path.strokeLineJoin) {
                1 -> StrokeJoin.Round
                2 -> StrokeJoin.Bevel
                else -> StrokeJoin.Miter
            }
        component.strokeLineMiter = path.strokeLineMiter.rf
        component.remoteStrokeColor = strokeColorDynamic
        component.remoteFillColor = fillColorDynamic
    }

    val pathAnimators = ArrayList<RemotePropertyAnimator>()
    for (i in animators.indices) {
        if (animators[i].propertyName == "pathData") {
            pathAnimators.add(animators[i])
        }
    }
    if (pathAnimators.size > 1) {
        pathAnimators.sortWith { a, b -> a.startDelay.compareTo(b.startDelay) }
    }

    if (pathAnimators.isEmpty()) {
        return RemotePathComponent().apply {
            name = path.name
            if (path.pathData.isNotEmpty()) {
                pathData = PathParser().parsePathString(path.pathData).toNodes().toRemotePathNodes()
            }
            targetPathData = null
            pathTween = 0f.rf
            populateSharedAttrs(this)
        }
    }

    val segments = extractPathSegments(pathAnimators, path.pathData, totalDuration)
    if (segments.isEmpty()) {
        return RemotePathComponent().apply {
            name = path.name
            if (path.pathData.isNotEmpty()) {
                pathData = PathParser().parsePathString(path.pathData).toNodes().toRemotePathNodes()
            }
            targetPathData = null
            pathTween = 0f.rf
            populateSharedAttrs(this)
        }
    }

    if (segments.size == 1) {
        val seg = segments[0]
        val segTween = computeSegmentTween(seg, progress)
        return RemotePathComponent().apply {
            name = path.name
            pathData = PathParser().parsePathString(seg.startPath).toNodes().toRemotePathNodes()
            if (seg.startPath != seg.endPath) {
                targetPathData =
                    PathParser().parsePathString(seg.endPath).toNodes().toRemotePathNodes()
                pathTween = segTween
            } else {
                targetPathData = null
                pathTween = 0f.rf
            }
            populateSharedAttrs(this)
        }
    }

    // Dynamic multi-stage chained segments
    val groupComponent = RemoteGroupComponent().apply { name = path.name }
    val n = segments.size
    for (i in 0 until n) {
        val seg = segments[i]
        val segTween = computeSegmentTween(seg, progress)
        val visibility: RemoteFloat =
            if (i == 0) {
                selectIfLt(progress, seg.cutoffFraction.rf, 1f.rf, 0f.rf)
            } else if (i == n - 1) {
                selectIfLt(progress, segments[i - 1].cutoffFraction.rf, 0f.rf, 1f.rf)
            } else {
                selectIfLt(
                    progress,
                    segments[i - 1].cutoffFraction.rf,
                    0f.rf,
                    selectIfLt(progress, seg.cutoffFraction.rf, 1f.rf, 0f.rf),
                )
            }

        val segComponent =
            RemotePathComponent().apply {
                name = "${path.name}_seg$i"
                pathData = PathParser().parsePathString(seg.startPath).toNodes().toRemotePathNodes()
                if (seg.startPath != seg.endPath) {
                    targetPathData =
                        PathParser().parsePathString(seg.endPath).toNodes().toRemotePathNodes()
                    pathTween = segTween
                } else {
                    targetPathData = null
                    pathTween = 0f.rf
                }
                populateSharedAttrs(
                    this,
                    fillAlphaOverride = fillAlphaFloat * visibility,
                    strokeAlphaOverride = strokeAlphaFloat * visibility,
                )
            }
        groupComponent.insertAt(groupComponent.numChildren, segComponent)
    }
    return groupComponent
}

private fun computeAnimatorLocalProgress(
    anim: RemotePropertyAnimator,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteFloat {
    if (totalDuration <= 0 || anim.duration <= 0) return 1f.rf
    val startFraction = anim.startDelay.toFloat() / totalDuration
    val endFraction = (anim.startDelay + anim.duration).toFloat() / totalDuration
    if (endFraction <= startFraction) return 1f.rf

    val rawLocal = (progress - startFraction.rf) / (endFraction - startFraction).rf
    val clamped = clamp(rawLocal, 0f, 1f)
    return transformInterpolator(anim.interpolator, clamped)
}

private fun evaluateDynamicFloat(
    animators: List<RemotePropertyAnimator>,
    propertyName: String,
    baseValue: Float,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteFloat {
    val propAnimators = ArrayList<RemotePropertyAnimator>()
    for (i in animators.indices) {
        if (animators[i].propertyName == propertyName) {
            propAnimators.add(animators[i])
        }
    }
    if (propAnimators.isEmpty() || totalDuration <= 0) return baseValue.rf
    if (propAnimators.size == 1) {
        return evaluateAnimatorFloat(propAnimators[0], baseValue, totalDuration, progress)
    }
    if (propAnimators.size > 1) {
        propAnimators.sortWith { a, b -> a.startDelay.compareTo(b.startDelay) }
    }
    val sorted = propAnimators
    var result: RemoteFloat =
        evaluateAnimatorFloat(sorted[sorted.lastIndex], baseValue, totalDuration, progress)
    for (i in (sorted.size - 2) downTo 0) {
        val anim = sorted[i]
        val stageExpr = evaluateAnimatorFloat(anim, baseValue, totalDuration, progress)
        val endFraction = (anim.startDelay + anim.duration).toFloat() / totalDuration
        result = selectIfLt(progress, endFraction.rf, stageExpr, result)
    }
    return result
}

private fun evaluateAnimatorFloat(
    anim: RemotePropertyAnimator,
    baseValue: Float,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteFloat {
    if (anim.keyframes.isEmpty()) return baseValue.rf
    if (anim.keyframes.size == 1) {
        return ((anim.keyframes[0].value as? Number)?.toFloat() ?: baseValue).rf
    }
    val localProgress = computeAnimatorLocalProgress(anim, totalDuration, progress)
    if (anim.keyframes.size == 2) {
        val from = (anim.keyframes[0].value as? Number)?.toFloat() ?: baseValue
        val to = (anim.keyframes[1].value as? Number)?.toFloat() ?: from
        if (from == to) return from.rf
        return lerp(from.rf, to.rf, localProgress)
    }
    val kf = anim.keyframes
    var kfResult: RemoteFloat = run {
        val last0 = (kf[kf.size - 2].value as? Number)?.toFloat() ?: baseValue
        val last1 = (kf[kf.size - 1].value as? Number)?.toFloat() ?: last0
        val frac0 = kf[kf.size - 2].fraction
        val frac1 = kf[kf.size - 1].fraction
        val segmentProgress =
            if (frac1 > frac0) {
                clamp((localProgress - frac0.rf) / (frac1 - frac0).rf, 0f, 1f)
            } else 1f.rf
        lerp(last0.rf, last1.rf, segmentProgress)
    }
    for (j in (kf.size - 3) downTo 0) {
        val val0 = (kf[j].value as? Number)?.toFloat() ?: baseValue
        val val1 = (kf[j + 1].value as? Number)?.toFloat() ?: val0
        val frac0 = kf[j].fraction
        val frac1 = kf[j + 1].fraction
        val segmentProgress =
            if (frac1 > frac0) {
                clamp((localProgress - frac0.rf) / (frac1 - frac0).rf, 0f, 1f)
            } else 1f.rf
        val stageExpr = lerp(val0.rf, val1.rf, segmentProgress)
        kfResult = selectIfLt(localProgress, frac1.rf, stageExpr, kfResult)
    }
    return kfResult
}

private fun evaluateDynamicColor(
    animators: List<RemotePropertyAnimator>,
    propertyName: String,
    baseColor: Int?,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteColor? {
    val propAnimators = ArrayList<RemotePropertyAnimator>()
    for (i in animators.indices) {
        if (animators[i].propertyName == propertyName) {
            propAnimators.add(animators[i])
        }
    }
    if (propAnimators.isEmpty() || totalDuration <= 0) {
        return baseColor?.let { RemoteColor(it) }
    }
    if (propAnimators.size == 1) {
        return evaluateAnimatorColor(propAnimators[0], baseColor, totalDuration, progress)
    }
    if (propAnimators.size > 1) {
        propAnimators.sortWith { a, b -> a.startDelay.compareTo(b.startDelay) }
    }
    val sorted = propAnimators
    var result: RemoteColor =
        evaluateAnimatorColor(sorted[sorted.lastIndex], baseColor, totalDuration, progress)
            ?: return baseColor?.let { RemoteColor(it) }
    for (i in (sorted.size - 2) downTo 0) {
        val anim = sorted[i]
        val stageExpr = evaluateAnimatorColor(anim, baseColor, totalDuration, progress) ?: continue
        val endFraction = (anim.startDelay + anim.duration).toFloat() / totalDuration
        result = progress.isLessThan(endFraction.rf).select(stageExpr, result)
    }
    return result
}

private fun evaluateAnimatorColor(
    anim: RemotePropertyAnimator,
    baseColor: Int?,
    totalDuration: Int,
    progress: RemoteFloat,
): RemoteColor? {
    if (anim.keyframes.isEmpty()) return baseColor?.let { RemoteColor(it) }
    if (anim.keyframes.size == 1) {
        val color = (anim.keyframes[0].value as? Number)?.toInt() ?: baseColor ?: return null
        return RemoteColor(color)
    }
    val localProgress = computeAnimatorLocalProgress(anim, totalDuration, progress)
    if (anim.keyframes.size == 2) {
        val from = (anim.keyframes[0].value as? Number)?.toInt() ?: baseColor ?: return null
        val to = (anim.keyframes[1].value as? Number)?.toInt() ?: from
        if (from == to) return RemoteColor(from)
        return tween(from, to, localProgress)
    }
    val kf = anim.keyframes
    var kfResult: RemoteColor = run {
        val last0 = (kf[kf.size - 2].value as? Number)?.toInt() ?: baseColor ?: return null
        val last1 = (kf[kf.size - 1].value as? Number)?.toInt() ?: last0
        val frac0 = kf[kf.size - 2].fraction
        val frac1 = kf[kf.size - 1].fraction
        val segmentProgress =
            if (frac1 > frac0) {
                clamp((localProgress - frac0.rf) / (frac1 - frac0).rf, 0f, 1f)
            } else 1f.rf
        tween(last0, last1, segmentProgress)
    }
    for (j in (kf.size - 3) downTo 0) {
        val val0 = (kf[j].value as? Number)?.toInt() ?: baseColor ?: return null
        val val1 = (kf[j + 1].value as? Number)?.toInt() ?: val0
        val frac0 = kf[j].fraction
        val frac1 = kf[j + 1].fraction
        val segmentProgress =
            if (frac1 > frac0) {
                clamp((localProgress - frac0.rf) / (frac1 - frac0).rf, 0f, 1f)
            } else 1f.rf
        val stageExpr = tween(val0, val1, segmentProgress)
        kfResult = localProgress.isLessThan(frac1.rf).select(stageExpr, kfResult)
    }
    return kfResult
}

/**
 * Converts a [RemoteVectorSnapshot] to a standard Compose [ImageVector].
 *
 * This allows rendering the evaluated state of an animated vector in standard Compose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteVectorSnapshot.toImageVector(): ImageVector {
    val builder =
        ImageVector.Builder(
            defaultWidth = width.dp,
            defaultHeight = height.dp,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )

    fun addPath(b: ImageVector.Builder, path: RemoteEvaluatedPath) {
        b.addPath(
            pathData =
                if (path.pathData.isNotEmpty()) {
                    PathParser().parsePathString(path.pathData).toNodes()
                } else {
                    emptyList()
                },
            name = path.name,
            fill = path.fillColor?.let { SolidColor(Color(it)) },
            fillAlpha = path.fillAlpha,
            stroke = path.strokeColor?.let { SolidColor(Color(it)) },
            strokeAlpha = path.strokeAlpha,
            strokeLineWidth = path.strokeWidth,
            strokeLineCap =
                when (path.strokeLineCap) {
                    1 -> StrokeCap.Round
                    2 -> StrokeCap.Square
                    else -> StrokeCap.Butt
                },
            strokeLineJoin =
                when (path.strokeLineJoin) {
                    1 -> StrokeJoin.Round
                    2 -> StrokeJoin.Bevel
                    else -> StrokeJoin.Miter
                },
            strokeLineMiter = path.strokeLineMiter,
            trimPathStart = path.trimPathStart,
            trimPathEnd = path.trimPathEnd,
            trimPathOffset = path.trimPathOffset,
        )
    }

    fun addGroup(b: ImageVector.Builder, group: RemoteEvaluatedGroup) {
        b.addGroup(
            name = group.name,
            rotate = group.rotation,
            pivotX = group.pivotX,
            pivotY = group.pivotY,
            scaleX = group.scaleX,
            scaleY = group.scaleY,
            translationX = group.translationX,
            translationY = group.translationY,
            clipPathData =
                if (group.clipPathData.isNotEmpty()) {
                    PathParser().parsePathString(group.clipPathData).toNodes()
                } else {
                    emptyList()
                },
        )
        for (i in group.children.indices) {
            when (val child = group.children[i]) {
                is RemoteEvaluatedGroup -> addGroup(b, child)
                is RemoteEvaluatedPath -> addPath(b, child)
            }
        }
        b.clearGroup()
    }

    for (i in rootGroup.children.indices) {
        when (val child = rootGroup.children[i]) {
            is RemoteEvaluatedGroup -> addGroup(builder, child)
            is RemoteEvaluatedPath -> addPath(builder, child)
        }
    }
    return builder.build()
}

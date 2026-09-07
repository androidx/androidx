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

import android.content.res.Resources
import android.util.AttributeSet
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/** Android XML resource parser for [RemoteAnimatedVector]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteAnimatedVectorParser {

    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private const val AAPT_NS = "http://schemas.android.com/aapt"

    @Throws(XmlPullParserException::class)
    public fun parse(res: Resources, @DrawableRes id: Int): RemoteAnimatedVector {
        @Suppress("ResourceType") val parser = res.getXml(id)
        seekToStartTag(parser)
        return parse(parser, res)
    }

    @Throws(XmlPullParserException::class)
    public fun parse(parser: XmlPullParser, res: Resources? = null): RemoteAnimatedVector {
        seekToStartTag(parser)
        if (parser.name != "animated-vector") {
            throw XmlPullParserException("Expected <animated-vector>, found <${parser.name}>")
        }

        var vectorRoot: RemoteAnimatedVector? = null
        val drawableAttr = parser.getAttribute("drawable")
        if (drawableAttr != null && res != null && drawableAttr.startsWith("@")) {
            val attrSet: AttributeSet? =
                (parser as? AttributeSet) ?: runCatching { Xml.asAttributeSet(parser) }.getOrNull()
            var drawableResId = attrSet?.getAttributeResourceValue(ANDROID_NS, "drawable", 0) ?: 0
            if (drawableResId == 0) {
                val cleanName = drawableAttr.removePrefix("@")
                val parts = cleanName.split("/")
                if (parts.size == 2) {
                    drawableResId = res.getIdentifier(parts[1], parts[0], null)
                }
            }
            if (drawableResId != 0) {
                val subParser = res.getXml(drawableResId)
                seekToStartTag(subParser)
                vectorRoot = parseVector(subParser, res)
            }
        }

        val targets = mutableListOf<RemoteAnimatedVectorTarget>()

        val depth = parser.depth
        var eventType = parser.next()
        while (
            eventType != XmlPullParser.END_DOCUMENT &&
                (parser.depth > depth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "attr",
                    "aapt:attr" -> {
                        val name =
                            parser.getAttributeValue(null, "name") ?: parser.getAttribute("name")
                        if (name == "android:drawable") {
                            parser.nextTag()
                            if (parser.name == "vector") {
                                vectorRoot = parseVector(parser, res)
                            }
                        }
                    }
                    "target" -> {
                        val target = parseTarget(parser, res)
                        if (target != null) {
                            targets.add(target)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (vectorRoot == null) {
            throw XmlPullParserException("Could not find <vector> in <animated-vector>")
        }

        return RemoteAnimatedVector(
            name = vectorRoot.name,
            width = vectorRoot.width,
            height = vectorRoot.height,
            viewportWidth = vectorRoot.viewportWidth,
            viewportHeight = vectorRoot.viewportHeight,
            rootGroup = vectorRoot.rootGroup,
            targets = targets,
        )
    }

    private fun parseVector(parser: XmlPullParser, res: Resources?): RemoteAnimatedVector {
        val name = parser.getAttribute("name") ?: ""
        val widthStr = parser.getAttribute("width") ?: "24dp"
        val heightStr = parser.getAttribute("height") ?: "24dp"
        val viewportWidth = parser.getAttribute("viewportWidth")?.toFloatOrNull() ?: 24f
        val viewportHeight = parser.getAttribute("viewportHeight")?.toFloatOrNull() ?: 24f

        val width = parseDimension(widthStr)
        val height = parseDimension(heightStr)

        val children = mutableListOf<RemoteAnimatedVectorNode>()

        val depth = parser.depth
        var eventType = parser.next()
        while (
            eventType != XmlPullParser.END_DOCUMENT &&
                (parser.depth > depth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "group" -> children.add(parseGroup(parser, res))
                    "path" -> children.add(parsePath(parser, res))
                    "clip-path" -> {
                        val clipName = parser.getAttribute("name") ?: ""
                        val clipPathData = parser.getAttribute("pathData") ?: ""
                        // Treat clip-path at vector root or in group as an enclosing group clip
                        children.add(
                            RemoteAnimatedVectorGroup(
                                name = clipName,
                                clipPathData = clipPathData,
                            )
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        val rootGroup =
            RemoteAnimatedVectorGroup(
                name = name,
                children = children,
            )

        return RemoteAnimatedVector(
            name = name,
            width = width,
            height = height,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            rootGroup = rootGroup,
        )
    }

    private fun parseGroup(parser: XmlPullParser, res: Resources?): RemoteAnimatedVectorGroup {
        val name = parser.getAttribute("name") ?: ""
        val rotation = parser.getAttribute("rotation")?.toFloatOrNull() ?: 0f
        val pivotX = parser.getAttribute("pivotX")?.toFloatOrNull() ?: 0f
        val pivotY = parser.getAttribute("pivotY")?.toFloatOrNull() ?: 0f
        val scaleX = parser.getAttribute("scaleX")?.toFloatOrNull() ?: 1f
        val scaleY = parser.getAttribute("scaleY")?.toFloatOrNull() ?: 1f
        val translateX = parser.getAttribute("translateX")?.toFloatOrNull() ?: 0f
        val translateY = parser.getAttribute("translateY")?.toFloatOrNull() ?: 0f

        var clipPathName = ""
        var clipPathData = ""
        val children = mutableListOf<RemoteAnimatedVectorNode>()

        val depth = parser.depth
        var eventType = parser.next()
        while (
            eventType != XmlPullParser.END_DOCUMENT &&
                (parser.depth > depth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "group" -> children.add(parseGroup(parser, res))
                    "path" -> children.add(parsePath(parser, res))
                    "clip-path" -> {
                        clipPathName = parser.getAttribute("name") ?: ""
                        clipPathData = parser.getAttribute("pathData") ?: ""
                    }
                }
            }
            eventType = parser.next()
        }

        return RemoteAnimatedVectorGroup(
            name = name,
            rotation = rotation,
            pivotX = pivotX,
            pivotY = pivotY,
            scaleX = scaleX,
            scaleY = scaleY,
            translationX = translateX,
            translationY = translateY,
            clipPathName = clipPathName,
            clipPathData = clipPathData,
            children = children,
        )
    }

    private fun parsePath(
        parser: XmlPullParser,
        res: Resources? = null,
    ): RemoteAnimatedVectorPath {
        val name = parser.getAttribute("name") ?: ""
        val pathData = parser.getAttribute("pathData") ?: ""
        val fillColor =
            parser.getAttribute("fillColor")?.let { parseColor(it, "fillColor", parser, res) }
        val fillAlpha = parser.getAttribute("fillAlpha")?.toFloatOrNull() ?: 1f
        val strokeColor =
            parser.getAttribute("strokeColor")?.let { parseColor(it, "strokeColor", parser, res) }
        val strokeAlpha = parser.getAttribute("strokeAlpha")?.toFloatOrNull() ?: 1f
        val strokeWidth = parser.getAttribute("strokeWidth")?.toFloatOrNull() ?: 0f
        val strokeLineCap =
            when (parser.getAttribute("strokeLineCap")) {
                "round" -> 1
                "square" -> 2
                else -> 0
            }
        val strokeLineJoin =
            when (parser.getAttribute("strokeLineJoin")) {
                "round" -> 1
                "bevel" -> 2
                else -> 0
            }
        val strokeLineMiter = parser.getAttribute("strokeMiterLimit")?.toFloatOrNull() ?: 4f
        val trimPathStart = parser.getAttribute("trimPathStart")?.toFloatOrNull() ?: 0f
        val trimPathEnd = parser.getAttribute("trimPathEnd")?.toFloatOrNull() ?: 1f
        val trimPathOffset = parser.getAttribute("trimPathOffset")?.toFloatOrNull() ?: 0f

        return RemoteAnimatedVectorPath(
            name = name,
            pathData = pathData,
            fillColor = fillColor,
            fillAlpha = fillAlpha,
            strokeColor = strokeColor,
            strokeAlpha = strokeAlpha,
            strokeWidth = strokeWidth,
            strokeLineCap = strokeLineCap,
            strokeLineJoin = strokeLineJoin,
            strokeLineMiter = strokeLineMiter,
            trimPathStart = trimPathStart,
            trimPathEnd = trimPathEnd,
            trimPathOffset = trimPathOffset,
        )
    }

    private fun parseTarget(parser: XmlPullParser, res: Resources?): RemoteAnimatedVectorTarget? {
        val name = parser.getAttribute("name") ?: return null
        val animators = mutableListOf<RemotePropertyAnimator>()

        val animAttr = parser.getAttribute("animation")
        if (animAttr != null && res != null && animAttr.startsWith("@")) {
            val attrSet: AttributeSet? =
                (parser as? AttributeSet) ?: runCatching { Xml.asAttributeSet(parser) }.getOrNull()
            var animResId = attrSet?.getAttributeResourceValue(ANDROID_NS, "animation", 0) ?: 0
            if (animResId == 0) {
                val cleanName = animAttr.removePrefix("@")
                val parts = cleanName.split("/")
                if (parts.size == 2) {
                    animResId = res.getIdentifier(parts[1], parts[0], null)
                }
            }
            if (animResId != 0) {
                val subParser = res.getXml(animResId)
                seekToStartTag(subParser)
                animators.addAll(parseAnimators(subParser, res))
            }
        }

        val depth = parser.depth
        var eventType = parser.next()
        while (
            eventType != XmlPullParser.END_DOCUMENT &&
                (parser.depth > depth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "attr",
                    "aapt:attr" -> {
                        val attrName =
                            parser.getAttributeValue(null, "name") ?: parser.getAttribute("name")
                        if (attrName == "android:animation") {
                            parser.nextTag()
                            animators.addAll(parseAnimators(parser, res))
                        }
                    }
                    "objectAnimator" -> {
                        val anim = parseObjectAnimator(parser, res)
                        if (anim != null) animators.add(anim)
                    }
                    "set" -> {
                        animators.addAll(parseSet(parser, res))
                    }
                }
            }
            eventType = parser.next()
        }

        return RemoteAnimatedVectorTarget(name = name, animators = animators)
    }

    private fun parseAnimators(
        parser: XmlPullParser,
        res: Resources?,
    ): List<RemotePropertyAnimator> {
        return when (parser.name) {
            "objectAnimator" -> listOfNotNull(parseObjectAnimator(parser, res))
            "set" -> parseSet(parser, res)
            else -> emptyList()
        }
    }

    private fun parseSet(parser: XmlPullParser, res: Resources?): List<RemotePropertyAnimator> {
        val animators = mutableListOf<RemotePropertyAnimator>()
        val ordering = parser.getAttribute("ordering") ?: "together"
        var accumulatedDelay = 0

        val depth = parser.depth
        var eventType = parser.next()
        while (
            eventType != XmlPullParser.END_DOCUMENT &&
                (parser.depth > depth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "objectAnimator" -> {
                        val anim = parseObjectAnimator(parser, res)
                        if (anim != null) {
                            if (ordering == "sequentially") {
                                val sequentialAnim =
                                    RemotePropertyAnimator(
                                        propertyName = anim.propertyName,
                                        duration = anim.duration,
                                        startDelay = accumulatedDelay + anim.startDelay,
                                        repeatCount = anim.repeatCount,
                                        repeatMode = anim.repeatMode,
                                        keyframes = anim.keyframes,
                                        interpolator = anim.interpolator,
                                    )
                                animators.add(sequentialAnim)
                                accumulatedDelay += anim.totalDuration
                            } else {
                                animators.add(anim)
                            }
                        }
                    }
                    "set" -> {
                        val childAnimators = parseSet(parser, res)
                        animators.addAll(childAnimators)
                    }
                }
            }
            eventType = parser.next()
        }
        return animators
    }

    private fun parseObjectAnimator(
        parser: XmlPullParser,
        res: Resources?,
    ): RemotePropertyAnimator? {
        val propertyName = parser.getAttribute("propertyName") ?: return null
        val duration = parser.getAttribute("duration")?.toIntOrNull() ?: 300
        val startOffset = parser.getAttribute("startOffset")?.toIntOrNull() ?: 0
        val valueFromStr = parser.getAttribute("valueFrom") ?: ""
        val valueToStr = parser.getAttribute("valueTo") ?: ""
        val valueType = parser.getAttribute("valueType")
        val repeatCount = parser.getAttribute("repeatCount")?.toIntOrNull() ?: 0
        val repeatMode =
            if (parser.getAttribute("repeatMode") == "reverse") {
                RemotePropertyAnimator.REPEAT_MODE_REVERSE
            } else {
                RemotePropertyAnimator.REPEAT_MODE_RESTART
            }

        var interpolator = parseInterpolator(parser.getAttribute("interpolator"))

        val depth = parser.depth
        var eventType = parser.next()
        while (
            eventType != XmlPullParser.END_DOCUMENT &&
                (parser.depth > depth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "attr",
                    "aapt:attr" -> {
                        val attrName =
                            parser.getAttributeValue(null, "name") ?: parser.getAttribute("name")
                        if (attrName == "android:interpolator") {
                            parser.nextTag()
                            if (parser.name == "pathInterpolator") {
                                interpolator = parsePathInterpolator(parser)
                            }
                        }
                    }
                    "pathInterpolator" -> {
                        interpolator = parsePathInterpolator(parser)
                    }
                }
            }
            eventType = parser.next()
        }

        val isColor =
            valueType == "colorType" ||
                propertyName.contains("Color", ignoreCase = true) ||
                valueFromStr.startsWith("#") ||
                valueToStr.startsWith("#") ||
                valueFromStr.startsWith("@") ||
                valueToStr.startsWith("@")

        val isPath = valueType == "pathType" || propertyName.equals("pathData", ignoreCase = true)

        val fromVal: Any =
            when {
                isColor -> parseColor(valueFromStr, "valueFrom", parser, res)
                isPath -> valueFromStr
                else -> valueFromStr.toFloatOrNull() ?: 0f
            }

        val toVal: Any =
            when {
                isColor -> parseColor(valueToStr, "valueTo", parser, res)
                isPath -> valueToStr
                else -> valueToStr.toFloatOrNull() ?: 0f
            }

        val keyframes =
            listOf(
                RemoteKeyframe(0f, fromVal, interpolator),
                RemoteKeyframe(1f, toVal, interpolator),
            )

        return RemotePropertyAnimator(
            propertyName = propertyName,
            duration = duration,
            startDelay = startOffset,
            repeatCount = repeatCount,
            repeatMode = repeatMode,
            keyframes = keyframes,
            interpolator = interpolator,
        )
    }

    private fun parsePathInterpolator(parser: XmlPullParser): RemoteInterpolator {
        val pathData = parser.getAttribute("pathData")
        if (pathData != null) {
            val coords = extractControlPointsFromPath(pathData)
            if (coords != null) {
                return RemoteInterpolator.CubicBezier(coords[0], coords[1], coords[2], coords[3])
            }
        }
        val x1 = parser.getAttribute("controlX1")?.toFloatOrNull() ?: 0f
        val y1 = parser.getAttribute("controlY1")?.toFloatOrNull() ?: 0f
        val x2 = parser.getAttribute("controlX2")?.toFloatOrNull() ?: 1f
        val y2 = parser.getAttribute("controlY2")?.toFloatOrNull() ?: 1f
        return RemoteInterpolator.CubicBezier(x1, y1, x2, y2)
    }

    private fun extractControlPointsFromPath(pathData: String): FloatArray? {
        return runCatching {
            val nodes = PathParser().parsePathString(pathData).toNodes()
            var currX = 0f
            var currY = 0f
            for (i in nodes.indices) {
                val node = nodes[i]
                when (node) {
                    is PathNode.MoveTo -> {
                        currX = node.x
                        currY = node.y
                    }
                    is PathNode.RelativeMoveTo -> {
                        currX += node.dx
                        currY += node.dy
                    }
                    is PathNode.CurveTo -> {
                        return floatArrayOf(node.x1, node.y1, node.x2, node.y2)
                    }
                    is PathNode.RelativeCurveTo -> {
                        return floatArrayOf(
                            currX + node.dx1,
                            currY + node.dy1,
                            currX + node.dx2,
                            currY + node.dy2,
                        )
                    }
                    is PathNode.QuadTo -> {
                        val cp1x = currX + (2f / 3f) * (node.x1 - currX)
                        val cp1y = currY + (2f / 3f) * (node.y1 - currY)
                        val cp2x = node.x2 + (2f / 3f) * (node.x1 - node.x2)
                        val cp2y = node.y2 + (2f / 3f) * (node.y1 - node.y2)
                        return floatArrayOf(cp1x, cp1y, cp2x, cp2y)
                    }
                    is PathNode.RelativeQuadTo -> {
                        val absX1 = currX + node.dx1
                        val absY1 = currY + node.dy1
                        val absX2 = currX + node.dx2
                        val absY2 = currY + node.dy2
                        val cp1x = currX + (2f / 3f) * (absX1 - currX)
                        val cp1y = currY + (2f / 3f) * (absY1 - currY)
                        val cp2x = absX2 + (2f / 3f) * (absX1 - absX2)
                        val cp2y = absY2 + (2f / 3f) * (absY1 - absY2)
                        return floatArrayOf(cp1x, cp1y, cp2x, cp2y)
                    }
                    is PathNode.LineTo,
                    is PathNode.RelativeLineTo -> {
                        return floatArrayOf(0f, 0f, 1f, 1f)
                    }
                    else -> {}
                }
            }
            null
        }
            .getOrNull()
    }

    private fun parseInterpolator(interpolatorStr: String?): RemoteInterpolator {
        if (interpolatorStr == null) return RemoteInterpolator.Linear
        return when {
            interpolatorStr.contains("fast_out_slow_in") -> RemoteInterpolator.FastOutSlowIn
            interpolatorStr.contains("fast_out_linear_in") -> RemoteInterpolator.FastOutLinearIn
            interpolatorStr.contains("linear_out_slow_in") -> RemoteInterpolator.LinearOutSlowIn
            interpolatorStr.contains("accelerate_decelerate") ->
                RemoteInterpolator.AccelerateDecelerate
            interpolatorStr.contains("accelerate_cubic") ||
                interpolatorStr.contains("accelerate_interpolator") ->
                RemoteInterpolator.Accelerate()
            interpolatorStr.contains("decelerate_cubic") ||
                interpolatorStr.contains("decelerate_interpolator") ->
                RemoteInterpolator.Decelerate()
            interpolatorStr.contains("linear") -> RemoteInterpolator.Linear
            else -> RemoteInterpolator.Linear
        }
    }

    private fun parseDimension(dim: String): Float {
        val clean =
            dim.trim().removeSuffix("dip").removeSuffix("dp").removeSuffix("px").removeSuffix("sp")
        return clean.toFloatOrNull() ?: 24f
    }

    private fun parseColor(
        colorStr: String,
        attrName: String? = null,
        parser: XmlPullParser? = null,
        res: Resources? = null,
    ): Int {
        if (colorStr.startsWith("@") && res != null) {
            val attrSet: AttributeSet? =
                (parser as? AttributeSet) ?: runCatching { Xml.asAttributeSet(parser) }.getOrNull()
            var colorResId =
                if (attrName != null) {
                    attrSet?.getAttributeResourceValue(ANDROID_NS, attrName, 0) ?: 0
                } else {
                    0
                }
            if (colorResId == 0) {
                val cleanName = colorStr.removePrefix("@")
                val parts = cleanName.split("/")
                if (parts.size == 2) {
                    colorResId = res.getIdentifier(parts[1], parts[0], null)
                }
            }
            if (colorResId != 0) {
                return res.getColor(colorResId, null)
            }
        }
        val clean = colorStr.trim().removePrefix("#")
        return when (clean.length) {
            6 -> (0xFF shl 24) or clean.toLong(16).toInt()
            8 -> clean.toLong(16).toInt()
            3 -> {
                val r = clean.substring(0, 1).repeat(2)
                val g = clean.substring(1, 2).repeat(2)
                val b = clean.substring(2, 3).repeat(2)
                (0xFF shl 24) or "$r$g$b".toLong(16).toInt()
            }
            else -> 0
        }
    }

    private fun XmlPullParser.getAttribute(name: String): String? {
        return getAttributeValue(ANDROID_NS, name) ?: getAttributeValue(null, name)
    }

    private fun seekToStartTag(parser: XmlPullParser) {
        var type = parser.eventType
        while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            type = parser.next()
        }
    }
}

/** Convenience extension to parse a [RemoteAnimatedVector] from resources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteAnimatedVector.Companion.fromXml(
    res: Resources,
    @DrawableRes id: Int,
): RemoteAnimatedVector = RemoteAnimatedVectorParser.parse(res, id)

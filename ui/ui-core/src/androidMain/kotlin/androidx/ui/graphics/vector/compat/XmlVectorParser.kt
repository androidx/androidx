/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector.compat

import android.content.res.Resources
import android.util.AttributeSet
import androidx.core.content.res.ComplexColorCompat
import androidx.core.content.res.TypedArrayUtils
import androidx.ui.unit.dp
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shader
import androidx.ui.graphics.ShaderBrush
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.graphics.vector.DefaultPivotX
import androidx.ui.graphics.vector.DefaultPivotY
import androidx.ui.graphics.vector.DefaultRotation
import androidx.ui.graphics.vector.DefaultScaleX
import androidx.ui.graphics.vector.DefaultScaleY
import androidx.ui.graphics.vector.DefaultTranslationX
import androidx.ui.graphics.vector.DefaultTranslationY
import androidx.ui.graphics.vector.EmptyPath
import androidx.ui.graphics.vector.PathNode
import androidx.ui.graphics.vector.VectorAssetBuilder
import androidx.ui.graphics.vector.addPathNodes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

private const val LINECAP_BUTT = 0
private const val LINECAP_ROUND = 1
private const val LINECAP_SQUARE = 2

private const val LINEJOIN_MITER = 0
private const val LINEJOIN_ROUND = 1
private const val LINEJOIN_BEVEL = 2

private val FILL_TYPE_WINDING = 0

private const val SHAPE_CLIP_PATH = "clip-path"
private const val SHAPE_GROUP = "group"
private const val SHAPE_PATH = "path"

private fun getStrokeLineCap(id: Int, defValue: StrokeCap = StrokeCap.butt): StrokeCap =
    when (id) {
        LINECAP_BUTT -> StrokeCap.butt
        LINECAP_ROUND -> StrokeCap.round
        LINECAP_SQUARE -> StrokeCap.square
        else -> defValue
    }

private fun getStrokeLineJoin(id: Int, defValue: StrokeJoin = StrokeJoin.miter): StrokeJoin =
    when (id) {
        LINEJOIN_MITER -> StrokeJoin.miter
        LINEJOIN_ROUND -> StrokeJoin.round
        LINEJOIN_BEVEL -> StrokeJoin.bevel
        else -> defValue
    }

internal fun XmlPullParser.isAtEnd(): Boolean =
    eventType == XmlPullParser.END_DOCUMENT ||
            (depth < 1 && eventType == XmlPullParser.END_TAG)

internal fun XmlPullParser.parseCurrentVectorNode(
    res: Resources,
    attrs: AttributeSet,
    theme: Resources.Theme? = null,
    builder: VectorAssetBuilder
) {
    when (eventType) {
        XmlPullParser.START_TAG -> {
            when (name) {
                SHAPE_PATH -> {
                    parsePath(res, theme, attrs, builder)
                }
                SHAPE_CLIP_PATH -> {
                    // TODO: b/147418351 - parse clipping paths
                }
                SHAPE_GROUP -> {
                    parseGroup(res, theme, attrs, builder)
                }
            }
        }
        XmlPullParser.END_TAG -> {
            if (SHAPE_GROUP == name) {
                builder.popGroup()
            }
        }
    }
}

/**
 * Helper method to seek to the first tag within the VectorDrawable xml asset
 */
@Throws(XmlPullParserException::class)
internal fun XmlPullParser.seekToStartTag(): XmlPullParser {
    var type = next()
    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        // Empty loop
        type = next()
    }
    if (type != XmlPullParser.START_TAG) {
        throw XmlPullParserException("No start tag found")
    }
    return this
}

@SuppressWarnings("RestrictedApi")
internal fun XmlPullParser.createVectorImageBuilder(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet
): VectorAssetBuilder {
    val vectorAttrs = TypedArrayUtils.obtainAttributes(
        res,
        theme,
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY
    )

    // TODO (njawad) handle mirroring here
//        state.mAutoMirrored = TypedArrayUtils.getNamedBoolean(a, parser, "autoMirrored",
//                AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_AUTO_MIRRORED, state.mAutoMirrored)

    val viewportWidth = TypedArrayUtils.getNamedFloat(
        vectorAttrs,
        this,
        "viewportWidth",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_WIDTH,
        0.0f
    )

    val viewportHeight = TypedArrayUtils.getNamedFloat(
        vectorAttrs,
        this,
        "viewportHeight",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_HEIGHT,
        0.0f
    )

    if (viewportWidth <= 0) {
        throw XmlPullParserException(
            vectorAttrs.positionDescription + "<VectorGraphic> tag requires viewportWidth > 0"
        )
    } else if (viewportHeight <= 0) {
        throw XmlPullParserException(
            vectorAttrs.positionDescription + "<VectorGraphic> tag requires viewportHeight > 0"
        )
    }

    val defaultWidth = vectorAttrs.getDimension(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_WIDTH, 0.0f
    )
    val defaultHeight = vectorAttrs.getDimension(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_HEIGHT, 0.0f
    )

    val defaultWidthDp = (defaultWidth / res.displayMetrics.density).dp
    val defaultHeightDp = (defaultHeight / res.displayMetrics.density).dp

    vectorAttrs.recycle()

    return VectorAssetBuilder(
        defaultWidth = defaultWidthDp,
        defaultHeight = defaultHeightDp,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight
    )
}

@Throws(IllegalArgumentException::class)
@SuppressWarnings("RestrictedApi")
internal fun XmlPullParser.parsePath(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: VectorAssetBuilder
) {
    val a = TypedArrayUtils.obtainAttributes(
        res,
        theme,
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH
    )

    val hasPathData = TypedArrayUtils.hasAttribute(this, "pathData")
    if (!hasPathData) {
        // If there is no pathData in the VPath tag, then this is an empty VPath,
        // nothing need to be drawn.
        throw IllegalArgumentException("No path data available")
    }

    val name: String = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_NAME) ?: ""

    val pathStr = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_PATH_DATA)

    val pathData: List<PathNode> = addPathNodes(pathStr)

    val fillColor = TypedArrayUtils.getNamedComplexColor(
        a,
        this,
        theme,
        "fillColor",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_COLOR, 0
    )
    val fillAlpha = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "fillAlpha",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_ALPHA, 1.0f
    )
    val lineCap = TypedArrayUtils.getNamedInt(
        a,
        this,
        "strokeLineCap",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_CAP, -1
    )
    val strokeLineCap = getStrokeLineCap(lineCap, StrokeCap.butt)
    val lineJoin = TypedArrayUtils.getNamedInt(
        a,
        this,
        "strokeLineJoin",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_JOIN, -1
    )
    val strokeLineJoin =
        getStrokeLineJoin(lineJoin, StrokeJoin.bevel)
    val strokeMiterLimit = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "strokeMiterLimit",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_MITER_LIMIT,
        1.0f
    )
    val strokeColor = TypedArrayUtils.getNamedComplexColor(
        a,
        this,
        theme,
        "strokeColor",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_COLOR, 0
    )
    val strokeAlpha = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "strokeAlpha",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_ALPHA, 1.0f
    )
    val strokeLineWidth = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "strokeWidth",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_WIDTH, 1.0f
    )

    // TODO (njawad) handle trim paths + fill rule
//    val trimPathEnd = TypedArrayUtils.getNamedFloat(
//        a, this, "trimPathEnd",
//        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_END, 1.0f
//    )
//    val trimPathOffset = TypedArrayUtils.getNamedFloat(
//        a, this, "trimPathOffset",
//        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_OFFSET,
//        0.0f
//    )
//    val trimPathStart = TypedArrayUtils.getNamedFloat(
//        a, this, "trimPathStart",
//        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_START,
//        0.0f
//    )
//    val fillRule = TypedArrayUtils.getNamedInt(
//        a, this, "fillType",
//        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_FILLTYPE,
//        FILL_TYPE_WINDING
//    )

    a.recycle()

    val fillBrush = obtainBrushFromComplexColor(fillColor)
    val strokeBrush = obtainBrushFromComplexColor(strokeColor)

    builder.addPath(
        pathData,
        name,
        fillBrush,
        fillAlpha,
        strokeBrush,
        strokeAlpha,
        strokeLineWidth,
        strokeLineCap,
        strokeLineJoin,
        strokeMiterLimit)
}

@SuppressWarnings("RestrictedApi")
private fun obtainBrushFromComplexColor(complexColor: ComplexColorCompat): Brush? =
    if (complexColor.willDraw()) {
        val shader = complexColor.shader
        if (shader != null) {
            ShaderBrush(Shader(shader))
        } else {
            SolidColor(Color(complexColor.color))
        }
    } else {
        null
    }

@SuppressWarnings("RestrictedApi")
internal fun XmlPullParser.parseGroup(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: VectorAssetBuilder
) {
    val a = TypedArrayUtils.obtainAttributes(
        res,
        theme,
        attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP
    )

    // Account for any configuration changes.
    // mChangingConfigurations |= Utils.getChangingConfigurations(a);

    // Extract the theme attributes, if any.
    // mThemeAttrs = null // TODO TINT THEME Not supported yet a.extractThemeAttrs();

    // This is added in API 11
    val rotate = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "rotation",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_ROTATION,
        DefaultRotation
    )

    val pivotX = a.getFloat(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_X,
        DefaultPivotX
    )
    val pivotY = a.getFloat(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_Y,
        DefaultPivotY
    )

    // This is added in API 11
    val scaleX = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "scaleX",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_X,
        DefaultScaleX
    )

    // This is added in API 11
    val scaleY = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "scaleY",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_Y,
        DefaultScaleY
    )

    val translateX = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "translationX",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_X,
        DefaultTranslationX
    )
    val translateY = TypedArrayUtils.getNamedFloat(
        a,
        this,
        "translationY",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_Y,
        DefaultTranslationY
    )

    val name: String =
        a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_NAME) ?: ""

    // TODO parse clip path
    val clipPathData = EmptyPath

    a.recycle()

    builder.pushGroup(
        name,
        rotate,
        pivotX,
        pivotY,
        scaleX,
        scaleY,
        translateX,
        translateY,
        clipPathData
    )
}

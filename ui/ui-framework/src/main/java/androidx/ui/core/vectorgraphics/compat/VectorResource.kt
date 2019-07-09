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

package androidx.ui.core.vectorgraphics.compat

import android.annotation.SuppressLint
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import androidx.core.content.res.TypedArrayUtils
import androidx.ui.core.vectorgraphics.addPathNodes
import androidx.ui.core.vectorgraphics.group
import androidx.ui.core.vectorgraphics.path
import androidx.ui.core.vectorgraphics.vector
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.core.vectorgraphics.DefaultPivotX
import androidx.ui.core.vectorgraphics.DefaultPivotY
import androidx.ui.core.vectorgraphics.DefaultRotate
import androidx.ui.core.vectorgraphics.DefaultScaleX
import androidx.ui.core.vectorgraphics.DefaultScaleY
import androidx.ui.core.vectorgraphics.DefaultTranslateX
import androidx.ui.core.vectorgraphics.DefaultTranslateY
import androidx.ui.core.vectorgraphics.EmptyBrush
import androidx.ui.core.vectorgraphics.EmptyPath
import androidx.ui.core.vectorgraphics.PathNode
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

private val LINECAP_BUTT = 0
private val LINECAP_ROUND = 1
private val LINECAP_SQUARE = 2

private val LINEJOIN_MITER = 0
private val LINEJOIN_ROUND = 1
private val LINEJOIN_BEVEL = 2

private val FILL_TYPE_WINDING = 0

private val SHAPE_CLIP_PATH = "clip-VPath"
private val SHAPE_GROUP = "group"
private val SHAPE_PATH = "path"

private val LOGTAG = "VectorGraphicCreator"

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

@Composable
@SuppressWarnings("RestrictedApi")
private fun inflateGroup(
    a: TypedArray,
    parser: XmlPullParser,
    @Suppress("UNUSED_PARAMETER") theme: Resources.Theme?,
    @Children childNodes: @Composable() () -> Unit
) {
    // Account for any configuration changes.
    // mChangingConfigurations |= Utils.getChangingConfigurations(a);

    // Extract the theme attributes, if any.
    // mThemeAttrs = null // TODO TINT THEME Not supported yet a.extractThemeAttrs();

    // This is added in API 11
    val rotate = TypedArrayUtils.getNamedFloat(
        a, parser, "rotation",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_ROTATION,
        DefaultRotate
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
        a, parser, "scaleX",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_X,
        DefaultScaleX
    )

    // This is added in API 11
    val scaleY = TypedArrayUtils.getNamedFloat(
        a, parser, "scaleY",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_Y,
        DefaultScaleY
    )

    val translateX = TypedArrayUtils.getNamedFloat(
        a, parser, "translateX",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_X,
        DefaultTranslateX
    )
    val translateY = TypedArrayUtils.getNamedFloat(
        a, parser, "translateY",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_Y,
        DefaultTranslateY
    )

    val name: String =
        a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_NAME) ?: ""

    parser.next()

    // TODO parse clip path
    val clipPathData = EmptyPath
    group(
        name = name,
        rotate = rotate,
        scaleX = scaleX,
        scaleY = scaleY,
        translateX = translateX,
        translateY = translateY,
        pivotX = pivotX,
        pivotY = pivotY,
        clipPathData = clipPathData
    ) {
        childNodes()
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun inflateClip(
    a: TypedArray,
    parser: XmlPullParser,
    theme: Resources.Theme?,
    @Children childNodes: @Composable() () -> Unit
) {
//    var pathName: String? =
//        a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_NAME)
//    if (pathName == null) {
//        pathName = ""
//    }
//    val pathData = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_PATH_DATA)

    // TODO (njawad) finish parsing clip paths from xml resources
}

@Composable
@SuppressWarnings("RestrictedApi")
private fun inflatePath(a: TypedArray, parser: XmlPullParser, theme: Resources.Theme?) {
    val hasPathData = TypedArrayUtils.hasAttribute(parser, "pathData")
    if (!hasPathData) {
        // If there is no pathData in the VPath tag, then this is an empty VPath,
        // nothing need to be drawn.
        Log.v("VectorPath", "no path data available skipping path")
        return
    }

    val name: String = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_NAME) ?: ""

    val pathStr = a.getString(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_PATH_DATA)

    val pathData: Array<PathNode> = addPathNodes(pathStr)

    val fillColor = TypedArrayUtils.getNamedComplexColor(
        a, parser, theme, "fillColor",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_COLOR, 0
    )
    // TODO(njawad): restore these when they are used
/*    val fillAlpha = TypedArrayUtils.getNamedFloat(
        a, parser, "fillAlpha",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_ALPHA, 1.0f
    )
    val lineCap = TypedArrayUtils.getNamedInt(
        a, parser, "strokeLineCap",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_CAP, -1
    )
    val strokeLineCap =
        getStrokeLineCap(lineCap, StrokeCap.butt)
    val lineJoin = TypedArrayUtils.getNamedInt(
        a, parser, "strokeLineJoin",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_JOIN, -1
    )
    val strokeLineJoin =
        getStrokeLineJoin(lineJoin, StrokeJoin.bevel)
    val strokeMiterlimit = TypedArrayUtils.getNamedFloat(
        a, parser, "strokeMiterLimit",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_MITER_LIMIT,
        1.0f
    )*/
    val strokeColor = TypedArrayUtils.getNamedComplexColor(
        a, parser, theme, "strokeColor",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_COLOR, 0
    )
    val strokeAlpha = TypedArrayUtils.getNamedFloat(
        a, parser, "strokeAlpha",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_ALPHA, 1.0f
    )
    val strokeLineWidth = TypedArrayUtils.getNamedFloat(
        a, parser, "strokeWidth",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_WIDTH, 1.0f
    )
    /*val trimPathEnd = TypedArrayUtils.getNamedFloat(
        a, parser, "trimPathEnd",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_END, 1.0f
    )
    val trimPathOffset = TypedArrayUtils.getNamedFloat(
        a, parser, "trimPathOffset",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_OFFSET,
        0.0f
    )
    val trimPathStart = TypedArrayUtils.getNamedFloat(
        a, parser, "trimPathStart",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_START,
        0.0f
    )
    val fillRule = TypedArrayUtils.getNamedInt(
        a, parser, "fillType",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_FILLTYPE,
        FILL_TYPE_WINDING
    )*/

    // TODO update path with additional params
    path(
        name = name,
        fill = if (fillColor.willDraw()) fillColor.color else EmptyBrush,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = strokeLineWidth,
        stroke = if (strokeColor.willDraw()) strokeColor.color else EmptyBrush,
        pathData = pathData
    )
}

@Composable
@SuppressWarnings("RestrictedApi")
private fun inflateInner(
    res: Resources,
    attrs: AttributeSet,
    theme: Resources.Theme?,
    innerDepth: Int,
    parser: XmlPullParser
) {
    var eventType = parser.getEventType()

    // Parse everything until the end of the VectorGraphic element.
    while (eventType != XmlPullParser.END_DOCUMENT &&
        (parser.getDepth() >= innerDepth || eventType != XmlPullParser.END_TAG)
    ) {
        if (eventType == XmlPullParser.START_TAG) {
            val tagName = parser.getName()
            if (SHAPE_PATH.equals(tagName)) {
                Log.v("VectorPath", "parsing path...")
                val a = TypedArrayUtils.obtainAttributes(
                    res, theme, attrs,
                    AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH
                )
                inflatePath(a = a, parser = parser, theme = theme)
                a.recycle()
            } else if (SHAPE_CLIP_PATH.equals(tagName)) {
                val a = TypedArrayUtils.obtainAttributes(
                    res, theme, attrs,
                    AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH
                )
                inflateClip(a = a, parser = parser, theme = theme) {
                    inflateInner(
                        res = res,
                        parser = parser,
                        attrs = attrs,
                        theme = theme,
                        innerDepth = innerDepth
                    )
                }
                a.recycle()
            } else if (SHAPE_GROUP.equals(tagName)) {
                val a = TypedArrayUtils.obtainAttributes(
                    res, theme, attrs,
                    AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP
                )
                inflateGroup(a = a, parser = parser, theme = theme) {
                    inflateInner(
                        res = res,
                        parser = parser,
                        attrs = attrs,
                        theme = theme,
                        innerDepth = innerDepth
                    )
                }
                a.recycle()
            }
        }
        eventType = parser.next()
    }
}

@Composable
@SuppressWarnings("RestrictedApi")
private fun inflate(
    res: Resources,
    parser: XmlPullParser,
    attrs: AttributeSet,
    theme: Resources.Theme?
) {
    val innerDepth = parser.getDepth() + 1

    val vectorAttrs = TypedArrayUtils.obtainAttributes(
        res, theme, attrs,
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY
    )

    // TODO (njawad) handle mirroring here
//        state.mAutoMirrored = TypedArrayUtils.getNamedBoolean(a, parser, "autoMirrored",
//                AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_AUTO_MIRRORED, state.mAutoMirrored)

    val viewportWidth = TypedArrayUtils.getNamedFloat(
        vectorAttrs, parser, "viewportWidth",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_WIDTH,
        0.0f
    )

    val viewportHeight = TypedArrayUtils.getNamedFloat(
        vectorAttrs, parser, "viewportHeight",
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_HEIGHT,
        0.0f
    )

    if (viewportWidth <= 0) {
        throw XmlPullParserException(
            vectorAttrs.getPositionDescription() +
                    "VectorGraphic requires viewportWidth > 0"
        )
    } else if (viewportHeight <= 0) {
        throw XmlPullParserException(
            vectorAttrs.getPositionDescription() +
                    "VectorGraphic requires viewportHeight > 0"
        )
    }

    val defaultWidth = vectorAttrs.getDimension(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_WIDTH, 0.0f
    )
    val defaultHeight = vectorAttrs.getDimension(
        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_HEIGHT, 0.0f
    )

    vector(
        name = "vector",
        defaultWidth = defaultWidth,
        defaultHeight = defaultHeight,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight
    ) {
        inflateInner(
            res = res,
            attrs = attrs,
            theme = theme,
            innerDepth = innerDepth,
            parser = parser
        )
    }
}

@Composable
fun vectorResource(res: Resources, resId: Int) {
    @SuppressLint("ResourceType") val parser = res.getXml(resId)
    val attrs = Xml.asAttributeSet(parser)
    var type: Int
    try {
        type = parser.next()
        while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
            type = parser.next()
        }
        if (type != XmlPullParser.START_TAG) {
            throw XmlPullParserException("No start tag found")
        }
        inflate(res = res, parser = parser, attrs = attrs, theme = null)
    } catch (e: XmlPullParserException) {
        Log.e(LOGTAG, "parser error", e)
    } catch (e: IOException) {
        Log.e(LOGTAG, "parser error", e)
    }
}

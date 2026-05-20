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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.modifiers.RecordingModifier

/** Root scope for building RemoteCompose documents. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @DslMarker public annotation class RcDslMarker

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcScope {

    /** Adds a [Box] layout to the document. */
    public fun Box(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcScope.() -> Unit = {},
    )

    /** Adds a [FitBox] layout to the document. */
    public fun FitBox(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcScope.() -> Unit = {},
    )

    /** Adds a [StateLayout] layout to the document. */
    public fun StateLayout(
        stateIndex: RcInteger,
        modifier: Modifier = Modifier,
        content: RcScope.() -> Unit = {},
    )

    /** Adds a [Column] layout to the document. */
    public fun Column(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcColumnVerticalPositioning = RcColumnVerticalPositioning.Top,
        content: RcColumnScope.() -> Unit = {},
    )

    /** Adds a [Row] layout to the document. */
    public fun Row(
        modifier: Modifier = Modifier,
        horizontal: RcRowHorizontalPositioning = RcRowHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcRowScope.() -> Unit = {},
    )

    /** Adds a [Flow] layout to the document. */
    public fun Flow(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        maxItemsInEachRow: Int = Int.MAX_VALUE,
        maxLines: Int = Int.MAX_VALUE,
        content: RcFlowScope.() -> Unit = {},
    )

    /** Adds a [CollapsibleColumn] layout to the document. */
    public fun CollapsibleColumn(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcCollapsibleColumnScope.() -> Unit = {},
    )

    /** Adds a [CollapsibleRow] layout to the document. */
    public fun CollapsibleRow(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcCollapsibleRowScope.() -> Unit = {},
    )

    /** Adds a [Text] component to the document. */
    public fun Text(
        text: String,
        modifier: Modifier = Modifier,
        color: Any = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        fontWeight: Float = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    )

    /** Adds a [Text] component using a remote string reference. */
    public fun Text(
        text: RcText,
        modifier: Modifier = Modifier,
        color: Any = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        fontWeight: Float = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    )

    /** Adds an [Image] component to the document. */
    public fun Image(
        image: RcImage,
        modifier: Modifier = Modifier,
        contentDescription: String? = null,
        contentScale: RcContentScale = RcContentScale.Fit,
        alpha: Float = 1f,
    )

    /** Adds a [Canvas] component to the document. */
    public fun Canvas(modifier: Modifier = Modifier, content: RcCanvasScope.() -> Unit)

    /** Executes a block with the current paint. */
    public fun applyPaint(block: RcPaint.() -> Unit)

    /**
     * Executes [block] with a typed [RcPaintScope] wrapping the current paint. Prefer this over
     * [applyPaint] — uses typed value classes (`RcColor`, `RcPaintStyle`, `RcStrokeCap`,
     * `RcBlendMode`, ...) instead of raw `Int` opcodes.
     *
     * Named differently from [applyPaint] because the JVM erases both method signatures to
     * `(Lkotlin/jvm/functions/Function1;)V`; same-name overloads with different lambda receivers
     * can't coexist on a Kotlin interface.
     */
    public fun paint(block: RcPaintScope.() -> Unit)

    /** Executes a block within a global section. */
    public fun Global(block: RcScope.() -> Unit)

    /**
     * Starts a global section. Prefer the block-form [Global] which guarantees a matched
     * [endGlobal] call.
     */
    public fun beginGlobal()

    /**
     * Ends a global section. Prefer the block-form [Global] which guarantees a matched
     * [beginGlobal] call.
     */
    public fun endGlobal()

    /**
     * Start a list of canvas operations. Prefer the block-form [canvasOperations] which guarantees
     * a matched [endCanvasOperations] call.
     */
    public fun startCanvasOperations()

    /**
     * End a list of canvas operations. Prefer the block-form [canvasOperations] which guarantees a
     * matched [startCanvasOperations] call.
     */
    public fun endCanvasOperations()

    /** In the context of a draw modifier, draw the component content */
    public fun drawComponentContent()

    /** Registers a text resource and returns its reference. */
    public fun remoteText(text: String): RcText

    /** Registers a named text resource and returns its reference. */
    public fun remoteNamedText(name: String, text: String): RcText

    /** Registers a string list resource and returns its reference. */
    public fun remoteArrayOf(vararg strings: String): RcTextList

    /** Returns a remote text lookup from the specified data set and index. */
    public fun textLookup(dataSetId: RcTextList, index: RcFloat): RcText

    public operator fun RcTextList.get(index: RcFloat): RcText {
        return textLookup(this, index)
    }

    /** Returns a remote text lookup from the specified data set and index. */
    public fun textLookup(dataSetId: RcTextList, indexId: RcInteger): RcText

    /** Merges two remote text references. */
    public fun textMerge(text1: RcText, text2: RcText): RcText

    public operator fun RcText.plus(v: RcText): RcText = textMerge(this, v)

    public operator fun String.plus(v: RcText): RcText = textMerge(remoteText(this), v)

    public operator fun RcText.plus(v: String): RcText = textMerge(this, remoteText(v))

    /** Registers a color resource and returns its reference. */
    public fun remoteColor(color: Int): RcColor

    public fun remoteColorValue(color: Int): RcColorValue

    /** Registers a color resource from a Long and returns its reference. */
    public fun remoteColorValue(color: Long): RcColorValue = remoteColorValue(color.toInt())

    /** Registers a named color resource and returns its reference. */
    public fun remoteNamedColor(name: String, color: Int): RcColor

    /** Registers a named color resource from a Long and returns its reference. */
    public fun remoteNamedColor(name: String, color: Long): RcColor =
        remoteNamedColor(name, color.toInt())

    /** Registers a themed color resource and returns its reference. */
    public fun remoteThemedColor(light: Int, dark: Int): RcColor

    /** Registers a themed color resource from Longs and returns its reference. */
    public fun remoteThemedColor(light: Long, dark: Long): RcColor =
        remoteThemedColor(light.toInt(), dark.toInt())

    /** Registers a themed color resource and returns its reference. */
    public fun remoteThemedColor(light: RcColor, dark: RcColor): RcColor

    /**
     * Registers a themed color resource with names and default values and returns its reference.
     */
    public fun remoteThemedColor(
        light: String,
        lightDefault: Int,
        dark: String,
        darkDefault: Int,
    ): RcColor

    /** Creates a color resource from an HSV expression. */
    public fun remoteColorExpression(alpha: Int, hue: RcFloat, sat: Float, value: Float): RcColor

    /** Creates a color resource from an HSV expression. */
    public fun remoteColorExpression(alpha: Int, hue: Float, sat: Float, value: Float): RcColor

    /** Creates a text resource from a float value with formatting. */
    public fun createTextFromFloat(value: Float, whole: Int, decimal: Int, flags: Int): RcText

    /** Creates a text resource from a remote float with formatting. */
    public fun createTextFromFloat(value: RcFloat, whole: Int, decimal: Int, flags: Int): RcText

    /** Registers a float array and returns its reference. */
    public fun remoteFloatArray(array: FloatArray): RcFloat

    /** Returns an [RcFloat] representing the current animation time. */
    public fun animationTime(): RcFloat

    /** Returns an [RcFloat] representing the last touch event time. */
    public fun touchTime(): RcFloat

    /** Returns an [RcFloat] representing the current day of the week. */
    public fun dayOfWeek(): RcFloat

    /** Returns an [RcFloat] representing the current day of the month. */
    public fun dayOfMonth(): RcFloat

    /** Returns an [RcFloat] representing the current hour. */
    public fun hour(): RcFloat

    /** Returns an [RcFloat] representing the current minutes. */
    public fun minutes(): RcFloat

    /** Returns an [RcFloat] representing the current seconds. */
    public fun seconds(): RcFloat

    /** Returns an [RcFloat] representing continuous seconds. */
    public fun continuousSeconds(): RcFloat

    /** Returns an [RcFloat] representing the maximum of [a] and [b]. */
    public fun max(a: RcFloat, b: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the maximum of [a] and [b]. */
    public fun max(a: Float, b: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the maximum of [a] and [b]. */
    public fun max(a: RcFloat, b: Float): RcFloat

    /** Returns an [RcFloat] representing the minimum of [a] and [b]. */
    public fun min(a: RcFloat, b: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the minimum of [a] and [b]. */
    public fun min(a: Float, b: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the minimum of [a] and [b]. */
    public fun min(a: RcFloat, b: Float): RcFloat

    /** Returns an [RcFloat] representing the sign of [v]. */
    public fun sign(v: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the sine of [v]. */
    public fun sin(v: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the cosine of [v]. */
    public fun cos(v: RcFloat): RcFloat

    public fun abs(v: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the maximum value in the [array]. */
    public fun arrayMax(array: RcFloat): RcFloat

    /** Returns an [RcFloat] representing the minimum value in the [array]. */
    public fun arrayMin(array: RcFloat): RcFloat

    /** Returns an [RcFloat] interpolated from [array] at [position]. */
    public fun arraySpline(array: RcFloat, position: RcFloat): RcFloat

    /** Returns an [RcFloat] interpolated from [array] at [position]. */
    public fun arraySpline(array: RcFloat, position: Float): RcFloat

    public fun arrayLength(a: RcFloat): RcFloat

    public fun arraySum(a: RcFloat): RcFloat

    public fun arraySum(a: RcFloat, index: RcFloat): RcFloat

    public fun arraySumXY(a: RcFloat, b: RcFloat): RcFloat

    public fun arraySumSqr(a: RcFloat): RcFloat

    public fun arrayAvg(a: RcFloat): RcFloat

    /**
     * Adds a XY path expression and returns its reference.
     *
     * @param expressionX the expression for the radius
     * @param expressionY the expression for the radius
     * @param start the start angle
     * @param end the end angle
     * @param count the number of points
     * @param type the path type (see [Rc.PathExpression])
     */
    public fun remoteXYPath(
        expressionX: RcFloat,
        expressionY: RcFloat,
        start: Float,
        end: Float,
        count: Int,
        type: RcPathType = RcPathType.Spline,
    ): RcPath

    /**
     * Adds a polar path expression and returns its reference.
     *
     * @param expression the expression for the radius
     * @param start the start angle
     * @param end the end angle
     * @param count the number of points
     * @param centerX the center x coordinate
     * @param centerY the center y coordinate
     * @param type the path type (see [Rc.PathExpression])
     */
    public fun remotePolarPath(
        expression: RcFloat,
        start: Float,
        end: Float,
        count: Int,
        centerX: RcFloat,
        centerY: RcFloat,
        type: RcPathType = RcPathType.Spline,
    ): RcPath

    /**
     * Returns an [RcFloat] representing a function of a single variable.
     *
     * @param block the function block
     */
    public fun rFun(block: (RcFloat) -> RcFloat): RcFloat

    public fun density(): RcFloat

    public fun fontSize(): RcFloat

    public fun apiLevel(): RcFloat

    /** Returns the window's width as a remote float. */
    public fun windowWidth(): RcFloat

    /** Returns the window's height as a remote float. */
    public fun windowHeight(): RcFloat

    /** CONTINUOUS_SEC is seconds from midnight looping every hour 0-3600 */
    public fun continuousSec(): RcFloat

    /** ID_OFFSET_TO_UTC is the offset from UTC in sec (typically / 3600f) */
    public fun utcOffset(): RcFloat

    /** Moth of Year quantized to MONTHS 1-12. 1 = January */
    public fun month(): RcFloat

    /** Returns the component's width as a remote float. */
    public fun componentWidth(): RcFloat

    /** Returns the component's height as a remote float. */
    public fun componentHeight(): RcFloat

    /** Registers a bitmap resource and returns its reference. */
    public fun remoteBitmap(image: Any): RcImage

    /** Registers a named bitmap resource and returns its reference. */
    public fun remoteNamedBitmap(name: String, image: Any): RcImage

    /** Registers a bitmap URL resource and returns its reference. */
    public fun remoteBitmapUrl(url: String): RcImage

    /** Registers a named bitmap URL resource and returns its reference. */
    public fun remoteNamedBitmapUrl(name: String, url: String): RcImage

    /** Registers a float variable and returns its reference. */
    public fun remoteFloat(value: Float): RcFloat

    /** Registers a named float variable and returns its reference. */
    public fun remoteNamedFloat(name: String, value: Float): RcFloat

    /** Registers an integer variable and returns its reference. */
    public fun remoteInteger(value: Int): RcInteger

    /** Registers a named integer variable and returns its reference. */
    public fun remoteNamedInteger(name: String, value: Int): RcInteger

    /** Registers a long constant and returns its reference. */
    public fun remoteLong(value: Long): RcInteger

    /** Registers a boolean constant and returns its reference. */
    public fun remoteBoolean(value: Boolean): RcInteger

    /** Registers a path data resource and returns its reference. */
    public fun remotePathData(path: RcPlatformServices.RcPathArrayCreator): RcPath

    /** Registers a path string resource and returns its reference. */
    //    public fun remotePathString(path: String): RcPath

    /** Registers a text style resource and returns its reference. */
    public fun remoteTextStyle(
        fontSize: RcSp?,
        color: Int? = null,
        fontWeight: Float? = null,
        textAlign: RcTextAlign? = null,
    ): RcTextStyle

    /** Registers a text style resource from a Long color and returns its reference. */
    public fun remoteTextStyle(
        fontSize: RcSp?,
        color: Long,
        fontWeight: Float? = null,
        textAlign: RcTextAlign? = null,
    ): RcTextStyle = remoteTextStyle(fontSize, color.toInt(), fontWeight, textAlign)

    /** Saves the current canvas state. */
    public fun save()

    /** Restores the previous canvas state. */
    public fun restore()

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: Float, scaleY: Float)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: RcFloat, scaleY: RcFloat)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: RcFloat, scaleY: RcFloat, centerX: RcFloat, centerY: RcFloat)

    /** Draws a rectangle. */
    public fun drawRect(left: Float, top: Float, right: Float, bottom: Float)

    /** Draws a rectangle using remote floats. */
    public fun drawRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** Draws a circle. */
    public fun drawCircle(centerX: Float, centerY: Float, radius: Float)

    /** Draws a circle using remote floats. */
    public fun drawCircle(centerX: RcFloat, centerY: RcFloat, radius: RcFloat)

    /** Draws a line. */
    public fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float)

    /** Draws a line using remote floats. */
    public fun drawLine(x1: RcFloat, y1: RcFloat, x2: RcFloat, y2: RcFloat)

    /** Draws an arc. */
    public fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    )

    /** Draws an arc using remote floats. */
    public fun drawArc(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        startAngle: RcFloat,
        sweepAngle: RcFloat,
    )

    /** Draws a sector. */
    public fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    )

    /** Draws a sector using remote floats. */
    public fun drawSector(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        startAngle: RcFloat,
        sweepAngle: RcFloat,
    )

    /** Draws an oval. */
    public fun drawOval(left: Float, top: Float, right: Float, bottom: Float)

    /** Draws an oval using remote floats. */
    public fun drawOval(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** Draws a rounded rectangle. */
    public fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    )

    /** Draws a rounded rectangle using remote floats. */
    public fun drawRoundRect(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        radiusX: RcFloat,
        radiusY: RcFloat,
    )

    /** Draws a path. */
    public fun drawPath(path: RcPath)

    /** Draws a path that interpolates between two other paths. */
    public fun drawTweenPath(path1: RcPath, path2: RcPath, tween: Float, start: Float, stop: Float)

    /** Draws a path that interpolates between two other paths using remote floats. */
    public fun drawTweenPath(
        path1: RcPath,
        path2: RcPath,
        tween: RcFloat,
        start: RcFloat,
        stop: RcFloat,
    )

    /** Draws a text with specified anchor/pan. */
    public fun drawTextAnchored(
        text: RcText,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int = 0,
    )

    /** Draws a text with specified anchor/pan using remote floats. */
    public fun drawTextAnchored(
        text: RcText,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        flags: Int = 0,
    )

    /** Draws a text along a path. */
    public fun drawTextOnPath(text: RcText, path: RcPath, hOffset: Float, vOffset: Float)

    /** Draws a text along a path using remote floats. */
    public fun drawTextOnPath(text: RcText, path: RcPath, hOffset: RcFloat, vOffset: RcFloat)

    /** Draws a text on a circle. */
    public fun drawTextOnCircle(
        text: RcText,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        warpRadiusOffset: Float = 0f,
        alignment: DrawTextOnCircle.Alignment = DrawTextOnCircle.Alignment.START,
        placement: DrawTextOnCircle.Placement = DrawTextOnCircle.Placement.OUTSIDE,
    )

    /** Draws a text on a circle using remote floats. */
    public fun drawTextOnCircle(
        text: RcText,
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        startAngle: RcFloat,
        warpRadiusOffset: RcFloat = RcFloat(0f),
        alignment: DrawTextOnCircle.Alignment = DrawTextOnCircle.Alignment.START,
        placement: DrawTextOnCircle.Placement = DrawTextOnCircle.Placement.OUTSIDE,
    )

    /** Draws a bitmap at the specified coordinates. */
    public fun drawBitmap(image: RcImage, left: Float, top: Float, right: Float, bottom: Float)

    /** Draws a bitmap at the specified position. */
    public fun drawBitmap(image: RcImage, left: Float, top: Float)

    /** Draws a bitmap at the specified coordinates using remote floats. */
    public fun drawBitmap(
        image: RcImage,
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
    )

    /** Draws a bitmap at the specified position using remote floats. */
    public fun drawBitmap(image: RcImage, left: RcFloat, top: RcFloat)

    /** Draws a scaled bitmap within source and destination rectangles. */
    public fun drawScaledBitmap(
        image: RcImage,
        srcLeft: Float,
        srcTop: Float,
        srcRight: Float,
        srcBottom: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        scaleType: RcContentScale = RcContentScale.None,
        scaleFactor: Float = 1f,
        contentDescription: String? = null,
    )

    /** Draws a scaled bitmap within source and destination rectangles using remote floats. */
    public fun drawScaledBitmap(
        image: RcImage,
        srcLeft: RcFloat,
        srcTop: RcFloat,
        srcRight: RcFloat,
        srcBottom: RcFloat,
        dstLeft: RcFloat,
        dstTop: RcFloat,
        dstRight: RcFloat,
        dstBottom: RcFloat,
        scaleType: RcContentScale = RcContentScale.None,
        scaleFactor: RcFloat = 1f.rf,
        contentDescription: String? = null,
    )

    /** Registers a new bitmap resource with the given dimensions. */
    public fun createBitmap(width: Int, height: Int): RcImage

    /** Redirects subsequent drawing operations to the specified bitmap resource. */
    public fun drawOnBitmap(
        image: RcImage,
        mode: DrawOnBitmapMode = DrawOnBitmapMode.CLEAR,
        color: RcColorValue,
        block: RcScope.() -> Unit,
    )

    /**
     * Registers and compiles a custom shader from a string definition, with optional uniform
     * builder.
     */
    public fun createShader(
        shaderString: String,
        block: (androidx.compose.remote.creation.RemoteComposeShader.() -> Unit)? = null,
    ): RcShader

    /**
     * Registers a custom shader and configures its uniforms via a typed [RcShaderScope]. Prefer
     * this over [createShader] — uniform setters are picked by value type (no reflection, no
     * `Any`).
     *
     * Named differently from [createShader] for the same JVM-erasure reason as [paint] vs
     * [applyPaint] above.
     */
    public fun shader(shaderString: String, block: RcShaderScope.() -> Unit): RcShader

    /** Adds a spacer component. */
    public fun Spacer(modifier: Modifier)

    /** debug */
    public fun debug(msg: String, value: RcFloat)

    /** Combines two paths using the specified operation. */
    public fun RcPath.combine(path2: RcPath, op: RcPathCombineOp): RcPath

    /** Performs a haptic feedback. */
    public fun performHaptic(feedbackConstant: Int)

    /** Tells the system to wake up in a given number of seconds. */
    public fun wakeIn(seconds: Float)

    /** Returns the color attribute. */
    public fun getColorAttribute(baseColor: RcColor, type: Short): RcFloat

    /** Returns a substring of the text. */
    public fun RcText.substring(start: RcFloat, len: RcFloat): RcText

    /** Returns a substring of the text. */
    public fun RcText.substring(start: Float, len: Float): RcText

    /** Measure bitmap font text dimensions. */
    public fun RcText.measure(
        bmFontId: RcBitmapFont,
        measureWidth: Int,
        glyphSpacing: Float,
    ): RcFloat

    /** Measure bitmap font text dimensions. */
    public fun RcBitmapFont.measure(
        text: RcText,
        type: BitmapTextMeasure,
        glyphSpacing: Float,
    ): RcFloat

    /** Registers a bitmap font and returns its reference. */
    public fun createBitmapFont(glyphs: Array<BitmapFontData.Glyph>): RcBitmapFont

    /** Registers a bitmap font with kerning table and returns its reference. */
    public fun createBitmapFont(
        glyphs: Array<BitmapFontData.Glyph>,
        kerningTable: Map<String, Short>,
    ): RcBitmapFont

    /** Draws a text run using a bitmap font. */
    public fun drawText(
        text: RcText,
        font: RcBitmapFont,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        glyphSpacing: Float,
    )

    /** Draws a text run using a bitmap font. */
    public fun drawText(
        text: RcText,
        font: RcBitmapFont,
        start: Int,
        end: Int,
        x: RcFloat,
        y: RcFloat,
        glyphSpacing: Float,
    )

    /** Draws a text run along a path using a bitmap font. */
    public fun drawTextOnPath(
        text: RcText,
        font: RcBitmapFont,
        path: RcPath,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float,
    )

    /** Draws a text run along a path using a bitmap font. */
    public fun drawTextOnPath(
        text: RcText,
        font: RcBitmapFont,
        path: RcPath,
        start: Int,
        end: Int,
        yAdj: RcFloat,
        glyphSpacing: Float,
    )

    /** Draws a text anchored using a bitmap font. */
    public fun drawTextAnchored(
        text: RcText,
        font: RcBitmapFont,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        glyphSpacing: Float,
    )

    /** Draws a text anchored using a bitmap font. */
    public fun drawTextAnchored(
        text: RcText,
        font: RcBitmapFont,
        start: RcFloat,
        end: RcFloat,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        glyphSpacing: Float,
    )

    /** Interpolates between two paths. */
    public fun RcPath.tween(path2: RcPath, tween: Float): RcPath

    /** Interpolates between two paths using a remote float. */
    public fun RcPath.tween(path2: RcPath, tween: RcFloat): RcPath

    /** Sets the name of a color variable. */
    public fun RcColor.setName(name: String)

    /** Sets the name of a text variable. */
    public fun RcText.setName(name: String)

    /** Sets the name of a float variable. */
    public fun setFloatName(value: RcFloat, name: String)

    public fun RcFloat.setName(name: String)

    /** Transforms the text. */
    public fun RcText.transform(start: RcFloat, len: RcFloat, operation: RcTextTransformOp): RcText

    /** Transforms the text. */
    public fun RcText.transform(start: Float, len: Float, operation: RcTextTransformOp): RcText

    /** Measure the text and return a measure as a float. */
    public fun textMeasure(text: RcText, mode: Int): RcFloat

    /** Returns the length of the text. */
    public fun textLength(text: RcText): RcFloat

    /** Creates a time attribute. */
    public fun timeAttribute(variable: RcInteger, type: Short, vararg args: Int): RcFloat

    /** Pre-concat the current matrix with the specified skew. */
    public fun skew(skewX: Float, skewY: Float)

    /** Sets the Matrix relative to the path. */
    public fun matrixFromPath(path: RcPath, fraction: Float, vOffset: Float, flags: Int)

    /** Sets the Matrix relative to the path using remote floats. */
    public fun matrixFromPath(path: RcPath, fraction: RcFloat, vOffset: RcFloat, flags: Int)

    /** Adds a conditional block based on the comparison of two values. */
    public fun conditionalOperations(
        type: Byte,
        a: RcFloat,
        b: RcFloat,
        content: RcScope.() -> Unit,
    )

    /** Loops from start to end with a specified step. */
    public fun rcLoop(start: RcFloat, step: Float, end: RcFloat, block: RcScope.(RcFloat) -> Unit)

    /**
     * Converts this [Float] to an [RcText] using the specified formatting.
     *
     * @param whole the number of digits before the decimal point
     * @param decimal the number of digits after the decimal point
     * @param flags formatting flags (see [Rc.TextFromFloat])
     */
    public fun Float.format(whole: Int, decimal: Int, flags: Int): RcText

    /** Extension property to convert an [Int] to a [RcFloat] within this scope. */
    public val Int.rf: RcFloat

    /** Extension property to convert a [Float] to a [RcFloat] within this scope. */
    public val Float.rf: RcFloat

    /** Extension property to register an [Int] constant as a [RcInteger] within this scope. */
    public val Int.ri: RcInteger

    /**
     * Extension property to register a [Boolean] constant as a typed [RcBool] within this scope.
     */
    public val Boolean.rb: RcBool

    /**
     * Registers a boolean constant and returns a typed [RcBool] reference.
     *
     * Distinct from [remoteBoolean] which returns the more general [RcInteger]. Prefer this
     * overload when the value is logically boolean to keep boolean→int arithmetic from compiling.
     */
    public fun remoteBool(value: Boolean): RcBool

    /** Add a particle system definition */
    public fun createParticles(
        variables: FloatArray,
        initialExpressions: Array<RcFloat>,
        particleCount: Int,
    ): RcFloat

    /** Add a particle loop */
    public fun particlesLoop(
        id: RcFloat,
        restart: RcFloat?,
        expressions: Array<RcFloat>,
        block: RcScope.() -> Unit,
    )

    /** Add a particle - particle comparison */
    public fun particlesComparison(
        id: RcFloat,
        flags: Short,
        min: RcFloat,
        max: RcFloat,
        condition: RcFloat?,
        then1: Array<RcFloat>?,
        then2: Array<RcFloat>?,
        block: RcScope.() -> Unit,
    )

    /** Add a particle test */
    public fun particlesComparison(
        id: RcFloat,
        flags: Short,
        min: RcFloat,
        max: RcFloat,
        condition: RcFloat?,
        then: Array<RcFloat>?,
        block: RcScope.() -> Unit,
    )

    /** Add an impulse container */
    public fun impulse(duration: RcFloat, start: RcFloat, block: RcImpulseScope.() -> Unit)

    /** Conditionally skip a segment */
    public fun skip(type: Short, value: Int, block: RcScope.() -> Unit)

    /** Conditionally skip a segment, returning an offset token. */
    public fun beginSkip(type: Short, value: Int): Int

    /** Concludes the skipped segment using the offset token. */
    public fun endSkip(offset: Int)

    /** Returns an [RcFloat] representing the animation delta time. */
    public fun deltaTime(): RcFloat

    public fun RcDynamicPath.lineTo(x: Float, y: Float)

    public fun RcDynamicPath.moveTo(x: Float, y: Float)

    public fun RcDynamicPath.quadTo(x1: Float, y1: Float, x2: Float, y2: Float)

    public fun RcDynamicPath.getPath(): RcPath

    public fun RcDynamicPath.close()

    public fun RcDynamicPath.reset()

    /** Registers an integer array and returns its reference. */
    public fun remoteIntArray(array: IntArray): RcFloat

    /** Registers a dynamic float array with the given size and returns its reference. */
    public fun remoteDynamicFloatArray(size: Float): RcFloat

    /** Registers a float list and returns its reference. */
    public fun remoteFloatList(values: FloatArray): RcFloat

    /** Registers a float map and returns its reference. */
    public fun remoteFloatMap(keys: Array<String>, values: FloatArray): RcFloat

    /** Sets a value in the array at the given index. */
    public fun setArrayValue(array: RcFloat, index: RcFloat, value: RcFloat)

    // =================================================================================
    // Typed default-method overloads. Each delegates to the raw-opcode method above.
    //
    // Declared as members (not extensions) so they don't require per-caller imports —
    // overload resolution finds them via the implicit RcScope receiver. The raw forms
    // remain abstract for backward compatibility; new code should prefer the typed.
    // =================================================================================

    /** Conditionally execute [content] if `op(a, b)` holds on the player. */
    public fun conditionalOperations(
        op: RcConditionOp,
        a: RcFloat,
        b: RcFloat,
        content: RcScope.() -> Unit,
    ): Unit = conditionalOperations(op.value, a, b, content)

    /** Read a single attribute (HSV/RGBA component) of a color resource. */
    public fun getColorAttribute(baseColor: RcColor, attr: RcColorAttr): RcFloat =
        getColorAttribute(baseColor, attr.value)

    /** Compute a derived float from a time-valued integer variable. */
    public fun timeAttribute(variable: RcInteger, attr: RcTimeAttr, vararg args: Int): RcFloat =
        timeAttribute(variable, attr.value, *args)

    /** Skip when the player matches [kind] against [value]. */
    public fun skip(kind: RcSkipKind, value: Int, block: RcScope.() -> Unit): Unit =
        skip(kind.value, value, block)

    /** Trigger a haptic-feedback pulse on the player device. */
    public fun performHaptic(haptic: RcHaptic): Unit = performHaptic(haptic.value)

    /** drawTextAnchored with typed [RcTextAnchorFlags]. */
    public fun drawTextAnchored(
        text: RcText,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: RcTextAnchorFlags,
    ): Unit = drawTextAnchored(text, x, y, panX, panY, flags.bits)

    /** drawTextAnchored with typed [RcTextAnchorFlags] using remote floats. */
    public fun drawTextAnchored(
        text: RcText,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        flags: RcTextAnchorFlags,
    ): Unit = drawTextAnchored(text, x, y, panX, panY, flags.bits)

    /** matrixFromPath with typed [RcMatrixFromPathFlags]. */
    public fun matrixFromPath(
        path: RcPath,
        fraction: Float,
        vOffset: Float,
        flags: RcMatrixFromPathFlags,
    ): Unit = matrixFromPath(path, fraction, vOffset, flags.bits)

    /** matrixFromPath with typed [RcMatrixFromPathFlags] using remote floats. */
    public fun matrixFromPath(
        path: RcPath,
        fraction: RcFloat,
        vOffset: RcFloat,
        flags: RcMatrixFromPathFlags,
    ): Unit = matrixFromPath(path, fraction, vOffset, flags.bits)

    /** createTextFromFloat with a typed [RcTextFromFloatSpec] format. */
    public fun createTextFromFloat(
        value: Float,
        whole: Int,
        decimal: Int,
        spec: RcTextFromFloatSpec,
    ): RcText = createTextFromFloat(value, whole, decimal, spec.bits)

    /** createTextFromFloat with a typed [RcTextFromFloatSpec] format using a remote float. */
    public fun createTextFromFloat(
        value: RcFloat,
        whole: Int,
        decimal: Int,
        spec: RcTextFromFloatSpec,
    ): RcText = createTextFromFloat(value, whole, decimal, spec.bits)

    /** [Text] overload taking a typed [RcColor] reference. */
    public fun Text(
        text: String,
        color: RcColor,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontWeight: Float = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    ): Unit = Text(text, modifier, color as Any, fontSize, fontWeight, textAlign, overflow, content)

    /** [Text] overload taking a typed [RcColorValue] reference. */
    public fun Text(
        text: String,
        color: RcColorValue,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontWeight: Float = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    ): Unit = Text(text, modifier, color as Any, fontSize, fontWeight, textAlign, overflow, content)

    /** [Text] overload taking a [RcText] reference and a typed [RcColor]. */
    public fun Text(
        text: RcText,
        color: RcColor,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontWeight: Float = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    ): Unit = Text(text, modifier, color as Any, fontSize, fontWeight, textAlign, overflow, content)

    /** [Text] overload taking a [RcText] reference and a typed [RcColorValue]. */
    public fun Text(
        text: RcText,
        color: RcColorValue,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontWeight: Float = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    ): Unit = Text(text, modifier, color as Any, fontSize, fontWeight, textAlign, overflow, content)

    /** [Text] overload taking a typed [RcWeight]. */
    public fun Text(
        text: String,
        weight: RcWeight,
        modifier: Modifier = Modifier,
        color: Any = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    ): Unit = Text(text, modifier, color, fontSize, weight.value, textAlign, overflow, content)

    /** [Text] overload taking a [RcText] reference and typed [RcWeight]. */
    public fun Text(
        text: RcText,
        weight: RcWeight,
        modifier: Modifier = Modifier,
        color: Any = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        content: RcScope.() -> Unit = {},
    ): Unit = Text(text, modifier, color, fontSize, weight.value, textAlign, overflow, content)

    /** [remoteTextStyle] overload taking a typed [RcWeight]. */
    public fun remoteTextStyle(
        fontSize: RcSp?,
        weight: RcWeight,
        color: Int? = null,
        textAlign: RcTextAlign? = null,
    ): RcTextStyle = remoteTextStyle(fontSize, color, weight.value, textAlign)

    /** [drawRect] taking a typed [RcRect]. */
    public fun drawRect(rect: RcRect): Unit = drawRect(rect.left, rect.top, rect.right, rect.bottom)

    /** [drawOval] taking a typed [RcRect]. */
    public fun drawOval(rect: RcRect): Unit = drawOval(rect.left, rect.top, rect.right, rect.bottom)

    /** [drawRoundRect] taking a typed [RcRect]. */
    public fun drawRoundRect(rect: RcRect, radiusX: Float, radiusY: Float): Unit =
        drawRoundRect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            RcFloat(radiusX),
            RcFloat(radiusY),
        )

    /** [drawArc] taking a typed [RcRect]. */
    public fun drawArc(rect: RcRect, startAngle: Float, sweepAngle: Float): Unit =
        drawArc(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            RcFloat(startAngle),
            RcFloat(sweepAngle),
        )

    /** [drawSector] taking a typed [RcRect]. */
    public fun drawSector(rect: RcRect, startAngle: Float, sweepAngle: Float): Unit =
        drawSector(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            RcFloat(startAngle),
            RcFloat(sweepAngle),
        )

    /** [drawCircle] taking a typed [RcPoint] center. */
    public fun drawCircle(center: RcPoint, radius: Float): Unit =
        drawCircle(center.x, center.y, RcFloat(radius))

    /** [drawLine] taking typed [RcPoint] endpoints. */
    public fun drawLine(from: RcPoint, to: RcPoint): Unit = drawLine(from.x, from.y, to.x, to.y)

    /** [drawBitmap] taking a typed destination [RcRect]. */
    public fun drawBitmap(image: RcImage, dst: RcRect): Unit =
        drawBitmap(image, dst.left, dst.top, dst.right, dst.bottom)

    /** [drawBitmap] taking a typed top-left [RcPoint]. */
    public fun drawBitmap(image: RcImage, topLeft: RcPoint): Unit =
        drawBitmap(image, topLeft.x, topLeft.y)

    // =================================================================================
    // Begin/end-pair safety (item 7).
    // =================================================================================

    /** Block-scoped wrapper for [startCanvasOperations] / [endCanvasOperations]. */
    public fun canvasOperations(block: RcScope.() -> Unit) {
        startCanvasOperations()
        block()
        endCanvasOperations()
    }

    /**
     * Begin a typed skip block; pair with [endSkip] using the returned token. Prefer the [skip]
     * block form when the scoping is statically known; this form exists for the rare case where
     * begin/end must be split.
     */
    public fun beginSkip(kind: RcSkipKind, value: Int): RcSkipToken =
        RcSkipToken(beginSkip(kind.value, value))

    /** Conclude a skip block opened by [beginSkip] using the typed token. */
    public fun endSkip(token: RcSkipToken): Unit = endSkip(token.offset)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcImpulseScope : RcScope {
    /** Add an impulse process container */
    public fun process(block: RcScope.() -> Unit)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcColumnScope : RcScope {
    /** Sets the vertical weight of the component within a [Column]. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcRowScope : RcScope {
    /** Sets the horizontal weight of the component within a [Row]. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcFlowScope : RcScope {
    /** Sets the horizontal weight of the component within a [Row]. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcCollapsibleColumnScope : RcScope {
    /** Sets the vertical weight of the component within a [CollapsibleColumn]. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcCollapsibleRowScope : RcScope {
    /** Sets the horizontal weight of the component within a [CollapsibleRow]. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcCanvasScope : RcScope {
    /** Returns the width of the canvas as a remote float. */
    public val width: RcFloat
    /** Returns the height of the canvas as a remote float. */
    public val height: RcFloat
    /** The current [RcPaint] object. */
    public val paint: RcPaint

    /** Executes a block within a save/restore pair. */
    public fun save(block: RcCanvasScope.() -> Unit)

    /** Clips the current canvas to the specified rectangle. */
    public fun clipRect(left: Float, top: Float, right: Float, bottom: Float)

    /** Clips the current canvas to the specified rectangle using remote floats. */
    public fun clipRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** Creates a new path starting at the specified position. */
    public fun remotePath(x: Float, y: Float): RcDynamicPath

    /** Creates a new path starting at the specified position and populates it. */
    //    public fun remotePath(x: Float, y: Float, block: RcPathScope.() -> Unit): RcDynamicPath

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: Float)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: RcFloat)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: Float, centerX: Float, centerY: Float)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: RcFloat, centerX: RcFloat, centerY: RcFloat)

    /** Loops from start to end with a specified step. */
    public fun loop(
        start: RcFloat,
        step: RcFloat,
        end: RcFloat,
        block: RcCanvasScope.(RcFloat) -> Unit,
    )

    /** Clips the current canvas to the specified path. */
    public fun clipPath(path: RcPath)

    /** Add touch handling on canvas. */
    public fun addTouch(
        defValue: Float,
        min: Float,
        max: Float,
        touchMode: Int,
        velocityId: Float,
        touchEffects: Int,
        touchSpec: FloatArray?,
        easingSpec: FloatArray?,
        vararg exp: Float,
    ): RcFloat

    /**
     * Typed touch handler.
     *
     * Replaces the raw `touchMode: Int` with a typed [RcTouchStopMode] enum and the raw
     * `touchEffects: Int` with a typed [RcHaptic] (the haptic to fire when a notch is crossed). The
     * `touchEffects` parameter at the wire level supports an additional "indirect via integer
     * variable" mode (bit 15); use the raw [addTouch] above for that case until it's typed
     * properly.
     *
     * @param defaultValue initial value of the touch variable
     * @param range allowed range of the touch variable
     * @param stopMode behavior when the user releases the touch
     * @param velocity remote-float channel exposed by the touch (see Rc.Touch.VELOCITY_*)
     * @param notchHaptic haptic kind to fire when the touch crosses a notch
     * @param notches notch positions (interpretation depends on [stopMode]); null for the non-notch
     *   modes
     * @param easing easing-spec FloatArray (see PaintPathEffects); null for default
     * @param expressions trailing variadic expression coefficients (raw)
     */
    public fun addTouch(
        defaultValue: Float,
        range: ClosedFloatingPointRange<Float>,
        stopMode: RcTouchStopMode,
        velocity: Float = 0f,
        notchHaptic: RcHaptic = RcHaptic.NoHaptics,
        notches: FloatArray? = null,
        easing: FloatArray? = null,
        vararg expressions: Float,
    ): RcFloat =
        addTouch(
            defaultValue,
            range.start,
            range.endInclusive,
            stopMode.value,
            velocity,
            notchHaptic.value,
            notches,
            easing,
            *expressions,
        )
}

/** Internal helper to convert the new [Modifier] chain to the legacy [RecordingModifier]. */
internal fun Modifier.toRecordingModifier(): RecordingModifier {
    val recording = RecordingModifier()
    foldIn(Unit) { _, element -> element.applyTo(recording) }
    return recording
}

/** Adds a spacer component defaulting to weight 1.0. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcColumnScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

/** Adds a spacer component defaulting to weight 1.0. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcRowScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

/** Adds a spacer component defaulting to weight 1.0. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcCollapsibleColumnScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

/** Adds a spacer component defaulting to weight 1.0. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcCollapsibleRowScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.format(
    value: Float,
    whole: Int,
    decimal: Int,
    spec: RcTextFromFloatSpec,
): RcText = value.format(whole, decimal, spec.bits)

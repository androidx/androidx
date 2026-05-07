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
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterInterface
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

    /** Executes a block within a global section. */
    public fun Global(block: RcScope.() -> Unit)

    /** Starts a global section */
    public fun beginGlobal()

    /** Ends a global section */
    public fun endGlobal()

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

    /** Registers a color resource from a Long and returns its reference. */
    public fun remoteColor(color: Long): RcColor = remoteColor(color.toInt())

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
    @Suppress("FunctionName") public fun dayOfWeek(): RcFloat

    /** Returns an [RcFloat] representing the current day of the month. */
    @Suppress("FunctionName") public fun dayOfMonth(): RcFloat

    /** Returns an [RcFloat] representing the current hour. */
    @Suppress("FunctionName") public fun hour(): RcFloat

    /** Returns an [RcFloat] representing the current minutes. */
    @Suppress("FunctionName") public fun minutes(): RcFloat

    /** Returns an [RcFloat] representing the current seconds. */
    @Suppress("FunctionName") public fun seconds(): RcFloat

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

    public operator fun RcFloat.get(index: RcFloat): RcFloat

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

    /** Adds a spacer component. */
    public fun Spacer(modifier: Modifier)

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

    /** Saves the current canvas state. */
    public fun save()

    /** Restores the previous canvas state. */
    public fun restore()

    /** Executes a block within a save/restore pair. */
    public fun save(block: RcCanvasScope.() -> Unit)

    /** Clips the current canvas to the specified rectangle. */
    public fun clipRect(left: Float, top: Float, right: Float, bottom: Float)

    /** Clips the current canvas to the specified rectangle using remote floats. */
    public fun clipRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** Creates a new path starting at the specified position. */
    public fun remotePath(x: Float, y: Float): RcPath

    /** Creates a new path starting at the specified position and populates it. */
    public fun remotePath(x: Float, y: Float, block: RcPathScope.() -> Unit): RcPath

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: Float, scaleY: Float)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: RcFloat, scaleY: RcFloat)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: RcFloat, scaleY: RcFloat, centerX: RcFloat, centerY: RcFloat)

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
        step: Float,
        end: RcFloat,
        block: RcCanvasScope.(RcFloat) -> Unit,
    )
}

/** Private implementation of [RcScope]. */
internal open class RcScopeImpl(internal val writer: RemoteComposeWriter) : RcScope {

    override fun Box(
        modifier: Modifier,
        horizontal: RcHorizontalPositioning,
        vertical: RcVerticalPositioning,
        content: RcScope.() -> Unit,
    ) {
        writer.startBox(modifier.toRecordingModifier(), horizontal.value, vertical.value)
        RcScopeImpl(writer).content()
        writer.endBox()
    }

    override fun Column(
        modifier: Modifier,
        horizontal: RcHorizontalPositioning,
        vertical: RcColumnVerticalPositioning,
        content: RcColumnScope.() -> Unit,
    ) {
        writer.startColumn(modifier.toRecordingModifier(), horizontal.value, vertical.value)
        RcColumnScopeImpl(writer).content()
        writer.endColumn()
    }

    override fun Row(
        modifier: Modifier,
        horizontal: RcRowHorizontalPositioning,
        vertical: RcVerticalPositioning,
        content: RcRowScope.() -> Unit,
    ) {
        writer.startRow(modifier.toRecordingModifier(), horizontal.value, vertical.value)
        RcRowScopeImpl(writer).content()
        writer.endRow()
    }

    override fun Flow(
        modifier: Modifier,
        horizontal: RcHorizontalPositioning,
        vertical: RcVerticalPositioning,
        maxItemsInEachRow: Int,
        maxLines: Int,
        content: RcFlowScope.() -> Unit,
    ) {
        writer.startFlow(
            modifier.toRecordingModifier(),
            horizontal.value,
            vertical.value,
            maxItemsInEachRow,
            maxLines,
        )
        RcFlowScopeImpl(writer).content()
        writer.endFlow()
    }

    override fun CollapsibleColumn(
        modifier: Modifier,
        horizontal: RcHorizontalPositioning,
        vertical: RcVerticalPositioning,
        content: RcCollapsibleColumnScope.() -> Unit,
    ) {
        writer.startCollapsibleColumn(
            modifier.toRecordingModifier(),
            horizontal.value,
            vertical.value,
        )
        RcCollapsibleColumnScopeImpl(writer).content()
        writer.endCollapsibleColumn()
    }

    override fun CollapsibleRow(
        modifier: Modifier,
        horizontal: RcHorizontalPositioning,
        vertical: RcVerticalPositioning,
        content: RcCollapsibleRowScope.() -> Unit,
    ) {
        writer.startCollapsibleRow(modifier.toRecordingModifier(), horizontal.value, vertical.value)
        RcCollapsibleRowScopeImpl(writer).content()
        writer.endCollapsibleRow()
    }

    override fun Spacer(modifier: Modifier) {
        writer.startBox(modifier.toRecordingModifier(), 0, 0)
        writer.endBox()
    }

    override fun Text(
        text: String,
        modifier: Modifier,
        color: Any,
        fontSize: RcSp,
        fontWeight: Float,
        textAlign: RcTextAlign,
        overflow: RcTextOverflow,
    ) {
        val textId = writer.addText(text)
        Text(RcText(textId), modifier, color, fontSize, fontWeight, textAlign, overflow)
    }

    override fun Text(
        text: RcText,
        modifier: Modifier,
        color: Any,
        fontSize: RcSp,
        fontWeight: Float,
        textAlign: RcTextAlign,
        overflow: RcTextOverflow,
    ) {
        if (color is RcColor) {
            writer.textComponent(
                modifier.toRecordingModifier(),
                text.id,
                -1, // textStyleId
                0, // color
                color.id,
                fontSize.value,
                -1f, // minFontSize
                -1f, // maxFontSize
                0, // fontStyle
                fontWeight,
                null, // fontFamily
                textAlign.value,
                overflow.value,
                Int.MAX_VALUE, // maxLines
                0f, // letterSpacing
                0f, // lineHeightAdd
                1f, // lineHeightMultiplier
                0, // lineBreakStrategy
                0, // hyphenationFrequency
                0, // justificationMode
                false, // underline
                false, // strikethrough
                null, // fontAxis
                null, // fontAxisValues
                false, // autosize
                0, // flags
            ) {}
        } else {
            val colorInt =
                when (color) {
                    is Int -> color
                    is Long -> color.toInt()
                    else -> 0xFF000000.toInt()
                }
            writer.textComponent(
                modifier.toRecordingModifier(),
                text.id,
                colorInt,
                fontSize.value,
                0,
                fontWeight,
                null,
                textAlign.value,
                overflow.value,
                1,
            ) {}
        }
    }

    override fun Image(
        image: RcImage,
        modifier: Modifier,
        contentDescription: String?,
        contentScale: RcContentScale,
        alpha: Float,
    ) {
        writer.image(modifier.toRecordingModifier(), image.id, contentScale.value, alpha)
    }

    override fun Canvas(modifier: Modifier, content: RcCanvasScope.() -> Unit) {
        writer.startCanvas(modifier.toRecordingModifier())
        RcCanvasScopeImpl(writer).content()
        writer.endCanvas()
    }

    override fun applyPaint(block: RcPaint.() -> Unit) {
        writer.rcPaint.block()
        writer.rcPaint.commit()
    }

    override fun Global(block: RcScope.() -> Unit) {
        writer.beginGlobal()
        this.block()
        writer.endGlobal()
    }

    override fun beginGlobal() {
        writer.beginGlobal()
    }

    override fun endGlobal() {
        writer.endGlobal()
    }

    override fun remoteText(text: String): RcText = RcText(writer.addText(text))

    override fun remoteNamedText(name: String, text: String): RcText =
        RcText(writer.addNamedString(name, text))

    override fun remoteArrayOf(vararg strings: String): RcTextList =
        RcTextList(writer.addStringList(*strings))

    override fun textLookup(dataSetId: RcTextList, index: RcFloat): RcText {
        val arrayId: Float = dataSetId.id
        val indexVal: Float = index.withWriter(writer).toFloat()
        return RcText(writer.textLookup(arrayId, indexVal))
    }

    override fun textLookup(dataSetId: RcTextList, indexId: RcInteger): RcText =
        RcText(writer.textLookup(dataSetId.id, indexId.id.toInt()))

    override fun textMerge(text1: RcText, text2: RcText): RcText =
        RcText(writer.textMerge(text1.id, text2.id))

    override fun remoteColor(color: Int): RcColor = RcColor(writer.addColor(color))

    override fun remoteNamedColor(name: String, color: Int): RcColor =
        RcColor(writer.addNamedColor(name, color))

    override fun remoteThemedColor(light: Int, dark: Int): RcColor =
        RcColor(writer.addThemedColor(light.toShort(), dark.toShort()).toInt())

    override fun remoteThemedColor(
        light: String,
        lightDefault: Int,
        dark: String,
        darkDefault: Int,
    ): RcColor = RcColor(writer.addThemedColor(light, lightDefault, dark, darkDefault).toInt())

    override fun remoteThemedColor(light: RcColor, dark: RcColor): RcColor =
        RcColor(writer.addThemedColor(light.id.toShort(), dark.id.toShort()).toInt())

    override fun createTextFromFloat(value: Float, whole: Int, decimal: Int, flags: Int): RcText =
        RcText(writer.createTextFromFloat(value, whole, decimal, flags))

    override fun createTextFromFloat(value: RcFloat, whole: Int, decimal: Int, flags: Int): RcText =
        value.withWriter(writer).format(whole, decimal, flags)

    override fun remoteFloatArray(array: FloatArray): RcFloat =
        RcFloat(writer, writer.addFloatArray(array))

    override fun animationTime(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Time.ANIMATION_TIME))

    override fun touchTime(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Touch.TOUCH_EVENT_TIME))

    override fun dayOfWeek(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Time.WEEK_DAY))

    override fun dayOfMonth(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Time.DAY_OF_MONTH))

    override fun hour(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Time.TIME_IN_HR))

    override fun minutes(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Time.TIME_IN_MIN))

    override fun seconds(): RcFloat = RcFloat(writer, floatArrayOf(Rc.Time.TIME_IN_SEC))

    override fun max(a: RcFloat, b: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MAX))

    override fun max(a: Float, b: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MAX))

    override fun max(a: RcFloat, b: Float): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MAX))

    override fun min(a: RcFloat, b: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.MIN))

    override fun min(a: Float, b: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(a, *b.toArray(), Rc.FloatExpression.MIN))

    override fun min(a: RcFloat, b: Float): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), b, Rc.FloatExpression.MIN))

    override fun sign(v: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*v.toArray(), Rc.FloatExpression.SIGN))

    override fun sin(v: RcFloat): RcFloat = v.sin()

    override fun cos(v: RcFloat): RcFloat = v.cos()

    override fun abs(v: RcFloat): RcFloat = v.abs()

    override fun arrayMax(array: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*array.toArray(), Rc.FloatExpression.A_MAX))

    override fun arrayMin(array: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*array.toArray(), Rc.FloatExpression.A_MIN))

    override fun arraySpline(array: RcFloat, position: RcFloat): RcFloat =
        RcFloat(
            writer,
            floatArrayOf(*array.toArray(), *position.toArray(), Rc.FloatExpression.A_SPLINE),
        )

    override fun arraySpline(array: RcFloat, position: Float): RcFloat =
        RcFloat(writer, floatArrayOf(*array.toArray(), position, Rc.FloatExpression.A_SPLINE))

    override fun arrayLength(a: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_LEN))

    override fun arraySum(a: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_SUM))

    override fun arraySum(a: RcFloat, index: RcFloat): RcFloat =
        RcFloat(
            writer,
            floatArrayOf(*a.toArray(), *index.toArray(), Rc.FloatExpression.A_SUM_UNTIL),
        )

    override fun arraySumXY(a: RcFloat, b: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), *b.toArray(), Rc.FloatExpression.A_SUM_XY))

    override fun arraySumSqr(a: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_SUM_SQR))

    override fun arrayAvg(a: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*a.toArray(), Rc.FloatExpression.A_AVG))

    override fun RcFloat.get(index: RcFloat): RcFloat =
        RcFloat(writer, floatArrayOf(*this.toArray(), *index.toArray(), Rc.FloatExpression.A_DEREF))

    override fun remotePolarPath(
        expression: RcFloat,
        start: Float,
        end: Float,
        count: Int,
        centerX: RcFloat,
        centerY: RcFloat,
        type: RcPathType,
    ): RcPath =
        RcPath(
            writer.addPolarPathExpression(
                expression.withWriter(writer).toArray(),
                start,
                end,
                count.toFloat(),
                centerX.withWriter(writer).toFloat(),
                centerY.withWriter(writer).toFloat(),
                type.value,
            ),
            writer,
        )

    override fun rFun(block: (RcFloat) -> RcFloat): RcFloat {
        val arg = RcFloat(writer, floatArrayOf(Rc.FloatExpression.VAR1))
        return block(arg)
    }

    override fun componentWidth(): RcFloat = RcFloat(writer, writer.addComponentWidthValue())

    override fun componentHeight(): RcFloat = RcFloat(writer, writer.addComponentHeightValue())

    override fun remoteBitmap(image: Any): RcImage = RcImage(writer.addBitmap(image))

    override fun remoteNamedBitmap(name: String, image: Any): RcImage =
        RcImage(writer.addNamedBitmap(name, image))

    override fun remoteBitmapUrl(url: String): RcImage = RcImage(writer.addBitmapUrl(url))

    override fun remoteNamedBitmapUrl(name: String, url: String): RcImage =
        RcImage(writer.addNamedBitmapUrl(name, url))

    override fun remoteFloat(value: Float): RcFloat =
        RcFloat(writer, writer.addFloatConstant(value))

    override fun remoteNamedFloat(name: String, value: Float): RcFloat =
        RcFloat(writer, writer.addNamedFloat(name, value))

    override fun remoteInteger(value: Int): RcInteger = RcInteger(writer.addInteger(value))

    override fun remoteNamedInteger(name: String, value: Int): RcInteger =
        RcInteger(writer.addNamedInt(name, value))

    override fun remoteLong(value: Long): RcInteger = RcInteger(writer.addLong(value).toLong())

    override fun remoteBoolean(value: Boolean): RcInteger =
        RcInteger(writer.addBoolean(value).toLong())

    override fun remotePathData(path: RcPlatformServices.RcPathArrayCreator): RcPath =
        RcPath(writer.addPathData(path), writer)

    //  override fun remotePathString(path: String): RcPath = RcPath(writer.addPathString(path),
    // writer)

    override fun remoteTextStyle(
        fontSize: RcSp?,
        color: Int?,
        fontWeight: Float?,
        textAlign: RcTextAlign?,
    ): RcTextStyle =
        RcTextStyle(
            writer.addTextStyle(
                color,
                null,
                fontSize?.value,
                null,
                null,
                null,
                fontWeight,
                null,
                textAlign?.value,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        )

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
        writer.drawRect(left, top, right, bottom)
    }

    override fun drawRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat) {
        writer.drawRect(
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
        )
    }

    override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {
        writer.drawCircle(centerX, centerY, radius)
    }

    override fun drawCircle(centerX: RcFloat, centerY: RcFloat, radius: RcFloat) {
        writer.drawCircle(
            centerX.withWriter(writer).toFloat(),
            centerY.withWriter(writer).toFloat(),
            radius.withWriter(writer).toFloat(),
        )
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        writer.drawLine(x1, y1, x2, y2)
    }

    override fun drawLine(x1: RcFloat, y1: RcFloat, x2: RcFloat, y2: RcFloat) {
        writer.drawLine(
            x1.withWriter(writer).toFloat(),
            y1.withWriter(writer).toFloat(),
            x2.withWriter(writer).toFloat(),
            y2.withWriter(writer).toFloat(),
        )
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        writer.drawArc(left, top, right, bottom, startAngle, sweepAngle)
    }

    override fun drawArc(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        startAngle: RcFloat,
        sweepAngle: RcFloat,
    ) {
        writer.drawArc(
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
            startAngle.withWriter(writer).toFloat(),
            sweepAngle.withWriter(writer).toFloat(),
        )
    }

    override fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        writer.drawSector(left, top, right, bottom, startAngle, sweepAngle)
    }

    override fun drawSector(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        startAngle: RcFloat,
        sweepAngle: RcFloat,
    ) {
        writer.drawSector(
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
            startAngle.withWriter(writer).toFloat(),
            sweepAngle.withWriter(writer).toFloat(),
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {
        writer.drawOval(left, top, right, bottom)
    }

    override fun drawOval(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat) {
        writer.drawOval(
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
        )
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {
        writer.drawRoundRect(left, top, right, bottom, radiusX, radiusY)
    }

    override fun drawRoundRect(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        radiusX: RcFloat,
        radiusY: RcFloat,
    ) {
        writer.drawRoundRect(
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
            radiusX.withWriter(writer).toFloat(),
            radiusY.withWriter(writer).toFloat(),
        )
    }

    override fun drawPath(path: RcPath) {
        writer.drawPath(path.id)
    }

    override fun drawTweenPath(
        path1: RcPath,
        path2: RcPath,
        tween: Float,
        start: Float,
        stop: Float,
    ) {
        writer.drawTweenPath(path1.id, path2.id, tween, start, stop)
    }

    override fun drawTweenPath(
        path1: RcPath,
        path2: RcPath,
        tween: RcFloat,
        start: RcFloat,
        stop: RcFloat,
    ) {
        writer.drawTweenPath(
            path1.id,
            path2.id,
            tween.withWriter(writer).toFloat(),
            start.withWriter(writer).toFloat(),
            stop.withWriter(writer).toFloat(),
        )
    }

    override fun drawTextAnchored(
        text: RcText,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int,
    ) {
        writer.drawTextAnchored(text.id, x, y, panX, panY, flags)
    }

    override fun drawTextAnchored(
        text: RcText,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        flags: Int,
    ) {
        writer.drawTextAnchored(
            text.id,
            x.withWriter(writer).toFloat(),
            y.withWriter(writer).toFloat(),
            panX.withWriter(writer).toFloat(),
            panY.withWriter(writer).toFloat(),
            flags,
        )
    }

    override fun drawTextOnPath(text: RcText, path: RcPath, hOffset: Float, vOffset: Float) {
        writer.drawTextOnPath(text.id, path.id, hOffset, vOffset)
    }

    override fun drawTextOnPath(text: RcText, path: RcPath, hOffset: RcFloat, vOffset: RcFloat) {
        writer.drawTextOnPath(
            text.id,
            path.id,
            hOffset.withWriter(writer).toFloat(),
            vOffset.withWriter(writer).toFloat(),
        )
    }

    override fun drawTextOnCircle(
        text: RcText,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        warpRadiusOffset: Float,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
    ) {
        writer.drawTextOnCircle(
            text.id,
            centerX,
            centerY,
            radius,
            startAngle,
            warpRadiusOffset,
            alignment,
            placement,
        )
    }

    override fun drawTextOnCircle(
        text: RcText,
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        startAngle: RcFloat,
        warpRadiusOffset: RcFloat,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
    ) {
        writer.drawTextOnCircle(
            text.id,
            centerX.withWriter(writer).toFloat(),
            centerY.withWriter(writer).toFloat(),
            radius.withWriter(writer).toFloat(),
            startAngle.withWriter(writer).toFloat(),
            warpRadiusOffset.withWriter(writer).toFloat(),
            alignment,
            placement,
        )
    }

    override fun drawBitmap(image: RcImage, left: Float, top: Float, right: Float, bottom: Float) {
        writer.drawBitmap(image.id, left, top, right, bottom, null)
    }

    override fun drawBitmap(image: RcImage, left: Float, top: Float) {
        writer.drawBitmap(image.id, left, top, null)
    }

    override fun drawBitmap(
        image: RcImage,
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
    ) {
        writer.drawBitmap(
            image.id,
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
            null,
        )
    }

    override fun drawBitmap(image: RcImage, left: RcFloat, top: RcFloat) {
        writer.drawBitmap(
            image.id,
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            null,
        )
    }

    override fun Float.format(whole: Int, decimal: Int, flags: Int): RcText {
        return RcText(writer.createTextFromFloat(this, whole, decimal, flags))
    }

    override val Int.rf: RcFloat
        get() = RcFloat(writer, this.toFloat())

    override val Float.rf: RcFloat
        get() = RcFloat(writer, this)
}

private class RcColumnScopeImpl(writer: RemoteComposeWriter) : RcScopeImpl(writer), RcColumnScope {
    override fun Modifier.weight(weight: Float): Modifier =
        then(WeightModifier(weight, vertical = true))
}

private class RcRowScopeImpl(writer: RemoteComposeWriter) : RcScopeImpl(writer), RcRowScope {
    override fun Modifier.weight(weight: Float): Modifier =
        then(WeightModifier(weight, vertical = false))
}

private class RcFlowScopeImpl(writer: RemoteComposeWriter) : RcScopeImpl(writer), RcFlowScope {
    override fun Modifier.weight(weight: Float): Modifier =
        then(WeightModifier(weight, vertical = false))
}

private class RcCollapsibleColumnScopeImpl(writer: RemoteComposeWriter) :
    RcScopeImpl(writer), RcCollapsibleColumnScope {
    override fun Modifier.weight(weight: Float): Modifier =
        then(WeightModifier(weight, vertical = true))
}

private class RcCollapsibleRowScopeImpl(writer: RemoteComposeWriter) :
    RcScopeImpl(writer), RcCollapsibleRowScope {
    override fun Modifier.weight(weight: Float): Modifier =
        then(WeightModifier(weight, vertical = false))
}

private class RcCanvasScopeImpl(writer: RemoteComposeWriter) : RcScopeImpl(writer), RcCanvasScope {
    override val width: RcFloat
        get() = RcFloat(writer, writer.addComponentWidthValue())

    override val height: RcFloat
        get() = RcFloat(writer, writer.addComponentHeightValue())

    override val paint: RcPaint
        get() = writer.rcPaint

    override fun save() {
        writer.save()
    }

    override fun restore() {
        writer.restore()
    }

    override fun save(block: RcCanvasScope.() -> Unit) {
        writer.save()
        this.block()
        writer.restore()
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        writer.clipRect(left, top, right, bottom)
    }

    override fun clipRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat) {
        writer.clipRect(
            left.withWriter(writer).toFloat(),
            top.withWriter(writer).toFloat(),
            right.withWriter(writer).toFloat(),
            bottom.withWriter(writer).toFloat(),
        )
    }

    override fun remotePath(x: Float, y: Float): RcPath = RcPath(writer.pathCreate(x, y), writer)

    override fun remotePath(x: Float, y: Float, block: RcPathScope.() -> Unit): RcPath {
        val path = remotePath(x, y)
        path.block()
        path.close()
        return path
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        writer.scale(scaleX, scaleY)
    }

    override fun scale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        writer.scale(scaleX, scaleY, centerX, centerY)
    }

    override fun scale(scaleX: RcFloat, scaleY: RcFloat) {
        writer.scale(scaleX.withWriter(writer).toFloat(), scaleY.withWriter(writer).toFloat())
    }

    override fun scale(scaleX: RcFloat, scaleY: RcFloat, centerX: RcFloat, centerY: RcFloat) {
        writer.scale(
            scaleX.withWriter(writer).toFloat(),
            scaleY.withWriter(writer).toFloat(),
            centerX.withWriter(writer).toFloat(),
            centerY.withWriter(writer).toFloat(),
        )
    }

    override fun rotate(angle: Float) {
        writer.rotate(angle)
    }

    override fun rotate(angle: RcFloat) {
        writer.rotate(angle.withWriter(writer).toFloat())
    }

    override fun rotate(angle: Float, centerX: Float, centerY: Float) {
        writer.rotate(angle, centerX, centerY)
    }

    override fun rotate(angle: RcFloat, centerX: RcFloat, centerY: RcFloat) {
        writer.rotate(
            angle.withWriter(writer).toFloat(),
            centerX.withWriter(writer).toFloat(),
            centerY.withWriter(writer).toFloat(),
        )
    }

    override fun loop(
        start: RcFloat,
        step: Float,
        end: RcFloat,
        block: RcCanvasScope.(RcFloat) -> Unit,
    ) {
        val indexId = writer.textCreateId("index")
        val from: Float = start.withWriter(writer).toFloat()
        val until: Float = end.withWriter(writer).toFloat()
        writer.loop(
            indexId,
            from,
            step,
            until,
            object : RemoteComposeWriterInterface {
                override fun run() {
                    val v =
                        androidx.compose.remote.creation.RFloat(
                            writer,
                            floatArrayOf(
                                androidx.compose.remote.core.operations.Utils.asNan(indexId)
                            ),
                        )
                    this@RcCanvasScopeImpl.block(RcFloat(writer, v.array))
                }
            },
        )
    }
}

/** Internal helper to convert the new [Modifier] chain to the legacy [RecordingModifier]. */
internal fun Modifier.toRecordingModifier(): RecordingModifier {
    val recording = RecordingModifier()
    foldIn(Unit) { _, element -> element.applyTo(recording) }
    return recording
}

/** Adds a spacer component defaulting to weight 1.0. */
public fun RcColumnScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

/** Adds a spacer component defaulting to weight 1.0. */
public fun RcRowScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

/** Adds a spacer component defaulting to weight 1.0. */
public fun RcCollapsibleColumnScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

/** Adds a spacer component defaulting to weight 1.0. */
public fun RcCollapsibleRowScope.Spacer(): Unit = Spacer(Modifier.weight(1f))

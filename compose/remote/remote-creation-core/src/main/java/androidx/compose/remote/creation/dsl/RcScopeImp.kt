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

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext.FLOAT_ANIMATION_DELTA_TIME
import androidx.compose.remote.core.RemoteContext.FLOAT_CALENDAR_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterInterface
import java.lang.Runnable

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

    override fun FitBox(
        modifier: Modifier,
        horizontal: RcHorizontalPositioning,
        vertical: RcVerticalPositioning,
        content: RcScope.() -> Unit,
    ) {
        writer.startFitBox(modifier.toRecordingModifier(), horizontal.value, vertical.value)
        RcScopeImpl(writer).content()
        writer.endFitBox()
    }

    override fun StateLayout(
        stateIndex: RcInteger,
        modifier: Modifier,
        content: RcScope.() -> Unit,
    ) {
        writer.startStateLayout(modifier.toRecordingModifier(), stateIndex.id.toInt())
        RcScopeImpl(writer).content()
        writer.endStateLayout()
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

    override fun debug(msg: String, value: RcFloat) {
        writer.addDebugMessage(msg, value.withWriter(writer).toFloat())
    }

    override fun RcPath.combine(path2: RcPath, op: RcPathCombineOp): RcPath {
        return RcPath(writer.pathCombine(this.id, path2.id, op.value))
    }

    override fun performHaptic(feedbackConstant: Int) {
        writer.performHaptic(feedbackConstant)
    }

    override fun wakeIn(seconds: Float) {
        writer.wakeIn(seconds)
    }

    override fun getColorAttribute(baseColor: RcColor, type: Short): RcFloat {
        return RcFloat(writer, writer.getColorAttribute(baseColor.id, type))
    }

    override fun RcText.substring(start: RcFloat, len: RcFloat): RcText {
        return RcText(
            writer.textSubtext(
                this.id,
                start.withWriter(writer).toFloat(),
                len.withWriter(writer).toFloat(),
            )
        )
    }

    override fun RcText.substring(start: Float, len: Float): RcText {
        return RcText(writer.textSubtext(this.id, start, len))
    }

    override fun RcText.measure(
        bmFontId: RcBitmapFont,
        measureWidth: Int,
        glyphSpacing: Float,
    ): RcFloat {
        return RcFloat(
            writer,
            writer.bitmapTextMeasure(this.id, bmFontId.id, measureWidth, glyphSpacing),
        )
    }

    override fun RcBitmapFont.measure(
        text: RcText,
        type: BitmapTextMeasure,
        glyphSpacing: Float,
    ): RcFloat {
        return RcFloat(writer, writer.bitmapTextMeasure(text.id, this.id, type.value, glyphSpacing))
    }

    override fun createBitmapFont(glyphs: Array<BitmapFontData.Glyph>): RcBitmapFont {
        return RcBitmapFont(writer.addBitmapFont(glyphs))
    }

    override fun createBitmapFont(
        glyphs: Array<BitmapFontData.Glyph>,
        kerningTable: Map<String, Short>,
    ): RcBitmapFont {
        return RcBitmapFont(writer.addBitmapFont(glyphs, kerningTable))
    }

    override fun drawText(
        text: RcText,
        font: RcBitmapFont,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        glyphSpacing: Float,
    ) {
        writer.drawBitmapFontTextRun(text.id, font.id, start, end, x, y, glyphSpacing)
    }

    override fun drawText(
        text: RcText,
        font: RcBitmapFont,
        start: Int,
        end: Int,
        x: RcFloat,
        y: RcFloat,
        glyphSpacing: Float,
    ) {
        writer.drawBitmapFontTextRun(
            text.id,
            font.id,
            start,
            end,
            x.withWriter(writer).toFloat(),
            y.withWriter(writer).toFloat(),
            glyphSpacing,
        )
    }

    override fun drawTextOnPath(
        text: RcText,
        font: RcBitmapFont,
        path: RcPath,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float,
    ) {
        writer.drawBitmapFontTextRunOnPath(
            text.id,
            font.id,
            path.id,
            start,
            end,
            yAdj,
            glyphSpacing,
        )
    }

    override fun drawTextOnPath(
        text: RcText,
        font: RcBitmapFont,
        path: RcPath,
        start: Int,
        end: Int,
        yAdj: RcFloat,
        glyphSpacing: Float,
    ) {
        writer.drawBitmapFontTextRunOnPath(
            text.id,
            font.id,
            path.id,
            start,
            end,
            yAdj.withWriter(writer).toFloat(),
            glyphSpacing,
        )
    }

    override fun drawTextAnchored(
        text: RcText,
        font: RcBitmapFont,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        glyphSpacing: Float,
    ) {
        writer.drawBitmapTextAnchored(text.id, font.id, start, end, x, y, panX, panY, glyphSpacing)
    }

    override fun drawTextAnchored(
        text: RcText,
        font: RcBitmapFont,
        start: RcFloat,
        end: RcFloat,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        glyphSpacing: Float,
    ) {
        writer.drawBitmapTextAnchored(
            text.id,
            font.id,
            start.withWriter(writer).toFloat(),
            end.withWriter(writer).toFloat(),
            x.withWriter(writer).toFloat(),
            y.withWriter(writer).toFloat(),
            panX.withWriter(writer).toFloat(),
            panY.withWriter(writer).toFloat(),
            glyphSpacing,
        )
    }

    override fun Text(
        text: String,
        modifier: Modifier,
        color: Any,
        fontSize: RcSp,
        fontWeight: Float,
        textAlign: RcTextAlign,
        overflow: RcTextOverflow,
        content: RcScope.() -> Unit,
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
        content: RcScope.() -> Unit,
    ) {
        // Resolve the color into a colorId (RcColor / RcColorValue) or a raw int.
        // Previously RcColor fell into the `else` and rendered as default black —
        // fixed here so the typed RcColor surface actually colors text.
        val colorId: Int =
            when (color) {
                is RcColor -> color.id
                is RcColorValue -> color.id
                else -> -1 // sentinel meaning "no colorId, use raw int below"
            }
        if (colorId != -1) {
            writer.textComponent(
                modifier.toRecordingModifier(),
                text.id,
                -1, // textStyleId
                0, // color
                colorId,
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
            ) {
                RcScopeImpl(writer).content()
            }
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
            ) {
                RcScopeImpl(writer).content()
            }
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

    override fun startCanvasOperations() {
        writer.startCanvasOperations()
    }

    override fun endCanvasOperations() {
        writer.endCanvasOperations()
    }

    override fun drawComponentContent() {
        writer.drawComponentContent()
    }

    override fun applyPaint(block: RcPaint.() -> Unit) {
        writer.rcPaint.block()
        writer.rcPaint.commit()
    }

    override fun paint(block: RcPaintScope.() -> Unit) {
        val scope = RcPaintScopeImpl(writer.rcPaint)
        scope.block()
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

    override fun save() {
        writer.save()
    }

    override fun restore() {
        writer.restore()
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

    override fun remoteColorValue(color: Int): RcColorValue = RcColorValue(color)

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

    override fun remoteColorExpression(
        alpha: Int,
        hue: RcFloat,
        sat: Float,
        value: Float,
    ): RcColor = RcColor(writer.addColorExpression(alpha, hue.toFloat(), sat, value).toInt())

    override fun remoteColorExpression(alpha: Int, hue: Float, sat: Float, value: Float): RcColor =
        RcColor(writer.addColorExpression(alpha, hue, sat, value).toInt())

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

    override fun continuousSeconds(): RcFloat =
        RcFloat(writer, floatArrayOf(Rc.Time.CONTINUOUS_SEC))

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
            )
        )

    override fun remoteXYPath(
        expressionX: RcFloat,
        expressionY: RcFloat,
        start: Float,
        end: Float,
        count: Int,
        type: RcPathType,
    ): RcPath =
        RcPath(
            writer.addPathExpression(
                expressionX.withWriter(writer).toArray(),
                expressionY.withWriter(writer).toArray(),
                start,
                end,
                count.toFloat(),
                type.value,
            )
        )

    override fun rFun(block: (RcFloat) -> RcFloat): RcFloat {
        val arg = RcFloat(writer, floatArrayOf(Rc.FloatExpression.VAR1))
        return block.invoke(arg)
    }

    override fun density(): RcFloat = RcFloat(writer, Rc.System.DENSITY)

    override fun fontSize(): RcFloat = RcFloat(writer, Rc.System.FONT_SIZE)

    override fun apiLevel(): RcFloat = RcFloat(writer, Rc.System.API_LEVEL)

    override fun windowWidth(): RcFloat = RcFloat(writer, Rc.System.WINDOW_WIDTH)

    override fun windowHeight(): RcFloat = RcFloat(writer, Rc.System.WINDOW_HEIGHT)

    override fun continuousSec(): RcFloat = RcFloat(writer, FLOAT_CONTINUOUS_SEC)

    override fun utcOffset(): RcFloat = RcFloat(writer, FLOAT_OFFSET_TO_UTC)

    override fun month(): RcFloat = RcFloat(writer, FLOAT_CALENDAR_MONTH)

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

    override fun remoteInteger(value: Int): RcInteger = RcInteger(writer.addInteger(value), writer)

    override fun remoteNamedInteger(name: String, value: Int): RcInteger =
        RcInteger(writer.addNamedInt(name, value), writer)

    override fun remoteLong(value: Long): RcInteger =
        RcInteger(writer.addLong(value).toLong(), writer)

    override fun remoteBoolean(value: Boolean): RcInteger =
        RcInteger(writer.addBoolean(value).toLong(), writer)

    override fun remotePathData(path: RcPlatformServices.RcPathArrayCreator): RcPath =
        RcPath(writer.addPathData(path))

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

    override fun drawScaledBitmap(
        image: RcImage,
        srcLeft: Float,
        srcTop: Float,
        srcRight: Float,
        srcBottom: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        scaleType: RcContentScale,
        scaleFactor: Float,
        contentDescription: String?,
    ) {
        writer.drawScaledBitmap(
            image.id,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            scaleType.value,
            scaleFactor,
            contentDescription,
        )
    }

    override fun drawScaledBitmap(
        image: RcImage,
        srcLeft: RcFloat,
        srcTop: RcFloat,
        srcRight: RcFloat,
        srcBottom: RcFloat,
        dstLeft: RcFloat,
        dstTop: RcFloat,
        dstRight: RcFloat,
        dstBottom: RcFloat,
        scaleType: RcContentScale,
        scaleFactor: RcFloat,
        contentDescription: String?,
    ) {
        writer.drawScaledBitmap(
            image.id,
            srcLeft.withWriter(writer).toFloat(),
            srcTop.withWriter(writer).toFloat(),
            srcRight.withWriter(writer).toFloat(),
            srcBottom.withWriter(writer).toFloat(),
            dstLeft.withWriter(writer).toFloat(),
            dstTop.withWriter(writer).toFloat(),
            dstRight.withWriter(writer).toFloat(),
            dstBottom.withWriter(writer).toFloat(),
            scaleType.value,
            scaleFactor.withWriter(writer).toFloat(),
            contentDescription,
        )
    }

    override fun createBitmap(width: Int, height: Int): RcImage {
        return RcImage(writer.createBitmap(width, height))
    }

    override fun drawOnBitmap(
        image: RcImage,
        mode: DrawOnBitmapMode,
        color: RcColorValue,
        block: RcScope.() -> Unit,
    ) {
        writer.drawOnBitmap(image.id, mode.value, color.id)
        RcScopeImpl(writer).block()
        writer.drawOnBitmap(0)
    }

    override fun createShader(
        shaderString: String,
        block: (androidx.compose.remote.creation.RemoteComposeShader.() -> Unit)?,
    ): RcShader {
        val shader = writer.createShader(shaderString)
        if (block != null) {
            shader.block()
        }
        return RcShader(shader.commit())
    }

    override fun shader(shaderString: String, block: RcShaderScope.() -> Unit): RcShader {
        val shader = writer.createShader(shaderString)
        val scope = RcShaderScopeImpl(shader)
        scope.block()
        return RcShader(shader.commit())
    }

    override fun Float.format(whole: Int, decimal: Int, flags: Int): RcText {
        return RcText(writer.createTextFromFloat(this, whole, decimal, flags))
    }

    override val Int.rf: RcFloat
        get() = RcFloat(writer, this.toFloat())

    override val Float.rf: RcFloat
        get() = RcFloat(writer, this)

    override val Int.ri: RcInteger
        get() = RcInteger(writer.addInteger(this), writer)

    override val Boolean.rb: RcBool
        get() = RcBool(writer.addBoolean(this).toLong())

    override fun remoteBool(value: Boolean): RcBool = RcBool(writer.addBoolean(value).toLong())

    override fun RcPath.tween(path2: RcPath, tween: Float): RcPath {
        return RcPath(writer.pathTween(this.id, path2.id, tween))
    }

    override fun RcPath.tween(path2: RcPath, tween: RcFloat): RcPath {
        return RcPath(writer.pathTween(this.id, path2.id, tween.withWriter(writer).toFloat()))
    }

    override fun RcColor.setName(name: String) {
        writer.setColorName(this.id, name)
    }

    override fun RcText.setName(name: String) {
        writer.setStringName(this.id, name)
    }

    override fun RcFloat.setName(name: String) {
        this@RcScopeImpl.writer.setFloatName(Utils.idFromNan(this.id), name)
    }

    override fun setFloatName(value: RcFloat, name: String) {
        writer.setFloatName(Utils.idFromNan(value.withWriter(writer).toFloat()), name)
    }

    override fun RcText.transform(
        start: RcFloat,
        len: RcFloat,
        operation: RcTextTransformOp,
    ): RcText {
        return RcText(
            writer.textTransform(
                this.id,
                start.withWriter(writer).toFloat(),
                len.withWriter(writer).toFloat(),
                operation.value,
            )
        )
    }

    override fun RcText.transform(start: Float, len: Float, operation: RcTextTransformOp): RcText {
        return RcText(writer.textTransform(this.id, start, len, operation.value))
    }

    override fun textMeasure(text: RcText, mode: Int): RcFloat {
        return RcFloat(writer, writer.textMeasure(text.id, mode))
    }

    override fun textLength(text: RcText): RcFloat {
        return RcFloat(writer, writer.textLength(text.id))
    }

    override fun timeAttribute(variable: RcInteger, type: Short, vararg args: Int): RcFloat {
        return RcFloat(
            writer,
            writer.timeAttribute((variable.id % 0x100000000L).toInt(), type, *args),
        )
    }

    override fun skew(skewX: Float, skewY: Float) {
        writer.skew(skewX, skewY)
    }

    override fun matrixFromPath(path: RcPath, fraction: Float, vOffset: Float, flags: Int) {
        writer.matrixFromPath(path.id, fraction, vOffset, flags)
    }

    override fun matrixFromPath(path: RcPath, fraction: RcFloat, vOffset: RcFloat, flags: Int) {
        writer.matrixFromPath(
            path.id,
            fraction.withWriter(writer).toFloat(),
            vOffset.withWriter(writer).toFloat(),
            flags,
        )
    }

    override fun conditionalOperations(
        type: Byte,
        a: RcFloat,
        b: RcFloat,
        content: RcScope.() -> Unit,
    ) {
        writer.conditionalOperations(
            type,
            a.withWriter(writer).toFloat(),
            b.withWriter(writer).toFloat(),
        )
        this.content()
        writer.endConditionalOperations()
    }

    override fun rcLoop(
        start: RcFloat,
        step: Float,
        end: RcFloat,
        block: RcScope.(RcFloat) -> Unit,
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
                    this@RcScopeImpl.block(RcFloat(writer, v.array))
                }
            },
        )
    }

    override fun createParticles(
        variables: FloatArray,
        initialExpressions: Array<RcFloat>,
        particleCount: Int,
    ): RcFloat {
        val initialExprArrays = initialExpressions.map { it.toArray() }.toTypedArray()
        return RcFloat(writer, writer.createParticles(variables, initialExprArrays, particleCount))
    }

    override fun particlesLoop(
        id: RcFloat,
        restart: RcFloat?,
        expressions: Array<RcFloat>,
        block: RcScope.() -> Unit,
    ) {
        val restartArray = restart?.toArray()
        val exprArrays = expressions.map { it.toArray() }.toTypedArray()
        writer.particlesLoop(
            id.withWriter(writer).toFloat(),
            restartArray,
            exprArrays,
            object : Runnable {
                override fun run() {
                    RcScopeImpl(writer).block()
                }
            },
        )
    }

    override fun particlesComparison(
        id: RcFloat,
        flags: Short,
        min: RcFloat,
        max: RcFloat,
        condition: RcFloat?,
        then1: Array<RcFloat>?,
        then2: Array<RcFloat>?,
        block: RcScope.() -> Unit,
    ) {
        val minVal = min.withWriter(writer).toFloat()
        val maxVal = max.withWriter(writer).toFloat()
        val condArray = condition?.toArray()
        val then1Arrays = then1?.map { it.toArray() }?.toTypedArray()
        val then2Arrays = then2?.map { it.toArray() }?.toTypedArray()
        writer.particlesComparison(
            id.withWriter(writer).toFloat(),
            flags,
            minVal,
            maxVal,
            condArray,
            then1Arrays,
            then2Arrays,
            object : Runnable {
                override fun run() {
                    RcScopeImpl(writer).block()
                }
            },
        )
    }

    override fun particlesComparison(
        id: RcFloat,
        flags: Short,
        min: RcFloat,
        max: RcFloat,
        condition: RcFloat?,
        then: Array<RcFloat>?,
        block: RcScope.() -> Unit,
    ) {
        val minVal = min.withWriter(writer).toFloat()
        val maxVal = max.withWriter(writer).toFloat()
        val condArray = condition?.toArray()
        val thenArrays = then?.map { it.toArray() }?.toTypedArray()
        writer.particlesComparison(
            id.withWriter(writer).toFloat(),
            flags,
            minVal,
            maxVal,
            condArray,
            thenArrays,
            object : Runnable {
                override fun run() {
                    RcScopeImpl(writer).block()
                }
            },
        )
    }

    override fun impulse(duration: RcFloat, start: RcFloat, block: RcImpulseScope.() -> Unit) {
        writer.impulse(
            duration.withWriter(writer).toFloat(),
            start.withWriter(writer).toFloat(),
            object : Runnable {
                override fun run() {
                    RcImpulseScopeImpl(writer).block()
                }
            },
        )
    }

    override fun skip(type: Short, value: Int, block: RcScope.() -> Unit) {
        val offset = writer.beginSkip(type, value)
        this.block()
        writer.endSkip(offset)
    }

    override fun beginSkip(type: Short, value: Int): Int {
        return writer.beginSkip(type, value)
    }

    override fun endSkip(offset: Int) {
        writer.endSkip(offset)
    }

    override fun deltaTime(): RcFloat = RcFloat(writer, floatArrayOf(FLOAT_ANIMATION_DELTA_TIME))

    override fun RcDynamicPath.lineTo(x: Float, y: Float) {
        writer.pathAppendLineTo(id, x, y)
    }

    override fun RcDynamicPath.moveTo(x: Float, y: Float) {
        writer.pathAppendMoveTo(id, x, y)
    }

    override fun RcDynamicPath.quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        writer.pathAppendQuadTo(id, x1, y2, x2, y2)
    }

    override fun RcDynamicPath.getPath(): RcPath {
        return RcPath(id)
    }

    override fun RcDynamicPath.close() {
        writer.pathAppendClose(id)
    }

    override fun RcDynamicPath.reset() {
        writer.pathAppendReset(id)
    }

    override fun remoteIntArray(array: IntArray): RcFloat = RcFloat(writer, writer.addList(array))

    override fun remoteDynamicFloatArray(size: Float): RcFloat =
        RcFloat(writer, writer.addDynamicFloatArray(size))

    override fun remoteFloatList(values: FloatArray): RcFloat =
        RcFloat(writer, writer.addFloatList(values))

    override fun remoteFloatMap(keys: Array<String>, values: FloatArray): RcFloat =
        RcFloat(writer, writer.addFloatMap(keys, values))

    override fun setArrayValue(array: RcFloat, index: RcFloat, value: RcFloat) {
        val arrayId = Utils.idFromNan(array.id)
        val indexVal = index.withWriter(writer).toFloat()
        val valueVal = value.withWriter(writer).toFloat()
        writer.setArrayValue(arrayId, indexVal, valueVal)
    }
}

private class RcImpulseScopeImpl(writer: RemoteComposeWriter) :
    RcScopeImpl(writer), RcImpulseScope {
    override fun process(block: RcScope.() -> Unit) {
        writer.impulseProcess(
            object : Runnable {
                override fun run() {
                    RcScopeImpl(writer).block()
                }
            }
        )
    }
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

    override fun remotePath(x: Float, y: Float): RcDynamicPath =
        RcDynamicPath(writer.pathCreate(x, y))

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
        step: RcFloat,
        end: RcFloat,
        block: RcCanvasScope.(RcFloat) -> Unit,
    ) {
        val indexId = writer.textCreateId("index")
        val from: Float = start.withWriter(writer).toFloat()
        val stepF: Float = step.withWriter(writer).toFloat()
        val until: Float = end.withWriter(writer).toFloat()
        writer.loop(indexId, from, stepF, until) {
            val v =
                androidx.compose.remote.creation.RFloat(writer, floatArrayOf(Utils.asNan(indexId)))
            this@RcCanvasScopeImpl.block(RcFloat(writer, v.array))
        }
    }

    override fun clipPath(path: RcPath) {
        writer.addClipPath(path.id)
    }

    override fun addTouch(
        defValue: Float,
        min: Float,
        max: Float,
        touchMode: Int,
        velocityId: Float,
        touchEffects: Int,
        touchSpec: FloatArray?,
        easingSpec: FloatArray?,
        vararg exp: Float,
    ): RcFloat {
        return RcFloat(
            writer,
            writer.addTouch(
                defValue,
                min,
                max,
                touchMode,
                velocityId,
                touchEffects,
                touchSpec,
                easingSpec,
                *exp,
            ),
        )
    }
}

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

package androidx.compose.remote.document

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo

/** Glyph descriptor for [RemoteDocumentWriter.addBitmapFont]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBitmapGlyph(
    public val chars: String,
    public val bitmapId: Int,
    @Suppress("NoByteOrShort") public val marginLeft: Short,
    @Suppress("NoByteOrShort") public val marginTop: Short,
    @Suppress("NoByteOrShort") public val marginRight: Short,
    @Suppress("NoByteOrShort") public val marginBottom: Short,
    @Suppress("NoByteOrShort") public val width: Short = 0,
    @Suppress("NoByteOrShort") public val height: Short = 0,
)

/** Property descriptor for [RemoteDocumentWriter.startCustom]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCustomProperty
private constructor(
    @Suppress("NoByteOrShort") public val id: Short,
    @Suppress("NoByteOrShort") public val type: Short,
    public val intValue: Int,
    public val floatValue: MaybeVariableFloat,
) {
    public companion object {
        public operator fun invoke(
            @Suppress("NoByteOrShort") id: Short,
            @Suppress("NoByteOrShort") type: Short,
            value: Int,
        ): RemoteCustomProperty = RemoteCustomProperty(id, type, value, 0f.mvf)

        public operator fun invoke(
            @Suppress("NoByteOrShort") id: Short,
            @Suppress("NoByteOrShort") type: Short,
            value: MaybeVariableFloat,
        ): RemoteCustomProperty = RemoteCustomProperty(id, type, 0, value)
    }
}

/** Delta paint writer passed to [RemoteDocumentWriter.writePaint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemotePaintWriter {
    public fun setColor(@ColorInt color: Int)

    public fun setColorId(colorId: RemoteColorId)

    public fun setStrokeWidth(width: MaybeVariableFloat)

    public fun setStrokeCap(cap: Int)

    public fun setStrokeJoin(join: Int)

    public fun setStrokeMiter(miter: MaybeVariableFloat)

    public fun setStyle(style: Int)

    public fun setAntiAlias(antiAlias: Boolean)

    public fun setFilterBitmap(filter: Boolean)

    public fun setBlendMode(blendMode: Int)

    public fun setTextSize(size: MaybeVariableFloat)

    public fun setTextStyle(typefaceId: Int, weight: Int, isItalic: Boolean)

    public fun setTextAxis(axisTagIds: IntArray, axisValues: List<MaybeVariableFloat>)

    public fun setColorFilter(@ColorInt color: Int, blendMode: Int)

    public fun setColorFilterId(colorId: RemoteColorId, blendMode: Int)

    public fun clearColorFilter()

    public fun setShader(shaderId: Int)

    public fun setShaderMatrix(matrixId: MaybeVariableFloat)

    public fun setLinearGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        startX: MaybeVariableFloat,
        startY: MaybeVariableFloat,
        endX: MaybeVariableFloat,
        endY: MaybeVariableFloat,
        tileMode: Int,
    )

    public fun setRadialGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
        tileMode: Int,
    )

    public fun setSweepGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
    )

    public fun setTextureShader(
        bitmapId: RemoteBitmapId,
        tileModeX: Int,
        tileModeY: Int,
        filterMode: Int,
        maxAnisotropy: Int,
    )
}

/**
 * Pure-JVM writer interface for authoring RemoteCompose documents without exposing raw NaN-encoded
 * floats or `remote-core` / `remote-creation-core` types.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteDocumentWriter {

    // ---- 1. Lifecycle, State & Global Hoisting ----
    public val bytesWritten: Int

    public val lastComponentId: Int

    public val writerCallback: Any?

    public fun allocateId(): Int

    public fun allocateFloatId(): RemoteFloatId

    public fun beginGlobal()

    public fun endGlobal()

    public fun checkAndClearForceSendingNewPaint(): Boolean

    /**
     * Serializes the document into a [ByteArray] using the compression mode configured on this
     * writer (for example, via a `Header.COMPRESS` header tag).
     */
    public fun toByteArray(): ByteArray

    // ---- 2. Component Values ----
    public fun addComponentValue(type: Int): RemoteFloatId

    public fun bindComponentValue(valueId: RemoteFloatId, type: Int, componentId: Int = -1)

    // ---- 3. Variables, Constants & Expressions ----
    public fun addFloatConstant(value: Float): RemoteFloatId

    public fun addMutableFloat(initialValue: Float): RemoteFloatId

    public fun addNamedFloat(name: String, initialValue: Float): RemoteFloatId

    public fun addFloatExpression(vararg values: Float): RemoteFloatId

    public fun addAnimatedFloat(expression: FloatArray, animation: FloatArray?): RemoteFloatId

    public fun addIntConstant(value: Int): RemoteIntId

    public fun addMutableInt(initialValue: Int): RemoteIntId

    public fun addNamedInt(name: String, initialValue: Int): RemoteIntId

    public fun addIntegerExpression(vararg values: Long): RemoteIntId

    public fun addLongConstant(value: Long): RemoteLongId

    public fun addMutableLong(initialValue: Long): RemoteLongId

    public fun addNamedLong(name: String, initialValue: Long): RemoteLongId

    public fun setNamedVariable(id: Int, name: String, type: Int)

    // ---- 4. Colors ----
    public fun addColorConstant(@ColorInt color: Int): RemoteColorId

    public fun addNamedColor(name: String, @ColorInt initialColor: Int): RemoteColorId

    public fun addThemedColor(
        groupName: String,
        @Suppress("NoByteOrShort") lightId: Short,
        @Suppress("NoByteOrShort") darkId: Short,
        @ColorInt lightFallback: Int,
        @ColorInt darkFallback: Int,
    ): RemoteColorId

    public fun addColorExpressionArgb(
        alpha: MaybeVariableFloat,
        red: MaybeVariableFloat,
        green: MaybeVariableFloat,
        blue: MaybeVariableFloat,
    ): RemoteColorId

    public fun addColorExpressionHsv(
        alpha: Int,
        hue: MaybeVariableFloat,
        saturation: MaybeVariableFloat,
        value: MaybeVariableFloat,
    ): RemoteColorId

    public fun addColorTween(
        @ColorInt color1: Int,
        @ColorInt color2: Int,
        tween: MaybeVariableFloat,
    ): RemoteColorId

    public fun addColorIdTween(
        colorId1: RemoteColorId,
        colorId2: RemoteColorId,
        tween: MaybeVariableFloat,
    ): RemoteColorId

    public fun getColorAttribute(baseColorId: RemoteColorId, attributeType: Int): RemoteFloatId

    // ---- 5. Strings, Arrays, Lists, Time & Matrices ----
    public fun addText(text: String): RemoteStringId

    public fun addMutableText(initialValue: String): RemoteStringId

    public fun addNamedString(name: String, initialValue: String): RemoteStringId

    public fun textFromFloat(
        value: MaybeVariableFloat,
        digitsBefore: Int,
        digitsAfter: Int,
        flags: Int,
    ): RemoteStringId

    public fun textMerge(first: RemoteStringId, second: RemoteStringId): RemoteStringId

    public fun textSubtext(
        textId: RemoteStringId,
        start: MaybeVariableFloat,
        len: MaybeVariableFloat,
    ): RemoteStringId

    public fun textTransform(
        textId: RemoteStringId,
        start: MaybeVariableFloat,
        len: MaybeVariableFloat,
        operation: Int,
    ): RemoteStringId

    public fun textLength(textId: RemoteStringId): RemoteFloatId

    public fun textAttribute(textId: RemoteStringId, attributeType: Int): RemoteFloatId

    public fun textLookup(arrayId: RemoteArrayId, index: MaybeVariableFloat): RemoteStringId

    public fun textLookup(arrayId: RemoteArrayId, indexId: RemoteIntId): RemoteStringId

    public fun addIdList(ids: IntArray): RemoteArrayId

    public fun idLookup(arrayId: RemoteArrayId, index: MaybeVariableFloat, destId: Int = -1): Int

    public fun addFloatArray(values: FloatArray): RemoteArrayId

    public fun addDynamicFloatArray(size: MaybeVariableFloat): RemoteArrayId

    public fun setArrayValue(
        arrayId: RemoteArrayId,
        index: MaybeVariableFloat,
        value: MaybeVariableFloat,
    )

    public fun timeAttribute(longId: RemoteLongId, attributeType: Int): RemoteFloatId

    public fun matrixExpression(vararg exp: Float): RemoteFloatId

    // ---- 6. Bitmaps, Bitmap Fonts & Paths ----
    public fun addBitmap(image: Any): RemoteBitmapId

    public fun addNamedBitmap(name: String, image: Any): RemoteBitmapId

    public fun addBitmapUrl(url: String, width: Int = 1, height: Int = 1): RemoteBitmapId

    public fun createBitmap(width: Int, height: Int): RemoteBitmapId

    public fun bitmapAttribute(bitmapId: RemoteBitmapId, attributeType: Int): RemoteFloatId

    public fun addBitmapFont(
        glyphs: List<RemoteBitmapGlyph>,
        @Suppress("NoByteOrShort") kerning: Map<String, Short> = emptyMap(),
    ): RemoteFontId

    public fun bitmapTextMeasure(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        measureType: Int,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    ): RemoteFloatId

    public fun addPathData(pathData: FloatArray, winding: Int = 0): RemotePathId

    public fun addPathString(pathData: String): RemotePathId

    public fun addShader(
        shaderSource: String,
        floatUniforms: Map<String, List<MaybeVariableFloat>> = emptyMap(),
        intUniforms: Map<String, IntArray> = emptyMap(),
        bitmapUniforms: Map<String, RemoteBitmapId> = emptyMap(),
    ): RemoteShaderId

    // ---- 7. Delta Paint ----
    public fun writePaint(block: (RemotePaintWriter) -> Unit)

    // ---- 8. Canvas Drawing, Transforms, Control Flow & Loom Patterns ----
    public fun save()

    public fun restore()

    public fun translate(dx: MaybeVariableFloat, dy: MaybeVariableFloat)

    public fun scale(
        scaleX: MaybeVariableFloat,
        scaleY: MaybeVariableFloat,
        pivotX: MaybeVariableFloat = MaybeVariableFloat.NaN,
        pivotY: MaybeVariableFloat = MaybeVariableFloat.NaN,
    )

    public fun rotate(
        degrees: MaybeVariableFloat,
        pivotX: MaybeVariableFloat = MaybeVariableFloat.NaN,
        pivotY: MaybeVariableFloat = MaybeVariableFloat.NaN,
    )

    public fun skew(skewX: MaybeVariableFloat, skewY: MaybeVariableFloat)

    public fun clipRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    )

    public fun clipPath(pathId: RemotePathId, regionOp: Int = 0)

    public fun drawContent()

    public fun drawRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    )

    public fun drawRoundRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        radiusX: MaybeVariableFloat,
        radiusY: MaybeVariableFloat,
    )

    public fun drawCircle(
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
    )

    public fun drawOval(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    )

    public fun drawArc(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        sweepAngle: MaybeVariableFloat,
    )

    public fun drawSector(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        sweepAngle: MaybeVariableFloat,
    )

    public fun drawLine(
        x1: MaybeVariableFloat,
        y1: MaybeVariableFloat,
        x2: MaybeVariableFloat,
        y2: MaybeVariableFloat,
    )

    public fun drawPath(pathId: RemotePathId)

    public fun drawTweenPath(
        path1Id: RemotePathId,
        path2Id: RemotePathId,
        tween: MaybeVariableFloat,
        start: MaybeVariableFloat = 0f.mvf,
        stop: MaybeVariableFloat = 1f.mvf,
    )

    public fun drawBitmap(
        imageId: RemoteBitmapId,
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        contentDescription: String? = null,
    )

    public fun drawBitmap(
        imageId: RemoteBitmapId,
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        contentDescription: String? = null,
    )

    public fun drawBitmapInt(
        imageId: RemoteBitmapId,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        contentDescription: String? = null,
    )

    public fun drawScaledBitmap(
        imageId: RemoteBitmapId,
        srcLeft: MaybeVariableFloat,
        srcTop: MaybeVariableFloat,
        srcRight: MaybeVariableFloat,
        srcBottom: MaybeVariableFloat,
        dstLeft: MaybeVariableFloat,
        dstTop: MaybeVariableFloat,
        dstRight: MaybeVariableFloat,
        dstBottom: MaybeVariableFloat,
        scaleType: Int,
        scaleFactor: MaybeVariableFloat,
        contentDescription: String? = null,
    )

    public fun drawTextRun(
        textId: RemoteStringId,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        rtl: Boolean,
    )

    public fun drawTextAnchored(
        textId: RemoteStringId,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        panX: MaybeVariableFloat = 0f.mvf,
        panY: MaybeVariableFloat = 0f.mvf,
        flags: Int = 0,
    )

    public fun drawTextOnPath(
        textId: RemoteStringId,
        pathId: RemotePathId,
        hOffset: MaybeVariableFloat,
        vOffset: MaybeVariableFloat,
    )

    public fun drawTextOnCircle(
        textId: RemoteStringId,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        warpRadiusOffset: MaybeVariableFloat,
        alignment: Int,
        placement: Int,
    )

    public fun drawBitmapFontTextRun(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        start: Int,
        end: Int,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    )

    public fun drawBitmapFontTextRunOnPath(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        pathId: RemotePathId,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float = 0f,
    )

    public fun drawBitmapTextAnchored(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        start: MaybeVariableFloat,
        end: MaybeVariableFloat,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        panX: MaybeVariableFloat = 0f.mvf,
        panY: MaybeVariableFloat = 0f.mvf,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    )

    public fun drawOnBitmap(bitmapId: RemoteBitmapId, mode: Int, @ColorInt color: Int)

    public fun conditionalOperations(
        type: Int,
        a: MaybeVariableFloat,
        b: MaybeVariableFloat,
        content: () -> Unit,
    )

    public fun startLoop(
        indexId: Int,
        from: MaybeVariableFloat,
        step: MaybeVariableFloat,
        until: MaybeVariableFloat,
    )

    public fun endLoop()

    public fun loop(
        indexId: Int,
        from: MaybeVariableFloat,
        step: MaybeVariableFloat,
        until: MaybeVariableFloat,
        content: () -> Unit,
    ) {
        startLoop(indexId, from, step, until)
        try {
            content()
        } finally {
            endLoop()
        }
    }

    public fun definePattern(patternId: Int, paramIds: IntArray)

    public fun endPatternDefine()

    public fun inflatePattern(patternId: Int, argIds: IntArray)

    public fun endPatternInflation()

    public fun startPatternForEach(collectionId: Int, itemId: Int)

    public fun endPatternForEach()

    // ---- 9. Layout Containers ----
    public fun startRoot(componentId: Int = -1): Int

    public fun endRoot()

    public fun root(componentId: Int = -1, content: () -> Unit) {
        startRoot(componentId)
        try {
            content()
        } finally {
            endRoot()
        }
    }

    public fun startBox(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endBox()

    public fun startFitBox(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endFitBox()

    public fun startRow(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endRow()

    public fun startColumn(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endColumn()

    public fun startCollapsibleRow(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endCollapsibleRow()

    public fun startCollapsibleColumn(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endCollapsibleColumn()

    public fun startFlow(
        positioning: Int,
        verticalAlignment: Int,
        maxItemsInEachRow: Int = Int.MAX_VALUE,
        maxLines: Int = Int.MAX_VALUE,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endFlow()

    public fun startStateLayout(
        indexId: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endStateLayout()

    public fun startCanvas(componentId: Int = -1, modifiers: () -> Unit = {}): Int

    public fun endCanvas()

    public fun startCanvasOperations()

    public fun endCanvasOperations()

    public fun imageComponent(
        imageId: RemoteBitmapId,
        scaleType: Int,
        alpha: MaybeVariableFloat,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun addTextStyle(
        color: Int? = null,
        colorId: Int? = null,
        fontSize: MaybeVariableFloat? = null,
        minFontSize: MaybeVariableFloat? = null,
        maxFontSize: MaybeVariableFloat? = null,
        fontStyle: Int? = null,
        fontWeight: MaybeVariableFloat? = null,
        fontFamily: String? = null,
        textAlign: Int? = null,
        overflow: Int? = null,
        maxLines: Int? = null,
        letterSpacing: MaybeVariableFloat? = null,
        lineHeightAdd: MaybeVariableFloat? = null,
        lineHeightMultiplier: MaybeVariableFloat? = null,
        lineBreakStrategy: Int? = null,
        hyphenationFrequency: Int? = null,
        justificationMode: Int? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        fontAxisNames: Array<String>? = null,
        fontAxisValues: List<MaybeVariableFloat>? = null,
        autosize: Boolean? = null,
        parentId: Int = 0,
    ): Int

    public fun textComponent(
        textId: RemoteStringId,
        styleId: Int,
        flags: Int = 0,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun textComponent(
        textId: RemoteStringId,
        color: Int,
        colorId: Int,
        fontSize: MaybeVariableFloat,
        minFontSize: MaybeVariableFloat?,
        maxFontSize: MaybeVariableFloat?,
        fontStyle: Int,
        fontWeight: MaybeVariableFloat,
        fontFamily: String?,
        textAlign: Int,
        overflow: Int,
        maxLines: Int,
        letterSpacing: MaybeVariableFloat,
        lineHeightAdd: MaybeVariableFloat,
        lineHeightMultiplier: MaybeVariableFloat,
        lineBreakStrategy: Int,
        hyphenationFrequency: Int,
        justificationMode: Int,
        underline: Boolean,
        strikethrough: Boolean,
        fontAxisNames: Array<String>?,
        fontAxisValues: List<MaybeVariableFloat>?,
        autosize: Boolean,
        flags: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun startCustom(
        config: String,
        properties: List<RemoteCustomProperty>,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    public fun endCustom()

    // ---- 10. Actions ----
    public fun addHostAction(actionId: Int)

    public fun addHostActionMetadata(actionId: Int, metadataId: RemoteStringId)

    public fun addHostNamedAction(nameId: RemoteStringId, type: Int, valueId: Int)

    public fun addValueIntegerChangeAction(targetValueId: RemoteIntId, value: Int)

    public fun addValueIntegerExpressionChangeAction(
        targetValueId: RemoteIntId,
        srcValueId: RemoteIntId,
    )

    public fun addValueFloatChangeAction(targetValueId: RemoteFloatId, value: MaybeVariableFloat)

    public fun addValueFloatExpressionChangeAction(
        targetValueId: RemoteFloatId,
        srcValueId: RemoteFloatId,
    )

    public fun addValueStringChangeAction(targetTextId: RemoteStringId, srcTextId: RemoteStringId)
}

/** Writes [ids] as a variable-ID list and returns its [RemoteArrayId]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDocumentWriter.addIdList(vararg ids: Int): RemoteArrayId = addIdList(ids)

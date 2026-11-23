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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.layout.managers.Custom.CustomProperty
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.document.MaybeVariableFloat
import androidx.compose.remote.document.NanEncoding
import androidx.compose.remote.document.RemoteArrayId
import androidx.compose.remote.document.RemoteBitmapGlyph
import androidx.compose.remote.document.RemoteBitmapId
import androidx.compose.remote.document.RemoteColorId
import androidx.compose.remote.document.RemoteCustomProperty
import androidx.compose.remote.document.RemoteDocumentWriter
import androidx.compose.remote.document.RemoteFloatId
import androidx.compose.remote.document.RemoteFontId
import androidx.compose.remote.document.RemoteIntId
import androidx.compose.remote.document.RemoteLongId
import androidx.compose.remote.document.RemotePaintWriter
import androidx.compose.remote.document.RemotePathId
import androidx.compose.remote.document.RemoteShaderId
import androidx.compose.remote.document.RemoteStringId

/**
 * Adapts a legacy [RemoteComposeWriter] to [RemoteDocumentWriter], encapsulating all `PaintBundle`,
 * `RecordingModifier`, and `RemoteComposeBuffer` interactions in one place.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeWriterAdapter(public val delegate: RemoteComposeWriter) :
    RemoteDocumentWriter {

    // ---- 1. Lifecycle, State & Global Hoisting ----

    override val bytesWritten: Int
        get() = delegate.buffer.buffer.size()

    override val lastComponentId: Int
        get() = delegate.buffer.lastComponentId

    override val writerCallback: Any?
        get() = delegate.writerCallback

    override fun allocateId(): Int = delegate.nextId()

    override fun allocateFloatId(): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.reserveFloatVariable()))

    override fun beginGlobal() {
        delegate.beginGlobal()
    }

    override fun endGlobal() {
        delegate.endGlobal()
    }

    override fun checkAndClearForceSendingNewPaint(): Boolean =
        delegate.checkAndClearForceSendingNewPaint()

    override fun toByteArray(): ByteArray = delegate.encodeToByteArray()

    // ---- 2. Component Values ----

    override fun addComponentValue(type: Int): RemoteFloatId {
        val nan =
            when (type) {
                ComponentValue.WIDTH -> delegate.addComponentWidthValue()
                ComponentValue.HEIGHT -> delegate.addComponentHeightValue()
                ComponentValue.POS_X -> delegate.addComponentXValue()
                ComponentValue.POS_Y -> delegate.addComponentYValue()
                ComponentValue.POS_ROOT_X -> delegate.addComponentRootXValue()
                ComponentValue.POS_ROOT_Y -> delegate.addComponentRootYValue()
                else -> {
                    val id = NanEncoding.idFromNan(delegate.reserveFloatVariable())
                    delegate.buffer.addComponentValue(id, type)
                    NanEncoding.asNan(id)
                }
            }
        return RemoteFloatId(NanEncoding.idFromNan(nan))
    }

    override fun bindComponentValue(valueId: RemoteFloatId, type: Int, componentId: Int) {
        val buffer = delegate.buffer
        if (componentId == -1 || buffer.lastComponentId == componentId) {
            buffer.addComponentValue(valueId.id, type)
        } else {
            ComponentValue.apply(buffer.buffer, type, componentId, valueId.id)
        }
    }

    // ---- 3. Variables, Constants & Expressions ----

    override fun addFloatConstant(value: Float): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.addFloatConstant(value)))

    override fun addMutableFloat(initialValue: Float): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.floatExpression(initialValue)))

    override fun addNamedFloat(name: String, initialValue: Float): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.addNamedFloat(name, initialValue)))

    override fun addFloatExpression(vararg values: Float): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.floatExpression(*values)))

    override fun addAnimatedFloat(expression: FloatArray, animation: FloatArray?): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.floatExpression(expression, animation)))

    override fun addIntConstant(value: Int): RemoteIntId =
        RemoteIntId(NanEncoding.decodeLongId(delegate.addInteger(value)))

    override fun addMutableInt(initialValue: Int): RemoteIntId =
        RemoteIntId(NanEncoding.decodeLongId(delegate.addInteger(initialValue)))

    override fun addNamedInt(name: String, initialValue: Int): RemoteIntId =
        RemoteIntId(NanEncoding.decodeLongId(delegate.addNamedInt(name, initialValue)))

    override fun addIntegerExpression(vararg values: Long): RemoteIntId =
        RemoteIntId(NanEncoding.decodeLongId(delegate.integerExpression(*values)))

    override fun addLongConstant(value: Long): RemoteLongId = RemoteLongId(delegate.addLong(value))

    override fun addMutableLong(initialValue: Long): RemoteLongId =
        RemoteLongId(delegate.addLong(initialValue))

    override fun addNamedLong(name: String, initialValue: Long): RemoteLongId =
        RemoteLongId(delegate.addNamedLong(name, initialValue))

    override fun setNamedVariable(id: Int, name: String, type: Int) {
        delegate.setNamedVariable(id, name, type)
    }

    // ---- 4. Colors ----

    override fun addColorConstant(color: Int): RemoteColorId =
        RemoteColorId(delegate.addColor(color))

    override fun addNamedColor(name: String, initialColor: Int): RemoteColorId =
        RemoteColorId(delegate.addNamedColor(name, initialColor))

    override fun addThemedColor(
        groupName: String,
        @Suppress("NoByteOrShort") lightId: Short,
        @Suppress("NoByteOrShort") darkId: Short,
        lightFallback: Int,
        darkFallback: Int,
    ): RemoteColorId =
        RemoteColorId(
            delegate.addThemedColor(groupName, lightId, darkId, lightFallback, darkFallback).toInt()
        )

    override fun addColorExpressionArgb(
        alpha: MaybeVariableFloat,
        red: MaybeVariableFloat,
        green: MaybeVariableFloat,
        blue: MaybeVariableFloat,
    ): RemoteColorId =
        RemoteColorId(
            delegate
                .addColorExpression(
                    alpha.floatValue,
                    red.floatValue,
                    green.floatValue,
                    blue.floatValue,
                )
                .toInt()
        )

    override fun addColorExpressionHsv(
        alpha: Int,
        hue: MaybeVariableFloat,
        saturation: MaybeVariableFloat,
        value: MaybeVariableFloat,
    ): RemoteColorId =
        RemoteColorId(
            delegate
                .addColorExpression(
                    alpha,
                    hue.floatValue,
                    saturation.floatValue,
                    value.floatValue,
                )
                .toInt()
        )

    override fun addColorTween(color1: Int, color2: Int, tween: MaybeVariableFloat): RemoteColorId =
        RemoteColorId(delegate.addColorExpression(color1, color2, tween.floatValue).toInt())

    override fun addColorIdTween(
        colorId1: RemoteColorId,
        colorId2: RemoteColorId,
        tween: MaybeVariableFloat,
    ): RemoteColorId =
        RemoteColorId(
            delegate
                .addColorExpression(colorId1.id.toShort(), colorId2.id.toShort(), tween.floatValue)
                .toInt()
        )

    override fun getColorAttribute(baseColorId: RemoteColorId, attributeType: Int): RemoteFloatId =
        RemoteFloatId(
            NanEncoding.idFromNan(
                delegate.getColorAttribute(baseColorId.id, attributeType.toShort())
            )
        )

    // ---- 5. Strings, Arrays, Lists, Time & Matrices ----

    override fun addText(text: String): RemoteStringId = RemoteStringId(delegate.addText(text))

    override fun addMutableText(initialValue: String): RemoteStringId {
        val id = delegate.nextId()
        delegate.buffer.addText(id, initialValue)
        return RemoteStringId(id)
    }

    override fun addNamedString(name: String, initialValue: String): RemoteStringId =
        RemoteStringId(delegate.addNamedString(name, initialValue))

    override fun textFromFloat(
        value: MaybeVariableFloat,
        digitsBefore: Int,
        digitsAfter: Int,
        flags: Int,
    ): RemoteStringId =
        RemoteStringId(
            delegate.createTextFromFloat(
                value.floatValue,
                digitsBefore.toShort(),
                digitsAfter.toShort(),
                flags,
            )
        )

    override fun textMerge(first: RemoteStringId, second: RemoteStringId): RemoteStringId =
        RemoteStringId(delegate.textMerge(first.id, second.id))

    override fun textSubtext(
        textId: RemoteStringId,
        start: MaybeVariableFloat,
        len: MaybeVariableFloat,
    ): RemoteStringId =
        RemoteStringId(delegate.textSubtext(textId.id, start.floatValue, len.floatValue))

    override fun textTransform(
        textId: RemoteStringId,
        start: MaybeVariableFloat,
        len: MaybeVariableFloat,
        operation: Int,
    ): RemoteStringId =
        RemoteStringId(
            delegate.textTransform(textId.id, start.floatValue, len.floatValue, operation)
        )

    override fun textLength(textId: RemoteStringId): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.textLength(textId.id)))

    override fun textAttribute(textId: RemoteStringId, attributeType: Int): RemoteFloatId =
        RemoteFloatId(
            NanEncoding.idFromNan(delegate.textAttribute(textId.id, attributeType.toShort()))
        )

    override fun textLookup(arrayId: RemoteArrayId, index: MaybeVariableFloat): RemoteStringId =
        RemoteStringId(delegate.textLookup(arrayId.toNanFloat(), index.floatValue))

    override fun textLookup(arrayId: RemoteArrayId, indexId: RemoteIntId): RemoteStringId =
        RemoteStringId(delegate.textLookup(arrayId.toNanFloat(), indexId.id))

    override fun addIdList(ids: IntArray): RemoteArrayId =
        RemoteArrayId(NanEncoding.idFromNan(delegate.addList(ids)))

    override fun idLookup(arrayId: RemoteArrayId, index: MaybeVariableFloat, destId: Int): Int {
        if (destId != -1) {
            delegate.buffer.idLookup(destId, arrayId.toNanFloat(), index.floatValue)
            return destId
        }
        return delegate.idLookup(arrayId.toNanFloat(), index.floatValue)
    }

    override fun addFloatArray(values: FloatArray): RemoteArrayId =
        RemoteArrayId(NanEncoding.idFromNan(delegate.addFloatArray(values)))

    override fun addDynamicFloatArray(size: MaybeVariableFloat): RemoteArrayId =
        RemoteArrayId(NanEncoding.idFromNan(delegate.addDynamicFloatArray(size.floatValue)))

    override fun setArrayValue(
        arrayId: RemoteArrayId,
        index: MaybeVariableFloat,
        value: MaybeVariableFloat,
    ) {
        delegate.setArrayValue(arrayId.id, index.floatValue, value.floatValue)
    }

    override fun timeAttribute(longId: RemoteLongId, attributeType: Int): RemoteFloatId =
        RemoteFloatId(
            NanEncoding.idFromNan(delegate.timeAttribute(longId.id, attributeType.toShort()))
        )

    override fun matrixExpression(vararg exp: Float): RemoteFloatId =
        RemoteFloatId(NanEncoding.idFromNan(delegate.matrixExpression(*exp)))

    // ---- 6. Bitmaps, Bitmap Fonts & Paths ----

    override fun addBitmap(image: Any): RemoteBitmapId = RemoteBitmapId(delegate.addBitmap(image))

    override fun addNamedBitmap(name: String, image: Any): RemoteBitmapId =
        RemoteBitmapId(delegate.addNamedBitmap(name, image))

    override fun addBitmapUrl(url: String, width: Int, height: Int): RemoteBitmapId =
        RemoteBitmapId(delegate.addBitmapUrl(url, width, height))

    override fun createBitmap(width: Int, height: Int): RemoteBitmapId =
        RemoteBitmapId(delegate.createBitmap(width, height))

    override fun bitmapAttribute(bitmapId: RemoteBitmapId, attributeType: Int): RemoteFloatId =
        RemoteFloatId(
            NanEncoding.idFromNan(delegate.bitmapAttribute(bitmapId.id, attributeType.toShort()))
        )

    override fun addBitmapFont(
        glyphs: List<RemoteBitmapGlyph>,
        @Suppress("NoByteOrShort", "PrimitiveInCollection") kerning: Map<String, Short>,
    ): RemoteFontId {
        val coreGlyphs =
            Array(glyphs.size) { index ->
                val g = glyphs[index]
                BitmapFontData.Glyph(
                    g.chars,
                    g.bitmapId,
                    g.marginLeft,
                    g.marginTop,
                    g.marginRight,
                    g.marginBottom,
                    g.width,
                    g.height,
                )
            }
        return RemoteFontId(delegate.addBitmapFont(coreGlyphs, kerning))
    }

    override fun bitmapTextMeasure(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        measureType: Int,
        glyphSpacing: MaybeVariableFloat,
    ): RemoteFloatId =
        RemoteFloatId(
            NanEncoding.idFromNan(
                delegate.bitmapTextMeasure(
                    textId.id,
                    bitmapFontId.id,
                    measureType,
                    glyphSpacing.floatValue,
                )
            )
        )

    override fun addPathData(pathData: FloatArray, winding: Int): RemotePathId =
        RemotePathId(delegate.addPathData(pathData, winding))

    override fun addPathString(pathData: String): RemotePathId =
        RemotePathId(delegate.addPathString(pathData))

    @Suppress("PrimitiveInCollection")
    override fun addShader(
        shaderSource: String,
        floatUniforms: Map<String, List<MaybeVariableFloat>>,
        intUniforms: Map<String, IntArray>,
        bitmapUniforms: Map<String, RemoteBitmapId>,
    ): RemoteShaderId {
        val shader = delegate.createShader(shaderSource)
        for ((name, values) in floatUniforms) {
            shader.setFloatUniform(name, FloatArray(values.size) { values[it].floatValue })
        }
        for ((name, values) in intUniforms) {
            when (values.size) {
                1 -> shader.setIntUniform(name, values[0])
                2 -> shader.setIntUniform(name, values[0], values[1])
                3 -> shader.setIntUniform(name, values[0], values[1], values[2])
                4 -> shader.setIntUniform(name, values[0], values[1], values[2], values[3])
                else ->
                    throw IllegalArgumentException(
                        "Int uniform '$name' supports 1..4 values, got ${values.size}"
                    )
            }
        }
        for ((name, bitmapId) in bitmapUniforms) {
            shader.setBitmapUniform(name, bitmapId.id)
        }
        return RemoteShaderId(shader.commit())
    }

    // ---- 7. Delta Paint ----

    override fun writePaint(block: (RemotePaintWriter) -> Unit) {
        val paintBundle = PaintBundle()
        val paintWriter = PaintBundleWriter(paintBundle)
        block(paintWriter)
        if (paintWriter.hasChanges) {
            delegate.buffer.addPaint(paintBundle)
        }
    }

    // ---- 8. Canvas Drawing, Transforms, Control Flow & Loom Patterns ----

    override fun save() {
        delegate.save()
    }

    override fun restore() {
        delegate.restore()
    }

    override fun translate(dx: MaybeVariableFloat, dy: MaybeVariableFloat) {
        delegate.buffer.addMatrixTranslate(dx.floatValue, dy.floatValue)
    }

    override fun scale(
        scaleX: MaybeVariableFloat,
        scaleY: MaybeVariableFloat,
        pivotX: MaybeVariableFloat,
        pivotY: MaybeVariableFloat,
    ) {
        delegate.buffer.addMatrixScale(
            scaleX.floatValue,
            scaleY.floatValue,
            pivotX.floatValue,
            pivotY.floatValue,
        )
    }

    override fun rotate(
        degrees: MaybeVariableFloat,
        pivotX: MaybeVariableFloat,
        pivotY: MaybeVariableFloat,
    ) {
        delegate.buffer.addMatrixRotate(degrees.floatValue, pivotX.floatValue, pivotY.floatValue)
    }

    override fun skew(skewX: MaybeVariableFloat, skewY: MaybeVariableFloat) {
        delegate.buffer.addMatrixSkew(skewX.floatValue, skewY.floatValue)
    }

    override fun clipRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    ) {
        delegate.clipRect(left.floatValue, top.floatValue, right.floatValue, bottom.floatValue)
    }

    override fun clipPath(pathId: RemotePathId, regionOp: Int) {
        // ClipPath.read unpacks pathId from bits 0..19 and regionOp from bits 24..31.
        val packed = if (regionOp == 0) pathId.id else (pathId.id and 0xFFFFF) or (regionOp shl 24)
        delegate.addClipPath(packed)
    }

    override fun drawContent() {
        delegate.drawComponentContent()
    }

    override fun drawRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    ) {
        delegate.drawRect(left.floatValue, top.floatValue, right.floatValue, bottom.floatValue)
    }

    override fun drawRoundRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        radiusX: MaybeVariableFloat,
        radiusY: MaybeVariableFloat,
    ) {
        delegate.drawRoundRect(
            left.floatValue,
            top.floatValue,
            right.floatValue,
            bottom.floatValue,
            radiusX.floatValue,
            radiusY.floatValue,
        )
    }

    override fun drawCircle(
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
    ) {
        delegate.drawCircle(centerX.floatValue, centerY.floatValue, radius.floatValue)
    }

    override fun drawOval(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    ) {
        delegate.drawOval(left.floatValue, top.floatValue, right.floatValue, bottom.floatValue)
    }

    override fun drawArc(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        sweepAngle: MaybeVariableFloat,
    ) {
        delegate.drawArc(
            left.floatValue,
            top.floatValue,
            right.floatValue,
            bottom.floatValue,
            startAngle.floatValue,
            sweepAngle.floatValue,
        )
    }

    override fun drawSector(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        sweepAngle: MaybeVariableFloat,
    ) {
        delegate.drawSector(
            left.floatValue,
            top.floatValue,
            right.floatValue,
            bottom.floatValue,
            startAngle.floatValue,
            sweepAngle.floatValue,
        )
    }

    override fun drawLine(
        x1: MaybeVariableFloat,
        y1: MaybeVariableFloat,
        x2: MaybeVariableFloat,
        y2: MaybeVariableFloat,
    ) {
        delegate.drawLine(x1.floatValue, y1.floatValue, x2.floatValue, y2.floatValue)
    }

    override fun drawPath(pathId: RemotePathId) {
        delegate.drawPath(pathId.id)
    }

    override fun drawTweenPath(
        path1Id: RemotePathId,
        path2Id: RemotePathId,
        tween: MaybeVariableFloat,
        start: MaybeVariableFloat,
        stop: MaybeVariableFloat,
    ) {
        delegate.drawTweenPath(
            path1Id.id,
            path2Id.id,
            tween.floatValue,
            start.floatValue,
            stop.floatValue,
        )
    }

    override fun drawBitmap(
        imageId: RemoteBitmapId,
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        contentDescription: String?,
    ) {
        delegate.drawBitmap(imageId.id, left.floatValue, top.floatValue, contentDescription)
    }

    override fun drawBitmap(
        imageId: RemoteBitmapId,
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        contentDescription: String?,
    ) {
        delegate.drawBitmap(
            imageId.id,
            left.floatValue,
            top.floatValue,
            right.floatValue,
            bottom.floatValue,
            contentDescription,
        )
    }

    override fun drawScaledBitmap(
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
        contentDescription: String?,
    ) {
        delegate.drawScaledBitmap(
            imageId.id,
            srcLeft.floatValue,
            srcTop.floatValue,
            srcRight.floatValue,
            srcBottom.floatValue,
            dstLeft.floatValue,
            dstTop.floatValue,
            dstRight.floatValue,
            dstBottom.floatValue,
            scaleType,
            scaleFactor.floatValue,
            contentDescription,
        )
    }

    override fun drawBitmapInt(
        imageId: RemoteBitmapId,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        contentDescription: String?,
    ) {
        val contentDescriptionId =
            if (contentDescription != null) delegate.addText(contentDescription) else 0
        delegate.buffer.drawBitmap(
            imageId.id,
            dstRight - dstLeft,
            dstBottom - dstTop,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            contentDescriptionId,
        )
    }

    override fun drawTextRun(
        textId: RemoteStringId,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        rtl: Boolean,
    ) {
        delegate.drawTextRun(
            textId.id,
            start,
            end,
            contextStart,
            contextEnd,
            x.floatValue,
            y.floatValue,
            rtl,
        )
    }

    override fun drawTextAnchored(
        textId: RemoteStringId,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        panX: MaybeVariableFloat,
        panY: MaybeVariableFloat,
        flags: Int,
    ) {
        delegate.drawTextAnchored(
            textId.id,
            x.floatValue,
            y.floatValue,
            panX.floatValue,
            panY.floatValue,
            flags,
        )
    }

    override fun drawTextOnPath(
        textId: RemoteStringId,
        pathId: RemotePathId,
        hOffset: MaybeVariableFloat,
        vOffset: MaybeVariableFloat,
    ) {
        delegate.drawTextOnPath(textId.id, pathId.id, hOffset.floatValue, vOffset.floatValue)
    }

    override fun drawTextOnCircle(
        textId: RemoteStringId,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        warpRadiusOffset: MaybeVariableFloat,
        alignment: Int,
        placement: Int,
    ) {
        delegate.drawTextOnCircle(
            textId.id,
            centerX.floatValue,
            centerY.floatValue,
            radius.floatValue,
            startAngle.floatValue,
            warpRadiusOffset.floatValue,
            DrawTextOnCircle.Alignment.fromInt(alignment),
            DrawTextOnCircle.Placement.fromInt(placement),
        )
    }

    override fun drawBitmapFontTextRun(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        start: Int,
        end: Int,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        glyphSpacing: MaybeVariableFloat,
    ) {
        delegate.drawBitmapFontTextRun(
            textId.id,
            bitmapFontId.id,
            start,
            end,
            x.floatValue,
            y.floatValue,
            glyphSpacing.floatValue,
        )
    }

    override fun drawBitmapFontTextRunOnPath(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        pathId: RemotePathId,
        start: Int,
        end: Int,
        yAdj: MaybeVariableFloat,
        glyphSpacing: MaybeVariableFloat,
    ) {
        delegate.drawBitmapFontTextRunOnPath(
            textId.id,
            bitmapFontId.id,
            pathId.id,
            start,
            end,
            yAdj.floatValue,
            glyphSpacing.floatValue,
        )
    }

    override fun drawBitmapTextAnchored(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        start: MaybeVariableFloat,
        end: MaybeVariableFloat,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        panX: MaybeVariableFloat,
        panY: MaybeVariableFloat,
        glyphSpacing: MaybeVariableFloat,
    ) {
        delegate.drawBitmapTextAnchored(
            textId.id,
            bitmapFontId.id,
            start.floatValue,
            end.floatValue,
            x.floatValue,
            y.floatValue,
            panX.floatValue,
            panY.floatValue,
            glyphSpacing.floatValue,
        )
    }

    override fun drawOnBitmap(bitmapId: RemoteBitmapId, mode: Int, color: Int) {
        delegate.drawOnBitmap(bitmapId.id, mode, color)
    }

    override fun conditionalOperations(
        type: Int,
        a: MaybeVariableFloat,
        b: MaybeVariableFloat,
        content: () -> Unit,
    ) {
        delegate.conditionalOperations(type.toByte(), a.floatValue, b.floatValue, content)
    }

    override fun startLoop(
        indexId: Int,
        from: MaybeVariableFloat,
        step: MaybeVariableFloat,
        until: MaybeVariableFloat,
    ) {
        delegate.startLoop(indexId, from.floatValue, step.floatValue, until.floatValue)
    }

    override fun endLoop() {
        delegate.endLoop()
    }

    override fun definePattern(patternId: Int, paramIds: IntArray) {
        delegate.definePattern(patternId, paramIds)
    }

    override fun endPatternDefine() {
        delegate.endPatternDefine()
    }

    override fun inflatePattern(patternId: Int, argIds: IntArray) {
        delegate.buffer.inflatePattern(patternId, argIds)
    }

    override fun endPatternInflation() {
        delegate.buffer.endPatternInflation()
    }

    override fun startPatternForEach(collectionId: Int, itemId: Int) {
        delegate.buffer.addPatternForEach(collectionId, itemId)
    }

    override fun endPatternForEach() {
        delegate.buffer.endPatternForEach()
    }

    // ---- 9. Layout Containers ----

    override fun startRoot(componentId: Int): Int {
        delegate.startRoot()
        return delegate.buffer.lastComponentId
    }

    override fun endRoot() {
        delegate.endRoot()
    }

    override fun root(componentId: Int, content: () -> Unit) {
        delegate.root(content)
    }

    override fun startBox(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startBox(
            buildRecordingModifier(componentId, modifiers = modifiers),
            horizontalAlignment,
            verticalAlignment,
        )
    }

    override fun endBox() {
        delegate.endBox()
    }

    override fun startFitBox(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startFitBox(
            buildRecordingModifier(componentId, modifiers = modifiers),
            horizontalAlignment,
            verticalAlignment,
        )
    }

    override fun endFitBox() {
        delegate.endFitBox()
    }

    override fun startRow(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startRow(
            buildRecordingModifier(componentId, spacedBy, modifiers),
            horizontalAlignment,
            verticalAlignment,
        )
    }

    override fun endRow() {
        delegate.endRow()
    }

    override fun startColumn(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startColumn(
            buildRecordingModifier(componentId, spacedBy, modifiers),
            horizontalAlignment,
            verticalAlignment,
        )
    }

    override fun endColumn() {
        delegate.endColumn()
    }

    override fun startCollapsibleRow(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startCollapsibleRow(
            buildRecordingModifier(componentId, spacedBy, modifiers),
            horizontalAlignment,
            verticalAlignment,
        )
    }

    override fun endCollapsibleRow() {
        delegate.endCollapsibleRow()
    }

    override fun startCollapsibleColumn(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startCollapsibleColumn(
            buildRecordingModifier(componentId, spacedBy, modifiers),
            horizontalAlignment,
            verticalAlignment,
        )
    }

    override fun endCollapsibleColumn() {
        delegate.endCollapsibleColumn()
    }

    override fun startFlow(
        positioning: Int,
        verticalAlignment: Int,
        maxItemsInEachRow: Int,
        maxLines: Int,
        spacedBy: MaybeVariableFloat,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startFlow(
            buildRecordingModifier(componentId, spacedBy, modifiers),
            positioning,
            verticalAlignment,
            maxItemsInEachRow,
            maxLines,
        )
    }

    override fun endFlow() {
        delegate.endFlow()
    }

    override fun startStateLayout(indexId: Int, componentId: Int, modifiers: () -> Unit) {
        delegate.startStateLayout(
            buildRecordingModifier(componentId, modifiers = modifiers),
            indexId,
        )
    }

    override fun endStateLayout() {
        delegate.endStateLayout()
    }

    override fun startCanvas(componentId: Int, modifiers: () -> Unit): Int {
        delegate.startCanvas(buildRecordingModifier(componentId, modifiers = modifiers))
        return delegate.buffer.lastComponentId
    }

    override fun endCanvas() {
        delegate.endCanvas()
    }

    override fun startCanvasOperations() {
        delegate.startCanvasOperations()
    }

    override fun endCanvasOperations() {
        delegate.endCanvasOperations()
    }

    override fun imageComponent(
        imageId: RemoteBitmapId,
        scaleType: Int,
        alpha: MaybeVariableFloat,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.image(
            buildRecordingModifier(componentId, modifiers = modifiers),
            imageId.id,
            scaleType,
            alpha.floatValue,
        )
    }

    override fun addTextStyle(
        color: Int?,
        colorId: Int?,
        fontSize: MaybeVariableFloat?,
        minFontSize: MaybeVariableFloat?,
        maxFontSize: MaybeVariableFloat?,
        fontStyle: Int?,
        fontWeight: MaybeVariableFloat?,
        fontFamily: String?,
        textAlign: Int?,
        overflow: Int?,
        maxLines: Int?,
        letterSpacing: MaybeVariableFloat?,
        lineHeightAdd: MaybeVariableFloat?,
        lineHeightMultiplier: MaybeVariableFloat?,
        lineBreakStrategy: Int?,
        hyphenationFrequency: Int?,
        justificationMode: Int?,
        underline: Boolean?,
        strikethrough: Boolean?,
        fontAxisNames: Array<String>?,
        @Suppress("PrimitiveInCollection") fontAxisValues: List<MaybeVariableFloat>?,
        autosize: Boolean?,
        parentId: Int,
    ): Int {
        val rawAxisValues = fontAxisValues?.let { list ->
            FloatArray(list.size) { list[it].floatValue }
        }
        return delegate.addTextStyle(
            color,
            colorId,
            fontSize?.floatValue,
            minFontSize?.floatValue,
            maxFontSize?.floatValue,
            fontStyle,
            fontWeight?.floatValue,
            fontFamily,
            textAlign,
            overflow,
            maxLines,
            letterSpacing?.floatValue,
            lineHeightAdd?.floatValue,
            lineHeightMultiplier?.floatValue,
            lineBreakStrategy,
            hyphenationFrequency,
            justificationMode,
            underline,
            strikethrough,
            fontAxisNames,
            rawAxisValues,
            autosize,
            parentId,
        )
    }

    override fun textComponent(
        textId: RemoteStringId,
        styleId: Int,
        flags: Int,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        delegate.startTextComponent(
            buildRecordingModifier(componentId, modifiers = modifiers),
            textId.id,
            styleId,
            flags,
        )
        delegate.endTextComponent()
    }

    override fun textComponent(
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
        @Suppress("PrimitiveInCollection") fontAxisValues: List<MaybeVariableFloat>?,
        autosize: Boolean,
        flags: Int,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        val rawAxisValues = fontAxisValues?.let { list ->
            FloatArray(list.size) { list[it].floatValue }
        }
        delegate.startTextComponent(
            buildRecordingModifier(componentId, modifiers = modifiers),
            textId.id,
            -1,
            color,
            colorId,
            fontSize.floatValue,
            minFontSize?.floatValue ?: -1f,
            maxFontSize?.floatValue ?: -1f,
            fontStyle,
            fontWeight.floatValue,
            fontFamily,
            textAlign,
            overflow,
            maxLines,
            letterSpacing.floatValue,
            lineHeightAdd.floatValue,
            lineHeightMultiplier.floatValue,
            lineBreakStrategy,
            hyphenationFrequency,
            justificationMode,
            underline,
            strikethrough,
            fontAxisNames,
            rawAxisValues,
            autosize,
            flags,
        )
        delegate.endTextComponent()
    }

    override fun startCustom(
        config: String,
        properties: List<RemoteCustomProperty>,
        componentId: Int,
        modifiers: () -> Unit,
    ) {
        val coreProps = ArrayList<CustomProperty>(properties.size)
        for (i in properties.indices) {
            val p = properties[i]
            val isFloat =
                p.type == CustomProperty.FLOAT_PROP || p.type == CustomProperty.FLOAT_RETURN
            coreProps.add(
                if (isFloat) {
                    CustomProperty(p.id, p.type, p.floatValue.floatValue)
                } else {
                    CustomProperty(p.id, p.type, p.intValue)
                }
            )
        }
        val resolvedId = if (componentId == -1) delegate.nextId() else componentId
        delegate.startCustom(
            buildRecordingModifier(resolvedId, modifiers = modifiers),
            config,
            coreProps,
        )
    }

    override fun endCustom() {
        delegate.endCustom()
    }

    // ---- 10. Actions ----

    override fun addHostAction(actionId: Int) {
        HostActionOperation.apply(delegate.buffer.buffer, actionId)
    }

    override fun addHostActionMetadata(actionId: Int, metadataId: RemoteStringId) {
        HostActionMetadataOperation.apply(delegate.buffer.buffer, actionId, metadataId.id)
    }

    override fun addHostNamedAction(nameId: RemoteStringId, type: Int, valueId: Int) {
        HostNamedActionOperation.apply(delegate.buffer.buffer, nameId.id, type, valueId)
    }

    override fun addValueIntegerChangeAction(targetValueId: RemoteIntId, value: Int) {
        delegate.addValueIntegerChangeActionOperation(targetValueId.id, value)
    }

    override fun addValueIntegerExpressionChangeAction(
        targetValueId: RemoteIntId,
        srcValueId: RemoteIntId,
    ) {
        delegate.addValueIntegerExpressionChangeActionOperation(
            targetValueId.id.toLong(),
            srcValueId.id.toLong(),
        )
    }

    override fun addValueFloatChangeAction(
        targetValueId: RemoteFloatId,
        value: MaybeVariableFloat,
    ) {
        delegate.addValueFloatChangeActionOperation(targetValueId.id, value.floatValue)
    }

    override fun addValueFloatExpressionChangeAction(
        targetValueId: RemoteFloatId,
        srcValueId: RemoteFloatId,
    ) {
        delegate.addValueFloatExpressionChangeActionOperation(targetValueId.id, srcValueId.id)
    }

    override fun addValueStringChangeAction(
        targetTextId: RemoteStringId,
        srcTextId: RemoteStringId,
    ) {
        delegate.addValueStringChangeActionOperation(targetTextId.id, srcTextId.id)
    }

    // ---- Internal RecordingModifier Bridge ----

    internal fun writeRecordingModifier(recordingModifier: RecordingModifier) {
        val list = recordingModifier.list
        for (i in 0 until list.size) {
            list[i].write(delegate)
        }
    }

    private fun buildRecordingModifier(
        componentId: Int,
        spacedBy: MaybeVariableFloat? = null,
        modifiers: () -> Unit,
    ): RecordingModifier {
        val rm = RecordingModifier()
        if (componentId != -1) {
            rm.componentId(componentId)
        }
        if (spacedBy != null && spacedBy.floatValue != 0f) {
            rm.spacedBy(spacedBy.floatValue)
        }
        rm.then { modifiers() }
        return rm
    }
}

internal class PaintBundleWriter(private val bundle: PaintBundle) : RemotePaintWriter {
    var hasChanges: Boolean = false
        private set

    override fun setColor(color: Int) {
        bundle.setColor(color)
        hasChanges = true
    }

    override fun setColorId(colorId: RemoteColorId) {
        bundle.setColorId(colorId.id)
        hasChanges = true
    }

    override fun setStrokeWidth(width: MaybeVariableFloat) {
        bundle.setStrokeWidth(width.floatValue)
        hasChanges = true
    }

    override fun setStrokeCap(cap: Int) {
        bundle.setStrokeCap(cap)
        hasChanges = true
    }

    override fun setStrokeJoin(join: Int) {
        bundle.setStrokeJoin(join)
        hasChanges = true
    }

    override fun setStrokeMiter(miter: MaybeVariableFloat) {
        bundle.setStrokeMiter(miter.floatValue)
        hasChanges = true
    }

    override fun setStyle(style: Int) {
        bundle.setStyle(style)
        hasChanges = true
    }

    override fun setAntiAlias(antiAlias: Boolean) {
        bundle.setAntiAlias(antiAlias)
        hasChanges = true
    }

    override fun setFilterBitmap(filter: Boolean) {
        bundle.setFilterBitmap(filter)
        hasChanges = true
    }

    override fun setBlendMode(blendMode: Int) {
        bundle.setBlendMode(blendMode)
        hasChanges = true
    }

    override fun setTextSize(size: MaybeVariableFloat) {
        bundle.setTextSize(size.floatValue)
        hasChanges = true
    }

    override fun setTextStyle(typefaceId: Int, weight: Int, isItalic: Boolean) {
        bundle.setTextStyle(typefaceId, weight, isItalic)
        hasChanges = true
    }

    override fun setTextAxis(axisTagIds: IntArray, axisValues: List<MaybeVariableFloat>) {
        val values = FloatArray(axisValues.size) { axisValues[it].floatValue }
        bundle.setTextAxis(axisTagIds, values)
        hasChanges = true
    }

    override fun setColorFilter(color: Int, blendMode: Int) {
        bundle.setColorFilter(color, blendMode)
        hasChanges = true
    }

    override fun setColorFilterId(colorId: RemoteColorId, blendMode: Int) {
        bundle.setColorFilterId(colorId.id, blendMode)
        hasChanges = true
    }

    override fun clearColorFilter() {
        bundle.clearColorFilter()
        hasChanges = true
    }

    override fun setShader(shaderId: Int) {
        bundle.setShader(shaderId)
        hasChanges = true
    }

    override fun setShaderMatrix(matrixId: MaybeVariableFloat) {
        bundle.setShaderMatrix(matrixId.floatValue)
        hasChanges = true
    }

    override fun setLinearGradient(
        colors: IntArray,
        colorIdMask: Int,
        @Suppress("PrimitiveInCollection") stops: List<MaybeVariableFloat>?,
        startX: MaybeVariableFloat,
        startY: MaybeVariableFloat,
        endX: MaybeVariableFloat,
        endY: MaybeVariableFloat,
        tileMode: Int,
    ) {
        val stopsArray = stops?.let { s -> FloatArray(s.size) { s[it].floatValue } }
        bundle.setLinearGradient(
            colors,
            colorIdMask,
            stopsArray,
            startX.floatValue,
            startY.floatValue,
            endX.floatValue,
            endY.floatValue,
            tileMode,
        )
        hasChanges = true
    }

    override fun setRadialGradient(
        colors: IntArray,
        colorIdMask: Int,
        @Suppress("PrimitiveInCollection") stops: List<MaybeVariableFloat>?,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
        tileMode: Int,
    ) {
        val stopsArray = stops?.let { s -> FloatArray(s.size) { s[it].floatValue } }
        bundle.setRadialGradient(
            colors,
            colorIdMask,
            stopsArray,
            centerX.floatValue,
            centerY.floatValue,
            radius.floatValue,
            tileMode,
        )
        hasChanges = true
    }

    override fun setSweepGradient(
        colors: IntArray,
        colorIdMask: Int,
        @Suppress("PrimitiveInCollection") stops: List<MaybeVariableFloat>?,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
    ) {
        val stopsArray = stops?.let { s -> FloatArray(s.size) { s[it].floatValue } }
        bundle.setSweepGradient(
            colors,
            colorIdMask,
            stopsArray,
            centerX.floatValue,
            centerY.floatValue,
        )
        hasChanges = true
    }

    override fun setTextureShader(
        bitmapId: RemoteBitmapId,
        tileModeX: Int,
        tileModeY: Int,
        filterMode: Int,
        maxAnisotropy: Int,
    ) {
        bundle.setTextureShader(
            bitmapId.id,
            tileModeX.toShort(),
            tileModeY.toShort(),
            filterMode.toShort(),
            maxAnisotropy.toShort(),
        )
        hasChanges = true
    }
}

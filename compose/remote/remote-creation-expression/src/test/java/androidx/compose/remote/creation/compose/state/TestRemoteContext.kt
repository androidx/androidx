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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.TouchListener
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.types.LongConstant
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.document.MaybeVariableFloat
import androidx.compose.remote.document.NanEncoding
import androidx.compose.remote.document.RemoteArrayId
import androidx.compose.remote.document.RemoteBitmapId
import androidx.compose.remote.document.RemoteColorId
import androidx.compose.remote.document.RemoteDocumentWriter
import androidx.compose.remote.document.RemoteFloatId
import androidx.compose.remote.document.RemoteIntId
import androidx.compose.remote.document.RemoteLongId
import androidx.compose.remote.document.RemoteStringId
import java.lang.reflect.Proxy

private val UNSUPPORTED_WRITER_PROXY: RemoteDocumentWriter =
    Proxy.newProxyInstance(
        RemoteDocumentWriter::class.java.classLoader,
        arrayOf(RemoteDocumentWriter::class.java),
    ) { _, method, _ ->
        throw UnsupportedOperationException(
            "Unsupported in TestRemoteDocumentWriter: ${method.name}"
        )
    } as RemoteDocumentWriter

internal class TestRemoteDocumentWriter(val delegate: RemoteComposeWriter) :
    RemoteDocumentWriter by UNSUPPORTED_WRITER_PROXY {

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
        val buf = delegate.buffer
        if (componentId == -1 || buf.lastComponentId == componentId) {
            buf.addComponentValue(valueId.id, type)
        } else {
            ComponentValue.apply(buf.buffer, type, componentId, valueId.id)
        }
    }

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

    override fun addColorConstant(color: Int): RemoteColorId =
        RemoteColorId(delegate.addColor(color))

    override fun addNamedColor(name: String, initialColor: Int): RemoteColorId =
        RemoteColorId(delegate.addNamedColor(name, initialColor))

    override fun addThemedColor(
        groupName: String,
        lightId: Short,
        darkId: Short,
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

    override fun addText(text: String): RemoteStringId = RemoteStringId(delegate.addText(text))

    override fun addMutableText(initialValue: String): RemoteStringId =
        RemoteStringId(delegate.addText(initialValue))

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

    override fun translate(dx: MaybeVariableFloat, dy: MaybeVariableFloat) {
        delegate.buffer.addMatrixTranslate(dx.floatValue, dy.floatValue)
    }

    override fun conditionalOperations(
        type: Int,
        a: MaybeVariableFloat,
        b: MaybeVariableFloat,
        content: () -> Unit,
    ) {
        delegate.conditionalOperations(type.toByte(), a.floatValue, b.floatValue, content)
    }
}

internal val RemoteDocumentWriter.buffer: RemoteComposeBuffer
    get() = (this as TestRemoteDocumentWriter).delegate.buffer

internal fun testCreationState(
    width: Int = 1,
    height: Int = 1,
    profiles: Int = 0,
    remoteDensity: RemoteDensity = RemoteDensity(1f.rf, 1f.rf),
    document: RemoteDocumentWriter =
        TestRemoteDocumentWriter(
            if (profiles == 0) {
                RemoteComposeWriter(width, height, "", RcPlatformServices.None)
            } else {
                RemoteComposeWriter(
                    width,
                    height,
                    "",
                    CoreDocument.DOCUMENT_API_LEVEL,
                    profiles,
                    RcPlatformServices.None,
                )
            }
        ),
): RemoteComposeCreationState =
    RemoteComposeCreationState().apply {
        this.document = document
        this.remoteDensity = remoteDensity
    }

class TestRemoteContext : RemoteContext() {
    private class VarName(val name: String, val id: Int, val type: Int)

    private val varNameHashMap = HashMap<String, ArrayList<VarName>>()

    init {
        mPaintContext = TestPaintContext()
    }

    fun loadDocument(
        creationState: RemoteComposeCreationState,
        runAfterInit: () -> Unit = {},
    ): CoreDocument =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(this@TestRemoteContext)
            runAfterInit()
            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(this@TestRemoteContext)
                }
                op.apply(this@TestRemoteContext)
            }
        }

    override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
        mRemoteComposeState.putPathData(instanceId, floatPath)
        mRemoteComposeState.putPathWinding(instanceId, winding)
    }

    override fun getPathData(instanceId: Int): FloatArray? =
        mRemoteComposeState.getPathData(instanceId)

    override fun clearVariables() {
        varNameHashMap.clear()
    }

    override fun loadVariableName(varName: String, varId: Int, varType: Int) {
        val list = varNameHashMap.getOrPut(varName) { ArrayList() }
        for (i in 0 until list.size) {
            if (list[i].id == varId) return
        }
        list.add(VarName(varName, varId, varType))
    }

    override fun setNamedStringOverride(stringName: String, value: String) {
        varNameHashMap[stringName]?.forEach { overrideText(it.id, value) }
    }

    override fun clearNamedStringOverride(stringName: String) {
        varNameHashMap[stringName]?.forEach { mRemoteComposeState.clearDataOverride(it.id) }
    }

    override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
        setNamedIntegerOverride(booleanName, if (value) 1 else 0)
    }

    override fun clearNamedBooleanOverride(booleanName: String) {
        clearNamedIntegerOverride(booleanName)
    }

    override fun setNamedIntegerOverride(integerName: String, value: Int) {
        varNameHashMap[integerName]?.forEach { overrideInteger(it.id, value) }
    }

    override fun clearNamedIntegerOverride(integerName: String) {
        varNameHashMap[integerName]?.forEach { mRemoteComposeState.clearIntegerOverride(it.id) }
    }

    override fun setNamedFloatOverride(floatName: String, value: Float) {
        varNameHashMap[floatName]?.forEach { overrideFloat(it.id, value) }
    }

    override fun clearNamedFloatOverride(floatName: String) {
        varNameHashMap[floatName]?.forEach { mRemoteComposeState.clearFloatOverride(it.id) }
    }

    override fun setNamedLong(name: String, value: Long) {
        varNameHashMap[name]?.forEach {
            val longConstant = mRemoteComposeState.getObject(it.id) as LongConstant
            longConstant.setValue(value)
        }
    }

    override fun setNamedDataOverride(dataName: String, value: Any) {
        varNameHashMap[dataName]?.forEach { mRemoteComposeState.overrideData(it.id, value) }
    }

    override fun clearNamedDataOverride(dataName: String) {
        varNameHashMap[dataName]?.forEach { mRemoteComposeState.clearDataOverride(it.id) }
    }

    override fun setNamedColorOverride(colorName: String, color: Int) {
        varNameHashMap[colorName]?.forEach { mRemoteComposeState.overrideColor(it.id, color) }
    }

    override fun addCollection(id: Int, collection: ArrayAccess) {
        mRemoteComposeState.addCollection(id, collection)
    }

    override fun putDataMap(id: Int, map: DataMap) {
        mRemoteComposeState.putDataMap(id, map)
    }

    override fun getDataMap(id: Int): DataMap? = mRemoteComposeState.getDataMap(id)

    override fun runAction(id: Int, metadata: String) {
        mDocument.performClick(this, id, metadata)
    }

    override fun runNamedAction(id: Int, value: Any?) {
        val text = getText(id)
        if (text != null) {
            mDocument.runNamedAction(text, value)
        }
    }

    override fun loadBitmap(
        imageId: Int,
        encoding: Short,
        type: Short,
        width: Int,
        height: Int,
        data: ByteArray,
    ) {}

    override fun loadText(id: Int, text: String) {
        if (!mRemoteComposeState.containsId(id)) {
            mRemoteComposeState.cacheData(id, text)
        } else {
            mRemoteComposeState.updateData(id, text)
        }
    }

    fun overrideText(id: Int, text: String) {
        mRemoteComposeState.overrideData(id, text)
    }

    override fun getText(id: Int): String? = mRemoteComposeState.getFromId(id) as? String

    override fun loadFloat(id: Int, value: Float) {
        mRemoteComposeState.updateFloat(id, value)
    }

    override fun overrideFloat(id: Int, value: Float) {
        mRemoteComposeState.overrideFloat(id, value)
    }

    override fun loadInteger(id: Int, value: Int) {
        mRemoteComposeState.updateInteger(id, value)
    }

    override fun markVariableDirty(id: Int) {
        mRemoteComposeState.markVariableDirty(id)
    }

    override fun overrideInteger(id: Int, value: Int) {
        mRemoteComposeState.overrideInteger(id, value)
    }

    override fun overrideText(id: Int, valueId: Int) {
        val text = getText(valueId)
        if (text != null) {
            overrideText(id, text)
        }
    }

    override fun loadColor(id: Int, color: Int) {
        mRemoteComposeState.updateColor(id, color)
    }

    override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
        mRemoteComposeState.cacheData(id, animatedFloat)
    }

    override fun loadShader(id: Int, value: ShaderData) {
        mRemoteComposeState.cacheData(id, value)
    }

    override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id)

    override fun putObject(id: Int, value: Any) {
        mRemoteComposeState.updateObject(id, value)
    }

    override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)

    override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)

    override fun getLong(id: Int): Long = (mRemoteComposeState.getObject(id) as LongConstant).value

    override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)

    override fun listensTo(id: Int, variableSupport: VariableSupport) {
        mRemoteComposeState.listenToVar(id, variableSupport)
    }

    override fun getListeners(id: Int): ArrayList<VariableSupport>? =
        mRemoteComposeState.getListeners(id)

    override fun updateOps(): Int = mRemoteComposeState.getOpsToUpdate(this, currentTime)

    override fun getShader(id: Int): ShaderData? = mRemoteComposeState.getFromId(id) as? ShaderData

    override fun addTouchListener(touchExpression: TouchListener) {
        mDocument.addTouchListener(touchExpression)
    }

    override fun addClickArea(
        id: Int,
        contentDescriptionId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        metadataId: Int,
    ) {}

    override fun hapticEffect(type: Int) {}

    private inner class TestPaintContext : PaintContext(this) {
        override fun drawBitmap(
            imageId: Int,
            srcLeft: Int,
            srcTop: Int,
            srcRight: Int,
            srcBottom: Int,
            dstLeft: Int,
            dstTop: Int,
            dstRight: Int,
            dstBottom: Int,
            cdId: Int,
        ) {}

        override fun scale(scaleX: Float, scaleY: Float) {}

        override fun translate(translateX: Float, translateY: Float) {}

        override fun drawArc(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}

        override fun drawSector(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}

        override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {}

        override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {}

        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {}

        override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun drawPath(id: Int, start: Float, end: Float) {}

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun savePaint() {}

        override fun restorePaint() {}

        override fun replacePaint(paintBundle: PaintBundle) {}

        override fun drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            radiusX: Float,
            radiusY: Float,
        ) {}

        override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {}

        override fun getTextBounds(
            textId: Int,
            start: Int,
            end: Int,
            flags: Int,
            bounds: FloatArray,
        ) {}

        override fun layoutComplexText(
            textId: Int,
            start: Int,
            end: Int,
            alignment: Int,
            overflow: Int,
            maxLines: Int,
            maxWidth: Float,
            maxHeight: Float,
            letterSpacing: Float,
            lineHeightAdd: Float,
            lineHeightMultiplier: Float,
            lineBreakStrategy: Int,
            hyphenationFrequency: Int,
            justificationMode: Int,
            useUnderline: Boolean,
            strikethrough: Boolean,
            flags: Int,
        ): RcPlatformServices.ComputedTextLayout? = null

        override fun drawTextRun(
            textId: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            rtl: Boolean,
        ) {}

        override fun drawComplexText(computedTextLayout: RcPlatformServices.ComputedTextLayout?) {}

        override fun drawTweenPath(
            path1Id: Int,
            path2Id: Int,
            tween: Float,
            start: Float,
            end: Float,
        ) {}

        override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}

        override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}

        override fun applyPaint(mPaintData: PaintBundle) {}

        override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}

        override fun matrixTranslate(translateX: Float, translateY: Float) {}

        override fun matrixSkew(skewX: Float, skewY: Float) {}

        override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}

        override fun matrixSave() {}

        override fun matrixRestore() {}

        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun clipPath(pathId: Int, regionOp: Int) {}

        override fun roundedClipRect(
            width: Float,
            height: Float,
            topStart: Float,
            topEnd: Float,
            bottomStart: Float,
            bottomEnd: Float,
        ) {}

        override fun reset() {}

        override fun startGraphicsLayer(w: Int, h: Int) {}

        override fun setGraphicsLayer(attributes: HashMap<Int?, in Any>) {}

        override fun endGraphicsLayer() {}

        override fun getText(id: Int): String? = this@TestRemoteContext.getText(id)

        override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

        override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
    }
}

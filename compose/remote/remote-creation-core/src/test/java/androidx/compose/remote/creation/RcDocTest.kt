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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.BinaryData
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.MapRcDocVariables
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RcDocLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RcDocTest {

    private fun createWriter(width: Int = 400, height: Int = 400): RemoteComposeWriter {
        return RemoteComposeWriter(
            RcPlatformServices.None,
            CoreDocument.DOCUMENT_API_LEVEL,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
            RemoteComposeWriter.hTag(
                Header.DOC_PROFILES,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            ),
        )
    }

    private fun decodeDocument(writer: RemoteComposeWriter): CoreDocument {
        val bytes = writer.encodeToByteArray()
        val readBuffer = RemoteComposeBuffer()
        readBuffer.buffer.setSystemInfo(
            SystemInfo(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            )
        )
        readBuffer.buffer.reset(bytes.size)
        for (b in bytes) {
            readBuffer.buffer.writeByte(b.toInt())
        }
        readBuffer.buffer.setIndex(0)
        val doc = CoreDocument()
        doc.initFromBuffer(readBuffer)
        return doc
    }

    private class TestRemoteContext : RemoteContext() {
        val drawnRects = mutableListOf<FloatArray>()
        val drawnTexts = mutableListOf<String>()
        private val varNames = HashMap<String, MutableList<Pair<Int, Int>>>()

        init {
            mWidth = 400f
            mHeight = 400f
            mPaintContext = TestPaintContext(this)
        }

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
            mRemoteComposeState.putPathData(instanceId, floatPath)
            mRemoteComposeState.putPathWinding(instanceId, winding)
        }

        override fun getPathData(instanceId: Int): FloatArray? =
            mRemoteComposeState.getPathData(instanceId)

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {
            varNames.getOrPut(varName) { mutableListOf() }.add(varId to varType)
        }

        override fun loadColor(id: Int, color: Int) {
            mRemoteComposeState.updateColor(id, color)
        }

        override fun setNamedColorOverride(colorName: String, color: Int) {
            varNames[colorName]?.forEach { (id, _) -> mRemoteComposeState.overrideColor(id, color) }
        }

        override fun setNamedStringOverride(stringName: String, value: String) {
            varNames[stringName]?.forEach { (id, _) -> overrideText(id, value) }
        }

        override fun clearNamedStringOverride(stringName: String) {
            varNames[stringName]?.forEach { (id, _) -> mRemoteComposeState.clearDataOverride(id) }
        }

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {
            varNames[booleanName]?.forEach { (id, _) -> overrideInteger(id, if (value) 1 else 0) }
        }

        override fun clearNamedBooleanOverride(booleanName: String) {
            varNames[booleanName]?.forEach { (id, _) ->
                mRemoteComposeState.clearIntegerOverride(id)
            }
        }

        override fun setNamedIntegerOverride(integerName: String, value: Int) {
            varNames[integerName]?.forEach { (id, _) -> overrideInteger(id, value) }
        }

        override fun clearNamedIntegerOverride(integerName: String) {
            varNames[integerName]?.forEach { (id, _) ->
                mRemoteComposeState.clearIntegerOverride(id)
            }
        }

        override fun setNamedFloatOverride(floatName: String, value: Float) {
            varNames[floatName]?.forEach { (id, _) -> overrideFloat(id, value) }
        }

        override fun clearNamedFloatOverride(floatName: String) {
            varNames[floatName]?.forEach { (id, _) -> mRemoteComposeState.clearFloatOverride(id) }
        }

        override fun setNamedLong(name: String, value: Long) {}

        override fun setNamedDataOverride(dataName: String, value: Any) {
            varNames[dataName]?.forEach { (id, _) -> mRemoteComposeState.overrideData(id, value) }
        }

        override fun clearNamedDataOverride(dataName: String) {
            varNames[dataName]?.forEach { (id, _) -> mRemoteComposeState.clearDataOverride(id) }
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

        override fun putObject(id: Int, value: Any) {
            mRemoteComposeState.updateObject(id, value)
        }

        override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)

        override fun hapticEffect(type: Int) {}

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}

        override fun loadText(id: Int, text: String) {
            if (!mRemoteComposeState.containsId(id)) {
                mRemoteComposeState.cacheData(id, text)
            } else {
                mRemoteComposeState.updateData(id, text)
            }
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

        override fun overrideInteger(id: Int, value: Int) {
            mRemoteComposeState.overrideInteger(id, value)
        }

        override fun overrideText(id: Int, valueId: Int) {
            val t = getText(valueId)
            if (t != null) {
                overrideText(id, t)
            }
        }

        fun overrideText(id: Int, text: String) {
            mRemoteComposeState.overrideData(id, text)
        }

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {
            mRemoteComposeState.cacheData(id, animatedFloat)
        }

        override fun loadShader(id: Int, value: ShaderData) {
            mRemoteComposeState.cacheData(id, value)
        }

        override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id)

        override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)

        override fun getLong(id: Int): Long = 0L

        override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)

        override fun listensTo(id: Int, variableSupport: VariableSupport) {
            mRemoteComposeState.listenToVar(id, variableSupport)
        }

        override fun updateOps(): Int = mRemoteComposeState.getOpsToUpdate(this, currentTime)

        override fun getShader(id: Int): ShaderData? =
            mRemoteComposeState.getFromId(id) as? ShaderData

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {
            mDocument.addClickArea(
                id,
                getText(contentDescriptionId),
                left,
                top,
                right,
                bottom,
                getText(metadataId),
            )
        }
    }

    private class TestPaintContext(private val rootContext: TestRemoteContext) :
        PaintContext(rootContext) {
        private var tx = 0f
        private var ty = 0f
        private val stack = ArrayDeque<Pair<Float, Float>>()

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

        override fun translate(translateX: Float, translateY: Float) {
            tx += translateX
            ty += translateY
        }

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

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
            rootContext.drawnRects.add(floatArrayOf(left + tx, top + ty, right + tx, bottom + ty))
        }

        override fun savePaint() {}

        override fun restorePaint() {}

        override fun replacePaint(paint: PaintBundle) {}

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
        ) {
            val text = mContext.getText(textId) ?: ""
            bounds[0] = 0f
            bounds[1] = 0f
            bounds[2] = text.length * 10f
            bounds[3] = 20f
        }

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

        override fun drawComplexText(computedTextLayout: RcPlatformServices.ComputedTextLayout?) {}

        override fun drawTextRun(
            textId: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            rtl: Boolean,
        ) {
            val text = mContext.getText(textId)
            if (text != null) {
                rootContext.drawnTexts.add(text)
            }
        }

        override fun drawTweenPath(
            path1Id: Int,
            path2Id: Int,
            tween: Float,
            start: Float,
            stop: Float,
        ) {}

        override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}

        override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}

        override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

        override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}

        override fun applyPaint(paintData: PaintBundle) {}

        override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}

        override fun matrixTranslate(translateX: Float, translateY: Float) {
            tx += translateX
            ty += translateY
        }

        override fun matrixSkew(skewX: Float, skewY: Float) {}

        override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}

        override fun matrixSave() {
            stack.addLast(tx to ty)
        }

        override fun matrixRestore() {
            if (stack.isNotEmpty()) {
                val (prevX, prevY) = stack.removeLast()
                tx = prevX
                ty = prevY
            }
        }

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

        override fun setGraphicsLayer(attributes: HashMap<Int, Any>) {}

        override fun endGraphicsLayer() {}

        override fun getText(textId: Int): String? = mContext.getText(textId)

        override fun save() {
            stack.addLast(tx to ty)
        }

        override fun saveLayer(x: Float, y: Float, width: Float, height: Float) {
            stack.addLast(tx to ty)
        }

        override fun restore() {
            if (stack.isNotEmpty()) {
                val (prevX, prevY) = stack.removeLast()
                tx = prevX
                ty = prevY
            }
        }
    }

    @Test
    fun rcDoc_loadsMeasuresAndPaintsEmbeddedDocument() {
        // 1. Build child document (intrinsic size 160x80) that draws a rect and text
        val childWriter = createWriter(width = 160, height = 80)
        childWriter.root {
            childWriter.canvas(RecordingModifier().fillMaxSize()) {
                val w = childWriter.addComponentWidthValue()
                val h = childWriter.addComponentHeightValue()
                childWriter.drawRect(0f, 0f, w, h)
                childWriter.drawTextRun("ChildHello", 0, 10, 0, 10, 10f, 20f, false)
            }
        }

        // 2. Build host document with an rcDoc at offset (30, 50)
        val hostWriter = createWriter(width = 400, height = 400)
        var docId = -1
        hostWriter.root {
            hostWriter.box(
                RecordingModifier().padding(30, 50, 0, 0),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {
                docId = hostWriter.rcDoc("widget://card")
            }
        }

        val hostDoc = decodeDocument(hostWriter)
        val rcDocComponent = hostDoc.getComponent(docId)
        assertThat(rcDocComponent).isInstanceOf(RcDocLayout::class.java)

        val context = TestRemoteContext()
        context.setRcResolver { url ->
            if (url == "widget://card") childWriter.encodeToByteArray() else null
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        // Wrap-content should adopt child document's intrinsic 160x80 size
        assertThat(rcDocComponent!!.width).isEqualTo(160f)
        assertThat(rcDocComponent.height).isEqualTo(80f)
        assertThat(context.drawnTexts).contains("ChildHello")
        assertThat(context.drawnRects).isNotEmpty()
        val rect = context.drawnRects.last()
        assertThat(rect[0]).isEqualTo(30f)
        assertThat(rect[1]).isEqualTo(50f)
        assertThat(rect[2]).isEqualTo(190f)
        assertThat(rect[3]).isEqualTo(130f)
    }

    @Test
    fun mapRcDocVariables_forwardReverseAndBidirectional() {
        // Build child document with named variables:
        // - "inTitle" (STRING, forward from host)
        // - "inScale" (FLOAT, forward from host)
        // - "outStatus" (STRING, reverse to host)
        // - "biCounter" (FLOAT, bidirectional with host; incremented on click)
        val childWriter = createWriter(width = 200, height = 100)
        val childTitleId = childWriter.addNamedString("inTitle", "defaultTitle")
        val childScaleFloat = childWriter.addNamedFloat("inScale", 1f)
        val childOutStatusId = childWriter.addNamedString("outStatus", "childReady")
        val childBiCounter = childWriter.addNamedFloat("biCounter", 10f)

        childWriter.root {
            childWriter.box(
                RecordingModifier()
                    .size(200, 100)
                    .onClick(ValueFloatChange(Utils.idFromNan(childBiCounter), 42f)),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {
                childWriter.canvas(RecordingModifier().fillMaxSize()) {
                    childWriter.drawTextRun(childTitleId, 0, 5, 0, 5, 0f, childScaleFloat, false)
                    childWriter.drawTextRun(childOutStatusId, 0, 5, 0, 5, 0f, 20f, false)
                }
            }
        }

        // Build host document
        val hostWriter = createWriter(width = 400, height = 400)
        val hostTitleId = hostWriter.addNamedString("hostTitle", "ParentTitle")
        val hostScaleFloat = hostWriter.addNamedFloat("hostScale", 25f)
        val hostStatusId = hostWriter.addNamedString("hostStatus", "initial")
        val hostCounterFloat = hostWriter.addNamedFloat("hostCounter", 7f)

        var docId = -1
        hostWriter.root {
            docId = hostWriter.rcDoc("widget://vars", RecordingModifier().size(200, 100))
            hostWriter.mapRcDocVariables(
                docId,
                RemoteComposeWriter.MAP_FORWARD,
                hostTitleId,
                "inTitle",
            )
            hostWriter.mapRcDocVariables(
                docId,
                RemoteComposeWriter.MAP_FORWARD,
                hostScaleFloat,
                "inScale",
            )
            hostWriter.mapRcDocVariables(
                docId,
                RemoteComposeWriter.MAP_REVERSE,
                hostStatusId,
                "outStatus",
            )
            hostWriter.mapRcDocVariables(
                docId,
                RemoteComposeWriter.MAP_BIDIRECTIONAL,
                hostCounterFloat,
                "biCounter",
            )
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        context.setRcResolver { url ->
            if (url == "widget://vars") childWriter.encodeToByteArray() else null
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        val rcDoc = hostDoc.getComponent(docId) as RcDocLayout
        val childDoc = rcDoc.childDocument!!

        // 1. Forward mapping pushed "ParentTitle" and 25f into child
        assertThat(context.drawnTexts).contains("ParentTitle")
        assertThat(childDoc.remoteComposeState.getFloat(Utils.idFromNan(childScaleFloat)))
            .isEqualTo(25f)

        // 2. Reverse mapping pulled "childReady" from child into hostStatusId
        assertThat(context.getText(hostStatusId)).isEqualTo("childReady")

        // 3. Bidirectional mapping initially pushed hostCounter (7f) into child
        assertThat(childDoc.remoteComposeState.getFloat(Utils.idFromNan(childBiCounter)))
            .isEqualTo(7f)

        // 4. Updating host variable propagates to child on next paint
        context.setNamedStringOverride("hostTitle", "UpdatedFromHost")
        context.setNamedFloatOverride("hostCounter", 15f)
        context.drawnTexts.clear()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).contains("UpdatedFromHost")
        assertThat(childDoc.remoteComposeState.getFloat(Utils.idFromNan(childBiCounter)))
            .isEqualTo(15f)

        // 5. Clicking inside the child updates biCounter to 42f and syncs it back to hostCounter!
        val clicked = hostDoc.onClick(context, 50f, 50f)
        assertThat(clicked).isTrue()
        assertThat(context.getFloat(Utils.idFromNan(hostCounterFloat))).isEqualTo(42f)
    }

    @Test
    fun mapRcDocVariables_hostActionMapsKnownIdToStringVarAndForwardsUnknownIdToRealHost() {
        val mappedActionId = 1001
        val unmappedActionId = 2002

        // Child document has two clickable boxes:
        // - Top half (0..50) fires HostAction(1001, metadata="mapped_payload")
        // - Bottom half (50..100) fires HostAction(2002, metadata="unmapped_payload")
        val childWriter = createWriter(width = 200, height = 100)
        val mappedMetaId = childWriter.addText("mapped_payload")
        val unmappedMetaId = childWriter.addText("unmapped_payload")
        childWriter.root {
            childWriter.column(
                RecordingModifier().size(200, 100),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                childWriter.box(
                    RecordingModifier()
                        .size(200, 50)
                        .onClick(HostAction(mappedActionId, mappedMetaId))
                )
                childWriter.box(
                    RecordingModifier()
                        .size(200, 50)
                        .onClick(HostAction(unmappedActionId, unmappedMetaId))
                )
            }
        }

        // Host document embeds the child at offset (10, 20) and maps actionId 1001 ->
        // actionResultVar
        val hostWriter = createWriter(width = 400, height = 400)
        val actionResultVar = hostWriter.addNamedString("actionResult", "none")
        var docId = -1
        hostWriter.root {
            hostWriter.box(
                RecordingModifier().padding(10, 20, 0, 0),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {
                docId = hostWriter.rcDoc("widget://actions", RecordingModifier().size(200, 100))
                hostWriter.mapRcDocVariables(
                    docId,
                    MapRcDocVariables.MAP_HOST_ACTION,
                    actionResultVar,
                    mappedActionId,
                )
            }
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        context.setRcResolver { url ->
            if (url == "widget://actions") childWriter.encodeToByteArray() else null
        }

        val hostReceivedActions = mutableListOf<Pair<Int, String?>>()
        hostDoc.addIdActionListener { id, metadata -> hostReceivedActions.add(id to metadata) }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(context.getText(actionResultVar)).isEqualTo("none")

        // 1. Click top box in child (host coordinates: x=50, y=20 + 25 = 45) -> triggers
        // mappedActionId (1001)
        val handledMapped = hostDoc.onClick(context, 50f, 45f)
        assertThat(handledMapped).isTrue()
        // Mapped action ID sets the host String variable and does NOT go to real host handler
        assertThat(context.getText(actionResultVar)).isEqualTo("mapped_payload")
        assertThat(hostReceivedActions).isEmpty()

        // 2. Click bottom box in child (host coordinates: x=50, y=20 + 75 = 95) -> triggers
        // unmappedActionId (2002)
        val handledUnmapped = hostDoc.onClick(context, 50f, 95f)
        assertThat(handledUnmapped).isTrue()
        // Unmapped action ID goes to the real host hostAction handler!
        assertThat(hostReceivedActions).containsExactly(unmappedActionId to "unmapped_payload")
    }

    @Test
    fun rcDoc_dynamicUrlVariableReloadsDocument() {
        val childA = createWriter(width = 100, height = 50)
        childA.root {
            childA.canvas(RecordingModifier().fillMaxSize()) {
                childA.drawTextRun("DocA", 0, 4, 0, 4, 0f, 10f, false)
            }
        }
        val childB = createWriter(width = 100, height = 50)
        childB.root {
            childB.column(RecordingModifier().fillMaxSize(), ColumnLayout.START, ColumnLayout.TOP) {
                childB.box(RecordingModifier().size(100, 20), BoxLayout.START, BoxLayout.TOP) {
                    childB.canvas(RecordingModifier().fillMaxSize()) {
                        childB.drawTextRun("DocB", 0, 4, 0, 4, 0f, 10f, false)
                    }
                }
                childB.box(
                    RecordingModifier().fillMaxWidth().verticalWeight(1f),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    childB.canvas(RecordingModifier().fillMaxSize()) {
                        val w = childB.addComponentWidthValue()
                        val h = childB.addComponentHeightValue()
                        childB.drawRect(0f, 0f, w, h)
                    }
                }
            }
        }

        val hostWriter = createWriter(width = 400, height = 400)
        val urlVarId = hostWriter.addNamedString("docUrl", "widget://a")
        hostWriter.root {
            hostWriter.column(
                RecordingModifier().size(100, 100),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                hostWriter.box(
                    RecordingModifier()
                        .size(100, 50)
                        .onClick(ValueStringChange(urlVarId, "widget://b"))
                )
                hostWriter.rcDoc(urlVarId, RecordingModifier().size(100, 50))
            }
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        context.setRcResolver { url ->
            when (url) {
                "widget://a" -> childA.encodeToByteArray()
                "widget://b" -> childB.encodeToByteArray()
                else -> null
            }
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).containsExactly("DocA")

        // Clicking the host button switches the URL variable to "widget://b"; the new child is
        // fully laid out: its weighted box fills the remaining 100x30 below the 20px title.
        context.drawnTexts.clear()
        context.drawnRects.clear()
        assertThat(hostDoc.onClick(context, 50f, 25f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).containsExactly("DocB")
        assertThat(context.drawnRects).isNotEmpty()
        val rect = context.drawnRects.last()
        assertThat(rect[2] - rect[0]).isEqualTo(100f)
        assertThat(rect[3] - rect[1]).isEqualTo(30f)
    }

    @Test
    fun rcDoc_embedsFiveRawDocumentsInCompositePage() {
        val rawDir = java.io.File("../integration-tests/player-view-demos/src/main/res/raw")
        if (!rawDir.isDirectory) {
            return
        }
        val hostWriter = createWriter(width = 1000, height = 1400)
        hostWriter.root {
            hostWriter.column(
                RecordingModifier().fillMaxSize(),
                ColumnLayout.CENTER,
                ColumnLayout.TOP,
            ) {
                hostWriter.rcDoc("cube32.rc", RecordingModifier().size(400, 350))
                hostWriter.rcDoc("flappy.rc", RecordingModifier().size(400, 350))
                hostWriter.rcDoc("stop_notches_even.rc", RecordingModifier().size(300, 300))
                hostWriter.rcDoc("shader_calendar.rc", RecordingModifier().size(400, 350))
                hostWriter.rcDoc("clock.rc", RecordingModifier().size(350, 350))
            }
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        val loadedUrls = mutableListOf<String>()
        context.setRcResolver { url ->
            loadedUrls.add(url)
            val fileName = if (url == "cube32.rc") "cube3d.rc" else url
            val file = java.io.File(rawDir, fileName)
            if (file.isFile) file.readBytes() else null
        }

        hostDoc.initializeContext(context)
        hostDoc.checkShaders(context) { true }
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(loadedUrls)
            .containsExactly(
                "cube32.rc",
                "flappy.rc",
                "stop_notches_even.rc",
                "shader_calendar.rc",
                "clock.rc",
            )
            .inOrder()
    }

    @Test
    fun rcDoc_unresolvedAndLateRegisteredRcResolver() {
        val childWriter = createWriter(width = 120, height = 60)
        childWriter.root {
            childWriter.canvas(RecordingModifier().fillMaxSize()) {
                childWriter.drawTextRun("ResolvedLate", 0, 12, 0, 12, 0f, 10f, false)
            }
        }

        val hostWriter = createWriter(width = 300, height = 300)
        var docId = -1
        hostWriter.root {
            docId = hostWriter.rcDoc("host://late", RecordingModifier().size(120, 60))
        }

        val hostDoc = decodeDocument(hostWriter)
        val rcDoc = hostDoc.getComponent(docId) as RcDocLayout
        val context = TestRemoteContext()

        // 1. Without an RcResolver, painting succeeds cleanly with no child document loaded
        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(rcDoc.childDocument).isNull()
        assertThat(context.drawnTexts).isEmpty()

        // 2. Registering an RcResolver that returns null does not spam resolve() on every paint
        var nullResolveCalls = 0
        context.setRcResolver { _ ->
            nullResolveCalls++
            null
        }
        hostDoc.invalidateMeasure()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(nullResolveCalls).isEqualTo(1)
        assertThat(rcDoc.childDocument).isNull()

        // 3. Swapping in a host RcResolver that returns valid bytes loads and paints the child
        context.setRcResolver { url ->
            if (url == "host://late") childWriter.encodeToByteArray() else null
        }
        hostDoc.invalidateMeasure()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(rcDoc.childDocument).isNotNull()
        assertThat(context.drawnTexts).containsExactly("ResolvedLate")
    }

    @Test
    fun rcDoc_nestedEmbeddedDocumentsDelegateToHostRcResolver() {
        val leafWriter = createWriter(width = 100, height = 40)
        leafWriter.root {
            leafWriter.canvas(RecordingModifier().fillMaxSize()) {
                leafWriter.drawTextRun("LeafDoc", 0, 7, 0, 7, 0f, 10f, false)
            }
        }

        val middleWriter = createWriter(width = 150, height = 80)
        middleWriter.root {
            middleWriter.box(RecordingModifier().size(150, 80), BoxLayout.START, BoxLayout.TOP) {
                middleWriter.rcDoc("nested://leaf", RecordingModifier().size(100, 40))
            }
        }

        val hostWriter = createWriter(width = 300, height = 300)
        hostWriter.root {
            hostWriter.rcDoc("nested://middle", RecordingModifier().size(150, 80))
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        val resolvedUrls = mutableListOf<String>()
        context.setRcResolver { url ->
            resolvedUrls.add(url)
            when (url) {
                "nested://middle" -> middleWriter.encodeToByteArray()
                "nested://leaf" -> leafWriter.encodeToByteArray()
                else -> null
            }
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(resolvedUrls).containsExactly("nested://middle", "nested://leaf").inOrder()
        assertThat(context.drawnTexts).containsExactly("LeafDoc")
    }

    @Test
    fun rcDoc_embeddedDocumentById_loadsWithoutResolver() {
        // Child: draws its full size as a rect and a marker text
        val childWriter = createWriter(width = 120, height = 60)
        val childTitle = childWriter.addNamedString("title", "default")
        childWriter.root {
            childWriter.canvas(RecordingModifier().fillMaxSize()) {
                val w = childWriter.addComponentWidthValue()
                val h = childWriter.addComponentHeightValue()
                childWriter.drawRect(0f, 0f, w, h)
                childWriter.drawTextRun(childTitle, 0, 5, 0, 5, 0f, 10f, false)
            }
        }
        val childBytes = childWriter.encodeToByteArray()

        // Parent carries the child bytes in a BinaryData blob and references it by id
        val hostWriter = createWriter(width = 400, height = 400)
        val hostTitle = hostWriter.addNamedString("hostTitle", "Inline")
        val embeddedId = hostWriter.addEmbeddedDocument(childBytes)
        var docId = -1
        hostWriter.root {
            hostWriter.box(
                RecordingModifier().padding(10, 20, 0, 0),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {
                docId = hostWriter.rcDoc(embeddedId, RecordingModifier().size(120, 60))
                hostWriter.mapRcDocVariables(
                    docId,
                    RemoteComposeWriter.MAP_FORWARD,
                    hostTitle,
                    "title",
                )
            }
        }

        // The parent byte array is self-contained: decode it and play with NO RcResolver
        val hostDoc = decodeDocument(hostWriter)
        assertThat(hostDoc.operations.filterIsInstance<BinaryData>().map { it.id })
            .containsExactly(embeddedId)
        val context = TestRemoteContext()
        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        val rcDoc = hostDoc.getComponent(docId) as RcDocLayout
        assertThat(rcDoc.childDocument).isNotNull()
        assertThat(context.drawnTexts).containsExactly("Inline")
        val rect = context.drawnRects.last()
        assertThat(rect).usingTolerance(0.01).containsExactly(10f, 20f, 130f, 80f).inOrder()
    }

    @Test
    fun rcDoc_embeddedUrlScheme_switchesBetweenEmbeddedAndResolved() {
        fun child(label: String): RemoteComposeWriter {
            val w = createWriter(width = 100, height = 50)
            w.root {
                w.canvas(RecordingModifier().fillMaxSize()) {
                    w.drawTextRun(label, 0, label.length, 0, label.length, 0f, 10f, false)
                }
            }
            return w
        }

        val hostWriter = createWriter(width = 400, height = 400)
        val embeddedA = hostWriter.addEmbeddedDocument(child("EmbeddedA").encodeToByteArray())
        val embeddedB = hostWriter.addEmbeddedDocument(child("EmbeddedB").encodeToByteArray())
        val urlVar =
            hostWriter.addNamedString("docUrl", RcDocLayout.EMBEDDED_URL_PREFIX + embeddedA)
        hostWriter.root {
            hostWriter.rcDoc(urlVar, RecordingModifier().size(100, 50))
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        val resolved = mutableListOf<String>()
        context.setRcResolver { url ->
            resolved.add(url)
            if (url == "host://c") child("ResolvedC").encodeToByteArray() else null
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).containsExactly("EmbeddedA")
        assertThat(resolved).isEmpty() // embedded urls never reach the host resolver

        context.drawnTexts.clear()
        context.setNamedStringOverride("docUrl", RcDocLayout.EMBEDDED_URL_PREFIX + embeddedB)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).containsExactly("EmbeddedB")
        assertThat(resolved).isEmpty()

        context.drawnTexts.clear()
        context.setNamedStringOverride("docUrl", "host://c")
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).containsExactly("ResolvedC")
        assertThat(resolved).containsExactly("host://c")

        // An embedded url that doesn't match a blob falls through to the resolver, which can't
        // serve it: the rcDoc renders nothing rather than crashing.
        context.drawnTexts.clear()
        context.setNamedStringOverride("docUrl", RcDocLayout.EMBEDDED_URL_PREFIX + "9999")
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).isEmpty()
    }

    @Test
    fun rcDoc_embeddedDocumentsNest_singleByteArray() {
        // leaf -> middle (embeds leaf) -> host (embeds middle): three documents, one byte array
        val leaf = createWriter(width = 100, height = 40)
        leaf.root {
            leaf.canvas(RecordingModifier().fillMaxSize()) {
                leaf.drawTextRun("Leaf", 0, 4, 0, 4, 0f, 10f, false)
            }
        }
        val middle = createWriter(width = 150, height = 80)
        val leafId = middle.addEmbeddedDocument(leaf.encodeToByteArray())
        middle.root {
            middle.box(RecordingModifier().size(150, 80), BoxLayout.START, BoxLayout.TOP) {
                middle.rcDoc(leafId, RecordingModifier().size(100, 40))
            }
        }
        val host = createWriter(width = 300, height = 300)
        val middleId = host.addEmbeddedDocument(middle.encodeToByteArray())
        host.root { host.rcDoc(middleId, RecordingModifier().size(150, 80)) }

        val hostDoc = decodeDocument(host)
        val context = TestRemoteContext() // no resolver at all
        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).containsExactly("Leaf")
    }

    private class ScrollFixture(
        val doc: CoreDocument,
        val context: TestRemoteContext,
        val posId: Int,
        val scroller: LayoutComponent,
        val scroll: ScrollModifierOperation,
    )

    /**
     * Builds a 600x600 document with a 400px scroller (column when [vertical], row otherwise) of
     * ten 200px boxes bound to the named float "pos", and a 200px button below it that runs
     * [action] on click. The button is at y 400..600 across the full width.
     */
    private fun scrollDocument(
        vertical: Boolean,
        action: (positionId: Int, stepId: Int) -> androidx.compose.remote.creation.actions.Action,
    ): ScrollFixture {
        val w = createWriter(width = 600, height = 600)
        val position = w.addNamedFloat("pos", 0f)
        val step = w.floatExpression(position, 200f, AnimatedFloatExpression.ADD)
        w.root {
            w.column(RecordingModifier().fillMaxSize(), ColumnLayout.START, ColumnLayout.TOP) {
                if (vertical) {
                    w.column(
                        RecordingModifier().fillMaxWidth().height(400).verticalScroll(position),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        for (i in 0 until 10) {
                            w.box(
                                RecordingModifier().size(200, 200),
                                BoxLayout.START,
                                BoxLayout.TOP,
                            )
                        }
                    }
                } else {
                    w.row(
                        RecordingModifier().fillMaxWidth().height(400).horizontalScroll(position),
                        RowLayout.START,
                        RowLayout.TOP,
                    ) {
                        for (i in 0 until 10) {
                            w.box(
                                RecordingModifier().size(200, 200),
                                BoxLayout.START,
                                BoxLayout.TOP,
                            )
                        }
                    }
                }
                w.box(
                    RecordingModifier()
                        .fillMaxWidth()
                        .height(200)
                        .onClick(action(Utils.idFromNan(position), Utils.idFromNan(step)))
                )
            }
        }
        val doc = decodeDocument(w)
        val context = TestRemoteContext()
        context.mWidth = 600f
        context.mHeight = 600f
        doc.initializeContext(context)
        doc.paint(context, Theme.UNSPECIFIED)
        doc.paint(context, Theme.UNSPECIFIED)
        val outer = doc.rootLayoutComponent!!.list.filterIsInstance<LayoutComponent>().single()
        val scroller = outer.childrenComponents[0] as LayoutComponent
        val scroll =
            scroller.componentModifiers.modifiersList
                .filterIsInstance<ScrollModifierOperation>()
                .single()
        return ScrollFixture(doc, context, Utils.idFromNan(position), scroller, scroll)
    }

    private fun ScrollFixture.offset(): Float =
        if (scroll.isVerticalScroll) scroller.scrollY else scroller.scrollX

    private fun checkScrollByActionKeepsPosition(vertical: Boolean) {
        // scrollBy-style action: position <- position + 200 (an override of the touch variable)
        val f =
            scrollDocument(vertical) { positionId, stepId ->
                ValueFloatExpressionChange(positionId, stepId)
            }
        assertThat(f.context.getFloat(f.posId)).isEqualTo(0f)
        assertThat(f.doc.onClick(f.context, 100f, 500f)).isTrue()
        // The TouchExpression behind the scroll adopts the value and keeps it, frame after frame.
        for (i in 0 until 5) {
            f.doc.paint(f.context, Theme.UNSPECIFIED)
            assertThat(f.context.getFloat(f.posId)).isEqualTo(200f)
            assertThat(f.offset()).isEqualTo(-200f)
        }
        assertThat(f.doc.onClick(f.context, 100f, 500f)).isTrue()
        f.doc.paint(f.context, Theme.UNSPECIFIED)
        assertThat(f.context.getFloat(f.posId)).isEqualTo(400f)
        assertThat(f.offset()).isEqualTo(-400f)
    }

    private fun checkScrollByOffset(vertical: Boolean) {
        val f = scrollDocument(vertical) { positionId, _ -> ValueFloatChange(positionId, 0f) }

        // Accessibility "scroll forward" by 300px: offsets are negative when scrolling forward.
        f.scroll.scrollByOffset(f.context, -300)
        assertThat(f.context.getFloat(f.posId)).isEqualTo(300f)
        for (i in 0 until 3) {
            f.doc.paint(f.context, Theme.UNSPECIFIED)
            assertThat(f.context.getFloat(f.posId)).isEqualTo(300f)
            assertThat(f.offset()).isEqualTo(-300f)
        }

        // Scroll back by 100px.
        f.scroll.scrollByOffset(f.context, 100)
        f.doc.paint(f.context, Theme.UNSPECIFIED)
        assertThat(f.context.getFloat(f.posId)).isEqualTo(200f)
        assertThat(f.offset()).isEqualTo(-200f)

        // Never past the start (paint() yields -0.0 for the offset at the start).
        f.scroll.scrollByOffset(f.context, 1000)
        f.doc.paint(f.context, Theme.UNSPECIFIED)
        assertThat(f.context.getFloat(f.posId)).isWithin(0f).of(0f)
        assertThat(f.offset()).isWithin(0f).of(0f)
    }

    private fun checkDragAfterScrollByAction(vertical: Boolean) {
        val f =
            scrollDocument(vertical) { positionId, stepId ->
                ValueFloatExpressionChange(positionId, stepId)
            }
        assertThat(f.doc.onClick(f.context, 100f, 500f)).isTrue()
        f.doc.paint(f.context, Theme.UNSPECIFIED)
        assertThat(f.context.getFloat(f.posId)).isEqualTo(200f)

        // Drag the scroller back by 150px along its axis: still draggable after the action.
        val (x0, y0) = 300f to 200f
        val (x1, y1) = if (vertical) 300f to 350f else 450f to 200f
        assertThat(f.doc.touchDown(f.context, x0, y0)).isTrue()
        assertThat(f.doc.touchDrag(f.context, x1, y1)).isTrue()
        f.doc.touchUp(f.context, x1, y1, 0f, 0f)
        f.doc.paint(f.context, Theme.UNSPECIFIED)
        assertThat(f.context.getFloat(f.posId)).isWithin(0.01f).of(50f)
        assertThat(f.offset()).isWithin(0.01f).of(-50f)
    }

    @Test
    fun verticalScroll_scrollByAction_keepsPositionAcrossPaints() =
        checkScrollByActionKeepsPosition(vertical = true)

    @Test
    fun horizontalScroll_scrollByAction_keepsPositionAcrossPaints() =
        checkScrollByActionKeepsPosition(vertical = false)

    @Test
    fun verticalScroll_scrollByOffset_updatesPositionAndSticks() =
        checkScrollByOffset(vertical = true)

    @Test
    fun horizontalScroll_scrollByOffset_updatesPositionAndSticks() =
        checkScrollByOffset(vertical = false)

    @Test
    fun verticalScroll_dragAfterScrollByAction() = checkDragAfterScrollByAction(vertical = true)

    @Test
    fun horizontalScroll_dragAfterScrollByAction() = checkDragAfterScrollByAction(vertical = false)

    @Test
    fun rcDoc_skeletonWatchLoadsAndPaints() {
        val file =
            java.io.File(
                "../integration-tests/player-watch-demos/src/main/res/raw/skeleton_watch.rc"
            )
        if (!file.isFile) {
            return
        }
        val doc =
            file.inputStream().use { stream ->
                val buf = RemoteComposeBuffer.fromInputStream(stream)
                CoreDocument().apply { initFromBuffer(buf) }
            }
        val context = TestRemoteContext()
        doc.initializeContext(context)
        doc.checkShaders(context) { true }
        assertThat(doc.width).isEqualTo(720)
        assertThat(doc.height).isEqualTo(720)
        doc.paint(context, Theme.UNSPECIFIED)
    }

    @Test
    fun rcDoc_parentModifiesChildAndGrandchildSlider() {
        // 1. Leaf child slider document ("child://slider", 200x80) with an addTouch slider (0..100)
        val childSliderWriter = createWriter(width = 200, height = 80)
        var childSliderVar = 0f
        childSliderWriter.root {
            childSliderWriter.canvas(RecordingModifier().fillMaxSize()) {
                val w = childSliderWriter.addComponentWidthValue()
                childSliderVar =
                    childSliderWriter.addTouch(
                        50f,
                        0f,
                        100f,
                        RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                        0f,
                        0,
                        null,
                        null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                        100f,
                        AnimatedFloatExpression.MUL,
                        w,
                        AnimatedFloatExpression.DIV,
                    )
                childSliderWriter.drawRect(0f, 0f, childSliderVar, 20f)
            }
        }
        val childSliderId = Utils.idFromNan(childSliderVar)
        childSliderWriter.setFloatName(childSliderId, "sliderValue")

        // 2. Intermediate nested container ("child://nested", 200x80) forwarding (100 - inLevel)
        // into a nested "child://slider" grandchild
        val nestedContainerWriter = createWriter(width = 200, height = 80)
        val inLevel = nestedContainerWriter.addNamedFloat("inLevel", 50f)
        val complementLevel =
            nestedContainerWriter.floatExpression(100f, inLevel, AnimatedFloatExpression.SUB)
        var grandchildDocId = -1
        nestedContainerWriter.root {
            grandchildDocId =
                nestedContainerWriter.rcDoc("child://slider", RecordingModifier().size(200, 80))
            nestedContainerWriter.mapRcDocVariables(
                grandchildDocId,
                RemoteComposeWriter.MAP_FORWARD,
                complementLevel,
                "sliderValue",
            )
        }

        // 3. Parent document with a master slider (y: 0..60), a 25% preset button (y: 60..100),
        // direct child slider (y: 100..180, MAP_BIDIRECTIONAL), and nested container (y: 180..260)
        val hostWriter = createWriter(width = 400, height = 400)
        var masterSlider = 0f
        var directDocId = -1
        var nestedDocId = -1
        hostWriter.root {
            hostWriter.column(
                RecordingModifier().size(200, 260),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                hostWriter.box(
                    RecordingModifier().size(200, 60),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    hostWriter.canvas(RecordingModifier().fillMaxSize()) {
                        val w = hostWriter.addComponentWidthValue()
                        masterSlider =
                            hostWriter.addTouch(
                                65f,
                                0f,
                                100f,
                                RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                100f,
                                AnimatedFloatExpression.MUL,
                                w,
                                AnimatedFloatExpression.DIV,
                            )
                    }
                }
                val masterId = Utils.idFromNan(masterSlider)
                hostWriter.box(
                    RecordingModifier().size(200, 40).onClick(ValueFloatChange(masterId, 25f))
                )
                directDocId = hostWriter.rcDoc("child://slider", RecordingModifier().size(200, 80))
                hostWriter.mapRcDocVariables(
                    directDocId,
                    RemoteComposeWriter.MAP_BIDIRECTIONAL,
                    masterSlider,
                    "sliderValue",
                )
                nestedDocId = hostWriter.rcDoc("child://nested", RecordingModifier().size(200, 80))
                hostWriter.mapRcDocVariables(
                    nestedDocId,
                    RemoteComposeWriter.MAP_FORWARD,
                    masterSlider,
                    "inLevel",
                )
            }
        }
        val masterId = Utils.idFromNan(masterSlider)

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        context.setRcResolver { url ->
            when (url) {
                "child://slider" -> childSliderWriter.encodeToByteArray()
                "child://nested" -> nestedContainerWriter.encodeToByteArray()
                else -> null
            }
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        val directRcDoc = hostDoc.getComponent(directDocId) as RcDocLayout
        val directChildDoc = directRcDoc.childDocument!!
        val nestedRcDoc = hostDoc.getComponent(nestedDocId) as RcDocLayout
        val grandchildRcDoc =
            nestedRcDoc.childDocument!!.getComponent(grandchildDocId) as RcDocLayout
        val grandchildDoc = grandchildRcDoc.childDocument!!

        // Initial sync: parent master (65) -> direct child (65) and 2-level grandchild (100 - 65 =
        // 35)
        assertThat(context.getFloat(masterId)).isWithin(0.01f).of(65f)
        assertThat(directChildDoc.remoteComposeState.getFloat(childSliderId))
            .isWithin(0.01f)
            .of(65f)
        assertThat(grandchildDoc.remoteComposeState.getFloat(childSliderId)).isWithin(0.01f).of(35f)

        // Drag direct child's slider (+40px over width 200 -> +20, so 65 -> 85)
        assertThat(hostDoc.touchDown(context, 130f, 140f)).isTrue()
        assertThat(hostDoc.touchDrag(context, 170f, 140f)).isTrue()
        hostDoc.touchUp(context, 170f, 140f, 0f, 0f)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(directChildDoc.remoteComposeState.getFloat(childSliderId))
            .isWithin(0.01f)
            .of(85f)
        assertThat(context.getFloat(masterId)).isWithin(0.01f).of(85f)
        assertThat(grandchildDoc.remoteComposeState.getFloat(childSliderId)).isWithin(0.01f).of(15f)

        // Click parent's 25% preset button -> modifies child's TouchExpression slider even after
        // touch!
        assertThat(hostDoc.onClick(context, 50f, 80f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(context.getFloat(masterId)).isWithin(0.01f).of(25f)
        assertThat(directChildDoc.remoteComposeState.getFloat(childSliderId))
            .isWithin(0.01f)
            .of(25f)
        assertThat(grandchildDoc.remoteComposeState.getFloat(childSliderId)).isWithin(0.01f).of(75f)

        // Drag parent's master slider (+80px over width 200 -> +40, so 25 -> 65)
        assertThat(hostDoc.touchDown(context, 50f, 30f)).isTrue()
        assertThat(hostDoc.touchDrag(context, 130f, 30f)).isTrue()
        hostDoc.touchUp(context, 130f, 30f, 0f, 0f)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(context.getFloat(masterId)).isWithin(0.01f).of(65f)
        assertThat(directChildDoc.remoteComposeState.getFloat(childSliderId))
            .isWithin(0.01f)
            .of(65f)
        assertThat(grandchildDoc.remoteComposeState.getFloat(childSliderId)).isWithin(0.01f).of(35f)
    }

    @Test
    fun rcDoc_childModifiesParentValuesAndMode() {
        val actionModeSelect = 2001

        // Child controller ("child://controller", 200x120):
        // - y: 0..60: speed slider ("outSpeed", initial 40)
        // - y: 60..90: boost +1 button ("outBoost", initial 0)
        // - y: 90..120: mode button firing HostAction(2001, "HYPER DRIVE")
        val childWriter = createWriter(width = 200, height = 120)
        val outBoost = childWriter.addNamedFloat("outBoost", 0f)
        val boostInc = childWriter.floatExpression(outBoost, 1f, AnimatedFloatExpression.ADD)
        val hyperModeId = childWriter.addText("HYPER DRIVE")
        var speedSlider = 0f
        childWriter.root {
            childWriter.column(
                RecordingModifier().size(200, 120),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                childWriter.box(
                    RecordingModifier().size(200, 60),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    childWriter.canvas(RecordingModifier().fillMaxSize()) {
                        val w = childWriter.addComponentWidthValue()
                        speedSlider =
                            childWriter.addTouch(
                                40f,
                                0f,
                                100f,
                                RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                100f,
                                AnimatedFloatExpression.MUL,
                                w,
                                AnimatedFloatExpression.DIV,
                            )
                    }
                }
                childWriter.box(
                    RecordingModifier()
                        .size(200, 30)
                        .onClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(outBoost),
                                Utils.idFromNan(boostInc),
                            )
                        )
                )
                childWriter.box(
                    RecordingModifier()
                        .size(200, 30)
                        .onClick(HostAction(actionModeSelect, hyperModeId))
                )
            }
        }
        childWriter.setFloatName(Utils.idFromNan(speedSlider), "outSpeed")

        // Parent document observing child outputs via MAP_REVERSE + MAP_HOST_ACTION
        val hostWriter = createWriter(width = 400, height = 400)
        val parentSpeed = hostWriter.addNamedFloat("parentSpeed", 10f)
        val parentBoost = hostWriter.addNamedFloat("parentBoost", 0f)
        val parentMode = hostWriter.addNamedString("parentMode", "ORBITAL PULSE")
        hostWriter.root {
            val ctrlId = hostWriter.rcDoc("child://controller", RecordingModifier().size(200, 120))
            hostWriter.mapRcDocVariables(
                ctrlId,
                RemoteComposeWriter.MAP_REVERSE,
                parentSpeed,
                "outSpeed",
            )
            hostWriter.mapRcDocVariables(
                ctrlId,
                RemoteComposeWriter.MAP_REVERSE,
                parentBoost,
                "outBoost",
            )
            hostWriter.mapRcDocVariables(
                ctrlId,
                RemoteComposeWriter.MAP_HOST_ACTION,
                parentMode,
                actionModeSelect,
            )
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        context.setRcResolver { url ->
            if (url == "child://controller") childWriter.encodeToByteArray() else null
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        val parentSpeedId = Utils.idFromNan(parentSpeed)
        val parentBoostId = Utils.idFromNan(parentBoost)

        // Child's initial 40f speed reverse-mapped into parentSpeed
        assertThat(context.getFloat(parentSpeedId)).isWithin(0.01f).of(40f)
        assertThat(context.getFloat(parentBoostId)).isEqualTo(0f)
        assertThat(context.getText(parentMode)).isEqualTo("ORBITAL PULSE")

        // Drag child's speed slider (+60px over width 200 -> +30, so 40 -> 70)
        assertThat(hostDoc.touchDown(context, 80f, 30f)).isTrue()
        assertThat(hostDoc.touchDrag(context, 140f, 30f)).isTrue()
        hostDoc.touchUp(context, 140f, 30f, 0f, 0f)
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.getFloat(parentSpeedId)).isWithin(0.01f).of(70f)

        // Click child's boost +1 button twice
        assertThat(hostDoc.onClick(context, 50f, 75f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.getFloat(parentBoostId)).isEqualTo(1f)

        assertThat(hostDoc.onClick(context, 50f, 75f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.getFloat(parentBoostId)).isEqualTo(2f)

        // Click child's HYPER DRIVE mode button -> updates parentMode via MAP_HOST_ACTION
        assertThat(hostDoc.onClick(context, 50f, 105f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.getText(parentMode)).isEqualTo("HYPER DRIVE")
    }

    @Test
    fun rcDoc_twoChildrenInteractViaParentBus() {
        val actionWaveSelect = 3001

        // Child A ("child://peerA", 200x100):
        // - y: 0..50: sharedFreq slider (0..10 Hz, initial 4)
        // - y: 50..100: button firing HostAction(3001, "PURE SINE")
        // - receives "inPings" from Child B
        val peerAWriter = createWriter(width = 200, height = 100)
        val inPingsA = peerAWriter.addNamedFloat("inPings", 0f)
        val pureSineId = peerAWriter.addText("PURE SINE")
        var freqSliderA = 0f
        peerAWriter.root {
            peerAWriter.column(
                RecordingModifier().size(200, 100),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                peerAWriter.box(
                    RecordingModifier().size(200, 50),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    peerAWriter.canvas(RecordingModifier().fillMaxSize()) {
                        val w = peerAWriter.addComponentWidthValue()
                        freqSliderA =
                            peerAWriter.addTouch(
                                4f,
                                0f,
                                10f,
                                RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                10f,
                                AnimatedFloatExpression.MUL,
                                w,
                                AnimatedFloatExpression.DIV,
                            )
                    }
                }
                peerAWriter.box(
                    RecordingModifier()
                        .size(200, 50)
                        .onClick(HostAction(actionWaveSelect, pureSineId))
                )
            }
        }
        val freqAId = Utils.idFromNan(freqSliderA)
        val inPingsAId = Utils.idFromNan(inPingsA)
        peerAWriter.setFloatName(freqAId, "sharedFreq")

        // Child B ("child://peerB", 200x100):
        // - y: 0..50: sharedFreq slider (0..10 Hz, initial 4)
        // - y: 50..100: Ack Ping button incrementing "outPings" -> sent back to Child A
        // - receives "inWaveLabel" from Child A
        val peerBWriter = createWriter(width = 200, height = 100)
        val inWaveLabelB = peerBWriter.addNamedString("inWaveLabel", "HARMONIC WAVE")
        val outPingsB = peerBWriter.addNamedFloat("outPings", 0f)
        val pingIncB = peerBWriter.floatExpression(outPingsB, 1f, AnimatedFloatExpression.ADD)
        var freqSliderB = 0f
        peerBWriter.root {
            peerBWriter.column(
                RecordingModifier().size(200, 100),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                peerBWriter.box(
                    RecordingModifier().size(200, 50),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    peerBWriter.canvas(RecordingModifier().fillMaxSize()) {
                        val w = peerBWriter.addComponentWidthValue()
                        freqSliderB =
                            peerBWriter.addTouch(
                                4f,
                                0f,
                                10f,
                                RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                10f,
                                AnimatedFloatExpression.MUL,
                                w,
                                AnimatedFloatExpression.DIV,
                            )
                        peerBWriter.drawTextRun(inWaveLabelB, 0, 9, 0, 9, 0f, 10f, false)
                    }
                }
                peerBWriter.box(
                    RecordingModifier()
                        .size(200, 50)
                        .onClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(outPingsB),
                                Utils.idFromNan(pingIncB),
                            )
                        )
                )
            }
        }
        val freqBId = Utils.idFromNan(freqSliderB)
        peerBWriter.setFloatName(freqBId, "sharedFreq")

        // Parent bus connecting Child A (y: 0..100) <-> Child B (y: 100..200)
        val hostWriter = createWriter(width = 400, height = 400)
        val busFreq = hostWriter.addNamedFloat("busFreq", 4f)
        val busWave = hostWriter.addNamedString("busWave", "HARMONIC WAVE")
        val busPings = hostWriter.addNamedFloat("busPings", 0f)
        var docAId = -1
        var docBId = -1
        hostWriter.root {
            hostWriter.column(
                RecordingModifier().size(200, 200),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                docAId = hostWriter.rcDoc("child://peerA", RecordingModifier().size(200, 100))
                hostWriter.mapRcDocVariables(
                    docAId,
                    RemoteComposeWriter.MAP_BIDIRECTIONAL,
                    busFreq,
                    "sharedFreq",
                )
                hostWriter.mapRcDocVariables(
                    docAId,
                    RemoteComposeWriter.MAP_HOST_ACTION,
                    busWave,
                    actionWaveSelect,
                )
                hostWriter.mapRcDocVariables(
                    docAId,
                    RemoteComposeWriter.MAP_FORWARD,
                    busPings,
                    "inPings",
                )

                docBId = hostWriter.rcDoc("child://peerB", RecordingModifier().size(200, 100))
                hostWriter.mapRcDocVariables(
                    docBId,
                    RemoteComposeWriter.MAP_BIDIRECTIONAL,
                    busFreq,
                    "sharedFreq",
                )
                hostWriter.mapRcDocVariables(
                    docBId,
                    RemoteComposeWriter.MAP_FORWARD,
                    busWave,
                    "inWaveLabel",
                )
                hostWriter.mapRcDocVariables(
                    docBId,
                    RemoteComposeWriter.MAP_REVERSE,
                    busPings,
                    "outPings",
                )
            }
        }

        val hostDoc = decodeDocument(hostWriter)
        val context = TestRemoteContext()
        context.setRcResolver { url ->
            when (url) {
                "child://peerA" -> peerAWriter.encodeToByteArray()
                "child://peerB" -> peerBWriter.encodeToByteArray()
                else -> null
            }
        }

        hostDoc.initializeContext(context)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        val childADoc = (hostDoc.getComponent(docAId) as RcDocLayout).childDocument!!
        val childBDoc = (hostDoc.getComponent(docBId) as RcDocLayout).childDocument!!
        val busFreqId = Utils.idFromNan(busFreq)

        // 1. Dragging Child A's slider (+60px over width 200 -> +3 Hz: 4 -> 7) moves Child B's
        // slider!
        assertThat(hostDoc.touchDown(context, 80f, 25f)).isTrue()
        assertThat(hostDoc.touchDrag(context, 140f, 25f)).isTrue()
        hostDoc.touchUp(context, 140f, 25f, 0f, 0f)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(childADoc.remoteComposeState.getFloat(freqAId)).isWithin(0.01f).of(7f)
        assertThat(context.getFloat(busFreqId)).isWithin(0.01f).of(7f)
        assertThat(childBDoc.remoteComposeState.getFloat(freqBId)).isWithin(0.01f).of(7f)

        // 2. Dragging Child B's slider (-80px over width 200 -> -4 Hz: 7 -> 3) moves Child A's
        // slider!
        assertThat(hostDoc.touchDown(context, 140f, 125f)).isTrue()
        assertThat(hostDoc.touchDrag(context, 60f, 125f)).isTrue()
        hostDoc.touchUp(context, 60f, 125f, 0f, 0f)
        hostDoc.paint(context, Theme.UNSPECIFIED)

        assertThat(childBDoc.remoteComposeState.getFloat(freqBId)).isWithin(0.01f).of(3f)
        assertThat(context.getFloat(busFreqId)).isWithin(0.01f).of(3f)
        assertThat(childADoc.remoteComposeState.getFloat(freqAId)).isWithin(0.01f).of(3f)

        // 3. Clicking wave mode in Child A updates Child B's wave label via HostAction -> forward
        context.drawnTexts.clear()
        assertThat(hostDoc.onClick(context, 50f, 75f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(context.drawnTexts).contains("PURE SINE")

        // 4. Clicking Ack Ping in Child B increments Child A's inPings via reverse -> forward
        assertThat(hostDoc.onClick(context, 50f, 175f)).isTrue()
        hostDoc.paint(context, Theme.UNSPECIFIED)
        assertThat(childADoc.remoteComposeState.getFloat(inPingsAId)).isEqualTo(1f)
    }
}

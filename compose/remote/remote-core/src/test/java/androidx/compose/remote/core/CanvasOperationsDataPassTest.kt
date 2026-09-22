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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CanvasOperationsDataPassTest {

    @Test
    fun `Canvas operations with FEATURE_DATA_PASS_CANVAS_OPS enabled executes in data pass`() {

        // Arrange.
        val context = TestRemoteContext()
        context.paintContext = TestPaintContext(context)

        val rcWriter =
            RemoteComposeWriter(
                RcPlatformServices.None,
                RemoteComposeWriter.hTag(Header.FEATURE_DATA_PASS_CANVAS_OPS, 1),
            )

        var exprId = -1
        rcWriter.root {
            rcWriter.startBox(RecordingModifier())
            rcWriter.startCanvasOperations()
            exprId = Utils.idFromNan(rcWriter.floatExpression(10f, 20f, ADD))
            rcWriter.endCanvasOperations()
            rcWriter.endBox()
        }

        val doc = CoreDocument()
        doc.initFromBuffer(rcWriter.buffer)

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: Operation in CanvasOperations was applied during DATA pass.
        assertThat(context.getFloat(exprId)).isEqualTo(30f)
    }

    @Test
    fun `Canvas operations with FEATURE_DATA_PASS_CANVAS_OPS disabled skips data pass`() {
        // Arrange.
        val context = TestRemoteContext()
        context.paintContext = TestPaintContext(context)

        val rcWriter =
            RemoteComposeWriter(
                RcPlatformServices.None,
                RemoteComposeWriter.hTag(Header.FEATURE_DATA_PASS_CANVAS_OPS, 0),
            )

        var exprId = -1
        rcWriter.root {
            rcWriter.startBox(RecordingModifier())
            rcWriter.startCanvasOperations()
            exprId = Utils.idFromNan(rcWriter.floatExpression(10f, 20f, ADD))
            rcWriter.endCanvasOperations()
            rcWriter.endBox()
        }

        val doc = CoreDocument()
        doc.initFromBuffer(rcWriter.buffer)

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: Operation in CanvasOperations was NOT applied during DATA pass.
        assertThat(context.getFloat(exprId)).isNaN()

        // Act: PAINT pass.
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: Operation in CanvasOperations was applied during PAINT pass.
        assertThat(context.getFloat(exprId)).isEqualTo(30f)
    }

    @Test
    fun `Canvas operations with FEATURE_DATA_PASS_CANVAS_OPS default executes in data pass`() {
        // Arrange.
        val context = TestRemoteContext()
        context.paintContext = TestPaintContext(context)

        val rcWriter = RemoteComposeWriter(RcPlatformServices.None)

        var exprId = -1
        rcWriter.root {
            rcWriter.startBox(RecordingModifier())
            rcWriter.startCanvasOperations()
            exprId = Utils.idFromNan(rcWriter.floatExpression(10f, 20f, ADD))
            rcWriter.endCanvasOperations()
            rcWriter.endBox()
        }

        val doc = CoreDocument()
        doc.initFromBuffer(rcWriter.buffer)

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: By default, operation in CanvasOperations was applied during DATA pass.
        assertThat(context.getFloat(exprId)).isEqualTo(30f)
    }

    private class TestRemoteContext : RemoteContext() {
        private val floats = HashMap<Int, Float>()

        override fun clearVariables() {
            super.clearVariables()
            floats.clear()
        }

        override fun loadFloat(id: Int, value: Float) {
            floats[id] = value
        }

        override fun overrideFloat(id: Int, value: Float) {
            floats[id] = value
        }

        override fun getFloat(id: Int): Float = floats[id] ?: Float.NaN

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

        override fun getPathData(instanceId: Int): FloatArray? = null

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {}

        override fun loadColor(id: Int, color: Int) {}

        override fun setNamedColorOverride(colorName: String, color: Int) {}

        override fun setNamedStringOverride(stringName: String, value: String) {}

        override fun clearNamedStringOverride(stringName: String) {}

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {}

        override fun clearNamedBooleanOverride(booleanName: String) {}

        override fun setNamedIntegerOverride(integerName: String, value: Int) {}

        override fun clearNamedIntegerOverride(integerName: String) {}

        override fun setNamedFloatOverride(floatName: String, value: Float) {}

        override fun clearNamedFloatOverride(floatName: String) {}

        override fun setNamedLong(name: String, value: Long) {}

        override fun setNamedDataOverride(dataName: String, value: Any) {}

        override fun clearNamedDataOverride(dataName: String) {}

        override fun addCollection(id: Int, collection: ArrayAccess) {}

        override fun putDataMap(id: Int, map: DataMap) {}

        override fun getDataMap(id: Int): DataMap? = null

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {}

        override fun getObject(id: Int): Any? = null

        override fun hapticEffect(type: Int) {}

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}

        override fun loadText(id: Int, text: String) {}

        override fun getText(id: Int): String? = null

        override fun loadInteger(id: Int, value: Int) {}

        override fun overrideInteger(id: Int, value: Int) {}

        override fun overrideText(id: Int, valueId: Int) {}

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

        override fun loadShader(id: Int, value: ShaderData) {}

        override fun getInteger(id: Int): Int = 0

        override fun getLong(id: Int): Long = 0L

        override fun getColor(id: Int): Int = 0

        override fun listensTo(id: Int, variableSupport: VariableSupport) {}

        override fun updateOps(): Int = 0

        override fun getShader(id: Int): ShaderData? = null

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {}
    }

    private class TestPaintContext(context: RemoteContext) : PaintContext(context) {
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

        override fun getText(id: Int): String? = null

        override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

        override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
    }
}

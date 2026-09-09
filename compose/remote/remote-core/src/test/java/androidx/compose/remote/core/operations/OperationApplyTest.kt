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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.RemoteContext.ContextMode
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.remote.core.serialize.MapSerializer
import com.google.common.truth.Truth.assertThat
import java.util.ArrayList
import java.util.HashMap
import kotlin.test.Test
import org.junit.Assume
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OperationApplyTest(private val operation: Operation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "type={0}")
        fun parameters() =
            arrayOf(
                TestOperation(),
                TestVariableSupportOperation(),
                TestContainerOperation(),
                TestContainerAndComponentDataOperation(),
                TestPaintOperation(),
                TestModifierOperation(),
            )
    }

    private fun createDocumentAndContext(
        vararg ops: Operation
    ): Pair<CoreDocument, TestRemoteContext> {
        val context =
            TestRemoteContext().apply {
                paintContext = TestPaintContext(this)
            }
        val flatList = ArrayList(ops.toList())
        val nestedList = CoreDocument.nestContainers(flatList, false, CoreDocument())
        val doc =
            CoreDocument().apply {
                operations.addAll(nestedList)
                assignAllLayoutIndices()
            }
        return Pair(doc, context)
    }

    @Test
    fun `Operation inside root`() {
        // Arrange.
        val (doc, context) =
            createDocumentAndContext(
                RootLayoutComponent(1),
                operation,
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()
    }

    @Test
    fun `Operation inside Component`() {
        // Arrange.
        val root = RootLayoutComponent(1)
        val (doc, context) =
            createDocumentAndContext(
                root,
                Component(2, 0f, 0f, 10f, 10f, root),
                operation,
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        if (operation is PaintOperation) {
            assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()
        } else {
            assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
        }
    }

    @Test
    fun `Operation inside LayoutComponent`() {
        // Arrange.
        val root = RootLayoutComponent(1)
        val columnLayout =
            ColumnLayout(root, 2, 0, 0f, 0f, 10f, 20f, ColumnLayout.START, ColumnLayout.TOP, 0f)
        val (doc, context) =
            createDocumentAndContext(
                root,
                columnLayout,
                operation,
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (
            (operation is Container && operation is ComponentData) || operation is ModifierOperation
        ) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        if (operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()
        }
    }

    @Test
    fun `Operation inside ComponentData containers in Modifiers in LayoutComponents`() {
        // Modifiers inside modifiers will receive apply() calls but their semantics are undefined,
        //  so we skip this test if the operation is a modifier.
        Assume.assumeFalse(operation is ModifierOperation)

        // Arrange.
        val root = RootLayoutComponent(1)
        val column =
            ColumnLayout(root, 2, 0, 0f, 0f, 10f, 20f, ColumnLayout.START, ColumnLayout.TOP, 0f)
        val modifier = TestModifierOperation()
        val (doc, context) =
            createDocumentAndContext(
                root,
                column,
                modifier,
                operation,
                ContainerEnd(),
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation is Container && operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA])
                .containsExactly(operation, modifier)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(modifier)
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        if (operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()
        }
    }

    @Test
    fun `Operation inside Canvas Operations in root`() {
        // Arrange.
        val (doc, context) =
            createDocumentAndContext(
                RootLayoutComponent(1),
                CanvasOperations(),
                operation,
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
    }

    @Test
    fun `Operation inside Canvas Operations inside LayoutComponents`() {
        // Arrange.
        val root = RootLayoutComponent(1)
        val column =
            ColumnLayout(root, 2, 0, 0f, 0f, 10f, 20f, ColumnLayout.START, ColumnLayout.TOP, 0f)
        val (doc, context) =
            createDocumentAndContext(
                root,
                column,
                CanvasOperations(),
                operation,
                ContainerEnd(),
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
    }

    @Test
    fun `Operation inside Canvas Operations inside nested LayoutComponents`() {
        // Arrange.
        val root = RootLayoutComponent(1)
        val column =
            ColumnLayout(root, 2, 0, 0f, 0f, 10f, 20f, ColumnLayout.START, ColumnLayout.TOP, 0f)
        val row = RowLayout(column, 3, 0, 0f, 0f, 10f, 10f, RowLayout.START, RowLayout.TOP, 0f)
        val (doc, context) =
            createDocumentAndContext(
                root,
                column,
                row,
                CanvasOperations(),
                operation,
                ContainerEnd(),
                ContainerEnd(),
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
    }

    @Test
    fun `Operation inside draw with content`() {
        // Arrange.
        val root = RootLayoutComponent(1)
        val (doc, context) =
            createDocumentAndContext(
                root,
                CanvasOperations(),
                operation,
                DrawContent(),
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).containsExactly(operation)
    }

    @Test
    fun `Operation inside ModifierOperation container`() {
        // Arrange.
        val root = RootLayoutComponent(1)
        val (doc, context) =
            createDocumentAndContext(
                root,
                ClickModifierOperation(),
                operation,
                ContainerEnd(),
                ContainerEnd(),
            )

        // Act: DATA pass.
        doc.initializeContext(context)

        // Assert: DATA pass.
        if (operation !is Container || operation is ComponentData) {
            assertThat(context.appliedOperations[ContextMode.DATA]).containsExactly(operation)
        } else {
            assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        }
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()

        // Act: PAINT pass.
        context.resetTrackedOperations()
        doc.measure(context, 0f, 100f, 0f, 100f)
        doc.paint(context, 0)

        // Assert: PAINT pass.
        assertThat(context.appliedOperations[ContextMode.DATA]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.UNSET]).isEmpty()
        assertThat(context.appliedOperations[ContextMode.PAINT]).isEmpty()
    }

    private open class BaseTestOperation : Operation() {
        override fun write(buffer: WireBuffer) {}

        override fun apply(context: RemoteContext) {
            if (context is TestRemoteContext) {
                context.appliedOperations[context.mode]?.add(this)
            }
        }

        override fun deepToString(indent: String) = "${indent}TestOperation"
    }

    private class TestOperation : BaseTestOperation() {
        override fun toString(): String = "Operation"
    }

    private class TestVariableSupportOperation : BaseTestOperation(), VariableSupport {
        override fun registerListening(context: RemoteContext) {}

        override fun updateVariables(context: RemoteContext) {}

        override fun toString(): String = "VariableSupportOperation"
    }

    private class TestContainerOperation : BaseTestOperation(), Container {
        val ops = ArrayList<Operation>()

        override fun getList(): ArrayList<Operation> = ops

        override fun toString(): String = "ContainerOperation"

        override fun apply(context: RemoteContext) {
            super.apply(context)
        }
    }

    private class TestContainerAndComponentDataOperation :
        BaseTestOperation(), Container, ComponentData {
        val ops = ArrayList<Operation>()

        override fun getList(): ArrayList<Operation> = ops

        override fun toString(): String = "ContainerAndComponentDataOperation"

        override fun apply(context: RemoteContext) {
            super.apply(context)
        }
    }

    private class TestPaintOperation : PaintOperation() {
        override fun serialize(serializer: MapSerializer) {}

        override fun write(buffer: WireBuffer) {}

        override fun deepToString(indent: String): String = indent + toString()

        override fun toString(): String = "PaintOperation"

        override fun apply(context: RemoteContext) {
            super.apply(context)
            if (context is TestRemoteContext) {
                context.appliedOperations[context.mode]?.add(this)
            }
        }

        override fun paint(context: PaintContext) {}
    }

    private class TestModifierOperation : BaseTestOperation(), ModifierOperation {
        override fun serialize(serializer: MapSerializer) {}

        override fun serializeToString(indent: Int, serializer: StringSerializer) {}

        override fun toString(): String = "ModifierOperation"
    }

    private class TestRemoteContext : RemoteContext() {
        var appliedOperations =
            hashMapOf<ContextMode, MutableList<Operation>>(
                ContextMode.UNSET to mutableListOf(),
                ContextMode.DATA to mutableListOf(),
                ContextMode.PAINT to mutableListOf(),
            )

        fun resetTrackedOperations() {
            appliedOperations.values.forEach { it.clear() }
        }

        override fun loadFloat(id: Int, value: Float) {}

        override fun overrideFloat(id: Int, value: Float) {}

        override fun getFloat(id: Int): Float = 0f

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

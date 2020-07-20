/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.tooling.inspector

import android.view.View
import android.view.ViewGroup
import androidx.compose.InternalComposeApi
import androidx.compose.resetSourceInfo
import androidx.test.filters.SmallTest
import androidx.ui.core.OwnedLayer
import androidx.compose.foundation.Text
import androidx.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.ui.material.Button
import androidx.ui.material.Surface
import androidx.ui.tooling.Group
import androidx.ui.tooling.Inspectable
import androidx.ui.tooling.R
import androidx.ui.tooling.SlotTableRecord
import androidx.ui.tooling.ToolingTest
import androidx.ui.tooling.asTree
import androidx.ui.tooling.position
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

private const val DEBUG = false
private const val LAMBDA_PATTERN = "(\\$\\d+)*\\.invoke"
private const val LAMBDA_REPLACEMENT = "[###].invoke"
private val lambdaExpr = Regex(LAMBDA_PATTERN)
private const val ESCAPE_PATTERN = "\\$([a-zA-Z])"
private const val ESCAPE_REPLACEMENT = "\\\\\\$$1"
private val escapeExpr = Regex(ESCAPE_PATTERN)

@SmallTest
@RunWith(JUnit4::class)
class LayoutInspectorTreeTest : ToolingTest() {
    private lateinit var density: Density
    private lateinit var view: View

    @Before
    fun density() {
        @OptIn(InternalComposeApi::class)
        resetSourceInfo()
        density = Density(activity)
        view = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
    }

    @Ignore("Manual test")
    @Test
    fun buildTree() {
        val slotTableRecord = SlotTableRecord.create()

        show {
            Inspectable(slotTableRecord) {
                Column {
                    Text(text = "Hello World", color = Color.Green)
                    Surface {
                        Button(onClick = {}) { Text(text = "OK") }
                    }
                }
            }
        }

        // TODO: Find out if we can set "settings put global debug_view_attributes 1" in tests
        view.setTag(R.id.inspection_slot_table_set, slotTableRecord.store)
        val viewWidth = with(density) { view.width.toDp() }
        val viewHeight = with(density) { view.height.toDp() }
        dumpSlotTableSet(slotTableRecord)
        val builder = LayoutInspectorTree()
        val nodes = builder.convert(view)
        dumpNodes(nodes)
        val nodeIterator = nodes.flatMap { flatten(it) }.iterator()

        fun validate(
            isRenderNode: Boolean = false,
            name: String,
            fileName: String,
            lineNumber: Int = -1,
            function: String,
            left: Dp,
            top: Dp,
            width: Dp,
            height: Dp,
            children: List<String> = emptyList(),
            block: ParameterValidationReceiver.() -> Unit = {}
        ) {
            assertThat(nodeIterator.hasNext()).isTrue()
            val node = nodeIterator.next()
            if (isRenderNode) {
                assertThat(node.id).isGreaterThan(0L)
            } else {
                assertThat(node.id).isLessThan(0L)
            }
            assertThat(node.name).isEqualTo(name)
            assertThat(node.fileName).isEqualTo(fileName)
            if (lineNumber != -1) {
                assertThat(node.lineNumber).isEqualTo(lineNumber)
            }
            assertThat(node.functionName.replace(lambdaExpr, LAMBDA_REPLACEMENT))
                .isEqualTo(function)
            with(density) {
                assertThat(node.left.toDp().value).isWithin(2.0f).of(left.value)
                assertThat(node.top.toDp().value).isWithin(2.0f).of(top.value)
                assertThat(node.width.toDp().value).isWithin(2.0f).of(width.value)
                assertThat(node.height.toDp().value).isWithin(2.0f).of(height.value)
            }
            assertThat(node.children.map { it.name }).containsExactlyElementsIn(children).inOrder()

            val receiver = ParameterValidationReceiver(node.parameters.listIterator())
            receiver.block()
            assertThat(receiver.parameterIterator.hasNext()).isFalse()
        }

        validate(
            name = "Box",
            fileName = "Box.kt",
            function = "androidx.compose.foundation.BoxKt.Box",
            left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
            children = listOf("Column")
        ) {
            parameter(name = "backgroundColor", type = ParameterType.Color, value = 0x0)
            parameter(name = "gravity", type = ParameterType.String, value = "TopStart")
            parameter(name = "padding", type = ParameterType.DimensionDp, value = 0.0f)
            parameter(name = "paddingBottom", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "paddingEnd", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "paddingStart", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "paddingTop", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "shape", type = ParameterType.String, value = "Shape")
        }
        validate(
            name = "Column",
            fileName = "Box.kt",
            function = "androidx.compose.foundation.BoxKt.Box",
            left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
            children = listOf("RowColumnImpl")
        )
        validate(
            name = "RowColumnImpl",
            fileName = "Column.kt",
            function = "androidx.compose.foundation.layout.ColumnKt.Column",
            left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
            children = listOf("Column")
        ) {
            parameter(name = "crossAxisSize", type = ParameterType.String, value = "Wrap")
            parameter(name = "orientation", type = ParameterType.String, value = "Vertical")
        }
        validate(
            name = "Column",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 54.9.dp,
            children = listOf("RowColumnImpl")
        ) {
            parameter(name = "horizontalGravity", type = ParameterType.String, value = "Start")
            parameter(name = "verticalArrangement", type = ParameterType.String, value = "Top")
        }
        validate(
            name = "RowColumnImpl",
            fileName = "Column.kt",
            function = "androidx.compose.foundation.layout.ColumnKt.Column",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 54.9.dp,
            children = listOf("Text", "Surface")
        ) {
            parameter(name = "crossAxisSize", type = ParameterType.String, value = "Wrap")
            parameter(name = "orientation", type = ParameterType.String, value = "Vertical")
        }
        validate(
            name = "Text",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 18.9.dp,
            children = listOf("CoreText")
        ) {
            parameter(name = "color", type = ParameterType.Color, value = 0xff00ff00.toInt())
            parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
            parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
            parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            parameter(name = "overflow", type = ParameterType.String, value = "Clip")
            parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
            parameter(name = "style", type = ParameterType.String, value = "TextStyle") {
                parameter(name = "color", type = ParameterType.String, value = "Unset")
                parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
                parameter(name = "background", type = ParameterType.String, value = "Unset")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            }
            parameter(name = "text", type = ParameterType.String, value = "Hello World")
        }
        validate(
            name = "CoreText",
            fileName = "CoreText.kt",
            function = "androidx.ui.text.CoreTextKt.CoreText",
            isRenderNode = true,
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 18.9.dp
        ) {
            parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            parameter(name = "overflow", type = ParameterType.String, value = "Clip")
            parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
            parameter(name = "style", type = ParameterType.String, value = "TextStyle") {
                parameter(name = "color", type = ParameterType.Color, value = 0xff00ff00.toInt())
                parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
                parameter(name = "background", type = ParameterType.String, value = "Unset")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            }
            parameter(name = "text", type = ParameterType.String, value = "Hello World")
        }
        validate(
            name = "Surface",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("SurfaceLayout")
        ) {
            parameter(name = "color", type = ParameterType.Color, value = 0xffffffff.toInt())
            parameter(name = "contentColor", type = ParameterType.Color, value = 0xff000000.toInt())
            parameter(name = "elevation", type = ParameterType.DimensionDp, value = 0.0f)
            parameter(name = "shape", type = ParameterType.String, value = "Shape")
        }
        validate(
            name = "SurfaceLayout",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.Surface",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            isRenderNode = true,
            children = listOf("Button")
        )
        validate(
            name = "Button",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("Surface")
        ) {
            parameter(name = "backgroundColor", type = ParameterType.Color,
                value = 0xff6200ee.toInt())
            parameter(name = "contentColor", type = ParameterType.Color, value = 0xffffffff.toInt())
            parameter(name = "disabledBackgroundColor", type = ParameterType.Color,
                value = 0xffe0e0e0.toInt())
            parameter(name = "disabledContentColor", type = ParameterType.Color, value = 0x61000000)
            parameter(name = "disabledElevation", type = ParameterType.DimensionDp, value = 0.0f)
            parameter(name = "elevation", type = ParameterType.DimensionDp, value = 2.0f)
            parameter(name = "enabled", type = ParameterType.Boolean, value = true)
            parameter(name = "padding", type = ParameterType.String, value = "InnerPadding") {
                parameter(name = "start", type = ParameterType.DimensionDp, value = 16.0f)
                parameter(name = "end", type = ParameterType.DimensionDp, value = 16.0f)
                parameter(name = "top", type = ParameterType.DimensionDp, value = 8.0f)
                parameter(name = "bottom", type = ParameterType.DimensionDp, value = 8.0f)
            }
            parameter(name = "shape", type = ParameterType.String, value = "RoundedCornerShape") {
                parameter(name = "topLeft", type = ParameterType.DimensionDp, value = 4.0f)
                parameter(name = "topRight", type = ParameterType.DimensionDp, value = 4.0f)
                parameter(name = "bottomLeft", type = ParameterType.DimensionDp, value = 4.0f)
                parameter(name = "bottomRight", type = ParameterType.DimensionDp, value = 4.0f)
            }
        }
        validate(
            name = "Surface",
            fileName = "Button.kt",
            function = "androidx.ui.material.ButtonKt.Button",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("SurfaceLayout")
        ) {
            parameter(name = "color", type = ParameterType.Color, value = 0xff6200ee.toInt())
            parameter(name = "contentColor", type = ParameterType.Color, value = 0xffffffff.toInt())
            parameter(name = "elevation", type = ParameterType.DimensionDp, value = 2.0f)
            parameter(name = "shape", type = ParameterType.String, value = "RoundedCornerShape") {
                parameter(name = "topLeft", type = ParameterType.DimensionDp, value = 4.0f)
                parameter(name = "topRight", type = ParameterType.DimensionDp, value = 4.0f)
                parameter(name = "bottomLeft", type = ParameterType.DimensionDp, value = 4.0f)
                parameter(name = "bottomRight", type = ParameterType.DimensionDp, value = 4.0f)
            }
        }
        validate(
            name = "SurfaceLayout",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.Surface",
            isRenderNode = true,
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("Box")
        )
        validate(
            name = "Box",
            fileName = "Button.kt",
            function = "androidx.ui.material.ButtonKt\$Button[###].invoke",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("Column")
        ) {
            parameter(name = "backgroundColor", type = ParameterType.Color, value = 0x0)
            parameter(name = "gravity", type = ParameterType.String, value = "TopStart")
            parameter(name = "padding", type = ParameterType.DimensionDp, value = 0.0f)
            parameter(name = "paddingBottom", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "paddingEnd", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "paddingStart", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "paddingTop", type = ParameterType.DimensionDp, value = Float.NaN)
            parameter(name = "shape", type = ParameterType.String, value = "Shape")
        }
        validate(
            name = "Column",
            fileName = "Box.kt",
            function = "androidx.compose.foundation.BoxKt.Box",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("RowColumnImpl")
        )
        validate(
            name = "RowColumnImpl",
            fileName = "Column.kt",
            function = "androidx.compose.foundation.layout.ColumnKt.Column",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("ProvideTextStyle")
        ) {
            parameter(name = "crossAxisSize", type = ParameterType.String, value = "Wrap")
            parameter(name = "orientation", type = ParameterType.String, value = "Vertical")
        }
        validate(
            name = "ProvideTextStyle",
            fileName = "Button.kt",
            function = "androidx.ui.material.ButtonKt\$Button[###].invoke",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("Row")
        ) {
            parameter(name = "value", type = ParameterType.String, value = "TextStyle") {
                parameter(name = "color", type = ParameterType.String, value = "Unset")
                parameter(name = "fontSize", type = ParameterType.DimensionSp, value = 14.0f)
                parameter(name = "fontWeight", type = ParameterType.String, value = "Medium")
                parameter(name = "fontFamily", type = ParameterType.String, value = "Default")
                parameter(name = "letterSpacing", type = ParameterType.DimensionSp, value = 1.25f)
                parameter(name = "background", type = ParameterType.String, value = "Unset")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            }
        }
        validate(
            name = "Row",
            fileName = "Button.kt",
            function = "androidx.ui.material.ButtonKt\$Button[###].invoke",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("RowColumnImpl")
        ) {
            parameter(name = "horizontalArrangement", type = ParameterType.String, value = "Center")
            parameter(name = "verticalGravity", type = ParameterType.String,
                value = "CenterVertically")
        }
        validate(
            name = "RowColumnImpl",
            fileName = "Row.kt",
            function = "androidx.compose.foundation.layout.RowKt.Row",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("Text")
        ) {
            parameter(name = "crossAxisSize", type = ParameterType.String, value = "Wrap")
            parameter(name = "orientation", type = ParameterType.String, value = "Horizontal")
        }
        validate(
            name = "Text",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp,
            children = listOf("CoreText")
        ) {
            parameter(name = "color", type = ParameterType.String, value = "Unset")
            parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
            parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
            parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            parameter(name = "overflow", type = ParameterType.String, value = "Clip")
            parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
            parameter(name = "style", type = ParameterType.String, value = "TextStyle") {
                parameter(name = "color", type = ParameterType.String, value = "Unset")
                parameter(name = "fontSize", type = ParameterType.DimensionSp, value = 14.0f)
                parameter(name = "fontWeight", type = ParameterType.String, value = "Medium")
                parameter(name = "fontFamily", type = ParameterType.String, value = "Default")
                parameter(name = "letterSpacing", type = ParameterType.DimensionSp, value = 1.25f)
                parameter(name = "background", type = ParameterType.String, value = "Unset")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            }
            parameter(name = "text", type = ParameterType.String, value = "OK")
        }
        validate(
            name = "CoreText",
            fileName = "CoreText.kt",
            function = "androidx.ui.text.CoreTextKt.CoreText",
            isRenderNode = true,
            left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp
        ) {
            parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            parameter(name = "overflow", type = ParameterType.String, value = "Clip")
            parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
            parameter(name = "style", type = ParameterType.String, value = "TextStyle") {
                parameter(name = "color", type = ParameterType.Color, value = 0xffffffff.toInt())
                parameter(name = "fontSize", type = ParameterType.DimensionSp, value = 14.0f)
                parameter(name = "fontWeight", type = ParameterType.String, value = "Medium")
                parameter(name = "fontFamily", type = ParameterType.String, value = "Default")
                parameter(name = "letterSpacing", type = ParameterType.DimensionSp, value = 1.25f)
                parameter(name = "background", type = ParameterType.String, value = "Unset")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
            }
            parameter(name = "text", type = ParameterType.String, value = "OK")
        }
        assertThat(nodeIterator.hasNext()).isFalse()
    }

    private fun flatten(node: InspectorNode): List<InspectorNode> =
        listOf(node).plus(node.children.flatMap { flatten(it) })

    // region DEBUG print methods
    private fun dumpNodes(nodes: List<InspectorNode>) {
        @Suppress("ConstantConditionIf")
        if (!DEBUG) {
            return
        }
        println()
        println("=================== Nodes ==========================")
        nodes.forEach { dumpNode(it, indent = 0) }
        println()
        println("=================== validate statements ==========================")
        nodes.forEach { generateValidate(it) }
    }

    private fun dumpNode(node: InspectorNode, indent: Int) {
        println(
            "\"${"  ".repeat(indent * 2)}\", \"${node.name}\", \"${node.fileName}\", " +
                    "${node.lineNumber}, \"${node.functionName}\", ${node.left}, ${node.top}, " +
                    "${node.width}, ${node.height}"
        )
        node.children.forEach { dumpNode(it, indent + 1) }
    }

    private fun generateValidate(node: InspectorNode) {
        with(density) {
            val left = round(node.left.toDp())
            val top = round(node.top.toDp())
            val width = if (node.width == view.width) "viewWidth" else round(node.width.toDp())
            val height = if (node.height == view.height) "viewHeight" else round(node.height.toDp())
            val function = node.functionName
                .replace(lambdaExpr, LAMBDA_REPLACEMENT)
                .replace(escapeExpr, ESCAPE_REPLACEMENT)
            val functionLF = if (function.length > 75) "\n${" ".repeat(23)}" else ""

            print(
                """
                  validate(
                      name = "${node.name}",
                      fileName = "${node.fileName}",
                      function = $functionLF"$function",
                      left = $left, top = $top, width = $width, height = $height""".trimIndent()
            )
        }
        if (node.id > 0L) {
            println(",")
            print("    isRenderNode = true")
        }
        if (node.children.isNotEmpty()) {
            println(",")
            val children = node.children.joinToString { "\"${it.name}\"" }
            print("    children = listOf($children)")
        }
        println()
        print(")")
        if (node.parameters.isNotEmpty()) {
            generateParameters(node.parameters, 0)
        }
        println()
        node.children.forEach { generateValidate(it) }
    }

    private fun generateParameters(parameters: List<NodeParameter>, indent: Int) {
        val indentation = " ".repeat(indent * 2)
        println(" {")
        for (param in parameters) {
            val name = param.name
            val type = param.type
            val value = toDisplayValue(type, param.value)
            print("$indentation  parameter(name = \"$name\", type = $type, value = $value)")
            if (param.elements.isNotEmpty()) {
                generateParameters(param.elements, indent + 1)
            }
            println()
        }
        print("$indentation}")
    }

    private fun toDisplayValue(type: ParameterType, value: Any?): String =
        when (type) {
            ParameterType.Boolean -> value.toString()
            ParameterType.Color ->
                "0x${Integer.toHexString(value as Int)}${if (value < 0) ".toInt()" else ""}"
            ParameterType.DimensionSp,
            ParameterType.DimensionDp -> "${value}f"
            ParameterType.Int32 -> value.toString()
            ParameterType.String -> "\"$value\""
            else -> value?.toString() ?: "null"
        }

    private fun dumpSlotTableSet(slotTableRecord: SlotTableRecord) {
        @Suppress("ConstantConditionIf")
        if (!DEBUG) {
            return
        }
        println()
        println("=================== Groups ==========================")
        slotTableRecord.store.forEach { dumpGroup(it.asTree(), indent = 0) }
    }

    private fun dumpGroup(group: Group, indent: Int) {
        val position = group.position?.let { "\"$it\"" } ?: "null"
        val box = group.box
        val id = group.modifierInfo.mapNotNull { (it.extra as? OwnedLayer)?.layerId }
            .singleOrNull() ?: 0
        println(
            "\"${"  ".repeat(indent)}\", ${group.javaClass.simpleName}, " +
                    "params: ${group.parameters.size}, children: ${group.children.size}, " +
                    "$id, $position, " +
                    "${box.left}, ${box.right}, ${box.right - box.left}, ${box.bottom - box.top}"
        )
        for (parameter in group.parameters) {
            println("\"${"  ".repeat(indent + 4)}\"- ${parameter.name}")
        }
        group.children.forEach { dumpGroup(it, indent + 1) }
    }

    private fun round(dp: Dp): Dp = Dp((dp.value * 10.0f).roundToInt() / 10.0f)

    //endregion
}

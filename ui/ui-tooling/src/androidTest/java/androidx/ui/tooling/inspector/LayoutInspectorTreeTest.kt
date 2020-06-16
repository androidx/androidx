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
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
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

    @Test
    fun buildTree() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Column {
                    Text(text = "Hello World")
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
            children: List<String> = emptyList()
        ) {
            assertThat(nodeIterator.hasNext()).isTrue()
            val node = nodeIterator.next()
            if (isRenderNode) {
                assertThat(node.id).isGreaterThan(0L)
            } else {
                assertThat(node.id).isEqualTo(0L)
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
        }

        validate(
            name = "Box",
            fileName = "Box.kt",
            function = "androidx.ui.foundation.BoxKt.Box",
            left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
            children = listOf("Column")
        )
        validate(
            name = "Column",
            fileName = "Column.kt",
            function = "androidx.ui.layout.ColumnKt.Column",
            left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
            children = listOf("RowColumnImpl")
        )
        validate(
            name = "RowColumnImpl",
            fileName = "RowColumnImpl.kt",
            function = "androidx.ui.layout.RowColumnImplKt.RowColumnImpl",
            left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
            children = listOf("Box")
        )
        validate(
            name = "Box",
            fileName = "Box.kt",
            function = "androidx.ui.foundation.BoxKt.Box",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 54.9.dp,
            children = listOf("Column")
        )
        validate(
            name = "Column",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 54.9.dp,
            children = listOf("RowColumnImpl")
        )
        validate(
            name = "RowColumnImpl",
            fileName = "RowColumnImpl.kt",
            function = "androidx.ui.layout.RowColumnImplKt.RowColumnImpl",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 54.9.dp,
            children = listOf("Text", "Surface")
        )
        validate(
            name = "Text",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 18.9.dp,
            children = listOf("CoreText")
        )
        validate(
            name = "CoreText",
            fileName = "Text.kt",
            function = "androidx.ui.foundation.TextKt\$Text[###].invoke",
            left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 18.9.dp
        )
        validate(
            name = "Surface",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("SurfaceLayout")
        )
        validate(
            name = "SurfaceLayout",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.SurfaceLayout",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            isRenderNode = true,
            children = listOf("Surface")
        )
        validate(
            name = "Surface",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.Surface",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("Button")
        )
        validate(
            name = "Button",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("Surface")
        )
        validate(
            name = "Surface",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.Surface",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("SurfaceLayout")
        )
        validate(
            name = "SurfaceLayout",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.SurfaceLayout",
            left = 0.0.dp, top = 18.9.dp, width = 64.0.dp, height = 36.0.dp,
            children = listOf("Surface")
        )
        validate(
            name = "Surface",
            fileName = "Surface.kt",
            function = "androidx.ui.material.SurfaceKt.Surface",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("Box")
        )
        validate(
            name = "Box",
            fileName = "Button.kt",
            function = "androidx.ui.material.ButtonKt\$Button[###].invoke",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("Column")
        )
        validate(
            name = "Column",
            fileName = "Column.kt",
            function = "androidx.ui.layout.ColumnKt.Column",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("RowColumnImpl")
        )
        validate(
            name = "RowColumnImpl",
            fileName = "RowColumnImpl.kt",
            function = "androidx.ui.layout.RowColumnImplKt.RowColumnImpl",
            left = 16.0.dp, top = 26.9.dp, width = 32.0.dp, height = 20.0.dp,
            children = listOf("Box")
        )
        validate(
            name = "Box",
            fileName = "Box.kt",
            function = "androidx.ui.foundation.BoxKt.Box",
            left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp,
            children = listOf("ProvideTextStyle")
        )
        validate(
            name = "ProvideTextStyle",
            fileName = "Button.kt",
            function = "androidx.ui.material.ButtonKt\$Button[###].invoke",
            left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp,
            children = listOf("Text")
        )
        validate(
            name = "Text",
            fileName = "LayoutInspectorTreeTest.kt",
            function =
            "androidx.ui.tooling.inspector.LayoutInspectorTreeTest\$buildTree[###].invoke",
            left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp,
            children = listOf("CoreText")
        )
        validate(
            name = "CoreText",
            fileName = "Text.kt",
            function = "androidx.ui.foundation.TextKt\$Text[###].invoke",
            left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp
        )
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
            val function = node.functionName.replace(lambdaExpr, LAMBDA_REPLACEMENT)
            print(
                """
                  validate(
                      name = "${node.name}",
                      fileName = "${node.fileName}",
                      function = "${function.replace(escapeExpr, ESCAPE_REPLACEMENT)}",
                      left = $left, top = $top, width = $width, height = $height""".trimIndent()
            )
        }
        if (node.id != 0L) {
            println(",")
            print("    isRenderNode = true")
        }
        if (node.children.isNotEmpty()) {
            println(",")
            val children = node.children.joinToString { "\"${it.name}\"" }
            print("    children = listOf($children)")
        }
        println()
        println(")")
        node.children.forEach { generateValidate(it) }
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
            "\"${"  ".repeat(indent * 2)}\", $id, $position, ${box.left}, ${box.right}, " +
                    "${box.right - box.left}, ${box.bottom - box.top}"
        )
        group.children.forEach { dumpGroup(it, indent + 1) }
    }

    private fun round(dp: Dp): Dp = Dp((dp.value * 10.0f).roundToInt() / 10.0f)

    //endregion
}

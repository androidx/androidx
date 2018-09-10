/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation.diagnostics

import androidx.ui.describeEnum
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.matchers.EqualsIgnoringHashCodes
import androidx.ui.matchers.HasGoodToStringDeep
import androidx.ui.painting.Color
import androidx.ui.runtimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DiagnosticsTest {

    class TestTree(
        val name: String? = null,
        val style: DiagnosticsTreeStyle? = null,
        val children: List<TestTree> = listOf(),
        val properties: List<DiagnosticsNode> = listOf()
    ) : DiagnosticableTree {

        override fun debugDescribeChildren(): List<DiagnosticsNode> {
            val children = mutableListOf<DiagnosticsNode>()
            this.children.forEach { child ->
                children.add(child.toDiagnosticsNode(
                        name = "child ${child.name}",
                        style = child.style
                ))
            }
            return children
        }

        override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
            if (style != null)
                properties.defaultDiagnosticsTreeStyle = style

            this.properties.forEach { properties.add(it) }
        }
    }

    enum class ExampleEnum {
        hello,
        world,
        deferToChild
    }

    /**
     * Encode and decode to JSON to make sure all objects in the JSON for the
     * [DiagnosticsNode) are valid JSON.
     */
    fun simulateJsonSerialization(node: DiagnosticsNode): Map<String, Any> {
        // TODO(Migration/Andrey): Originally also encodes into JSON and decodes back
        // There is no out of the box logic to convert map to json in Kotlin
        // So lets skip it for now
        return node.toJsonMap() // json.decode(json.encode(node.toJsonMap()));
    }

    fun validateNodeJsonSerialization(node: DiagnosticsNode) {
        validateNodeJsonSerializationHelper(simulateJsonSerialization(node), node)
    }

    fun validateNodeJsonSerializationHelper(json: Map<String, Any>, node: DiagnosticsNode) {
        assertEquals(node.name.toString(), json["name"])
        assertEquals(node.showSeparator, json["showSeparator"])
        assertEquals(node.toDescription(), json["description"])
        assertEquals(describeEnum(node.getLevel()), json["level"])
        assertEquals(node.getShowName(), json["showName"])
        assertEquals(node.getEmptyBodyDescription().toString(), json["emptyBodyDescription"])
        assertEquals(describeEnum(node.getStyle()!!), json["style"])
        val valueToString = if (node is DiagnosticsProperty<*>) node.valueToString()
        else node.getValue().toString()
        assertEquals(valueToString, json["valueToString"])
        assertEquals(node.runtimeType().toString(), json["type"])
        assertEquals(node.getChildren().isNotEmpty(), json["hasChildren"])
    }

    fun validatePropertyJsonSerialization(property: DiagnosticsProperty<*>) {
        validatePropertyJsonSerializationHelper(simulateJsonSerialization(property), property)
    }

    fun validateStringPropertyJsonSerialization(property: StringProperty) {
        val json: Map<String, Any> = simulateJsonSerialization(property)
        assertEquals(property.quoted, json["quoted"])
        validatePropertyJsonSerializationHelper(json, property)
    }

    fun validateFlagPropertyJsonSerialization(property: FlagProperty) {
        val json: Map<String, Any> = simulateJsonSerialization(property)
        assertEquals(property.ifTrue, json["ifTrue"])

        if (property.ifTrue != null) {
            assertEquals(property.ifTrue, json["ifTrue"])
        } else {
            assertFalse(json.containsKey("ifTrue"))
        }

        if (property.ifFalse != null) {
            assertEquals(property.ifFalse, json["ifFalse"])
        } else {
            assertFalse(json.containsKey("isFalse"))
        }
        validatePropertyJsonSerializationHelper(json, property)
    }

    fun validateDoublePropertyJsonSerialization(property: DoubleProperty) {
        val json: Map<String, Any> = simulateJsonSerialization(property)
        if (property.unit != null) {
            assertEquals(property.unit, json["unit"])
        } else {
            assertFalse(json.containsKey("unit"))
        }

        assertEquals(property.numberToString(), json["numberToString"])

        validatePropertyJsonSerializationHelper(json, property)
    }

    fun validateObjectFlagPropertyJsonSerialization(property: ObjectFlagProperty<*>) {
        val json: Map<String, Any> = simulateJsonSerialization(property)
        if (property.ifPresent != null) {
            assertEquals(property.ifPresent, json["ifPresent"])
        } else {
            assertFalse(json.containsKey("ifPresent"))
        }

        validatePropertyJsonSerializationHelper(json, property)
    }

    fun validateIterablePropertyJsonSerialization(property: IterableProperty<Any>) {
        val json: Map<String, Any> = simulateJsonSerialization(property)
        if (property.getValue() != null) {
            val valuesJson: List<Any> = json["values"] as List<Any>
            val propertyList: List<Any> = property.getValue() as List<Any>
            val assertEqualsedValues: List<String> = propertyList.map { it.toString() }
            assertTrue(valuesJson == assertEqualsedValues)
        } else {
            assertFalse(json.containsKey("values"))
        }

        validatePropertyJsonSerializationHelper(json, property)
    }

    fun validatePropertyJsonSerializationHelper(
        json: Map<String, Any>,
        property: DiagnosticsProperty<*>
    ) {
        if (property.defaultValue != kNoDefaultValue) {
            assertEquals(property.defaultValue.toString(), json["defaultValue"])
        } else {
            assertFalse(json.containsKey("defaultValue"))
        }

        if (property.ifEmpty != null) {
            assertEquals(property.ifEmpty, json["ifEmpty"])
        } else {
            assertFalse(json.containsKey("ifEmpty"))
        }
        if (property.ifNull != null) {
            assertEquals(property.ifNull, json["ifNull"])
        } else {
            assertFalse(json.containsKey("ifNull"))
        }

        if (property.tooltip != null) {
            assertEquals(property.tooltip, json["tooltip"])
        } else {
            assertFalse(json.containsKey("tooltip"))
        }

        assertEquals(property.missingIfNull, json["missingIfNull"])
        if (property.getException() != null) {
            assertEquals(property.getException().toString(), json["exception"])
        } else {
            assertFalse(json.containsKey("exception"))
        }
        assertEquals(property.propertyType().toString(), json["propertyType"])
        assertEquals(property.valueToString(), json["valueToString"])
        assertTrue(json.containsKey("defaultLevel"))
        if (property.getValue() is Diagnosticable) {
            assertTrue(json["isDiagnosticableValue"] as Boolean)
        } else {
            assertFalse(json.containsKey("isDiagnosticableValue"))
        }
        validateNodeJsonSerializationHelper(json, property)
    }

    private fun goldenStyleTestControl(
        description: String? = null,
        style: DiagnosticsTreeStyle? = null,
        lastChildStyle: DiagnosticsTreeStyle? = null,
        golden: String = ""
    ) {
        val tree = TestTree(children = listOf(
                TestTree(name = "node A", style = style),
                TestTree(
                        name = "node B",
                        children = listOf(
                                TestTree(name = "node B1", style = style),
                                TestTree(name = "node B2", style = style),
                                TestTree(name = "node B3", style = lastChildStyle ?: style)
                        ),
                        style = style
                ),
                TestTree(name = "node C", style = lastChildStyle ?: style)
        ), style = lastChildStyle)

        assertThat(tree, HasGoodToStringDeep)
        assertThat(description,
                tree.toDiagnosticsNode(style = style).toStringDeep(),
                EqualsIgnoringHashCodes(golden))

        validateNodeJsonSerialization(tree.toDiagnosticsNode())
    }

    @Test
    fun `TreeDiagnosticsMixin control test`() {
        goldenStyleTestControl(
                "dense",
                style = DiagnosticsTreeStyle.dense,
                golden =
                "TestTree#00000\n" +
                        "├child node A: TestTree#00000\n" +
                        "├child node B: TestTree#00000\n" +
                        "│├child node B1: TestTree#00000\n" +
                        "│├child node B2: TestTree#00000\n" +
                        "│└child node B3: TestTree#00000\n" +
                        "└child node C: TestTree#00000\n"
        )

        goldenStyleTestControl(
                "sparse",
                style = DiagnosticsTreeStyle.sparse,
                golden =
                "TestTree#00000\n" +
                        " ├─child node A: TestTree#00000\n" +
                        " ├─child node B: TestTree#00000\n" +
                        " │ ├─child node B1: TestTree#00000\n" +
                        " │ ├─child node B2: TestTree#00000\n" +
                        " │ └─child node B3: TestTree#00000\n" +
                        " └─child node C: TestTree#00000\n"
        )

        goldenStyleTestControl(
                "dashed",
                style = DiagnosticsTreeStyle.offstage,
                golden =
                "TestTree#00000\n" +
                        " ╎╌child node A: TestTree#00000\n" +
                        " ╎╌child node B: TestTree#00000\n" +
                        " ╎ ╎╌child node B1: TestTree#00000\n" +
                        " ╎ ╎╌child node B2: TestTree#00000\n" +
                        " ╎ └╌child node B3: TestTree#00000\n" +
                        " └╌child node C: TestTree#00000\n"
        )

        goldenStyleTestControl(
                "leaf children",
                style = DiagnosticsTreeStyle.sparse,
                lastChildStyle = DiagnosticsTreeStyle.transition,
                golden =
                "TestTree#00000\n" +
                        " ├─child node A: TestTree#00000\n" +
                        " ├─child node B: TestTree#00000\n" +
                        " │ ├─child node B1: TestTree#00000\n" +
                        " │ ├─child node B2: TestTree#00000\n" +
                        " │ ╘═╦══ child node B3 ═══\n" +
                        " │   ║ TestTree#00000\n" +
                        " │   ╚═══════════\n" +
                        " ╘═╦══ child node C ═══\n" +
                        "   ║ TestTree#00000\n" +
                        "   ╚═══════════\n"
        )

        // You would never really want to make everything a leaf child like this
        // but you can and still get a readable tree.
        // The joint between single and double lines here is a bit clunky
        // but we could correct that if there is any real use for this style.
        goldenStyleTestControl(
                "leaf",
                style = DiagnosticsTreeStyle.transition,
                golden =
                "TestTree#00000:\n" +
                        "  ╞═╦══ child node A ═══\n" +
                        "  │ ║ TestTree#00000\n" +
                        "  │ ╚═══════════\n" +
                        "  ╞═╦══ child node B ═══\n" +
                        "  │ ║ TestTree#00000:\n" +
                        "  │ ║   ╞═╦══ child node B1 ═══\n" +
                        "  │ ║   │ ║ TestTree#00000\n" +
                        "  │ ║   │ ╚═══════════\n" +
                        "  │ ║   ╞═╦══ child node B2 ═══\n" +
                        "  │ ║   │ ║ TestTree#00000\n" +
                        "  │ ║   │ ╚═══════════\n" +
                        "  │ ║   ╘═╦══ child node B3 ═══\n" +
                        "  │ ║     ║ TestTree#00000\n" +
                        "  │ ║     ╚═══════════\n" +
                        "  │ ╚═══════════\n" +
                        "  ╘═╦══ child node C ═══\n" +
                        "    ║ TestTree#00000\n" +
                        "    ╚═══════════\n"
        )

        goldenStyleTestControl(
                "whitespace",
                style = DiagnosticsTreeStyle.whitespace,
                golden =
                "TestTree#00000:\n" +
                        "  child node A: TestTree#00000\n" +
                        "  child node B: TestTree#00000:\n" +
                        "    child node B1: TestTree#00000\n" +
                        "    child node B2: TestTree#00000\n" +
                        "    child node B3: TestTree#00000\n" +
                        "  child node C: TestTree#00000\n"
        )

        // Single line mode does not display children.
        goldenStyleTestControl(
                "single line",
                style = DiagnosticsTreeStyle.singleLine,
                golden = "TestTree#00000"
        )
    }

    private fun goldenStyleTestProperties(
        description: String? = null,
        style: DiagnosticsTreeStyle? = null,
        lastChildStyle: DiagnosticsTreeStyle? = null,
        golden: String
    ) {
        val tree = TestTree(
                properties = listOf(
                        StringProperty("stringProperty1", "value1", quoted = false),
                        DoubleProperty.create("doubleProperty1", 42.5),
                        DoubleProperty.create("roundedProperty", 1.0 / 3.0),
                        StringProperty("DO_NOT_SHOW", "DO_NOT_SHOW", level = DiagnosticLevel.hidden,
                                quoted = false),
                        DiagnosticsProperty.create("DO_NOT_SHOW_NULL", null, defaultValue = null),
                        DiagnosticsProperty.create("nullProperty", null),
                        StringProperty("node_type", "<root node>", showName = false, quoted = false)
                ),
                children = listOf(
                        TestTree(name = "node A", style = style),
                        TestTree(
                                name = "node B",
                                properties = listOf(
                                        StringProperty("p1", "v1", quoted = false),
                                        StringProperty("p2", "v2", quoted = false)
                                ),
                                children = listOf(
                                        TestTree(name = "node B1", style = style),
                                        TestTree(
                                                name = "node B2",
                                                properties = listOf(
                                                        StringProperty("property1", "value1",
                                                                quoted = false)),
                                                style = style
                                        ),
                                        TestTree(
                                                name = "node B3",
                                                properties = listOf(
                                                        StringProperty("node_type", "<leaf node>",
                                                                showName = false, quoted = false),
                                                        IntProperty("foo", 42)
                                                ),
                                                style = lastChildStyle ?: style
                                        )
                                ),
                                style = style
                        ),
                        TestTree(
                                name = "node C",
                                properties = listOf(
                                        StringProperty("foo", "multi\nline\nvalue!", quoted = false)
                                ),
                                style = lastChildStyle ?: style
                        )
                ),
                style = lastChildStyle
        )

        if (tree.style != DiagnosticsTreeStyle.singleLine) {
            assertThat(tree, HasGoodToStringDeep)
        }

        assertThat(description,
                tree.toDiagnosticsNode(style = style).toStringDeep(),
                EqualsIgnoringHashCodes(golden))

        validateNodeJsonSerialization(tree.toDiagnosticsNode())
    }

    @Test
    fun `TreeDiagnosticsMixin tree with properties test`() {
        goldenStyleTestProperties(
                "sparse",
                style = DiagnosticsTreeStyle.sparse,
                golden =
                "TestTree#00000\n" +
                        " │ stringProperty1: value1\n" +
                        " │ doubleProperty1: 42.5\n" +
                        " │ roundedProperty: 0.3\n" +
                        " │ nullProperty: null\n" +
                        " │ <root node>\n" +
                        " │\n" +
                        " ├─child node A: TestTree#00000\n" +
                        " ├─child node B: TestTree#00000\n" +
                        " │ │ p1: v1\n" +
                        " │ │ p2: v2\n" +
                        " │ │\n" +
                        " │ ├─child node B1: TestTree#00000\n" +
                        " │ ├─child node B2: TestTree#00000\n" +
                        " │ │   property1: value1\n" +
                        " │ │\n" +
                        " │ └─child node B3: TestTree#00000\n" +
                        " │     <leaf node>\n" +
                        " │     foo: 42\n" +
                        " │\n" +
                        " └─child node C: TestTree#00000\n" +
                        "     foo:\n" +
                        "       multi\n" +
                        "       line\n" +
                        "       value!\n"
        )

        goldenStyleTestProperties(
                "dense",
                style = DiagnosticsTreeStyle.dense,
                golden =
                "TestTree#00000(stringProperty1: value1, doubleProperty1: 42.5, " +
                        "roundedProperty: 0.3, nullProperty: null, <root node>)\n" +
                        "├child node A: TestTree#00000\n" +
                        "├child node B: TestTree#00000(p1: v1, p2: v2)\n" +
                        "│├child node B1: TestTree#00000\n" +
                        "│├child node B2: TestTree#00000(property1: value1)\n" +
                        "│└child node B3: TestTree#00000(<leaf node>, foo: 42)\n" +
                        "└child node C: TestTree#00000(foo: multi\\nline\\nvalue!)\n"
        )

        goldenStyleTestProperties(
                "dashed",
                style = DiagnosticsTreeStyle.offstage,
                golden =
                "TestTree#00000\n" +
                        " │ stringProperty1: value1\n" +
                        " │ doubleProperty1: 42.5\n" +
                        " │ roundedProperty: 0.3\n" +
                        " │ nullProperty: null\n" +
                        " │ <root node>\n" +
                        " │\n" +
                        " ╎╌child node A: TestTree#00000\n" +
                        " ╎╌child node B: TestTree#00000\n" +
                        " ╎ │ p1: v1\n" +
                        " ╎ │ p2: v2\n" +
                        " ╎ │\n" +
                        " ╎ ╎╌child node B1: TestTree#00000\n" +
                        " ╎ ╎╌child node B2: TestTree#00000\n" +
                        " ╎ ╎   property1: value1\n" +
                        " ╎ ╎\n" +
                        " ╎ └╌child node B3: TestTree#00000\n" +
                        " ╎     <leaf node>\n" +
                        " ╎     foo: 42\n" +
                        " ╎\n" +
                        " └╌child node C: TestTree#00000\n" +
                        "     foo:\n" +
                        "       multi\n" +
                        "       line\n" +
                        "       value!\n"
        )

        goldenStyleTestProperties(
                "leaf children",
                style = DiagnosticsTreeStyle.sparse,
                lastChildStyle = DiagnosticsTreeStyle.transition,
                golden =
                "TestTree#00000\n" +
                        " │ stringProperty1: value1\n" +
                        " │ doubleProperty1: 42.5\n" +
                        " │ roundedProperty: 0.3\n" +
                        " │ nullProperty: null\n" +
                        " │ <root node>\n" +
                        " │\n" +
                        " ├─child node A: TestTree#00000\n" +
                        " ├─child node B: TestTree#00000\n" +
                        " │ │ p1: v1\n" +
                        " │ │ p2: v2\n" +
                        " │ │\n" +
                        " │ ├─child node B1: TestTree#00000\n" +
                        " │ ├─child node B2: TestTree#00000\n" +
                        " │ │   property1: value1\n" +
                        " │ │\n" +
                        " │ ╘═╦══ child node B3 ═══\n" +
                        " │   ║ TestTree#00000:\n" +
                        " │   ║   <leaf node>\n" +
                        " │   ║   foo: 42\n" +
                        " │   ╚═══════════\n" +
                        " ╘═╦══ child node C ═══\n" +
                        "   ║ TestTree#00000:\n" +
                        "   ║   foo:\n" +
                        "   ║     multi\n" +
                        "   ║     line\n" +
                        "   ║     value!\n" +
                        "   ╚═══════════\n"
        )

        // You would never really want to make everything a transition child like
        // this but you can and still get a readable tree.
        goldenStyleTestProperties(
                "transition",
                style = DiagnosticsTreeStyle.transition,
                golden =
                "TestTree#00000:\n" +
                        "  stringProperty1: value1\n" +
                        "  doubleProperty1: 42.5\n" +
                        "  roundedProperty: 0.3\n" +
                        "  nullProperty: null\n" +
                        "  <root node>\n" +
                        "  ╞═╦══ child node A ═══\n" +
                        "  │ ║ TestTree#00000\n" +
                        "  │ ╚═══════════\n" +
                        "  ╞═╦══ child node B ═══\n" +
                        "  │ ║ TestTree#00000:\n" +
                        "  │ ║   p1: v1\n" +
                        "  │ ║   p2: v2\n" +
                        "  │ ║   ╞═╦══ child node B1 ═══\n" +
                        "  │ ║   │ ║ TestTree#00000\n" +
                        "  │ ║   │ ╚═══════════\n" +
                        "  │ ║   ╞═╦══ child node B2 ═══\n" +
                        "  │ ║   │ ║ TestTree#00000:\n" +
                        "  │ ║   │ ║   property1: value1\n" +
                        "  │ ║   │ ╚═══════════\n" +
                        "  │ ║   ╘═╦══ child node B3 ═══\n" +
                        "  │ ║     ║ TestTree#00000:\n" +
                        "  │ ║     ║   <leaf node>\n" +
                        "  │ ║     ║   foo: 42\n" +
                        "  │ ║     ╚═══════════\n" +
                        "  │ ╚═══════════\n" +
                        "  ╘═╦══ child node C ═══\n" +
                        "    ║ TestTree#00000:\n" +
                        "    ║   foo:\n" +
                        "    ║     multi\n" +
                        "    ║     line\n" +
                        "    ║     value!\n" +
                        "    ╚═══════════\n"
        )

        goldenStyleTestProperties(
                "whitespace",
                style = DiagnosticsTreeStyle.whitespace,
                golden =
                "TestTree#00000:\n" +
                        "  stringProperty1: value1\n" +
                        "  doubleProperty1: 42.5\n" +
                        "  roundedProperty: 0.3\n" +
                        "  nullProperty: null\n" +
                        "  <root node>\n" +
                        "  child node A: TestTree#00000\n" +
                        "  child node B: TestTree#00000:\n" +
                        "    p1: v1\n" +
                        "    p2: v2\n" +
                        "    child node B1: TestTree#00000\n" +
                        "    child node B2: TestTree#00000:\n" +
                        "      property1: value1\n" +
                        "    child node B3: TestTree#00000:\n" +
                        "      <leaf node>\n" +
                        "      foo: 42\n" +
                        "  child node C: TestTree#00000:\n" +
                        "    foo:\n" +
                        "      multi\n" +
                        "      line\n" +
                        "      value!\n"
        )

        // Single line mode does not display children.
        goldenStyleTestProperties(
                "single line",
                style = DiagnosticsTreeStyle.singleLine,
                golden = "TestTree#00000(stringProperty1: value1, doubleProperty1: 42.5, " +
                        "roundedProperty: 0.3, nullProperty: null, <root node>)"
        )

        // There isn"t anything interesting for this case as the children look the
        // same with and without children. TODO(jacobr): this is an ugly test case.
        // only difference is odd not clearly desirable density of B3 being right
        // next to node C.
        goldenStyleTestProperties(
                "single line last child",
                style = DiagnosticsTreeStyle.sparse,
                lastChildStyle = DiagnosticsTreeStyle.singleLine,
                golden =
                "TestTree#00000\n" +
                        " │ stringProperty1: value1\n" +
                        " │ doubleProperty1: 42.5\n" +
                        " │ roundedProperty: 0.3\n" +
                        " │ nullProperty: null\n" +
                        " │ <root node>\n" +
                        " │\n" +
                        " ├─child node A: TestTree#00000\n" +
                        " ├─child node B: TestTree#00000\n" +
                        " │ │ p1: v1\n" +
                        " │ │ p2: v2\n" +
                        " │ │\n" +
                        " │ ├─child node B1: TestTree#00000\n" +
                        " │ ├─child node B2: TestTree#00000\n" +
                        " │ │   property1: value1\n" +
                        " │ │\n" +
                        " │ └─child node B3: TestTree#00000(<leaf node>, foo: 42)\n" +
                        " └─child node C: TestTree#00000(foo: multi\\nline\\nvalue!)\n"
        )
    }

    @Test
    fun `transition test`() {
        // Test multiple styles integrating together in the same tree due to using
        // transition to go between styles that would otherwise be incompatible.
        val tree = TestTree(
                style = DiagnosticsTreeStyle.sparse,
                properties = listOf(
                        StringProperty("stringProperty1", "value1")
                ),
                children = listOf(
                        TestTree(
                                style = DiagnosticsTreeStyle.transition,
                                name = "node transition",
                                properties = listOf(
                                        StringProperty("p1", "v1"),
                                        TestTree(
                                                properties = listOf(
                                                        DiagnosticsProperty.create("survived", true)
                                                )
                                        ).toDiagnosticsNode(name = "tree property",
                                                style = DiagnosticsTreeStyle.whitespace)
                                ),
                                children = listOf(
                                        TestTree(name = "dense child",
                                                style = DiagnosticsTreeStyle.dense),
                                        TestTree(
                                                name = "dense",
                                                properties = listOf(
                                                        StringProperty("property1", "value1")),
                                                style = DiagnosticsTreeStyle.dense
                                        ),
                                        TestTree(
                                                name = "node B3",
                                                properties = listOf(
                                                        StringProperty("node_type", "<leaf node>",
                                                                showName = false, quoted = false),
                                                        IntProperty("foo", 42)
                                                ),
                                                style = DiagnosticsTreeStyle.dense
                                        )
                                )
                        ),
                        TestTree(
                                name = "node C",
                                properties = listOf(
                                        StringProperty("foo", "multi\nline\nvalue!", quoted = false)
                                ),
                                style = DiagnosticsTreeStyle.sparse
                        )
                )
        )

        assertThat(tree, HasGoodToStringDeep)
        assertThat(tree.toDiagnosticsNode().toStringDeep(),
                EqualsIgnoringHashCodes("TestTree#00000\n" +
                        " │ stringProperty1: \"value1\"\n" +
                        " ╞═╦══ child node transition ═══\n" +
                        " │ ║ TestTree#00000:\n" +
                        " │ ║   p1: \"v1\"\n" +
                        " │ ║   tree property: TestTree#00000:\n" +
                        " │ ║     survived: true\n" +
                        " │ ║   ├child dense child: TestTree#00000\n" +
                        " │ ║   ├child dense: TestTree#00000(property1: \"value1\")\n" +
                        " │ ║   └child node B3: TestTree#00000(<leaf node>, foo: 42)\n" +
                        " │ ╚═══════════\n" +
                        " └─child node C: TestTree#00000\n" +
                        "     foo:\n" +
                        "       multi\n" +
                        "       line\n" +
                        "       value!\n"
                ))
    }

    @Test
    fun `describeEnum test`() {
        assertEquals("hello", describeEnum(ExampleEnum.hello))
        assertEquals("world", describeEnum(ExampleEnum.world))
        assertEquals("deferToChild", describeEnum(ExampleEnum.deferToChild))
    }

    @Test
    fun `string property test`() {
        assertEquals(
                StringProperty("name", "value", quoted = false).toString(),
                "name: value"
        )

        val stringProperty = StringProperty(
                "name",
                "value",
                description = "VALUE",
                ifEmpty = "<hidden>",
                quoted = false
        )
        assertEquals("name: VALUE", stringProperty.toString())
        validateStringPropertyJsonSerialization(stringProperty)

        assertEquals(
                StringProperty(
                        "name",
                        "value",
                        showName = false,
                        ifEmpty = "<hidden>",
                        quoted = false
                ).toString(),
                "value"
        )

        assertEquals(
                StringProperty("name", "", ifEmpty = "<hidden>").toString(),
                "name: <hidden>"
        )

        assertEquals(
                StringProperty(
                        "name",
                        "",
                        ifEmpty = "<hidden>",
                        showName = false
                ).toString(),
                "<hidden>"
        )

        assertFalse(StringProperty("name", null).isFiltered(DiagnosticLevel.info))
        assertTrue(StringProperty("name", "value", level = DiagnosticLevel.hidden).isFiltered(
                DiagnosticLevel.info))
        assertTrue(
                StringProperty("name", null, defaultValue = null).isFiltered(DiagnosticLevel.info))
        val quoted = StringProperty(
                "name",
                "value",
                quoted = true
        )
        assertEquals("name: \"value\"", quoted.toString())
        validateStringPropertyJsonSerialization(quoted)

        assertEquals(
                StringProperty("name", "value", showName = false).toString(),
                "\"value\""
        )

        assertEquals(
                StringProperty(
                        "name",
                        null,
                        showName = false,
                        quoted = true
                ).toString(),
                "null"
        )
    }

    @Test
    fun `bool property test`() {
        val trueProperty = DiagnosticsProperty.create("name", true)
        val falseProperty = DiagnosticsProperty.create("name", false)
        assertEquals("name: true", trueProperty.toString())
        assertFalse(trueProperty.isFiltered(DiagnosticLevel.info))
        assertTrue(trueProperty.getValue() as Boolean)
        assertEquals("name: false", falseProperty.toString())
        assertFalse(falseProperty.getValue() as Boolean)
        assertFalse(falseProperty.isFiltered(DiagnosticLevel.info))
        validatePropertyJsonSerialization(trueProperty)
        validatePropertyJsonSerialization(falseProperty)
        val truthyProperty = DiagnosticsProperty.create(
                "name",
                true,
                description = "truthy"
        )
        assertEquals(
                truthyProperty.toString(),
                "name: truthy"
        )
        validatePropertyJsonSerialization(truthyProperty)
        assertEquals(
                DiagnosticsProperty.create("name", true, showName = false).toString(),
                "true"
        )

        assertFalse(DiagnosticsProperty.create("name", null).isFiltered(DiagnosticLevel.info))
        assertTrue(
                DiagnosticsProperty.create("name", true, level = DiagnosticLevel.hidden).isFiltered(
                        DiagnosticLevel.info))
        assertTrue(DiagnosticsProperty.create("name", null, defaultValue = null).isFiltered(
                DiagnosticLevel.info))
        val missingBool = DiagnosticsProperty.create("name", null, ifNull = "missing")
        assertEquals(
                missingBool.toString(),
                "name: missing")

        validatePropertyJsonSerialization(missingBool)
    }

    @Test
    fun `flag property test`() {
        val trueFlag = FlagProperty(
                "myFlag",
                value = true,
                ifTrue = "myFlag"
        )
        val falseFlag = FlagProperty(
                "myFlag",
                value = false,
                ifTrue = "myFlag"
        )
        assertEquals("myFlag", trueFlag.toString())
        validateFlagPropertyJsonSerialization(trueFlag)
        validateFlagPropertyJsonSerialization(falseFlag)

        assertTrue(trueFlag.getValue() as Boolean)
        assertFalse(falseFlag.getValue() as Boolean)

        assertFalse(trueFlag.isFiltered(DiagnosticLevel.fine))
        assertTrue(falseFlag.isFiltered(DiagnosticLevel.fine))
    }

    @Test
    fun `property with tooltip test`() {
        val withTooltip = DiagnosticsProperty.create(
                "name",
                "value",
                tooltip = "tooltip"
        )
        assertEquals(
                withTooltip.toString(),
                "name: value (tooltip)"
        )
        assertEquals("value", withTooltip.getValue())
        assertFalse(withTooltip.isFiltered(DiagnosticLevel.fine))
        validatePropertyJsonSerialization(withTooltip)
    }

    @Test
    fun `double property test`() {
        val doubleProperty = DoubleProperty.create(
                "name",
                42.0
        )
        assertEquals("name: 42.0", doubleProperty.toString())
        assertFalse(doubleProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(42.0, doubleProperty.getValue())
        validateDoublePropertyJsonSerialization(doubleProperty)

        assertEquals("name: 1.3", DoubleProperty.create("name", 1.3333).toString())

        assertEquals("name: null", DoubleProperty.create("name", null).toString())
        assertEquals(false, DoubleProperty.create("name", null).isFiltered(DiagnosticLevel.info))

        assertEquals(
                DoubleProperty.create("name", null, ifNull = "missing").toString(),
                "name: missing"
        )

        val doubleWithUnit = DoubleProperty.create("name", 42.0, unit = "px")
        assertEquals("name: 42.0px", doubleWithUnit.toString())
        validateDoublePropertyJsonSerialization(doubleWithUnit)
    }

    @Test
    fun `unsafe double property test`() {
        val safe = DoubleProperty.createLazy(
                "name",
                { 42.0 }
        )
        assertEquals("name: 42.0", safe.toString())
        assertFalse(safe.isFiltered(DiagnosticLevel.info))
        assertEquals(42.0, safe.getValue())
        validateDoublePropertyJsonSerialization(safe)
        assertEquals(
                DoubleProperty.createLazy("name", { 1.3333 }).toString(),
                "name: 1.3"
        )

        assertEquals(
                DoubleProperty.createLazy("name", { null }).toString(),
                "name: null"
        )
        assertEquals(
                DoubleProperty.createLazy("name", { null }).isFiltered(DiagnosticLevel.info),
                false
        )

        val throwingProperty = DoubleProperty.createLazy(
                "name",
                { throw FlutterError("Invalid constraints") }
        )
        // TODO(jacobr): it would be better if throwingProperty.Any threw an
        // exception.
        assertNull(throwingProperty.getValue())
        assertFalse(throwingProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(
                throwingProperty.toString(),
                "name: EXCEPTION (FlutterError)"
        )
        assertEquals(DiagnosticLevel.error, throwingProperty.getLevel())
        validateDoublePropertyJsonSerialization(throwingProperty)
    }

    @Test
    fun `percent property`() {
        assertEquals(
                PercentProperty("name", 0.4).toString(),
                "name: 40.0%"
        )

        val complexPercentProperty = PercentProperty("name", 0.99, unit = "invisible",
                tooltip = "almost transparent")
        assertEquals(
                complexPercentProperty.toString(),
                "name: 99.0% invisible (almost transparent)"
        )
        validateDoublePropertyJsonSerialization(complexPercentProperty)

        assertEquals(
                PercentProperty("name", null, unit = "invisible", tooltip = "!").toString(),
                "name: null (!)"
        )

        assertEquals(
                PercentProperty("name", 0.4).getValue(),
                0.4
        )
        assertEquals(
                PercentProperty("name", 0.0).toString(),
                "name: 0.0%"
        )
        assertEquals(
                PercentProperty("name", -10.0).toString(),
                "name: 0.0%"
        )
        assertEquals(
                PercentProperty("name", 1.0).toString(),
                "name: 100.0%"
        )
        assertEquals(
                PercentProperty("name", 3.0).toString(),
                "name: 100.0%"
        )
        assertEquals(
                PercentProperty("name", null).toString(),
                "name: null"
        )
        assertEquals(
                PercentProperty(
                        "name",
                        null,
                        ifNull = "missing"
                ).toString(),
                "name: missing"
        )
        assertEquals(
                PercentProperty(
                        "name",
                        null,
                        ifNull = "missing",
                        showName = false
                ).toString(),
                "missing"
        )
        assertEquals(
                PercentProperty(
                        "name",
                        0.5,
                        showName = false
                ).toString(),
                "50.0%"
        )
    }

    @Test
    fun `callback property test`() {
        val onClick = {}
        val present = ObjectFlagProperty(
                "onClick",
                onClick,
                ifPresent = "clickable"
        )
        val missing = ObjectFlagProperty(
                "onClick",
                null,
                ifPresent = "clickable"
        )

        assertEquals("clickable", present.toString())
        assertFalse(present.isFiltered(DiagnosticLevel.info))
        assertEquals(onClick, present.getValue())
        validateObjectFlagPropertyJsonSerialization(present)
        assertEquals("onClick: null", missing.toString())
        assertTrue(missing.isFiltered(DiagnosticLevel.fine))
        validateObjectFlagPropertyJsonSerialization(missing)
    }

    @Test
    fun `missing callback property test`() {
        val onClick = { }

        val present = ObjectFlagProperty(
                "onClick",
                onClick,
                ifNull = "disabled"
        )
        val missing = ObjectFlagProperty(
                "onClick",
                null,
                ifNull = "disabled"
        )

        assertEquals("onClick: Function0<kotlin.Unit>", present.toString())
        assertTrue(present.isFiltered(DiagnosticLevel.fine))
        assertEquals(onClick, present.getValue())
        assertEquals("disabled", missing.toString())
        assertFalse(missing.isFiltered(DiagnosticLevel.info))
        validateObjectFlagPropertyJsonSerialization(present)
        validateObjectFlagPropertyJsonSerialization(missing)
    }

    @Test
    fun `describe bool property`() {
        val yes = FlagProperty(
                "name",
                value = true,
                ifTrue = "YES",
                ifFalse = "NO",
                showName = true
        )
        val no = FlagProperty(
                "name",
                value = false,
                ifTrue = "YES",
                ifFalse = "NO",
                showName = true
        )
        assertEquals("name: YES", yes.toString())
        assertEquals(DiagnosticLevel.info, yes.getLevel())
        assertTrue(yes.getValue() as Boolean)
        validateFlagPropertyJsonSerialization(yes)
        assertEquals("name: NO", no.toString())
        assertEquals(DiagnosticLevel.info, no.getLevel())
        assertFalse(no.getValue() as Boolean)
        validateFlagPropertyJsonSerialization(no)

        assertEquals(
                FlagProperty(
                        "name",
                        value = true,
                        ifTrue = "YES",
                        ifFalse = "NO"
                ).toString(),
                "YES"
        )

        assertEquals(
                FlagProperty(
                        "name",
                        value = false,
                        ifTrue = "YES",
                        ifFalse = "NO"
                ).toString(),
                "NO"
        )

        assertEquals(
                FlagProperty(
                        "name",
                        value = true,
                        ifTrue = "YES",
                        ifFalse = "NO",
                        level = DiagnosticLevel.hidden,
                        showName = true
                ).getLevel(),
                DiagnosticLevel.hidden
        )
    }

    @Test
    fun `enum property test`() {
        val hello = EnumProperty(
                "name",
                ExampleEnum.hello
        )
        val world = EnumProperty(
                "name",
                ExampleEnum.world
        )
        val deferToChild = EnumProperty(
                "name",
                ExampleEnum.deferToChild
        )
        val nullEnum = EnumProperty<ExampleEnum>(
                "name",
                null
        )
        assertEquals(DiagnosticLevel.info, hello.getLevel())
        assertEquals(ExampleEnum.hello, hello.getValue())
        assertEquals("name: hello", hello.toString())
        validatePropertyJsonSerialization(hello)

        assertEquals(DiagnosticLevel.info, world.getLevel())
        assertEquals(ExampleEnum.world, world.getValue())
        assertEquals("name: world", world.toString())
        validatePropertyJsonSerialization(world)

        assertEquals(DiagnosticLevel.info, deferToChild.getLevel())
        assertEquals(ExampleEnum.deferToChild, deferToChild.getValue())
        assertEquals("name: deferToChild", deferToChild.toString())
        validatePropertyJsonSerialization(deferToChild)

        assertEquals(DiagnosticLevel.info, nullEnum.getLevel())
        assertNull(nullEnum.getValue())
        assertEquals("name: null", nullEnum.toString())
        validatePropertyJsonSerialization(nullEnum)

        val matchesDefault = EnumProperty(
                "name",
                ExampleEnum.hello,
                defaultValue = ExampleEnum.hello
        )
        assertEquals("name: hello", matchesDefault.toString())
        assertEquals(ExampleEnum.hello, matchesDefault.getValue())
        assertTrue(matchesDefault.isFiltered(DiagnosticLevel.info))
        validatePropertyJsonSerialization(matchesDefault)

        assertEquals(
                EnumProperty(
                        "name",
                        ExampleEnum.hello,
                        level = DiagnosticLevel.hidden
                ).getLevel(),
                DiagnosticLevel.hidden
        )
    }

    @Test
    fun `int property test`() {
        val regular = IntProperty(
                "name",
                42
        )
        assertEquals("name: 42", regular.toString())
        assertEquals(42, regular.getValue())
        assertEquals(DiagnosticLevel.info, regular.getLevel())

        val nullValue = IntProperty(
                "name",
                null
        )
        assertEquals("name: null", nullValue.toString())
        assertNull(nullValue.getValue())
        assertEquals(DiagnosticLevel.info, nullValue.getLevel())

        val hideNull = IntProperty(
                "name",
                null,
                defaultValue = null
        )
        assertEquals("name: null", hideNull.toString())
        assertNull(hideNull.getValue())
        assertTrue(hideNull.isFiltered(DiagnosticLevel.info))

        val nullDescription = IntProperty(
                "name",
                null,
                ifNull = "missing"
        )
        assertEquals("name: missing", nullDescription.toString())
        assertNull(nullDescription.getValue())
        assertEquals(DiagnosticLevel.info, nullDescription.getLevel())

        val hideName = IntProperty(
                "name",
                42,
                showName = false
        )
        assertEquals("42", hideName.toString())
        assertEquals(42, hideName.getValue())
        assertEquals(DiagnosticLevel.info, hideName.getLevel())

        val withUnit = IntProperty(
                "name",
                42,
                unit = "pt"
        )
        assertEquals("name: 42pt", withUnit.toString())
        assertEquals(42, withUnit.getValue())
        assertEquals(DiagnosticLevel.info, withUnit.getLevel())

        val defaultValue = IntProperty(
                "name",
                42,
                defaultValue = 42
        )
        assertEquals("name: 42", defaultValue.toString())
        assertEquals(42, defaultValue.getValue())
        assertTrue(defaultValue.isFiltered(DiagnosticLevel.info))

        val notDefaultValue = IntProperty(
                "name",
                43,
                defaultValue = 42
        )
        assertEquals("name: 43", notDefaultValue.toString())
        assertEquals(43, notDefaultValue.getValue())
        assertEquals(DiagnosticLevel.info, notDefaultValue.getLevel())

        val hidden = IntProperty(
                "name",
                42,
                level = DiagnosticLevel.hidden
        )
        assertEquals("name: 42", hidden.toString())
        assertEquals(42, hidden.getValue())
        assertEquals(DiagnosticLevel.hidden, hidden.getLevel())
    }

    @Test
    fun `Any property test`() {
        val rect = Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)
        val simple = DiagnosticsProperty.create(
                "name",
                rect
        )
        assertEquals(rect, simple.getValue())
        assertEquals(DiagnosticLevel.info, simple.getLevel())
        assertEquals(simple.toString(), "name: Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)")
        validatePropertyJsonSerialization(simple)

        val withDescription = DiagnosticsProperty.create(
                "name",
                rect,
                description = "small rect"
        )
        assertEquals(rect, withDescription.getValue())
        assertEquals(DiagnosticLevel.info, withDescription.getLevel())
        assertEquals("name: small rect", withDescription.toString())
        validatePropertyJsonSerialization(withDescription)

        val nullProperty = DiagnosticsProperty.create(
                "name",
                null
        )
        assertNull(nullProperty.getValue())
        assertEquals(DiagnosticLevel.info, nullProperty.getLevel())
        assertEquals("name: null", nullProperty.toString())
        validatePropertyJsonSerialization(nullProperty)

        val hideNullProperty = DiagnosticsProperty.create(
                "name",
                null,
                defaultValue = null
        )
        assertNull(hideNullProperty.getValue())
        assertTrue(hideNullProperty.isFiltered(DiagnosticLevel.info))
        assertEquals("name: null", hideNullProperty.toString())
        validatePropertyJsonSerialization(hideNullProperty)

        val nullDescription = DiagnosticsProperty.create(
                "name",
                null,
                ifNull = "missing"
        )
        assertNull(nullDescription.getValue())
        assertEquals(DiagnosticLevel.info, nullDescription.getLevel())
        assertEquals("name: missing", nullDescription.toString())
        validatePropertyJsonSerialization(nullDescription)

        val hideName = DiagnosticsProperty.create(
                "name",
                rect,
                showName = false,
                level = DiagnosticLevel.warning
        )
        assertEquals(rect, hideName.getValue())
        assertEquals(DiagnosticLevel.warning, hideName.getLevel())
        assertEquals(hideName.toString(), "Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)")
        validatePropertyJsonSerialization(hideName)

        val hideSeparator = DiagnosticsProperty.create(
                "Creator",
                rect,
                showSeparator = false
        )
        assertEquals(rect, hideSeparator.getValue())
        assertEquals(DiagnosticLevel.info, hideSeparator.getLevel())
        assertEquals(
                hideSeparator.toString(),
                "Creator Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)"
        )
        validatePropertyJsonSerialization(hideSeparator)
    }

    @Test
    fun `lazy Any property test`() {
        val rect = Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)
        val simple = DiagnosticsProperty.createLazy(
                "name",
                { rect },
                description = "small rect"
        )
        assertEquals(rect, simple.getValue())
        assertEquals(DiagnosticLevel.info, simple.getLevel())
        assertEquals("name: small rect", simple.toString())
        validatePropertyJsonSerialization(simple)

        val nullProperty = DiagnosticsProperty.createLazy(
                "name",
                { null },
                description = "missing"
        )
        assertNull(nullProperty.getValue())
        assertFalse(nullProperty.isFiltered(DiagnosticLevel.info))
        assertEquals("name: missing", nullProperty.toString())
        validatePropertyJsonSerialization(nullProperty)

        val hideNullProperty = DiagnosticsProperty.createLazy(
                "name",
                { null },
                description = "missing",
                defaultValue = null
        )
        assertNull(hideNullProperty.getValue())
        assertTrue(hideNullProperty.isFiltered(DiagnosticLevel.info))
        assertEquals("name: missing", hideNullProperty.toString())
        validatePropertyJsonSerialization(hideNullProperty)

        val hideName = DiagnosticsProperty.createLazy(
                "name",
                { rect },
                description = "small rect",
                showName = false
        )
        assertEquals(rect, hideName.getValue())
        assertFalse(hideName.isFiltered(DiagnosticLevel.info))
        assertEquals("small rect", hideName.toString())
        validatePropertyJsonSerialization(hideName)

        val throwingWithDescription = DiagnosticsProperty.createLazy(
                "name",
                { throw FlutterError("Property not available") },
                description = "missing",
                defaultValue = null
        )
        assertNull(throwingWithDescription.getValue())
        assertTrue(throwingWithDescription.getException() is FlutterError)
        assertEquals(false, throwingWithDescription.isFiltered(DiagnosticLevel.info))
        assertEquals("name: missing", throwingWithDescription.toString())
        validatePropertyJsonSerialization(throwingWithDescription)

        val throwingProperty = DiagnosticsProperty.createLazy(
                "name",
                { throw FlutterError("Property not available") },
                defaultValue = null
        )
        assertNull(throwingProperty.getValue())
        assertTrue(throwingProperty.getException() is FlutterError)
        assertEquals(false, throwingProperty.isFiltered(DiagnosticLevel.info))
        assertEquals("name: EXCEPTION (FlutterError)", throwingProperty.toString())
        validatePropertyJsonSerialization(throwingProperty)
    }

    @Test
    fun `color property test`() {
        // Add more tests if colorProperty becomes more than a wrapper around
        // objectProperty.
        val color = Color.fromARGB(255, 255, 255, 255)
        val simple = DiagnosticsProperty.create(
                "name",
                color
        )
        validatePropertyJsonSerialization(simple)
        assertFalse(simple.isFiltered(DiagnosticLevel.info))
        assertEquals(color, simple.getValue())
        assertEquals("name: Color(0xffffffff)", simple.toString())
        validatePropertyJsonSerialization(simple)
    }

    @Test
    fun `flag property test2`() {
        val show = FlagProperty(
                "wasLayout",
                value = true,
                ifTrue = "layout computed"
        )
        assertEquals("wasLayout", show.name)
        assertTrue(show.getValue() as Boolean)
        assertFalse(show.isFiltered(DiagnosticLevel.info))
        assertEquals("layout computed", show.toString())
        validateFlagPropertyJsonSerialization(show)

        val hide = FlagProperty(
                "wasLayout",
                value = false,
                ifTrue = "layout computed"
        )
        assertEquals("wasLayout", hide.name)
        assertFalse(hide.getValue() as Boolean)
        assertEquals(DiagnosticLevel.hidden, hide.getLevel())
        assertEquals("wasLayout: false", hide.toString())
        validateFlagPropertyJsonSerialization(hide)

        val hideTrue = FlagProperty(
                "wasLayout",
                value = true,
                ifFalse = "no layout computed"
        )
        assertEquals("wasLayout", hideTrue.name)
        assertTrue(hideTrue.getValue() as Boolean)
        assertEquals(DiagnosticLevel.hidden, hideTrue.getLevel())
        assertEquals("wasLayout: true", hideTrue.toString())
        validateFlagPropertyJsonSerialization(hideTrue)
    }

    @Test
    fun `has property test`() {
        val onClick = {}
        val has = ObjectFlagProperty.has("onClick", onClick)
        assertEquals("onClick", has.name)
        assertEquals(onClick, has.getValue())
        assertFalse(has.isFiltered(DiagnosticLevel.info))
        assertEquals("has onClick", has.toString())
        validateObjectFlagPropertyJsonSerialization(has)

        val missing = ObjectFlagProperty.has("onClick", null)
        assertEquals("onClick", missing.name)
        assertNull(missing.getValue())
        assertTrue(missing.isFiltered(DiagnosticLevel.info))
        assertEquals("onClick: null", missing.toString())
        validateObjectFlagPropertyJsonSerialization(missing)
    }

    @Test
    fun `iterable property test`() {
        val ints = listOf(1, 2, 3)
        val intsProperty = IterableProperty(
                "ints",
                ints
        )
        assertEquals(ints, intsProperty.getValue())
        assertFalse(intsProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(intsProperty.toString(), "ints: 1, 2, 3")

        val emptyProperty = IterableProperty<Any>(
                "name",
                listOf()
        )
        assertTrue(emptyProperty.getValue()!!.toList().isEmpty())
        assertFalse(emptyProperty.isFiltered(DiagnosticLevel.info))
        assertEquals("name: []", emptyProperty.toString())
        validateIterablePropertyJsonSerialization(emptyProperty)

        val nullProperty = IterableProperty<Any>(
                "list",
                null
        )
        assertNull(nullProperty.getValue())
        assertFalse(nullProperty.isFiltered(DiagnosticLevel.info))
        assertEquals("list: null", nullProperty.toString())
        validateIterablePropertyJsonSerialization(nullProperty)

        val hideNullProperty = IterableProperty<Any>(
                "list",
                null,
                defaultValue = null
        )
        assertNull(hideNullProperty.getValue())
        assertTrue(hideNullProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(DiagnosticLevel.fine, hideNullProperty.getLevel())
        assertEquals("list: null", hideNullProperty.toString())
        validateIterablePropertyJsonSerialization(hideNullProperty)

        // TODO(Migration/Andrey): Replaced Color class usages with ColorInt
        val objects = listOf(
                Rect.fromLTRB(0.0, 0.0, 20.0, 20.0),
                Color.fromARGB(255, 255, 255, 255)
        )
        val objectsProperty = IterableProperty(
                "Objects",
                objects
        )
        assertEquals(objects, objectsProperty.getValue())
        assertFalse(objectsProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(
                objectsProperty.toString(),
                "Objects: Rect.fromLTRB(0.0, 0.0, 20.0, 20.0), Color(0xffffffff)"
        )
        validateIterablePropertyJsonSerialization(objectsProperty)

        val multiLineProperty = IterableProperty(
                "Objects",
                objects,
                style = DiagnosticsTreeStyle.whitespace
        )
        assertEquals(objects, multiLineProperty.getValue())
        assertFalse(multiLineProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(
                multiLineProperty.toString(),

                "Objects:\n" +
                        "Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)\n" +
                        "Color(0xffffffff)"

        )
        assertEquals(
                multiLineProperty.toStringDeep(),

                "Objects:\n" +
                        "  Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)\n" +
                        "  Color(0xffffffff)\n"

        )
        validateIterablePropertyJsonSerialization(multiLineProperty)

        assertThat(TestTree(properties = listOf(multiLineProperty)).toStringDeep(),
                EqualsIgnoringHashCodes("TestTree#00000\n" +
                        "   Objects:\n" +
                        "     Rect.fromLTRB(0.0, 0.0, 20.0, 20.0)\n" +
                        "     Color(0xffffffff)\n"))

        assertThat(TestTree(
                properties = listOf(objectsProperty, IntProperty("foo", 42)),
                style = DiagnosticsTreeStyle.singleLine
        ).toStringDeep(),
                EqualsIgnoringHashCodes("TestTree#00000(Objects: " +
                        "[Rect.fromLTRB(0.0, 0.0, 20.0, 20.0), " +
                        "Color(0xffffffff)], foo: 42)"))

        // Iterable with a single entry. Verify that rendering is sensible and that
        // multi line rendering isn"t used even though it is not helpful.
        val singleElementList = listOf<Any>(
                Color.fromARGB(255, 255, 255, 255))

        val objectProperty = IterableProperty(
                "Object",
                singleElementList,
                style = DiagnosticsTreeStyle.whitespace
        )
        assertEquals(singleElementList, objectProperty.getValue())
        assertFalse(objectProperty.isFiltered(DiagnosticLevel.info))
        assertEquals(
                objectProperty.toString(),
                "Object: Color(0xffffffff)"
        )
        assertEquals(
                objectProperty.toStringDeep(),
                "Object: Color(0xffffffff)\n"
        )
        validateIterablePropertyJsonSerialization(objectProperty)
        assertThat(TestTree(
                name = "root",
                properties = listOf(objectProperty)
        ).toStringDeep(),
                EqualsIgnoringHashCodes("TestTree#00000\n   Object: Color(0xffffffff)\n"))
    }

    @Test
    fun `message test`() {
        val message = DiagnosticsNode.message("hello world")
        assertEquals("hello world", message.toString())
        assertTrue(message.name!!.isEmpty())
        assertNull(message.getValue())
        assertFalse(message.getShowName())
        validateNodeJsonSerialization(message)

        val messageProperty = MessageProperty("diagnostics", "hello world")
        assertEquals("diagnostics: hello world", messageProperty.toString())
        assertEquals("diagnostics", messageProperty.name)
        assertNull(messageProperty.getValue())
        assertTrue(messageProperty.getShowName())
        validatePropertyJsonSerialization(messageProperty)
    }
}
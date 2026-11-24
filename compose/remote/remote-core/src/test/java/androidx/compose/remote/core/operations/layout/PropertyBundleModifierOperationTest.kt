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

package androidx.compose.remote.core.operations.layout

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.remote.core.serialize.MapSerializer
import androidx.compose.remote.core.serialize.Serializable
import androidx.compose.remote.core.serialize.SerializeTags
import androidx.compose.remote.serialization.yaml.YAMLSerializer
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PropertyBundleModifierOperationTest {

    @Test
    fun `constructor stores bundleIds and maps propertyIds to values`() {
        val op =
            PropertyBundleModifierOperation(
                /* bundleId = */ 11,
                /* parentBundleId = */ 22,
                /* propertyIds = */ intArrayOf(15, 20),
                /* values = */ intArrayOf(1, 0),
            )

        assertThat(op.bundleId).isEqualTo(11)
        assertThat(op.parentBundleId).isEqualTo(22)
        assertThat(op.hasProperty(15)).isTrue()
        assertThat(op.hasProperty(20)).isTrue()
        assertThat(op.hasProperty(99)).isFalse()
        assertThat(op.getValue(15)).isEqualTo(1)
        assertThat(op.getValue(20)).isEqualTo(0)
        assertThat(op.getValue(99)).isNull()
        assertThat(op.getValue(15, -1)).isEqualTo(1)
        assertThat(op.getValue(20, -1)).isEqualTo(0)
        assertThat(op.getValue(99, -1)).isEqualTo(-1)
    }

    @Test
    fun `constructor defensively copies input arrays`() {
        val propertyIds = intArrayOf(15, 20)
        val values = intArrayOf(1, 2)
        val op = PropertyBundleModifierOperation(0, 0, propertyIds, values)

        propertyIds[0] = 999
        values[0] = -999

        assertThat(op.hasProperty(15)).isTrue()
        assertThat(op.hasProperty(999)).isFalse()
        assertThat(op.getValue(15)).isEqualTo(1)
    }

    @Test
    fun `constructor overwrites earlier value when duplicate propertyId is provided`() {
        val op =
            PropertyBundleModifierOperation(
                /* bundleId = */ 1,
                /* parentBundleId = */ 2,
                /* propertyIds = */ intArrayOf(15, 20, 15),
                /* values = */ intArrayOf(10, 20, 30),
            )

        assertThat(op.hasProperty(15)).isTrue()
        assertThat(op.getValue(15)).isEqualTo(30)
        assertThat(op.getValue(20)).isEqualTo(20)
    }

    @Test
    fun `constructor accepts empty parallel arrays`() {
        val op = PropertyBundleModifierOperation(3, 4, intArrayOf(), intArrayOf())

        assertThat(op.bundleId).isEqualTo(3)
        assertThat(op.parentBundleId).isEqualTo(4)
        assertThat(op.hasProperty(0)).isFalse()
        assertThat(op.getValue(0)).isNull()
        assertThat(op.getValue(0, 42)).isEqualTo(42)
    }

    @Test
    fun `constructor throws when parallel arrays have mismatched lengths`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PropertyBundleModifierOperation(0, 0, intArrayOf(15, 20), intArrayOf(1))
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Parallel arrays must have identical lengths")
    }

    @Test
    fun `constructor accepts MAX_PROPERTY_BUNDLE_ENTRIES and throws when exceeded`() {
        val maxIds = IntArray(Limits.MAX_PROPERTY_BUNDLE_ENTRIES) { it }
        val maxValues = IntArray(Limits.MAX_PROPERTY_BUNDLE_ENTRIES) { it * 10 }
        val maxOp = PropertyBundleModifierOperation(0, 0, maxIds, maxValues)
        assertThat(maxOp.getValue(Limits.MAX_PROPERTY_BUNDLE_ENTRIES - 1))
            .isEqualTo((Limits.MAX_PROPERTY_BUNDLE_ENTRIES - 1) * 10)

        val tooManyIds = IntArray(Limits.MAX_PROPERTY_BUNDLE_ENTRIES + 1)
        val tooManyValues = IntArray(Limits.MAX_PROPERTY_BUNDLE_ENTRIES + 1)
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PropertyBundleModifierOperation(0, 0, tooManyIds, tooManyValues)
            }
        assertThat(exception).hasMessageThat().contains("Too many property bundle entries")
    }

    @Test
    fun `apply throws when parallel arrays have mismatched lengths or exceed limit`() {
        val buffer = WireBuffer()

        val mismatchException =
            assertFailsWith<IllegalArgumentException> {
                PropertyBundleModifierOperation.apply(
                    buffer,
                    0,
                    0,
                    intArrayOf(1),
                    intArrayOf(1, 2),
                )
            }
        assertThat(mismatchException)
            .hasMessageThat()
            .contains("Parallel arrays must have identical lengths")

        val tooMany = IntArray(Limits.MAX_PROPERTY_BUNDLE_ENTRIES + 1)
        val limitException =
            assertFailsWith<IllegalArgumentException> {
                PropertyBundleModifierOperation.apply(buffer, 0, 0, tooMany, tooMany)
            }
        assertThat(limitException).hasMessageThat().contains("Too many property bundle entries")
    }

    @Test
    fun `write and read round-trip preserves fields`() {
        val original =
            PropertyBundleModifierOperation(
                /* bundleId = */ 5,
                /* parentBundleId = */ 9,
                /* propertyIds = */ intArrayOf(15, 20, Int.MAX_VALUE, Int.MIN_VALUE),
                /* values = */ intArrayOf(1, -42, Int.MIN_VALUE, Int.MAX_VALUE),
            )
        val buffer = WireBuffer()

        original.write(buffer)
        buffer.index = 0
        assertThat(buffer.readByte()).isEqualTo(Operations.MODIFIER_PROPERTY_BUNDLE)

        val operations = ArrayList<Operation>()
        PropertyBundleModifierOperation.read(buffer, operations)

        val decoded = operations.single() as PropertyBundleModifierOperation
        assertThat(decoded.bundleId).isEqualTo(5)
        assertThat(decoded.parentBundleId).isEqualTo(9)
        assertThat(decoded.getValue(15)).isEqualTo(1)
        assertThat(decoded.getValue(20)).isEqualTo(-42)
        assertThat(decoded.getValue(Int.MAX_VALUE)).isEqualTo(Int.MIN_VALUE)
        assertThat(decoded.getValue(Int.MIN_VALUE)).isEqualTo(Int.MAX_VALUE)
        assertThat(decoded.hasProperty(999)).isFalse()
        assertThat(decoded.getValue(999)).isNull()
    }

    @Test
    fun `write and read round-trip with empty entries`() {
        val original = PropertyBundleModifierOperation(0, 0, intArrayOf(), intArrayOf())
        val buffer = WireBuffer()

        original.write(buffer)
        buffer.index = 0
        assertThat(buffer.readByte()).isEqualTo(Operations.MODIFIER_PROPERTY_BUNDLE)

        val operations = ArrayList<Operation>()
        PropertyBundleModifierOperation.read(buffer, operations)

        val decoded = operations.single() as PropertyBundleModifierOperation
        assertThat(decoded.hasProperty(0)).isFalse()
        assertThat(decoded.toString())
            .isEqualTo(
                "PropertyBundleModifierOperation(bundleId=0, parentBundleId=0, properties={})"
            )
    }

    @Test
    fun `toString and deepToString format class name, indent, and prefixed fields`() {
        val op = PropertyBundleModifierOperation(5, 9, intArrayOf(15, 20), intArrayOf(1, 2))
        val expected =
            "PropertyBundleModifierOperation(bundleId=5, parentBundleId=9, properties={15=1, 20=2})"

        assertThat(op.toString()).isEqualTo(expected)
        assertThat(op.deepToString("")).isEqualTo(expected)
        assertThat(op.deepToString("  ")).isEqualTo("  $expected")
    }

    @Test
    fun `serializeToString appends indented modifier summary`() {
        val op = PropertyBundleModifierOperation(5, 9, intArrayOf(15, 20), intArrayOf(1, 2))
        val serializer = StringSerializer()

        op.serializeToString(2, serializer)

        assertThat(serializer.toString())
            .isEqualTo(
                "    PROPERTY_BUNDLE_MODIFIER bundleId=5 parentBundleId=9 properties={15=1, 20=2}\n"
            )
    }

    @Test
    fun `serialize populates MapSerializer and YAMLSerializer with modifier fields`() {
        val op = PropertyBundleModifierOperation(5, 9, intArrayOf(15, 20), intArrayOf(100, -42))
        val mapSerializer = RecordingMapSerializer()

        op.serialize(mapSerializer)

        assertThat(mapSerializer.values["tags"]).isEqualTo(listOf(SerializeTags.MODIFIER))
        assertThat(mapSerializer.values["type_name"])
            .isEqualTo(PropertyBundleModifierOperation.CLASS_NAME)
        assertThat(mapSerializer.values["bundleId"]).isEqualTo(5)
        assertThat(mapSerializer.values["parentBundleId"]).isEqualTo(9)
        assertThat(mapSerializer.values["properties"])
            .isEqualTo(linkedMapOf("15" to 100, "20" to -42))

        val yamlSerializer = YAMLSerializer()
        op.serialize(yamlSerializer.serializeMap())
        val yaml = yamlSerializer.toString()
        assertThat(yaml).contains("bundleId: 5")
        assertThat(yaml).contains("parentBundleId: 9")
        assertThat(yaml).contains("100")
        assertThat(yaml).contains("-42")
    }

    @Test
    fun `read throws on unsupported version`() {
        val buffer =
            WireBuffer().apply {
                writeInt(1) // unsupported version
                writeInt(0) // bundleId
                writeInt(0) // parentBundleId
                writeInt(0) // count
                index = 0
            }

        val exception =
            assertFailsWith<RuntimeException> {
                PropertyBundleModifierOperation.read(buffer, ArrayList())
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Unsupported PropertyBundleModifierOperation version: 1")
    }

    @Test
    fun `read throws on negative or excessive entry count`() {
        val negativeBuffer =
            WireBuffer().apply {
                writeInt(0) // version
                writeInt(0) // bundleId
                writeInt(0) // parentBundleId
                writeInt(-1) // negative count
                index = 0
            }
        val negativeException =
            assertFailsWith<RuntimeException> {
                PropertyBundleModifierOperation.read(negativeBuffer, ArrayList())
            }
        assertThat(negativeException)
            .hasMessageThat()
            .contains("Invalid PropertyBundle entry count: -1")

        val excessiveBuffer =
            WireBuffer().apply {
                writeInt(0) // version
                writeInt(0) // bundleId
                writeInt(0) // parentBundleId
                writeInt(Limits.MAX_PROPERTY_BUNDLE_ENTRIES + 1)
                index = 0
            }
        val excessiveException =
            assertFailsWith<RuntimeException> {
                PropertyBundleModifierOperation.read(excessiveBuffer, ArrayList())
            }
        assertThat(excessiveException)
            .hasMessageThat()
            .contains(
                "Invalid PropertyBundle entry count: ${Limits.MAX_PROPERTY_BUNDLE_ENTRIES + 1}"
            )
    }

    @Test
    fun `Component getPropertyBundle is null until inflated with modifier`() {
        val component = Component(null, 1, -1, 0f, 0f, 100f, 100f)
        assertThat(component.propertyBundle).isNull()

        component.inflate()

        assertThat(component.propertyBundle).isNull()
    }

    @Test
    fun `Component inflate attaches PropertyBundleModifierOperation directly from mList`() {
        val component = Component(null, 1, -1, 0f, 0f, 100f, 100f)
        val op = PropertyBundleModifierOperation(11, 22, intArrayOf(15, 25), intArrayOf(1, 0))
        component.mList.add(op)

        component.inflate()

        val attached = assertNotNull(component.propertyBundle)
        assertThat(attached).isSameInstanceAs(op)
        assertThat(attached.bundleId).isEqualTo(11)
        assertThat(attached.parentBundleId).isEqualTo(22)
        assertThat(attached.getValue(15)).isEqualTo(1)
        assertThat(attached.getValue(25)).isEqualTo(0)
        assertThat(attached.getValue(99)).isNull()

        val secondOp = PropertyBundleModifierOperation(33, 44, intArrayOf(99), intArrayOf(7))
        component.mList.add(secondOp)
        component.inflate()
        assertThat(component.propertyBundle).isSameInstanceAs(secondOp)

        component.mList.clear()
        component.inflate()
        assertThat(component.propertyBundle).isNull()
    }

    @Test
    fun `LayoutComponent inflate keeps propertyBundle null when no PropertyBundleModifierOperation is present`() {
        val box = BoxLayout(null, 1, -1, 0, 0)

        box.inflate()

        assertThat(box.propertyBundle).isNull()
    }

    @Test
    fun `LayoutComponent inflate attaches PropertyBundleModifierOperation directly`() {
        val box = BoxLayout(null, 1, -1, 0, 0)
        val op = PropertyBundleModifierOperation(33, 44, intArrayOf(7, 8), intArrayOf(70, 80))
        box.mList.add(op)

        box.inflate()

        val attached = assertNotNull(box.propertyBundle)
        assertThat(attached).isSameInstanceAs(op)
        assertThat(attached.bundleId).isEqualTo(33)
        assertThat(attached.parentBundleId).isEqualTo(44)
        assertThat(attached.getValue(7)).isEqualTo(70)
        assertThat(attached.getValue(8)).isEqualTo(80)
    }

    @Test
    fun `LayoutComponent inflate applies multiple PropertyBundleModifierOperations in order`() {
        val box = BoxLayout(null, 1, -1, 0, 0)
        val firstOp = PropertyBundleModifierOperation(1, 0, intArrayOf(1, 2), intArrayOf(10, 20))
        val secondOp = PropertyBundleModifierOperation(2, 1, intArrayOf(3), intArrayOf(30))
        box.mList.add(firstOp)
        box.mList.add(secondOp)

        box.inflate()

        val attached = assertNotNull(box.propertyBundle)
        assertThat(attached).isSameInstanceAs(secondOp)
        assertThat(attached.bundleId).isEqualTo(2)
        assertThat(attached.parentBundleId).isEqualTo(1)
        assertThat(attached.hasProperty(1)).isFalse()
        assertThat(attached.getValue(3)).isEqualTo(30)
    }

    @Test
    fun `RemoteComposeBuffer inflates modifier into componentModifiers and attaches directly to Component`() {
        val doc = CoreDocument().useExperimental()
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addColumnStart(
                    /* componentId = */ 1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                    /* spacedBy = */ 0f,
                )
                addPropertyBundleModifierOperation(
                    /* bundleId = */ 10,
                    /* parentBundleId = */ 0,
                    /* propertyIds = */ intArrayOf(100),
                    /* values = */ intArrayOf(7),
                )
                addContentStart()

                // Child box with its own property bundle
                addBoxStart(
                    /* componentId = */ 2,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addPropertyBundleModifierOperation(
                    /* bundleId = */ 11,
                    /* parentBundleId = */ 10,
                    /* propertyIds = */ intArrayOf(15, 20),
                    /* values = */ intArrayOf(1, 2),
                )
                addContentStart()
                addContainerEnd() // box content
                addContainerEnd() // box

                // Sibling box with no property bundle
                addBoxStart(
                    /* componentId = */ 3,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addContentStart()
                addContainerEnd() // sibling box content
                addContainerEnd() // sibling box

                addContainerEnd() // column content
                addContainerEnd() // column
                addContainerEnd() // root
            }

        doc.initFromBuffer(buffer)

        val root = doc.operations.filterIsInstance<RootLayoutComponent>().single()
        val column = root.list.filterIsInstance<ColumnLayout>().single()
        val boxes = column.list.filterIsInstance<BoxLayout>()
        assertThat(boxes).hasSize(2)
        val boxWithProps = boxes[0]
        val plainBox = boxes[1]

        // Modifier resides in componentModifiers and is attached directly to the Component
        val columnMod =
            column.componentModifiers.modifiersList
                .filterIsInstance<PropertyBundleModifierOperation>()
                .single()
        assertThat(column.list.filterIsInstance<PropertyBundleModifierOperation>()).isEmpty()
        assertThat(columnMod.bundleId).isEqualTo(10)
        assertThat(columnMod.parentBundleId).isEqualTo(0)

        val columnBundle = assertNotNull(column.propertyBundle)
        assertThat(columnBundle).isSameInstanceAs(columnMod)
        assertThat(columnBundle.bundleId).isEqualTo(10)
        assertThat(columnBundle.parentBundleId).isEqualTo(0)
        assertThat(columnBundle.getValue(100)).isEqualTo(7)
        assertThat(columnBundle.hasProperty(15)).isFalse()

        val boxMod =
            boxWithProps.componentModifiers.modifiersList
                .filterIsInstance<PropertyBundleModifierOperation>()
                .single()
        assertThat(boxWithProps.list.filterIsInstance<PropertyBundleModifierOperation>()).isEmpty()
        assertThat(boxMod.bundleId).isEqualTo(11)
        assertThat(boxMod.parentBundleId).isEqualTo(10)

        val boxBundle = assertNotNull(boxWithProps.propertyBundle)
        assertThat(boxBundle).isSameInstanceAs(boxMod)
        assertThat(boxBundle.bundleId).isEqualTo(11)
        assertThat(boxBundle.parentBundleId).isEqualTo(10)
        assertThat(boxBundle.getValue(15)).isEqualTo(1)
        assertThat(boxBundle.getValue(20)).isEqualTo(2)
        assertThat(boxBundle.getValue(99, -1)).isEqualTo(-1)

        assertThat(plainBox.propertyBundle).isNull()
        assertThat(doc.displayHierarchy())
            .contains(
                "PROPERTY_BUNDLE_MODIFIER bundleId=11 parentBundleId=10 properties={15=1, 20=2}"
            )
    }

    @Test
    fun `read remaps entry variable IDs via readId without remapping bundle or property IDs`() {
        val original =
            PropertyBundleModifierOperation(
                /* bundleId = */ 7,
                /* parentBundleId = */ 3,
                /* propertyIds = */ intArrayOf(10, 20),
                /* values = */ intArrayOf(41, 42),
            )

        val rawBuffer = WireBuffer()
        original.write(rawBuffer)

        val remappingBuffer =
            object : WireBuffer() {
                override fun readId(): Int = super.readInt() + 100
            }
        remappingBuffer.reset(rawBuffer.size)
        for (i in 0 until rawBuffer.size) {
            remappingBuffer.writeByte(rawBuffer.buffer[i].toInt())
        }
        remappingBuffer.index = 0
        assertThat(remappingBuffer.readByte()).isEqualTo(Operations.MODIFIER_PROPERTY_BUNDLE)

        val decodedOps = ArrayList<Operation>()
        PropertyBundleModifierOperation.read(remappingBuffer, decodedOps)
        val decoded = decodedOps.single() as PropertyBundleModifierOperation

        assertThat(decoded.bundleId).isEqualTo(7)
        assertThat(decoded.parentBundleId).isEqualTo(3)
        assertThat(decoded.getValue(10)).isEqualTo(141)
        assertThat(decoded.getValue(20)).isEqualTo(142)
    }

    private fun RemoteComposeBuffer.useExperimental(): RemoteComposeBuffer = apply {
        val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val map = Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, profileMask)!!
        setVersion(CoreDocument.DOCUMENT_API_LEVEL, profileMask, map.keySet())
        setVersion(CoreDocument.DOCUMENT_API_LEVEL, profileMask, map)
    }

    private fun CoreDocument.useExperimental(): CoreDocument = apply {
        mProfileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
    }

    private class RecordingMapSerializer : MapSerializer {
        val values = mutableMapOf<String, Any?>()

        override fun addType(type: String) = record("type_name", type)

        override fun addFloatExpressionSrc(key: String, value: FloatArray) =
            record(key, value.toList())

        override fun addIntExpressionSrc(key: String, value: IntArray, mask: Int) =
            record(key, value.toList())

        override fun addPath(key: String, path: FloatArray) = record(key, path.toList())

        override fun addTags(vararg value: SerializeTags) = record("tags", value.toList())

        override fun <T> add(key: String, value: List<T>?) = record(key, value)

        override fun <T> add(key: String, value: Map<String, T>?) = record(key, value)

        override fun add(key: String, value: Serializable?) = record(key, value)

        override fun add(key: String, value: String?) = record(key, value)

        override fun add(key: String, a: Float, r: Float, g: Float, b: Float) =
            record(key, listOf(a, r, g, b))

        override fun add(key: String, id: Float, value: Float) = record(key, listOf(id, value))

        override fun add(key: String, value: Byte?) = record(key, value)

        override fun add(key: String, value: Short?) = record(key, value)

        override fun add(key: String, value: Int?) = record(key, value)

        override fun add(key: String, value: Long?) = record(key, value)

        override fun add(key: String, value: Float?) = record(key, value)

        override fun add(key: String, value: Double?) = record(key, value)

        override fun add(key: String, value: Boolean?) = record(key, value)

        override fun <T : Enum<T>> add(key: String, value: Enum<T>?) = record(key, value)

        private fun record(key: String, value: Any?): MapSerializer {
            values[key] = value
            return this
        }
    }
}

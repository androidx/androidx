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
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.remote.core.serialize.MapSerializer
import androidx.compose.remote.core.serialize.Serializable
import androidx.compose.remote.core.serialize.SerializeTags
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.serialization.yaml.YAMLSerializer
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PropertyBundleLookupOperationTest {

    @Test
    fun `constructor and accessors store valueId, componentId, propertyId, and defaultValue`() {
        val op =
            PropertyBundleLookupOperation(
                /* valueId = */ 42,
                /* componentId = */ 11,
                /* propertyId = */ 15,
                /* defaultValue = */ -7,
            )

        assertThat(PropertyBundleLookupOperation.id()).isEqualTo(Operations.PROPERTY_BUNDLE_LOOKUP)
        assertThat(PropertyBundleLookupOperation.id()).isEqualTo(96)
        assertThat(PropertyBundleLookupOperation.name()).isEqualTo("PropertyBundleLookupOperation")
        assertThat(op.valueId).isEqualTo(42)
        assertThat(op.id).isEqualTo(42)
        assertThat(op.componentId).isEqualTo(11)
        assertThat(op.propertyId).isEqualTo(15)
        assertThat(op.defaultValue).isEqualTo(-7)

        op.id = 99
        op.componentId = 22
        assertThat(op.valueId).isEqualTo(99)
        assertThat(op.id).isEqualTo(99)
        assertThat(op.componentId).isEqualTo(22)
    }

    @Test
    fun `write and read round-trip preserves fields`() {
        val original =
            PropertyBundleLookupOperation(
                /* valueId = */ 45,
                /* componentId = */ 12,
                /* propertyId = */ Int.MAX_VALUE,
                /* defaultValue = */ Int.MIN_VALUE,
            )
        val buffer = WireBuffer()

        original.write(buffer)
        buffer.index = 0
        assertThat(buffer.readByte()).isEqualTo(Operations.PROPERTY_BUNDLE_LOOKUP)

        val operations = ArrayList<Operation>()
        PropertyBundleLookupOperation.read(buffer, operations)

        val decoded = assertIs<PropertyBundleLookupOperation>(operations.single())
        assertThat(decoded.valueId).isEqualTo(45)
        assertThat(decoded.componentId).isEqualTo(12)
        assertThat(decoded.propertyId).isEqualTo(Int.MAX_VALUE)
        assertThat(decoded.defaultValue).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun `read remaps valueId via declareId and componentId via readId`() {
        val remappingBuffer =
            object : WireBuffer() {
                override fun declareId(): Int = super.readInt() + 1000

                override fun readId(): Int = super.readInt() + 2000
            }
        PropertyBundleLookupOperation.apply(
            remappingBuffer,
            /* valueId = */ 50,
            /* componentId = */ 10,
            /* propertyId = */ 15,
            /* defaultValue = */ 99,
        )
        remappingBuffer.index = 0
        assertThat(remappingBuffer.readByte()).isEqualTo(Operations.PROPERTY_BUNDLE_LOOKUP)

        val operations = ArrayList<Operation>()
        PropertyBundleLookupOperation.read(remappingBuffer, operations)

        val decoded = assertIs<PropertyBundleLookupOperation>(operations.single())
        assertThat(decoded.valueId).isEqualTo(1050)
        assertThat(decoded.componentId).isEqualTo(2010)
        assertThat(decoded.propertyId).isEqualTo(15)
        assertThat(decoded.defaultValue).isEqualTo(99)
    }

    @Test
    fun `read throws on unsupported version`() {
        val buffer =
            WireBuffer().apply {
                writeInt(1) // unsupported version
                writeInt(42) // valueId
                writeInt(10) // componentId
                writeInt(15) // propertyId
                writeInt(0) // defaultValue
                index = 0
            }

        val exception =
            assertFailsWith<RuntimeException> {
                PropertyBundleLookupOperation.read(buffer, ArrayList())
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Unsupported PropertyBundleLookupOperation version: 1")
    }

    @Test
    fun `toString, deepToString, serializeToString, and serialize format fields`() {
        val op = PropertyBundleLookupOperation(42, 7, 15, -1)
        val expected =
            "PropertyBundleLookupOperation(valueId=42, componentId=7, propertyId=15, defaultValue=-1)"

        assertThat(op.toString()).isEqualTo(expected)
        assertThat(op.deepToString("")).isEqualTo(expected)
        assertThat(op.deepToString("  ")).isEqualTo("  $expected")

        val stringSerializer = StringSerializer()
        op.serializeToString(2, stringSerializer)
        assertThat(stringSerializer.toString())
            .isEqualTo(
                "    PROPERTY_BUNDLE_LOOKUP valueId=42 componentId=7 propertyId=15 defaultValue=-1\n"
            )

        val mapSerializer = RecordingMapSerializer()
        op.serialize(mapSerializer)
        assertThat(mapSerializer.values["tags"]).isEqualTo(listOf(SerializeTags.EXPRESSION))
        assertThat(mapSerializer.values["type_name"])
            .isEqualTo(PropertyBundleLookupOperation.CLASS_NAME)
        assertThat(mapSerializer.values["valueId"]).isEqualTo(42)
        assertThat(mapSerializer.values["componentId"]).isEqualTo(7)
        assertThat(mapSerializer.values["propertyId"]).isEqualTo(15)
        assertThat(mapSerializer.values["defaultValue"]).isEqualTo(-1)

        val yamlSerializer = YAMLSerializer()
        op.serialize(yamlSerializer.serializeMap())
        val yaml = yamlSerializer.toString()
        assertThat(yaml).contains("valueId: 42")
        assertThat(yaml).contains("componentId: 7")
        assertThat(yaml).contains("propertyId: 15")
        assertThat(yaml).contains("defaultValue: -1")
    }

    @Test
    fun `Component and LayoutComponent inflate bind enclosing componentId without clobbering explicit targets`() {
        val plainComponent = Component(null, 5, -1, 0f, 0f, 100f, 100f)
        val implicitOnPlain = PropertyBundleLookupOperation(42, -1, 15, 0)
        val explicitOnPlain = PropertyBundleLookupOperation(43, 99, 15, 0)
        plainComponent.mList.add(implicitOnPlain)
        plainComponent.mList.add(explicitOnPlain)

        plainComponent.inflate()

        assertThat(implicitOnPlain.componentId).isEqualTo(5)
        assertThat(explicitOnPlain.componentId).isEqualTo(99)

        val box = BoxLayout(null, 8, -1, 0, 0)
        val content = LayoutComponentContent(-3, 0f, 0f, 0f, 0f, null, -1)
        val implicitMinusOne = PropertyBundleLookupOperation(44, -1, 15, 0)
        val implicitFromContent = PropertyBundleLookupOperation(45, -3, 20, 0)
        val explicitParentTarget = PropertyBundleLookupOperation(46, 1, 25, 0)
        content.mList.add(implicitMinusOne)
        content.mList.add(implicitFromContent)
        content.mList.add(explicitParentTarget)
        box.mList.add(content)

        box.inflate()

        assertThat(implicitMinusOne.componentId).isEqualTo(8)
        assertThat(implicitFromContent.componentId).isEqualTo(8)
        assertThat(explicitParentTarget.componentId).isEqualTo(1)
    }

    @Test
    fun `CoreDocument resolves present properties, falls back to defaultValue, and feeds IntegerExpression`() {
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

                // Child box with its own bundle
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
                    /* values = */ intArrayOf(40, -12),
                )
                addContentStart()
                // 1) Implicit enclosing component lookup -> 40
                addPropertyBundleLookupOperation(
                    /* valueId = */ 50,
                    /* propertyId = */ 15,
                    /* defaultValue = */ -1,
                )
                // 2) Missing property on enclosing component -> defaultValue (-99)
                addPropertyBundleLookupOperation(
                    /* valueId = */ 51,
                    /* propertyId = */ 999,
                    /* defaultValue = */ -99,
                )
                // 3) Explicit lookup targeting parent column (id = 1) -> 7
                addPropertyBundleLookupOperation(
                    /* valueId = */ 52,
                    /* componentId = */ 1,
                    /* propertyId = */ 100,
                    /* defaultValue = */ -1,
                )
                // 4) Downstream IntegerExpression reading valueId 50 + valueId 52 -> 47
                addIntegerExpression(
                    /* id = */ 53,
                    /* mask = */ 0b111,
                    /* value = */ intArrayOf(50, 52, IntegerExpressionEvaluator.I_ADD),
                )
                addContainerEnd() // box 2 content
                addContainerEnd() // box 2

                // Sibling box with NO property bundle attached
                addBoxStart(
                    /* componentId = */ 3,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addContentStart()
                // 5) Component has no bundle -> defaultValue (123)
                addPropertyBundleLookupOperation(
                    /* valueId = */ 54,
                    /* propertyId = */ 15,
                    /* defaultValue = */ 123,
                )
                // 6) Non-existent componentId -> defaultValue (456)
                addPropertyBundleLookupOperation(
                    /* valueId = */ 55,
                    /* componentId = */ 777,
                    /* propertyId = */ 15,
                    /* defaultValue = */ 456,
                )
                addContainerEnd() // box 3 content
                addContainerEnd() // box 3

                // CanvasLayout with CanvasContent resolving bundle attached to CanvasLayout
                addCanvasStart(/* componentId= */ 4, /* animationId= */ -1)
                addPropertyBundleModifierOperation(
                    /* bundleId = */ 12,
                    /* parentBundleId = */ 0,
                    /* propertyIds = */ intArrayOf(30),
                    /* values = */ intArrayOf(88),
                )
                addContentStart()
                addCanvasContentStart(/* componentId= */ 5)
                addPropertyBundleLookupOperation(
                    /* valueId = */ 56,
                    /* propertyId = */ 30,
                    /* defaultValue = */ 0,
                )
                addContainerEnd() // canvas content
                addContainerEnd() // layout content
                addContainerEnd() // canvas

                addContainerEnd() // column content
                addContainerEnd() // column
                addContainerEnd() // root
            }

        doc.initFromBuffer(buffer)
        val context = HeadlessRemoteContext()
        doc.initializeContext(context)

        assertThat(context.getInteger(50)).isEqualTo(40)
        assertThat(context.getInteger(51)).isEqualTo(-99)
        assertThat(context.getInteger(52)).isEqualTo(7)
        assertThat(context.getInteger(53)).isEqualTo(47)
        assertThat(context.getInteger(54)).isEqualTo(123)
        assertThat(context.getInteger(55)).isEqualTo(456)
        assertThat(context.getInteger(56)).isEqualTo(88)
        assertThat(doc.displayHierarchy())
            .contains(
                "PROPERTY_BUNDLE_LOOKUP valueId=50 componentId=2 propertyId=15 defaultValue=-1"
            )
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

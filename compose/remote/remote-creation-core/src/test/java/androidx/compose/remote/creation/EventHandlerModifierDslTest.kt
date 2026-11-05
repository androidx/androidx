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
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.EventHandlerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation
import androidx.compose.remote.core.serialize.MapSerializer
import androidx.compose.remote.core.serialize.Serializable
import androidx.compose.remote.core.serialize.SerializeTags
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.onEvent
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EventHandlerModifierDslTest {

    private val profile =
        RcProfile(
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                RcPlatformServices.None,
            ) { info, profile, _ ->
                RemoteComposeWriter(
                    profile,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, info.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, info.height),
                )
            }
        )

    @Test
    fun onEvent_writesEventHandlerModifierWithNestedActionsAndTheirTextData() {
        // Arrange.
        val dataIds = intArrayOf(7001, 7002)
        val condition = floatArrayOf(1f)
        var greetingId = -1

        // Act.
        val document = buildDocument {
            val greeting = remoteText("initial")
            greetingId = greeting.id
            Box(
                modifier =
                    Modifier.onEvent(
                        type = 10,
                        filter = 20,
                        flags = 30,
                        dataIds = dataIds,
                        condition = condition,
                    ) {
                        setValue(greeting, "hello")
                        hostAction("doIt")
                    }
            ) {}
        }

        // Assert - the handler is a modifier of the box, not an operation in its list.
        val box = document.findBox()
        val eventHandler = box.findEventHandlers().single()
        assertThat(box.list.filterIsInstance<EventHandlerModifierOperation>()).isEmpty()
        assertThat(eventHandler.toString())
            .isEqualTo("EventHandlerModifier(type=10, filter=20, flags=30)")

        // Assert - each action's inline text is nested right before the action that uses it.
        val nested = eventHandler.list
        assertThat(nested.map { it.javaClass })
            .containsExactly(
                TextData::class.java,
                ValueStringChangeActionOperation::class.java,
                TextData::class.java,
                HostNamedActionOperation::class.java,
            )
            .inOrder()
        val texts = nested.filterIsInstance<TextData>()
        assertThat(texts.map { it.mText }).containsExactly("hello", "doIt").inOrder()
        assertThat((nested[1] as ValueStringChangeActionOperation).actionId).isEqualTo(greetingId)
    }

    @Test
    fun onEvent_passesDataIdsAndConditionThrough() {
        // Arrange.
        val dataIds = intArrayOf(7001, 0, 7003)
        val condition = floatArrayOf(1f, 2f, 3f)

        // Act.
        val document = buildDocument {
            Box(
                modifier =
                    Modifier.onEvent(
                        type = 10,
                        filter = 20,
                        flags = 30,
                        dataIds = dataIds,
                        condition = condition,
                    ) {}
            ) {}
        }

        // Assert.
        val serialized = document.findBox().findEventHandlers().single().serializeToMap()
        assertThat(serialized["type"]).isEqualTo(10)
        assertThat(serialized["filter"]).isEqualTo(20)
        assertThat(serialized["flags"]).isEqualTo(30)
        assertThat(serialized["dataIds"]).isEqualTo(dataIds.contentToString())
        assertThat(serialized["condition"]).isEqualTo(condition.toList())
    }

    @Test
    fun onEvent_defaultsToNoFlagsNoDataIdsAndNoCondition() {
        // Act.
        val document = buildDocument {
            Box(modifier = Modifier.onEvent(type = 10, filter = 20) {}) {}
        }

        // Assert.
        val eventHandler = document.findBox().findEventHandlers().single()
        assertThat(eventHandler.toString())
            .isEqualTo("EventHandlerModifier(type=10, filter=20, flags=0)")
        val serialized = eventHandler.serializeToMap()
        assertThat(serialized["dataIds"]).isNull()
        assertThat(serialized).doesNotContainKey("condition")
    }

    @Test
    fun onEvent_withEmptyBlockWritesHandlerWithoutNestedOperations() {
        // Act.
        val document = buildDocument {
            Box(modifier = Modifier.onEvent(type = 10, filter = 20) {}) {}
        }

        // Assert.
        val eventHandler = document.findBox().findEventHandlers().single()
        assertThat(eventHandler.list).isEmpty()
    }

    @Test
    fun onEvent_withFloatActionDoesNotNestTextData() {
        // Arrange.
        var counterId = -1

        // Act.
        val document = buildDocument {
            val counter = remoteFloat(0f)
            counterId = Utils.idFromNan(counter.toFloat())
            Box(modifier = Modifier.onEvent(type = 10, filter = 20) { setValue(counter, 5f) }) {}
        }

        // Assert.
        val nested = document.findBox().findEventHandlers().single().list
        assertThat(nested).hasSize(1)
        val action = nested.single() as ValueFloatChangeActionOperation
        assertThat(action.targetValueId).isEqualTo(counterId)
        assertThat(action.value).isEqualTo(5f)
    }

    @Test
    fun onEvent_withHostActionMetadataReusesExistingText() {
        // Act.
        val document = buildDocument {
            val metadata = remoteText("meta")
            Box(modifier = Modifier.onEvent(type = 10, filter = 20) { hostAction(42, metadata) }) {}
        }

        // Assert - the metadata text was declared outside the handler, so only the action nests.
        val nested = document.findBox().findEventHandlers().single().list
        assertThat(nested.map { it.javaClass })
            .containsExactly(HostActionMetadataOperation::class.java)
    }

    @Test
    fun onEvent_canBeChainedWithOtherModifiersInOrder() {
        // Act.
        val document = buildDocument {
            Box(
                modifier =
                    Modifier.padding(4f)
                        .onEvent(type = 10, filter = 20) {}
                        .background(0xFF0000FF.toInt())
            ) {}
        }

        // Assert.
        val modifiers = document.findBox().componentModifiers.modifiersList
        assertThat(modifiers.map { it.javaClass })
            .containsExactly(
                PaddingModifierOperation::class.java,
                EventHandlerModifierOperation::class.java,
                BackgroundModifierOperation::class.java,
            )
            .inOrder()
    }

    @Test
    fun onEvent_multipleHandlersOnOneComponentAreKeptInOrder() {
        // Act.
        val document = buildDocument {
            Box(
                modifier =
                    Modifier.onEvent(type = 10, filter = 20) {}
                        .onEvent(type = 11, filter = 21) {}
                        .onEvent(type = 10, filter = 22) {}
            ) {}
        }

        // Assert.
        val eventHandlers = document.findBox().findEventHandlers()
        assertThat(eventHandlers.map { it.toString() })
            .containsExactly(
                "EventHandlerModifier(type=10, filter=20, flags=0)",
                "EventHandlerModifier(type=11, filter=21, flags=0)",
                "EventHandlerModifier(type=10, filter=22, flags=0)",
            )
            .inOrder()
    }

    @Test
    fun onEvent_onNestedComponentAttachesToThatComponentOnly() {
        // Act.
        val document = buildDocument {
            Column(modifier = Modifier.onEvent(type = 10, filter = 20) {}) {
                Box(modifier = Modifier.onEvent(type = 11, filter = 21) {}) {}
            }
        }

        // Assert.
        val column = document.rootLayoutComponent!!.list.filterIsInstance<ColumnLayout>().single()
        val box = column.list.filterIsInstance<BoxLayout>().single()
        assertThat(column.findEventHandlers().map { it.toString() })
            .containsExactly("EventHandlerModifier(type=10, filter=20, flags=0)")
        assertThat(box.findEventHandlers().map { it.toString() })
            .containsExactly("EventHandlerModifier(type=11, filter=21, flags=0)")
    }

    private fun buildDocument(content: RcScope.() -> Unit): CoreDocument {
        val bytes =
            createRcBuffer(
                profile,
                experimental = true,
                RcHeaderTag.docWidth(100),
                RcHeaderTag.docHeight(100),
                content = content,
            )
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
        return CoreDocument().apply { initFromBuffer(readBuffer) }
    }

    private fun CoreDocument.findBox(): BoxLayout =
        rootLayoutComponent!!.list.filterIsInstance<BoxLayout>().single()

    private fun LayoutComponent.findEventHandlers(): List<EventHandlerModifierOperation> =
        componentModifiers.modifiersList.filterIsInstance<EventHandlerModifierOperation>()

    private fun EventHandlerModifierOperation.serializeToMap(): Map<String, Any?> =
        RecordingMapSerializer().also { serialize(it) }.values

    /** Minimal [MapSerializer] that records every key/value pair it is given. */
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

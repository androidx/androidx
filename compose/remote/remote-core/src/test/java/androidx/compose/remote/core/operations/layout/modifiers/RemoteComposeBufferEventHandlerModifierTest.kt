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

package androidx.compose.remote.core.operations.layout.modifiers

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RemoteComposeBufferEventHandlerModifierTest {

    @Test
    fun `handler with no dataIds and no condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ null,
                    /* condition = */ null,
                )
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }

        // Act.
        doc.initFromBuffer(buffer)

        // Assert.
        with(doc.findEventHandlerModifier()) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isNull()
            assertThat(mCondition).isNull()
        }
    }

    @Test
    fun `handler with dataIds and no condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val dataIds = intArrayOf(101, 102, 103)
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ dataIds,
                    /* condition = */ null,
                )
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }

        // Act.
        doc.initFromBuffer(buffer)

        // Assert.
        with(doc.findEventHandlerModifier()) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isEqualTo(dataIds)
            assertThat(mCondition).isNull()
        }
    }

    @Test
    fun `handler with no dataIds and condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val condition = floatArrayOf(1.0f, 2.0f)
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ null,
                    /* condition = */ condition,
                )
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }

        // Act.
        doc.initFromBuffer(buffer)

        // Assert.
        with(doc.findEventHandlerModifier()) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isNull()
            assertThat(mCondition).isEqualTo(condition)
        }
    }

    @Test
    fun `handler with both dataIds and condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val dataIds = intArrayOf(101, 102)
        val condition = floatArrayOf(1.0f, 2.0f)
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ dataIds,
                    /* condition = */ condition,
                )
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }

        // Act.
        doc.initFromBuffer(buffer)

        // Assert.
        with(doc.findEventHandlerModifier()) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isEqualTo(dataIds)
            assertThat(mCondition).isEqualTo(condition)
        }
    }

    @Test
    fun `modifier is inflated into the component modifiers and not the component list`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ null,
                    /* condition = */ null,
                )
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }

        // Act.
        doc.initFromBuffer(buffer)

        // Assert.
        val box = doc.findBox()
        val modifiers = box.componentModifiers.modifiersList
        assertThat(modifiers.filterIsInstance<EventHandlerModifierOperation>()).hasSize(1)
        assertThat(box.list.filterIsInstance<EventHandlerModifierOperation>()).isEmpty()
    }

    @Test
    fun `addEventHandlerModifierOperation throws when condition expression is too long`() {
        // Arrange.
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
            }
        val excessiveCondition = FloatArray(Limits.MAX_EXPRESSION_SIZE + 1)

        // Act.
        val exception =
            assertFailsWith<RuntimeException> {
                buffer.addEventHandlerModifierOperation(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ null,
                    /* condition = */ excessiveCondition,
                )
            }

        // Assert.
        assertThat(exception)
            .hasMessageThat()
            .contains("Condition expression passed to EventHandler is too long")
    }

    @Test
    fun `read throws exception when encountering unsupported version`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val buffer =
            RemoteComposeBuffer().useExperimental().apply {
                addRootStart()
                addBoxStart(
                    /* componentId = */ -1,
                    /* animationId = */ -1,
                    /* horizontal = */ 0,
                    /* vertical = */ 0,
                )
                // Manually serialize with an unsupported version = 1
                buffer.start(Operations.MODIFIER_EVENT_HANDLER)
                buffer.writeInt(1) // version = 1 (unsupported)
                buffer.writeInt(EVENT_TYPE_1)
                buffer.writeInt(300)
                buffer.writeShort(FLAG_UNCONDITIONAL or FLAG_NO_DATA)
                buffer.writeShort(345)
                addContainerEnd() // event handler actions
                addContentStart()
                addContainerEnd() // content
                addContainerEnd() // box
                addContainerEnd() // root
            }

        // Act.
        val exception = assertFailsWith<RuntimeException> { doc.initFromBuffer(buffer) }

        // Assert.
        assertThat(exception)
            .hasMessageThat()
            .contains("Unsupported EventHandlerModifierOperation version: 1")
    }

    private fun CoreDocument.findBox(): BoxLayout {
        val root = operations.filterIsInstance<RootLayoutComponent>().single()
        return root.list.filterIsInstance<BoxLayout>().single()
    }

    private fun CoreDocument.findEventHandlerModifier(): EventHandlerModifierOperation =
        findBox()
            .componentModifiers
            .modifiersList
            .filterIsInstance<EventHandlerModifierOperation>()
            .single()

    private fun RemoteComposeBuffer.useExperimental(): RemoteComposeBuffer = apply {
        val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val map = Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, profileMask)!!
        setVersion(CoreDocument.DOCUMENT_API_LEVEL, profileMask, map.keySet())
        setVersion(CoreDocument.DOCUMENT_API_LEVEL, profileMask, map)
    }

    private fun CoreDocument.useExperimental(): CoreDocument = apply {
        mProfileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
    }

    companion object {
        const val EVENT_TYPE_1 = 1
        const val FLAG_UNCONDITIONAL = 1
        const val FLAG_NO_DATA = 2
    }
}

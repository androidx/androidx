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
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RemoteComposeBufferEventActionTest {

    @Test
    fun `handler with no dataIds and no condition - operation to buffer`() {
        // Arrange.
        val buffer = RemoteComposeBuffer().useExperimental()

        // Act.
        buffer.startEventActions(
            /* type = */ EVENT_TYPE_1,
            /* filter = */ 300,
            /* flags = */ 345,
            /* dataIds = */ null,
            /* condition = */ null,
        )

        // Assert.
        with(buffer.buffer) {
            index = 0
            readOperationType().let { opCode ->
                assertThat(opCode).isEqualTo(Operations.EVENT_ACTION)
            }
            readInt().let { version -> assertThat(version).isEqualTo(0) }
            readInt().let { eventType -> assertThat(eventType).isEqualTo(EVENT_TYPE_1) }
            readInt().let { filter -> assertThat(filter).isEqualTo(300) }
            readShort().let { commonFlags ->
                assertWithMessage("Unconditional flag should be set")
                    .that(commonFlags and FLAG_UNCONDITIONAL)
                    .isNotEqualTo(0)
                assertWithMessage("NoData flag should be set")
                    .that(commonFlags and FLAG_NO_DATA)
                    .isNotEqualTo(0)
            }
            readShort().let { flags -> assertThat(flags).isEqualTo(345) }
        }
    }

    @Test
    fun `handler with no dataIds and no condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val buffer = RemoteComposeBuffer().useExperimental()

        // Act.
        doc.initFromBuffer(
            buffer.apply {
                startEventActions(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ null,
                    /* condition = */ null,
                )
            }
        )

        // Assert.
        val operations = doc.operations
        assertThat(operations).hasSize(1)
        with(operations[0] as EventActionOperation) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isNull()
            assertThat(mCondition).isNull()
        }
    }

    @Test
    fun `handler with dataIds and no condition - operation to buffer`() {
        // Arrange.
        val buffer = RemoteComposeBuffer().useExperimental()
        val dataIds = intArrayOf(101, 102, 103)

        // Act.
        buffer.startEventActions(
            /* type = */ EVENT_TYPE_1,
            /* filter = */ 300,
            /* flags = */ 345,
            /* dataIds = */ dataIds,
            /* condition = */ null,
        )

        // Assert.
        with(buffer.buffer) {
            index = 0
            readOperationType().let { opCode ->
                assertThat(opCode).isEqualTo(Operations.EVENT_ACTION)
            }
            readInt().let { version -> assertThat(version).isEqualTo(0) }
            readInt().let { eventType -> assertThat(eventType).isEqualTo(EVENT_TYPE_1) }
            readInt().let { filter -> assertThat(filter).isEqualTo(300) }
            readShort().let { commonFlags ->
                assertWithMessage("Unconditional flag should be set")
                    .that(commonFlags and FLAG_UNCONDITIONAL)
                    .isNotEqualTo(0)
                assertWithMessage("NoData flag should not be set")
                    .that(commonFlags and FLAG_NO_DATA)
                    .isEqualTo(0)
            }
            readShort().let { flags -> assertThat(flags).isEqualTo(345) }
            readInt().let { size -> assertThat(size).isEqualTo(3) }
            readInt().let { dataId -> assertThat(dataId).isEqualTo(101) }
            readInt().let { dataId -> assertThat(dataId).isEqualTo(102) }
            readInt().let { dataId -> assertThat(dataId).isEqualTo(103) }
        }
    }

    @Test
    fun `handler with dataIds and no condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val dataIds = intArrayOf(101, 102, 103)
        val buffer = RemoteComposeBuffer().useExperimental()

        // Act.
        doc.initFromBuffer(
            buffer.apply {
                startEventActions(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ dataIds,
                    /* condition = */ null,
                )
            }
        )

        // Assert.
        val operations = doc.operations
        assertThat(operations).hasSize(1)
        with(operations[0] as EventActionOperation) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isEqualTo(dataIds)
            assertThat(mCondition).isNull()
        }
    }

    @Test
    fun `handler with no dataIds and condition - operation to buffer`() {
        // Arrange.
        val buffer = RemoteComposeBuffer().useExperimental()
        val condition = floatArrayOf(1.0f, 2.0f)

        // Act.
        buffer.startEventActions(
            /* type = */ EVENT_TYPE_1,
            /* filter = */ 300,
            /* flags = */ 345,
            /* dataIds = */ null,
            /* condition = */ condition,
        )

        // Assert.
        with(buffer.buffer) {
            index = 0
            readOperationType().let { opCode ->
                assertThat(opCode).isEqualTo(Operations.EVENT_ACTION)
            }
            readInt().let { version -> assertThat(version).isEqualTo(0) }
            readInt().let { eventType -> assertThat(eventType).isEqualTo(EVENT_TYPE_1) }
            readInt().let { filter -> assertThat(filter).isEqualTo(300) }
            readShort().let { commonFlags ->
                assertWithMessage("Unconditional flag should not be set")
                    .that(commonFlags and FLAG_UNCONDITIONAL)
                    .isEqualTo(0)
                assertWithMessage("NoData flag should be set")
                    .that(commonFlags and FLAG_NO_DATA)
                    .isNotEqualTo(0)
            }
            readShort().let { flags -> assertThat(flags).isEqualTo(345) }
            readInt().let { size -> assertThat(size).isEqualTo(2) }
            readFloat().let { value -> assertThat(value).isEqualTo(1.0f) }
            readFloat().let { value -> assertThat(value).isEqualTo(2.0f) }
        }
    }

    @Test
    fun `handler with no dataIds and condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val condition = floatArrayOf(1.0f, 2.0f)
        val buffer = RemoteComposeBuffer().useExperimental()

        // Act.
        doc.initFromBuffer(
            buffer.apply {
                startEventActions(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ null,
                    /* condition = */ condition,
                )
            }
        )

        // Assert.
        val operations = doc.operations
        assertThat(operations).hasSize(1)
        with(operations[0] as EventActionOperation) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isNull()
            assertThat(mCondition).isEqualTo(condition)
        }
    }

    @Test
    fun `handler with both dataIds and condition - operation to buffer`() {
        // Arrange.
        val buffer = RemoteComposeBuffer().useExperimental()
        val dataIds = intArrayOf(101, 102)
        val condition = floatArrayOf(1.0f, 2.0f)

        // Act.
        buffer.startEventActions(
            /* type = */ EVENT_TYPE_1,
            /* filter = */ 300,
            /* flags = */ 345,
            /* dataIds = */ dataIds,
            /* condition = */ condition,
        )

        // Assert.
        with(buffer.buffer) {
            index = 0
            readOperationType().let { opCode ->
                assertThat(opCode).isEqualTo(Operations.EVENT_ACTION)
            }
            readInt().let { version -> assertThat(version).isEqualTo(0) }
            readInt().let { eventType -> assertThat(eventType).isEqualTo(EVENT_TYPE_1) }
            readInt().let { filter -> assertThat(filter).isEqualTo(300) }
            readShort().let { commonFlags ->
                assertWithMessage("Unconditional flag should not be set")
                    .that(commonFlags and FLAG_UNCONDITIONAL)
                    .isEqualTo(0)
                assertWithMessage("NoData flag should not be set")
                    .that(commonFlags and FLAG_NO_DATA)
                    .isEqualTo(0)
            }
            readShort().let { flags -> assertThat(flags).isEqualTo(345) }
            readInt().let { size -> assertThat(size).isEqualTo(2) }
            readInt().let { dataId -> assertThat(dataId).isEqualTo(101) }
            readInt().let { dataId -> assertThat(dataId).isEqualTo(102) }
            readInt().let { size -> assertThat(size).isEqualTo(2) }
            readFloat().let { value -> assertThat(value).isEqualTo(1.0f) }
            readFloat().let { value -> assertThat(value).isEqualTo(2.0f) }
        }
    }

    @Test
    fun `handler with both dataIds and condition - buffer to operation`() {
        // Arrange.
        val doc = CoreDocument().useExperimental()
        val dataIds = intArrayOf(101, 102)
        val condition = floatArrayOf(1.0f, 2.0f)
        val buffer = RemoteComposeBuffer().useExperimental()

        // Act.
        doc.initFromBuffer(
            buffer.apply {
                startEventActions(
                    /* type = */ EVENT_TYPE_1,
                    /* filter = */ 300,
                    /* flags = */ 345,
                    /* dataIds = */ dataIds,
                    /* condition = */ condition,
                )
            }
        )

        // Assert.
        val operations = doc.operations
        assertThat(operations).hasSize(1)
        with(operations[0] as EventActionOperation) {
            assertThat(mType).isEqualTo(EVENT_TYPE_1)
            assertThat(mFilter).isEqualTo(300)
            assertThat(mFlags).isEqualTo(345)
            assertThat(mDataIds).isEqualTo(dataIds)
            assertThat(mCondition).isEqualTo(condition)
        }
    }

    @Test
    fun `startEventActions throws when condition expression is too long`() {
        // Arrange.
        val buffer = RemoteComposeBuffer().useExperimental()
        val excessiveCondition = FloatArray(Limits.MAX_EXPRESSION_SIZE + 1)

        // Act.
        val exception =
            assertFailsWith<RuntimeException> {
                buffer.startEventActions(
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
                // Manually serialize with an unsupported version = 1
                buffer.start(Operations.EVENT_ACTION)
                buffer.writeInt(1) // version = 1 (unsupported)
                buffer.writeInt(EVENT_TYPE_1)
                buffer.writeInt(300)
                buffer.writeShort(FLAG_UNCONDITIONAL or FLAG_NO_DATA)
                buffer.writeShort(345)
            }

        // Act.
        val exception = assertFailsWith<RuntimeException> { doc.initFromBuffer(buffer) }

        // Assert.
        assertThat(exception)
            .hasMessageThat()
            .contains("Unsupported EventActionOperation version: 1")
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

    companion object {
        const val EVENT_TYPE_1 = 1
        const val FLAG_UNCONDITIONAL = 1
        const val FLAG_NO_DATA = 2
    }
}

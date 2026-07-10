/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class RecordingRemoteComposeBufferTest {
    private lateinit var rcPlatform: RcPlatformServices
    private lateinit var recordingRemoteComposeBuffer: RecordingRemoteComposeBuffer
    private lateinit var profileWithRecordingRemoteComposeBuffer: Profile
    private lateinit var profileWithRemoteComposeBuffer: Profile

    val creationDisplayInfo = CreationDisplayInfo(450, 450, 2 * 160)

    @Before
    fun setUp() {
        rcPlatform = mock<RcPlatformServices>()
        recordingRemoteComposeBuffer = RecordingRemoteComposeBuffer()
        profileWithRecordingRemoteComposeBuffer =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                RemoteComposeWriter(
                    profile,
                    recordingRemoteComposeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        profileWithRemoteComposeBuffer =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                RemoteComposeWriter(
                    profile,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }
    }

    @Test
    fun testDependencyReordered() {
        val actualWriter =
            profileWithRecordingRemoteComposeBuffer.create(creationDisplayInfo, "test")
        actualWriter.drawRect(0f, 0f, 10f, 10f)
        // ---
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        var id0 = actualWriter.floatExpression(123.0f)
        var id1 = actualWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        actualWriter.drawRect(id1, 20f, 30f, 30f)
        actualWriter.endConditionalOperations()
        // ---
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        var id2 = actualWriter.floatExpression(20f, id0, AnimatedFloatExpression.ADD)
        actualWriter.drawRect(id2, 30f, 40f, 40f)
        actualWriter.endConditionalOperations()
        recordingRemoteComposeBuffer.writeToBuffer()

        val expectedWriter = profileWithRemoteComposeBuffer.create(creationDisplayInfo, "test")
        expectedWriter.drawRect(0f, 0f, 10f, 10f)
        id0 = expectedWriter.floatExpression(123.0f)
        // ---
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        id1 = expectedWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        expectedWriter.drawRect(id1, 20f, 30f, 30f)
        expectedWriter.addContainerEnd()
        // ---
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        id2 = expectedWriter.floatExpression(20f, id0, AnimatedFloatExpression.ADD)
        expectedWriter.drawRect(id2, 30f, 40f, 40f)
        expectedWriter.addContainerEnd()

        val trimmedExpected =
            expectedWriter.buffer.buffer.buffer.copyOfRange(0, expectedWriter.buffer.buffer.index)
        val trimmedActual =
            actualWriter.buffer.buffer.buffer.copyOfRange(0, actualWriter.buffer.buffer.index)
        Assert.assertArrayEquals(trimmedExpected, trimmedActual)
    }

    @Test
    fun testDependencyReorderedNestedConditionals() {
        val actualWriter =
            profileWithRecordingRemoteComposeBuffer.create(creationDisplayInfo, "test")
        actualWriter.drawRect(0f, 0f, 10f, 10f)
        // ---
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        var id0 = actualWriter.floatExpression(123.0f)
        var id1 = actualWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        actualWriter.drawRect(id1, 20f, 30f, 30f)
        actualWriter.endConditionalOperations()
        actualWriter.endConditionalOperations()
        // ---
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        var id2 = actualWriter.floatExpression(20f, id0, AnimatedFloatExpression.ADD)
        actualWriter.drawRect(id2, 30f, 40f, 40f)
        actualWriter.endConditionalOperations()
        actualWriter.endConditionalOperations()
        recordingRemoteComposeBuffer.writeToBuffer()

        val expectedWriter = profileWithRemoteComposeBuffer.create(creationDisplayInfo, "test")
        expectedWriter.drawRect(0f, 0f, 10f, 10f)
        id0 = expectedWriter.floatExpression(123.0f)
        // ---
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        id1 = expectedWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        expectedWriter.drawRect(id1, 20f, 30f, 30f)
        expectedWriter.addContainerEnd()
        expectedWriter.addContainerEnd()
        // ---
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        id2 = expectedWriter.floatExpression(20f, id0, AnimatedFloatExpression.ADD)
        expectedWriter.drawRect(id2, 30f, 40f, 40f)
        expectedWriter.addContainerEnd()
        expectedWriter.addContainerEnd()

        val trimmedExpected =
            expectedWriter.buffer.buffer.buffer.copyOfRange(0, expectedWriter.buffer.buffer.index)
        val trimmedActual =
            actualWriter.buffer.buffer.buffer.copyOfRange(0, actualWriter.buffer.buffer.index)
        Assert.assertArrayEquals(trimmedExpected, trimmedActual)
    }

    @Test
    fun testExpressionOnlyUsedInConditionalNotReordered() {
        val actualWriter =
            profileWithRecordingRemoteComposeBuffer.create(creationDisplayInfo, "test")
        actualWriter.drawRect(0f, 0f, 10f, 10f)
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        var id0 = actualWriter.floatExpression(123.0f)
        var id1 = actualWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        actualWriter.drawRect(id1, 20f, 30f, 30f)
        actualWriter.endConditionalOperations()
        recordingRemoteComposeBuffer.writeToBuffer()

        val expectedWriter = profileWithRemoteComposeBuffer.create(creationDisplayInfo, "test")
        expectedWriter.drawRect(0f, 0f, 10f, 10f)
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        id0 = expectedWriter.floatExpression(123.0f)
        id1 = expectedWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        expectedWriter.drawRect(id1, 20f, 30f, 30f)
        expectedWriter.addContainerEnd()

        val trimmedExpected =
            expectedWriter.buffer.buffer.buffer.copyOfRange(0, expectedWriter.buffer.buffer.index)
        val trimmedActual =
            actualWriter.buffer.buffer.buffer.copyOfRange(0, actualWriter.buffer.buffer.index)
        Assert.assertArrayEquals(trimmedExpected, trimmedActual)
    }

    @Test
    fun testAddListWithTextFromFloatInConditionalHoisted() {
        val actualWriter =
            profileWithRecordingRemoteComposeBuffer.create(creationDisplayInfo, "test")
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        val id1 = actualWriter.createTextFromFloat(1.2f, 1, 2, 0)
        val id2 = actualWriter.createTextFromFloat(3.4f, 1, 2, 0)
        actualWriter.addList(intArrayOf(id1, id2))
        actualWriter.endConditionalOperations()
        recordingRemoteComposeBuffer.writeToBuffer()

        // Re b/465085573, to work around a rust player bug, we require the list to be hoisted
        // to the root span.
        val expectedWriter = profileWithRemoteComposeBuffer.create(creationDisplayInfo, "test")
        val eid1 = expectedWriter.createTextFromFloat(1.2f, 1, 2, 0)
        val eid2 = expectedWriter.createTextFromFloat(3.4f, 1, 2, 0)
        expectedWriter.addList(intArrayOf(eid1, eid2))

        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        expectedWriter.addContainerEnd()

        val trimmedExpected =
            expectedWriter.buffer.buffer.buffer.copyOfRange(0, expectedWriter.buffer.buffer.index)
        val trimmedActual =
            actualWriter.buffer.buffer.buffer.copyOfRange(0, actualWriter.buffer.buffer.index)
        Assert.assertArrayEquals(trimmedExpected, trimmedActual)
    }

    @Test
    fun testTextTransformOverload() {
        val actualWriter =
            profileWithRecordingRemoteComposeBuffer.create(creationDisplayInfo, "test")
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        val txtId = actualWriter.textCreateId("hello world")
        val id = actualWriter.textTransform(txtId, 1f, 5f, TextTransform.TEXT_TO_UPPERCASE)
        actualWriter.drawTextRun(id, 0, -1, 0, 0, 0f, 0f, false)
        actualWriter.endConditionalOperations()
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        actualWriter.drawTextRun(id, 0, -1, 0, 0, 0f, 0f, false)
        actualWriter.endConditionalOperations()
        recordingRemoteComposeBuffer.writeToBuffer()

        val expectedWriter = profileWithRemoteComposeBuffer.create(creationDisplayInfo, "test")
        val expectedTxtId = expectedWriter.textCreateId("hello world")
        expectedWriter.textTransform(expectedTxtId, 1f, 5f, TextTransform.TEXT_TO_UPPERCASE)
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        expectedWriter.drawTextRun(id, 0, -1, 0, 0, 0f, 0f, false)
        expectedWriter.endConditionalOperations()
        expectedWriter.conditionalOperations(ConditionalOperations.TYPE_NEQ, 0f, 0f)
        expectedWriter.drawTextRun(id, 0, -1, 0, 0, 0f, 0f, false)
        expectedWriter.endConditionalOperations()

        val trimmedExpected =
            expectedWriter.buffer.buffer.buffer.copyOfRange(0, expectedWriter.buffer.buffer.index)
        val trimmedActual =
            actualWriter.buffer.buffer.buffer.copyOfRange(0, actualWriter.buffer.buffer.index)
        Assert.assertArrayEquals(trimmedExpected, trimmedActual)
    }
}

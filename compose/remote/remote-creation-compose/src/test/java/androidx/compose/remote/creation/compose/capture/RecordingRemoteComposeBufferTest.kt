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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
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

    @Before
    fun setUp() {
        rcPlatform = mock<RcPlatformServices>()
        recordingRemoteComposeBuffer = RecordingRemoteComposeBuffer()
        profileWithRecordingRemoteComposeBuffer =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
                width,
                height,
                contentDescription,
                profile ->
                RemoteComposeWriter(
                    profile,
                    recordingRemoteComposeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                    RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, contentDescription),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        profileWithRemoteComposeBuffer =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
                width,
                height,
                contentDescription,
                profile ->
                RemoteComposeWriter(
                    profile,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                    RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, contentDescription),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }
    }

    @Test
    fun testDependencyReordered() {
        val actualWriter = profileWithRecordingRemoteComposeBuffer.create(450, 450, "test")
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

        val expectedWriter = profileWithRemoteComposeBuffer.create(450, 450, "test")
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
        val actualWriter = profileWithRecordingRemoteComposeBuffer.create(450, 450, "test")
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

        val expectedWriter = profileWithRemoteComposeBuffer.create(450, 450, "test")
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
        val actualWriter = profileWithRecordingRemoteComposeBuffer.create(450, 450, "test")
        actualWriter.drawRect(0f, 0f, 10f, 10f)
        actualWriter.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        var id0 = actualWriter.floatExpression(123.0f)
        var id1 = actualWriter.floatExpression(42.0f, id0, AnimatedFloatExpression.ADD)
        actualWriter.drawRect(id1, 20f, 30f, 30f)
        actualWriter.endConditionalOperations()
        recordingRemoteComposeBuffer.writeToBuffer()

        val expectedWriter = profileWithRemoteComposeBuffer.create(450, 450, "test")
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
}

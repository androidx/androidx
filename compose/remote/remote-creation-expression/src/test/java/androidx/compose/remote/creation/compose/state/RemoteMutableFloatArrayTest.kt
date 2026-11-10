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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RecordingRemoteComposeBuffer
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteMutableFloatArrayTest {
    val context = TestRemoteContext()
    val creationState = testCreationState(profiles = RcProfiles.PROFILE_ANDROIDX)

    @Test
    fun arraySet_once_constIndex() {
        val remoteFloatArray = RemoteMutableFloatArray(4)

        remoteFloatArray[2] = 1234.rf
        val result = remoteFloatArray[2]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(1234f)
    }

    @Test
    fun arraySet_twice_constIndex() {
        val remoteFloatArray = RemoteMutableFloatArray(4)

        remoteFloatArray[2] = 1234.rf
        val result1 = remoteFloatArray[2]
        val resultId1 = result1.getIdForCreationState(creationState)
        remoteFloatArray[2] = 5678.rf
        val result2 = remoteFloatArray[2]
        val resultId2 = result2.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId1)).isEqualTo(1234f)
        assertThat(context.getFloat(resultId2)).isEqualTo(5678f)
    }

    @Test
    fun arraySet_once_dynamicIndex() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val index = createNamedRemoteInt("testInt", 2)

        remoteFloatArray[index] = 1234.rf
        val result = remoteFloatArray[2]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(1234f)
    }

    @Test
    fun arraySet_twice_dynamicIndex() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val index = createNamedRemoteInt("testInt", 2)

        remoteFloatArray[index] = 1234.rf
        val result1 = remoteFloatArray[2]
        val resultId1 = result1.getIdForCreationState(creationState)
        remoteFloatArray[index] = 5678.rf
        val result2 = remoteFloatArray[2]
        val resultId2 = result2.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId1)).isEqualTo(1234f)
        assertThat(context.getFloat(resultId2)).isEqualTo(5678f)
    }

    @Test
    fun writeToRemoteComposeCreationState_inside_conditional() {
        val platform = RcPlatformServices.None
        val recordingBuffer = RecordingRemoteComposeBuffer()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                _ ->
                TestRemoteComposeWriter(
                    profile,
                    recordingBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }
        val creationState =
            testCreationState(document = profile.create(CreationDisplayInfo(100, 100, 160), null))
        creationState.document.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f, 0f)
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val index = createNamedRemoteInt("testInt", 2)

        remoteFloatArray[index] = 1234.rf
        val result1 = remoteFloatArray[2]
        val resultId1 = result1.getIdForCreationState(creationState)
        val resultFloatId1 = result1.getFloatIdForCreationState(creationState)
        remoteFloatArray[index] = 5678.rf
        val result2 = remoteFloatArray[2]
        val resultId2 = result2.getIdForCreationState(creationState)
        val resultFloatId2 = result2.getFloatIdForCreationState(creationState)
        creationState.document.translate(resultFloatId1, resultFloatId2)
        creationState.document.endConditionalOperations()

        recordingBuffer.writeToBuffer()

        makeAndPaintCoreDocument(creationState)

        assertThat(context.getFloat(resultId1)).isEqualTo(1234f)
        assertThat(context.getFloat(resultId2)).isEqualTo(5678f)
    }

    @Test
    fun arrayGet_initialValue_isZero() {
        val remoteFloatArray = RemoteMutableFloatArray(4)

        val result = remoteFloatArray[2]

        assertThat(result.hasConstantValue).isTrue()
        assertThat(result.constantValue).isEqualTo(0f)
    }

    @Test
    fun arrayGet_cachedValue_isSameObject() {
        val remoteFloatArray = RemoteMutableFloatArray(4)

        val val1 = remoteFloatArray[2]
        val val2 = remoteFloatArray[2]

        assertThat(val1).isSameInstanceAs(val2)
    }

    @Test
    fun arraySet_constIndex_updatesCache() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val value = 1234.rf

        remoteFloatArray[2] = value
        val result = remoteFloatArray[2]

        assertThat(result).isSameInstanceAs(value)
    }

    @Test
    fun arraySet_constRemoteInt_updatesCache() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val index = 2.ri
        val value = 1234.rf

        remoteFloatArray[index] = value
        val result = remoteFloatArray[2]

        assertThat(result).isSameInstanceAs(value)
    }

    @Test
    fun arraySet_dynamicIndex_clearsCache() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val dynamicIndex = createNamedRemoteInt("testInt", 2)

        remoteFloatArray[1] = 123.rf
        assertThat(remoteFloatArray[1].constantValue).isEqualTo(123f)

        // Setting a dynamic index should clear the cache
        remoteFloatArray[dynamicIndex] = 456.rf

        val resultAfterDynamicSet = remoteFloatArray[1]
        // It should no longer be a constant from cache, but a dynamic expression
        assertThat(resultAfterDynamicSet.hasConstantValue).isFalse()
    }

    @Test
    fun toDebugString_stateArray() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        assertThat(remoteFloatArray.toDebugString()).isEqualTo("mutableFloatArray(size=4)")
    }

    @Test
    fun toDebugString_indexing() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val dynIdx = createNamedRemoteInt("i", 1)
        val result = remoteFloatArray[dynIdx]
        assertThat(result.toDebugString()).isEqualTo("mutableFloatArray(size=4)[user:i]")
    }

    private fun makeAndPaintCoreDocument(cs: RemoteComposeCreationState = creationState) =
        context.loadDocument(cs)
}

private class TestRemoteComposeWriter(
    profile: Profile,
    buffer: RemoteComposeBuffer,
    vararg tags: HTag,
) : RemoteComposeWriter(profile, buffer, *tags)

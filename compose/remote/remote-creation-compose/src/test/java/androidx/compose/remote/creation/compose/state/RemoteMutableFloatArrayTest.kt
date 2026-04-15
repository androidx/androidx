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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteMutableFloatArrayTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState =
        RemoteComposeCreationState(
            AndroidxRcPlatformServices(),
            Size(1f, 1f),
            CoreDocument.DOCUMENT_API_LEVEL,
            PROFILE_ANDROIDX,
        )

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
        val index = RemoteInt.createNamedRemoteInt("testInt", 2)

        remoteFloatArray[index] = 1234.rf
        val result = remoteFloatArray[2]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(1234f)
    }

    @Test
    fun arraySet_twice_dynamicIndex() {
        val remoteFloatArray = RemoteMutableFloatArray(4)
        val index = RemoteInt.createNamedRemoteInt("testInt", 2)

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

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}

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

package androidx.compose.remote.frontend.state

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 26)
@RunWith(RobolectricTestRunner::class)
class RemoteDpTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

    @Test
    fun constructor_createsCorrectly() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = RemoteDp(remoteFloat)

        assertThat(remoteFloatDp.value).isEqualTo(remoteFloat)
    }

    @Test
    fun newInstance_hasSameFloatValueAsOriginalRemoteFloat() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = RemoteDp(remoteFloat)
        val resultId = remoteFloat.getIdForCreationState(creationState)
        val resultDpId = remoteFloatDp.value.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(floatValue)
        assertThat(context.getFloat(resultDpId)).isEqualTo(floatValue)
    }

    @Test
    fun newInstance_hasSameIdFromOriginalRemoteFloat() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = RemoteDp(remoteFloat)

        val resultId = remoteFloat.getIdForCreationState(creationState)
        val resultDpId = remoteFloatDp.value.getIdForCreationState(creationState)

        assertThat(resultId).isEqualTo(resultDpId)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}

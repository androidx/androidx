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
class RemoteFloatArrayTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

    @Test
    fun arrayDeref_fetchesValueFromArray() {
        val remoteFloatArray = RemoteFloatArray(listOf(1.rf, 2.rf, 3.rf, 4.rf))

        val result = remoteFloatArray[1.rf]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(2f)
    }

    @Test
    fun arrayDeref_fetchesVariableFromArray() {
        val remoteFloatArray =
            RemoteFloatArray(listOf(1.rf, RemoteFloat(2.rf.internalAsFloat()), 3.rf))

        val result = remoteFloatArray[1.rf]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(2f)
    }

    @Test
    fun arrayDeref_variableIndexFetchesFromArray() {
        val remoteFloatArray = RemoteFloatArray(listOf(1.rf, 2.rf, 3.rf, 4.rf))
        val index = RemoteFloat(1.rf.internalAsFloat())

        val result = remoteFloatArray[index]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(2f)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}

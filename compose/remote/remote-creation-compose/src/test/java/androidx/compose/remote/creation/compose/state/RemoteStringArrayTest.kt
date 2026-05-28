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

package androidx.compose.remote.creation.compose.state

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteStringArrayTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    @Test
    fun arrayDeref_fetchesValueFromArray() {
        val remoteStringArray = RemoteStringArray(listOf("A".rs, "B".rs, "C".rs))

        val result = remoteStringArray[1]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("B")
    }

    @Test
    fun arrayDeref_fetchesVariableFromArray() {
        val remoteStringArray =
            RemoteStringArray(
                listOf("A".rs, RemoteString.createNamedRemoteString("test", "B"), "C".rs)
            )

        val result = remoteStringArray[1]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("B")
    }

    @Test
    fun arrayDeref_variableIntIndexFetchesFromArray() {
        val remoteStringArray = RemoteStringArray(listOf("A".rs, "B".rs, "C".rs))
        val index = 1.ri

        val result = remoteStringArray[index]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("B")
    }

    @Test
    fun arrayDeref_variableFloatIndexFetchesFromArray() {
        val remoteStringArray = RemoteStringArray(listOf("A".rs, "B".rs, "C".rs))
        val index = 1f.rf

        val result = remoteStringArray[index.toRemoteInt()]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("B")
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}

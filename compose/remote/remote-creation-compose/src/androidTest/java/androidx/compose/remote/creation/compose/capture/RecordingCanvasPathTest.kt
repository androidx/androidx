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

package androidx.compose.remote.creation.compose.capture

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RecordingRemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RecordingCanvasPathTest {
    private val recordingBuffer = RecordingRemoteComposeBuffer()
    private val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, _ ->
            RemoteComposeWriter(
                profile,
                recordingBuffer,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        }

    private val creationState = RemoteComposeCreationState(Size(400f, 400f), profile)
    private val recordingCanvas =
        RecordingCanvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    @Before
    fun setUp() {
        recordingCanvas.setRemoteComposeCreationState(creationState)
    }

    private fun inflateOperations(): ArrayList<Operation> {
        recordingCanvas.flush()
        recordingBuffer.writeToBuffer()
        val buffer = creationState.document.buffer
        buffer.buffer.index = 0
        val operations = ArrayList<Operation>()
        buffer.inflateFromBuffer(operations)
        return operations
    }

    @Test
    fun testPathMutationBeforeFlushPreservesGeometry() {
        val path =
            Path().apply {
                moveTo(10f, 10f)
                lineTo(100f, 100f)
            }
        recordingCanvas.drawPath(path, Paint())

        // Mutate and clear the path prior to calling flush()
        path.reset()

        val operations = inflateOperations()
        val pathData = operations.filterIsInstance<PathData>().first()
        assertThat(pathData.toString()).contains("100.0 100.0")
    }
}

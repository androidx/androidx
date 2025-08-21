/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun bitmap(width: Int, height: Int) =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

@RunWith(RobolectricTestRunner::class)
class RemoteBitmapFontTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState =
        RemoteComposeCreationState(
            AndroidxPlatformServices(),
            density = 1f,
            Size(1f, 1f),
            CoreDocument.DOCUMENT_API_LEVEL,
            Operations.PROFILE_ANDROIDX,
        )

    val bitmapFont =
        RemoteBitmapFont(
            listOf(
                RemoteBitmapFont.Glyph("a", bitmap(10, 20), 1, 2, 3, 4),
                RemoteBitmapFont.Glyph("b", bitmap(20, 20), 10, 20, 30, 40),
                RemoteBitmapFont.Glyph("c", bitmap(30, 20), 2, 4, 6, 8),
                RemoteBitmapFont.Glyph(" ", null, 20, 0, 0, 0),
            )
        )

    @Test
    fun measureWidth() {
        val result = bitmapFont.measureWidth(RemoteString("ab c"))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // Width is the sum of:
        // a: 1 + 10 + 3 = 14
        // b: 10 + 20 + 30 = 60
        //  : 20 = 20
        // c: 2 + 30 + 6 = 38
        assertThat(context.getInteger(resultId)).isEqualTo(14 + 60 + 20 + 38)
    }

    @Test
    fun measureHeight() {
        val result = bitmapFont.measureHeight(RemoteString("ab c"))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // Height is the max of:
        // a: 2 + 20 + 4 = 26
        // b: 20 + 20 + 40 = 70
        //  : 0
        // c: 4 + 20 + 8 = 32
        assertThat(context.getInteger(resultId)).isEqualTo(80)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}

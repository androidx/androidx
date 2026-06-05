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

package androidx.compose.remote.player.compose

import android.graphics.Bitmap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.player.compose.impl.RemoteDocumentComposePlayer
import androidx.compose.remote.testing.LimitsRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeImageSizeTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val limitsRule = LimitsRule().enableImageUrls(true).enableImageFiles(true)

    @Test
    fun testComposePlayer_inlineLargePngFails() {
        val declaredWidth = 10
        val declaredHeight = 10
        val attackWidth = 2000
        val attackHeight = 2000

        val bigImage = Bitmap.createBitmap(attackWidth, attackHeight, Bitmap.Config.ARGB_8888)
        val bos = ByteArrayOutputStream()
        bigImage.compress(Bitmap.CompressFormat.PNG, 100, bos)
        val bigPngBytes = bos.toByteArray()
        bigImage.recycle()

        val buffer = RemoteComposeBuffer()
        buffer.header(600, 600, 1.0f, 0L)
        buffer.storeBitmap(1, declaredWidth, declaredHeight, bigPngBytes)
        buffer.addDrawBitmap(1, 0f, 0f, 100f, 100f, 0)

        val size = buffer.buffer.size
        val b = buffer.buffer.buffer.copyOf(size)

        val remoteComposeDocument: CoreDocument?

        try {
            remoteComposeDocument =
                CoreDocument().apply {
                    ByteArrayInputStream(b).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
        } catch (e: Exception) {
            Truth.assertThat(e).hasMessageThat().contains("invalid size")
            println("successfully caught exception")
            return
        }
        assertThrows(RuntimeException::class.java) {
                composeTestRule.setContent {
                    RemoteDocumentComposePlayer(
                        document = remoteComposeDocument,
                        documentWidth = 200,
                        documentHeight = 200,
                    )
                }
                composeTestRule.waitForIdle()
            }
            .also { assertThat(it).hasMessageThat().contains("dimensions don't match") }
    }
}

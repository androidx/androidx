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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Security regression: an inline PNG whose *declared* dimensions are tiny but whose *actual*
 * decoded dimensions are huge (a decompression-bomb / dimension-spoof attack) must be rejected, not
 * loaded.
 *
 * Ported to the embedded [RcPlayer] from the deleted `ComposeImageSizeTest`, which exercised the
 * removed legacy `PaintContext`-based player. In the embedded player, bitmaps are decoded eagerly
 * when the player first composes (`BitmapData.apply` -> `AndroidRemoteContext.loadBitmap` ->
 * `RemoteBitmapDecoder`, which bounds-checks the declared-vs-actual size). So the attack is
 * rejected either at document load ("invalid size") or when `RcPlayer` composes ("dimensions don't
 * match").
 *
 * Runs under Robolectric `NATIVE` graphics so `BitmapFactory` decodes real PNG bounds.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerImageSizeTest {

    @get:Rule val rule = createComposeRule()

    private fun Throwable.chainMessages(): String =
        generateSequence(this) { it.cause }.joinToString("\n") { it.message ?: it.toString() }

    @Test
    fun oversizedInlinePngIsRejected() {
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
        // Declares 10x10 but the bytes decode to 2000x2000.
        buffer.storeBitmap(1, declaredWidth, declaredHeight, bigPngBytes)
        buffer.addDrawBitmap(1, 0f, 0f, 100f, 100f, 0)

        val size = buffer.buffer.size
        val bytes = buffer.buffer.buffer.copyOf(size)

        val document: CoreDocument
        try {
            document =
                CoreDocument().apply {
                    ByteArrayInputStream(bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
        } catch (e: Exception) {
            // Rejected already at document load.
            assertThat(e.chainMessages()).contains("invalid size")
            return
        }

        // Otherwise it must be rejected when the embedded player decodes the bitmap during
        // composition — the decoder refuses the declared-vs-actual dimension mismatch.
        val thrown =
            assertThrows(Throwable::class.java) {
                rule.setContent { RcPlayer(document = document) }
                rule.waitForIdle()
            }
        assertThat(thrown.chainMessages()).contains("dimensions don't match")
    }
}

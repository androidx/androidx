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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.BitmapData
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcUrlBitmapParseTest {

    private fun createUrlBitmapBuffer(): ByteArray {
        val buffer = WireBuffer()
        BitmapData.apply(
            buffer,
            1, // imageId
            BitmapData.TYPE_PNG_8888,
            100.toShort(),
            BitmapData.ENCODING_URL,
            100.toShort(),
            "https://example.com/test.png".toByteArray(),
        )
        return buffer.buffer
    }

    @Test
    fun urlBitmapFailsWhenLimitsDisabled() {
        val bytes = createUrlBitmapBuffer()
        Limits.ENABLE_IMAGE_URLS = false

        assertThrows(RuntimeException::class.java) {
            val doc = CoreDocument(RemoteClock.SYSTEM)
            ByteArrayInputStream(bytes).use {
                doc.initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    @Test
    fun urlBitmapSucceedsWhenLimitsEnabled() {
        val bytes = createUrlBitmapBuffer()
        RemoteImageSupport.enableEncodedImageReferences()

        val doc = CoreDocument(RemoteClock.SYSTEM)
        ByteArrayInputStream(bytes).use {
            doc.initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
        }

        assertThat(doc.getOperationsReflection()).isNotEmpty()
    }
}

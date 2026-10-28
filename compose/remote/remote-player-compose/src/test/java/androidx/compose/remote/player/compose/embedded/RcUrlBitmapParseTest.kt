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

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCapturedDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcUrlBitmapParseTest {

    private suspend fun createUrlBitmapDocument(): RemoteCapturedDocument {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return captureSingleRemoteDocument(context = context) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                document.addBitmapUrl("https://example.com/test.png", 100, 100)
            }
        }
    }

    @Test
    fun urlBitmapFailsWhenLimitsDisabled(): Unit = runBlocking {
        val captured = createUrlBitmapDocument()
        Limits.ENABLE_IMAGE_URLS = false

        assertThrows(RuntimeException::class.java) {
            val doc = CoreDocument(RemoteClock.SYSTEM)
            ByteArrayInputStream(captured.bytes).use {
                doc.initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    @Test
    fun urlBitmapSucceedsWhenLimitsEnabled(): Unit = runBlocking {
        val captured = createUrlBitmapDocument()
        Limits.ENABLE_IMAGE_URLS = false

        val state = RcPlayerState(captured)

        assertThat(state.document.operations).isNotEmpty()
    }
}

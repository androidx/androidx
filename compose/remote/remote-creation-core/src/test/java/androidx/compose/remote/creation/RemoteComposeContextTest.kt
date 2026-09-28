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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteComposeContextTest {
    private val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            RcPlatformServices.None,
        ) { info, p, callback ->
            RemoteComposeWriter(info, null, p, callback)
        }

    @Test
    fun bufferSize_notCompressing_isRawDocumentSize() {
        val context = createContext()

        assertThat(context.bufferSize()).isEqualTo(context.writer.bufferSize())
        assertThat(context.buffer()).hasLength(context.bufferSize())
    }

    @Test
    fun bufferSize_compressing_isSizeOfCompressedBuffer() {
        val context =
            createContext(RemoteComposeWriter.hTag(Header.COMPRESS, Header.COMPRESSION_DEFLATE))

        val buffer = context.buffer()

        // Callers copy buffer() up to bufferSize(): any other size would corrupt the zlib stream.
        assertThat(context.bufferSize()).isEqualTo(buffer.size)
        assertThat(buffer.size).isLessThan(context.writer.bufferSize())
        assertThat(Header.decompressDocument(buffer, context.bufferSize()))
            .isEqualTo(context.writer.buffer().copyOf(context.writer.bufferSize()))
    }

    private fun createContext(vararg tags: RemoteComposeWriter.HTag): RemoteComposeContext =
        RemoteComposeContext(
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 300),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, profile.operationsProfiles),
            *tags,
            profile = profile,
        ) {
            root { column { repeat(20) { writer.addText("Item $it") } } }
        }
}

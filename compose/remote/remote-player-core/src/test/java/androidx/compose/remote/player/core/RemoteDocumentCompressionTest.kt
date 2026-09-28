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

package androidx.compose.remote.player.core

import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TextData
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Players load documents through [RemoteDocument], which decompresses them transparently. */
class RemoteDocumentCompressionTest {
    private val plain = createDocument()
    private val compressed = Header.compressDocument(plain, plain.size)

    @Test
    fun byteArray_compressedDocument_loadsLikeUncompressedDocument() {
        val document = RemoteDocument(compressed)

        assertTrue(compressed.size < plain.size)
        assertEquals(operationNames(RemoteDocument(plain)), operationNames(document))
        assertEquals(
            (1..40).map { "Item $it" },
            document.document.operations.filterIsInstance<TextData>().map { it.mText },
        )
    }

    @Test
    fun inputStream_compressedDocument_loadsLikeUncompressedDocument() {
        val document = RemoteDocument(ByteArrayInputStream(compressed))

        assertEquals(operationNames(RemoteDocument(plain)), operationNames(document))
    }

    @Test
    fun reinflate_compressedDocument_keepsOperations() {
        val document = RemoteDocument(compressed)
        val operations = operationNames(document)

        document.reinflate()

        assertEquals(operations, operationNames(document))
    }

    @Test
    fun truncatedCompressedDocument_throws() {
        val truncated = compressed.copyOf(compressed.size - 8)

        assertThrows(RuntimeException::class.java) { RemoteDocument(truncated) }
    }

    private fun operationNames(document: RemoteDocument): List<String> =
        document.document.operations.map { it.javaClass.simpleName }

    /** A document with repetitive texts, like real layouts, so it compresses well. */
    private fun createDocument(): ByteArray {
        val buffer = RemoteComposeBuffer()
        Header.apply(
            buffer.buffer,
            8,
            shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT),
            arrayOf<Any>(300, 400),
        )
        for (i in 1..40) {
            buffer.addText(i, "Item $i")
        }
        return buffer.buffer.cloneBytes()
    }
}

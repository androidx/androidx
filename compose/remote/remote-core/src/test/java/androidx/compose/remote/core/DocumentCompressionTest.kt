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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DocumentCompressionTest {
    private val profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
    private val profile =
        Profile(CoreDocument.DOCUMENT_API_LEVEL, profiles, RcPlatformServices.None) { info, p, _ ->
            RemoteComposeWriter(info, null, p)
        }

    private val plain = createDocument()
    private val compressed = Header.compressDocument(plain, plain.size)

    @Test
    fun compressDocument_keepsHeaderReadable() {
        val header = Header.readDirect(ByteArrayInputStream(compressed))

        assertThat(header.getInt(Header.COMPRESS.toInt(), 0)).isEqualTo(Header.COMPRESSION_DEFLATE)
        assertThat(header.getInt(Header.DOC_WIDTH.toInt(), 0)).isEqualTo(300)
        assertThat(header.getInt(Header.DOC_HEIGHT.toInt(), 0)).isEqualTo(400)
        assertThat(header.profiles).isEqualTo(profiles)
        assertThat(compressed.size).isLessThan(plain.size / 2)
    }

    @Test
    fun decompressDocument_restoresOriginalDocument() {
        assertThat(Header.decompressDocument(compressed, compressed.size)).isEqualTo(plain)
    }

    @Test
    fun compressAndDecompress_documentAlreadyInTargetForm_returnsCopy() {
        assertThat(Header.compressDocument(compressed, compressed.size)).isEqualTo(compressed)
        assertThat(Header.decompressDocument(plain, plain.size)).isEqualTo(plain)
    }

    @Test
    fun compressDocument_legacyHeader_throws() {
        val legacy =
            RemoteComposeWriter(100, 100, "legacy", RcPlatformServices.None).encodeToByteArray()

        assertThrows(IllegalArgumentException::class.java) {
            Header.compressDocument(legacy, legacy.size)
        }
    }

    @Test
    fun inflateFromBuffer_compressedDocument_inflatesDecompressedOperations() {
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(compressed))
        val operations = ArrayList<Operation>()

        buffer.inflateFromBuffer(operations)

        assertThat(operations.map { it.javaClass })
            .containsExactlyElementsIn(inflate(plain).map { it.javaClass })
            .inOrder()
        // The buffer now holds the decompressed document, so reinflating won't decompress again.
        assertThat(buffer.buffer.cloneBytes()).isEqualTo(plain)
    }

    @Test
    fun initFromBuffer_compressedDocument_canBeReinflated() {
        val document = CoreDocument()
        document.initFromBuffer(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(compressed))
        )
        val operationCount = document.operations.size

        document.reinflate()

        assertThat(document.operations).hasSize(operationCount)
        assertThat(document.profileMask).isEqualTo(profiles)
        assertThat(document.mHeader!!.get(Header.COMPRESS)).isNull()
    }

    @Test
    fun decompressDocument_compressPropertyInMiddleOfHeader_isRemoved() {
        // Other producers may write COMPRESS anywhere among the properties, not just last.
        val document =
            withHeader(
                Header.DOC_WIDTH to 300,
                Header.COMPRESS to Header.COMPRESSION_DEFLATE,
                Header.DOC_PROFILES to profiles,
                body = zlib(plainBody),
            )

        val restored = Header.decompressDocument(document, document.size)

        val restoredHeader = Header.readDirect(ByteArrayInputStream(restored))
        assertThat(restoredHeader.get(Header.COMPRESS)).isNull()
        assertThat(restoredHeader.getInt(Header.DOC_WIDTH.toInt(), 0)).isEqualTo(300)
        assertThat(restoredHeader.profiles).isEqualTo(profiles)
        assertThat(restored.copyOfRange(headerSize(restored), restored.size)).isEqualTo(plainBody)
    }

    @Test
    fun decompressDocument_unsupportedCompression_throws() {
        // compressDocument() appends COMPRESS last, so its value ends the header.
        val document = compressed.copyOf()
        ByteBuffer.wrap(document).putInt(headerSize(document) - 4, 2)

        val e =
            assertThrows(IOException::class.java) {
                Header.decompressDocument(document, document.size)
            }
        assertThat(e).hasMessageThat().contains("Unsupported")
    }

    @Test
    fun decompressDocument_truncatedDocument_throws() {
        val document = compressed.copyOf(compressed.size - 10)

        val e =
            assertThrows(IOException::class.java) {
                Header.decompressDocument(document, document.size)
            }
        assertThat(e).hasMessageThat().contains("Truncated")
    }

    @Test
    fun decompressDocument_trailingData_throws() {
        val document = compressed + byteArrayOf(0, 0, 0, 0)

        val e =
            assertThrows(IOException::class.java) {
                Header.decompressDocument(document, document.size)
            }
        assertThat(e).hasMessageThat().contains("Unexpected data")
    }

    @Test
    fun inflateFromBuffer_corruptedDocument_throws() {
        val document = compressed.copyOf()
        val middle = (headerSize(document) + document.size) / 2
        document[middle] = (document[middle].toInt() xor 0xFF).toByte()
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document))

        val e = assertThrows(RuntimeException::class.java) { buffer.inflateFromBuffer(ArrayList()) }
        assertThat(e).hasCauseThat().isInstanceOf(IOException::class.java)
    }

    @Test
    fun decompressDocument_respectsSizeLimit() {
        val bodySize = plain.size - headerSize(plain)
        val limit = Limits.MAX_DECOMPRESSED_SIZE
        try {
            Limits.MAX_DECOMPRESSED_SIZE = bodySize
            assertThat(Header.decompressDocument(compressed, compressed.size)).isEqualTo(plain)

            Limits.MAX_DECOMPRESSED_SIZE = bodySize - 1
            val e =
                assertThrows(IOException::class.java) {
                    Header.decompressDocument(compressed, compressed.size)
                }
            assertThat(e).hasMessageThat().contains("exceeds")
        } finally {
            Limits.MAX_DECOMPRESSED_SIZE = limit
        }
    }

    @Test
    fun readDirect_peeksHeaderWithoutCompressedOperations() {
        // The header alone is enough to peek at a document, e.g. at the start of a download.
        val header = Header.readDirect(ByteArrayInputStream(compressed, 0, headerSize(compressed)))

        assertThat(header.get(Header.COMPRESS)).isEqualTo(Header.COMPRESSION_DEFLATE)
        assertThat(header.getInt(Header.DOC_WIDTH.toInt(), 0)).isEqualTo(300)
        assertThat(header.profiles).isEqualTo(profiles)
    }

    @Test
    fun compressDocument_isDeterministic() {
        // Flow captures drop documents equal to the previous one, which relies on equal documents
        // compressing to equal bytes.
        assertThat(Header.compressDocument(plain, plain.size)).isEqualTo(compressed)
    }

    @Test
    fun compressAndDecompress_ignoreBytesPastSize() {
        // Writers pass their whole backing array along with the number of bytes used.
        val padding = ByteArray(16) { 0x55 }

        assertThat(Header.compressDocument(plain + padding, plain.size)).isEqualTo(compressed)
        assertThat(Header.decompressDocument(compressed + padding, compressed.size))
            .isEqualTo(plain)
    }

    @Test
    fun compressDocument_headerOnlyDocument_roundTrips() {
        val header = plain.copyOf(headerSize(plain))

        val compressedHeader = Header.compressDocument(header, header.size)

        assertThat(Header.decompressDocument(compressedHeader, compressedHeader.size))
            .isEqualTo(header)
    }

    @Test
    fun compressDocument_notADocument_throws() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        assertThrows(IllegalArgumentException::class.java) {
            Header.compressDocument(bytes, bytes.size)
        }
    }

    @Test
    fun decompressDocument_explicitlyUncompressed_returnsDocumentAsIs() {
        val document =
            withHeader(
                Header.DOC_WIDTH to 300,
                Header.COMPRESS to Header.COMPRESSION_NONE,
                Header.DOC_PROFILES to profiles,
            )

        assertThat(Header.decompressDocument(document, document.size)).isEqualTo(document)
        assertThat(inflate(document).map { it.javaClass })
            .containsExactlyElementsIn(inflate(plain).map { it.javaClass })
            .inOrder()
    }

    @Test
    fun decompressDocument_nonIntegerCompression_throws() {
        val document = withHeader(Header.COMPRESS to "deflate", Header.DOC_PROFILES to profiles)

        val e =
            assertThrows(IOException::class.java) {
                Header.decompressDocument(document, document.size)
            }
        assertThat(e).hasMessageThat().contains("Unsupported")
    }

    @Test
    fun inflateFromBuffer_nonIntegerCompression_throwsWithIOExceptionCause() {
        val document = withHeader(Header.COMPRESS to "deflate", Header.DOC_PROFILES to profiles)
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document))

        val e = assertThrows(RuntimeException::class.java) { buffer.inflateFromBuffer(ArrayList()) }
        assertThat(e).hasCauseThat().isInstanceOf(IOException::class.java)
    }

    @Test
    fun compressedOperations_startWithAnOpcodeNoPlayerKnows() {
        // Players that can't decompress read the zlib stream as operations. It starts with 0x78
        // (DEFLATE, 32 KB window), which no API level uses as an opcode, so those players stop on
        // an unknown operation instead of misreading the document.
        val firstByte = compressed[headerSize(compressed)].toInt() and 0xFF

        assertThat(firstByte).isEqualTo(0x78)
        for (apiLevel in 6..CoreDocument.DOCUMENT_API_LEVEL + 1) {
            assertThat(Operations.getOperation(apiLevel, firstByte)).isNull()
        }
    }

    /** A column of texts: repetitive, like real layouts, so it compresses well. */
    private fun createDocument(): ByteArray {
        val writer =
            RemoteComposeWriter(
                profile,
                RemoteComposeBuffer(profile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 300),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profiles),
            )
        val texts = (1..40).map { writer.textCreateId("Item $it") }
        writer.root {
            for (text in texts) {
                writer.startTextComponent(RecordingModifier(), text, -1, 0)
                writer.endTextComponent()
            }
        }
        return writer.encodeToByteArray()
    }

    private fun inflate(document: ByteArray): List<Operation> {
        val operations = ArrayList<Operation>()
        RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document))
            .inflateFromBuffer(operations)
        return operations
    }

    /** Returns the size of the header that starts [document], as found by the header parser. */
    private fun headerSize(document: ByteArray): Int {
        val stream = ByteArrayInputStream(document)
        Header.readDirect(stream)
        return document.size - stream.available()
    }

    /** The operations of [plain], without its header. */
    private val plainBody: ByteArray
        get() = plain.copyOfRange(headerSize(plain), plain.size)

    /** Returns a document made of a header holding [properties], followed by [body]. */
    private fun withHeader(
        vararg properties: Pair<Short, Any>,
        body: ByteArray = plainBody,
    ): ByteArray {
        val header = WireBuffer()
        Header.apply(
            header,
            CoreDocument.DOCUMENT_API_LEVEL,
            properties.map { it.first }.toShortArray(),
            properties.map { it.second }.toTypedArray(),
        )
        return header.cloneBytes() + body
    }

    private fun zlib(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }
}

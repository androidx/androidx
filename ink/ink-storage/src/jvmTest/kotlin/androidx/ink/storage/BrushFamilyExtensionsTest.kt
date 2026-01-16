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

package androidx.ink.storage

import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import java.util.zip.GZIPOutputStream
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalInkCustomBrushApi::class)
class BrushFamilyExtensionsTest {

    private val notGzippedBytes = byteArrayOf(0)

    private val gzippedNotProtoBytes =
        ByteArrayOutputStream().use { byteArrayStream ->
            GZIPOutputStream(byteArrayStream).use { it.write(notGzippedBytes) }
            byteArrayStream.toByteArray()
        }

    /**
     * Gzipped binary-proto of a BrushFamily that fails validation. Generated with:
     * ```
     * val invalidProto = brushFamily {
     *   coats += brushCoat { tip = brushTip { particleGapDurationSeconds = -1f } }
     * }
     * val invalidProtoBytes =
     *   ByteArrayOutputStream().use { byteArrayStream ->
     *     GZIPOutputStream(byteArrayStream).use { gzipStream ->
     *       gzipStream.write(invalidProto.toByteArray())
     *     }
     *     byteArrayStream.toByteArray()
     *   }
     * Base64.getEncoder().encodeToString(invalidProtoBytes)
     * ```
     */
    private val gzippedInvalidProtoBytes =
        Base64.getDecoder().decode("H4sIAAAAAAAA/1Ni52INZWBo2A8Agg/YJAkAAAA=")

    @Test
    fun encode_decode_roundTrip() {
        // This wraps the native encode/decode, so the details are tested in the tests for the
        // underlying C++ library.
        val original = BrushFamily()
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use { assertThat(BrushFamily.decode(it)).isEqualTo(original) }
    }

    @Test
    fun decode_notGzippedBytes_throws() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue")
            ByteArrayInputStream(notGzippedBytes).use { BrushFamily.decode(it) }
        }
    }

    @Test
    fun decode_gzippedNotProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                ByteArrayInputStream(gzippedNotProtoBytes).use { BrushFamily.decode(it) }
            }
        assertThat(exception).hasMessageThat().contains("Failed to parse ink.proto.BrushFamily")
    }

    @Test
    fun decode_gzippedInvalidProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                ByteArrayInputStream(gzippedInvalidProtoBytes).use { BrushFamily.decode(it) }
            }
        assertThat(exception).hasMessageThat().contains("particle_gap_duration")
    }

    @Test
    fun encode_decode_roundTrip_staticApi() {
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = BrushFamily()
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamilySerialization.decode(it)).isEqualTo(original)
        }
    }

    @Test
    fun decode_notGzippedBytes_throws_staticApi() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue")
            ByteArrayInputStream(notGzippedBytes).use { BrushFamilySerialization.decode(it) }
        }
    }
}

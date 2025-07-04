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

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalInkCustomBrushApi::class)
class AndroidBrushFamilyExtensionsTest {

    private val textureId1: String = "texture_id_1"
    private val textureId2: String = "texture_id_2"
    private val unknownId: String = "unknown_id"

    private val testBitmap1x4: Bitmap =
        Bitmap.createBitmap(
            intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE),
            1,
            4,
            Bitmap.Config.ARGB_8888,
        )

    private val testBitmap2x2: Bitmap =
        Bitmap.createBitmap(
            intArrayOf(0x00112233, 0x66554433, 0x33442211, 0x22334455),
            2,
            2,
            Bitmap.Config.ARGB_8888,
        )
    private val textureBitmapStore =
        object : TextureBitmapStore {
            val store = mapOf(textureId1 to testBitmap1x4, textureId2 to testBitmap2x2)

            override fun get(clientTextureId: String): Bitmap? = store[clientTextureId]
        }

    private val testFamily =
        BrushFamily(
            coats =
                listOf(
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId1,
                                            sizeX = 1f,
                                            sizeY = 4f,
                                        ),
                                        BrushPaint.TextureLayer(
                                            clientTextureId = unknownId,
                                            sizeX = 2f,
                                            sizeY = 5f,
                                        ),
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId2,
                                            sizeX = 2f,
                                            sizeY = 2f,
                                        ),
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId2,
                                            sizeX = 2f,
                                            sizeY = 2f,
                                        ),
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId1,
                                            sizeX = 1f,
                                            sizeY = 4f,
                                        ),
                                    )
                            )
                    ),
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId1,
                                            sizeX = 1f,
                                            sizeY = 4f,
                                        ),
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId2,
                                            sizeX = 2f,
                                            sizeY = 2f,
                                        ),
                                        BrushPaint.TextureLayer(
                                            clientTextureId = textureId1,
                                            sizeX = 1f,
                                            sizeY = 4f,
                                        ),
                                    )
                            )
                    ),
                )
        )

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
        Base64.decode("H4sIAAAAAAAA/1Ni52INZWBo2A8Agg/YJAkAAAA=", Base64.DEFAULT)

    @Test
    fun decode_notGzippedBytes_throws() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue")
            ByteArrayInputStream(notGzippedBytes).use { BrushFamily.decode(it, { _, _ -> "id" }) }
        }
    }

    @Test
    fun decode_gzippedNotProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                ByteArrayInputStream(gzippedNotProtoBytes).use {
                    BrushFamily.decode(it, { _, _ -> "id" })
                }
            }
        assertThat(exception).hasMessageThat().contains("Failed to parse ink.proto.BrushFamily")
    }

    @Test
    fun decode_gzippedInvalidProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                ByteArrayInputStream(gzippedInvalidProtoBytes).use {
                    BrushFamily.decode(it, { _, _ -> "id" })
                }
            }
        assertThat(exception).hasMessageThat().contains("particle_gap_duration")
    }

    @Test
    fun decode_callbackThrows_exceptionIsRaised() {
        val original = testFamily
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it, textureBitmapStore)
                it.toByteArray()
            }
        val callbackException = RuntimeException("callbackException")
        val resultException =
            assertFailsWith<RuntimeException> {
                ByteArrayInputStream(encoded).use {
                    @Suppress("CheckReturnValue")
                    BrushFamily.decode(it, { _, _ -> throw callbackException })
                }
            }
        assertThat(resultException).isSameInstanceAs(callbackException)
    }

    @Test
    fun encode_decode_roundTrip_staticApi() {
        val decodedTextureBitmapStore = mutableMapOf<String, Bitmap>()
        val decodeCallback = BrushFamilyDecodeCallback { id: String, bitmap: Bitmap? ->
            if (bitmap != null) {
                decodedTextureBitmapStore[id] = bitmap
            }
            id
        }
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = testFamily
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it, textureBitmapStore)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(AndroidBrushFamilySerialization.decode(it, decodeCallback))
                .isEqualTo(original)
        }
        assertEquals(decodedTextureBitmapStore.size, 2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureBitmapStore[textureId1]
        assertTrue(actualBitmap1!!.sameAs(expectedBitmap1))

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureBitmapStore[textureId2]
        assertTrue(actualBitmap2!!.sameAs(expectedBitmap2))
    }

    @Test
    fun decode_notGzippedBytes_throws_staticApi() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue")
            ByteArrayInputStream(notGzippedBytes).use {
                AndroidBrushFamilySerialization.decode(it, { _, _ -> "id" })
            }
        }
    }

    @Test
    fun encode_decode_roundTrip() {
        val decodedTextureBitmapStore = mutableMapOf<String, Bitmap>()
        val decodeCallback = BrushFamilyDecodeCallback { id: String, bitmap: Bitmap? ->
            if (bitmap != null) {
                decodedTextureBitmapStore[id] = bitmap
            }
            id
        }
        val originalFamily = testFamily
        val encoded =
            ByteArrayOutputStream().use {
                originalFamily.encode(it, textureBitmapStore)
                it.toByteArray()
            }

        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamily.decode(it, decodeCallback)).isEqualTo(originalFamily)
        }

        assertEquals(decodedTextureBitmapStore.size, 2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureBitmapStore[textureId1]
        assertTrue(actualBitmap1!!.sameAs(expectedBitmap1))

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureBitmapStore[textureId2]
        assertTrue(actualBitmap2!!.sameAs(expectedBitmap2))
    }
}

/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.compose.state

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.creation.compose.capture.createTestCreationState
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.LimitsRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun bitmap(width: Int, height: Int) =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteBitmapFontTest {
    @get:Rule val limitsRule = LimitsRule()

    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState = createTestCreationState(profiles = PROFILE_ANDROIDX)

    val bitmapFont =
        RemoteBitmapFont(
            listOf(
                RemoteBitmapFont.Glyph(
                    "a",
                    RemoteImageBitmap(bitmap(10, 20).asImageBitmap()),
                    1,
                    2,
                    3,
                    4,
                    10,
                    20,
                ),
                RemoteBitmapFont.Glyph(
                    "b",
                    RemoteImageBitmap(bitmap(20, 20).asImageBitmap()),
                    10,
                    20,
                    30,
                    40,
                    20,
                    20,
                ),
                RemoteBitmapFont.Glyph(
                    "c",
                    RemoteImageBitmap(bitmap(30, 20).asImageBitmap()),
                    2,
                    4,
                    6,
                    8,
                    30,
                    20,
                ),
                RemoteBitmapFont.Glyph(" ", null, 20, 0, 0, 0, 0, 0),
            )
        )

    @Test
    fun measureWidth() {
        val result = bitmapFont.measureWidth(RemoteString("ab c"), RemoteFloat(0f))
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
    fun measureWidthPositiveGlyphSpacing() {
        val result = bitmapFont.measureWidth(RemoteString("abc"), RemoteFloat(5f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // Width is the sum of:
        // a: 1 + 10 + 3 = 14
        //    5
        // b: 10 + 20 + 30 = 60
        //    5
        // c: 2 + 30 + 6 = 38
        assertThat(context.getInteger(resultId)).isEqualTo(14 + 5 + 60 + 5 + 38)
    }

    @Test
    fun measureWidthNegativeGlyphSpacing() {
        val result = bitmapFont.measureWidth(RemoteString("abc"), RemoteFloat(-5f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // Width is the sum of:
        // a: 1 + 10 + 3 = 14
        //    -5
        // b: 10 + 20 + 30 = 60
        //    -5
        // c: 2 + 30 + 6 = 38
        assertThat(context.getInteger(resultId)).isEqualTo(14 - 5 + 60 - 5 + 38)
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

    @Test
    fun measureWithKerning() {
        val kerningFont =
            RemoteBitmapFont(
                listOf(
                    RemoteBitmapFont.Glyph(
                        "a",
                        RemoteImageBitmap(bitmap(10, 20).asImageBitmap()),
                        1,
                        0,
                        1,
                        0,
                        10,
                        20,
                    ),
                    RemoteBitmapFont.Glyph(
                        "b",
                        RemoteImageBitmap(bitmap(10, 20).asImageBitmap()),
                        1,
                        0,
                        1,
                        0,
                        10,
                        20,
                    ),
                ),
                mapOf("ab" to (-2).toShort()),
            )
        val result = kerningFont.measureWidth(RemoteString("ab"), RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // a: 1 + 10 + 1 = 12
        // kerning: -2
        // b: 1 + 10 + 1 = 12
        // Total: 12 - 2 + 12 = 22
        assertThat(context.getInteger(resultId)).isEqualTo(22)
    }

    @Test
    fun measureEmptyString() {
        val result = bitmapFont.measureWidth(RemoteString(""), RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(0)
    }

    @Test
    fun measureUnmatchedGlyphs() {
        val result = bitmapFont.measureWidth(RemoteString("z"), RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // "z" is not in the font, so it's ignored.
        assertThat(context.getInteger(resultId)).isEqualTo(0)
    }

    @Test
    fun measureNonConstantWidth() {
        // We use a RemoteString that doesn't have a constant value.
        val nonConstantString = createNamedRemoteString("testWidth", "abc")

        val result = bitmapFont.measureWidth(nonConstantString, RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // "abc" width: 14 + 60 + 38 = 112
        assertThat(context.getInteger(resultId)).isEqualTo(112)
    }

    @Test
    fun measureGreedyMatching() {
        val greedyFont =
            RemoteBitmapFont(
                listOf(
                    RemoteBitmapFont.Glyph(
                        "a",
                        RemoteImageBitmap(bitmap(10, 20).asImageBitmap()),
                        0,
                        0,
                        0,
                        0,
                        10,
                        20,
                    ),
                    RemoteBitmapFont.Glyph(
                        "ab",
                        RemoteImageBitmap(bitmap(20, 20).asImageBitmap()),
                        0,
                        0,
                        0,
                        0,
                        20,
                        20,
                    ),
                    RemoteBitmapFont.Glyph(
                        "abc",
                        RemoteImageBitmap(bitmap(30, 20).asImageBitmap()),
                        0,
                        0,
                        0,
                        0,
                        30,
                        20,
                    ),
                )
            )

        val result = greedyFont.measureWidth(RemoteString("abc"), RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        // If it's truly greedy, it should match "abc" (width 30)
        // even if "a" comes first in the list.
        assertThat(context.getInteger(resultId)).isEqualTo(30)
    }

    @Test
    fun glyphWithRemoteImageBitmap() {
        val preallocatedId = creationState.document.createBitmap(18, 26)
        val remoteFont =
            RemoteBitmapFont(
                listOf(
                    RemoteBitmapFont.Glyph(
                        chars = "a",
                        bitmap = RemoteImageBitmap.createOffscreenRemoteBitmap(12, 24),
                        marginLeft = 1,
                        marginTop = 2,
                        marginRight = 3,
                        marginBottom = 4,
                        width = 12,
                        height = 24,
                    ),
                    RemoteBitmapFont.Glyph(
                        chars = "b",
                        bitmap = RemoteImageBitmap.createForId(preallocatedId),
                        marginLeft = 2,
                        marginTop = 3,
                        marginRight = 4,
                        marginBottom = 5,
                        width = 18,
                        height = 26,
                    ),
                    RemoteBitmapFont.Glyph(" ", null, 15, 0, 0, 0, 0, 0),
                )
            )

        assertThat(remoteFont.measureWidth(RemoteString("a b"), RemoteFloat(0f)).constantValue)
            .isEqualTo(16f + 15f + 24f)
        assertThat(remoteFont.measureHeight(RemoteString("a b")).constantValue).isEqualTo(34f)

        val nonConstantText = createNamedRemoteString("remoteGlyphText", "a b")
        val widthId =
            remoteFont
                .measureWidth(nonConstantText, RemoteFloat(0f))
                .getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(widthId)).isEqualTo(16 + 15 + 24)

        limitsRule.setEnableImageUrls(true)
        val urlState = createTestCreationState(profiles = PROFILE_ANDROIDX)
        val urlFontId =
            RemoteBitmapFont(
                    listOf(
                        RemoteBitmapFont.Glyph(
                            chars = "u",
                            bitmap = RemoteImageBitmap("https://example.com/u.png"),
                            marginLeft = 0,
                            marginTop = 0,
                            marginRight = 0,
                            marginBottom = 0,
                            width = 18,
                            height = 26,
                        )
                    )
                )
                .getIdForCreationState(urlState)
        assertThat(urlFontId).isGreaterThan(0)
        CoreDocument().apply {
            val buffer = urlState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
        }
    }

    @Test
    fun remoteImageBitmapGlyphWireParity() {
        val glyphImage = bitmap(10, 20).asImageBitmap()
        val remoteImage = RemoteImageBitmap(glyphImage)
        val explicitGlyph = RemoteBitmapFont.Glyph("a", remoteImage, 1, 2, 3, 4, 16, 28)
        assertThat(explicitGlyph.width).isEqualTo(16.toShort())
        assertThat(explicitGlyph.height).isEqualTo(28.toShort())
        assertThat(explicitGlyph.bitmap).isSameInstanceAs(remoteImage)

        val expectedState = createTestCreationState(profiles = PROFILE_ANDROIDX)
        val actualState = createTestCreationState(profiles = PROFILE_ANDROIDX)

        val expectedFontId =
            expectedState.document.addBitmapFont(
                arrayOf(
                    BitmapFontData.Glyph(
                        "a",
                        expectedState.document.addBitmap(glyphImage.asAndroidBitmap()),
                        1,
                        2,
                        3,
                        4,
                        10,
                        20,
                    ),
                    BitmapFontData.Glyph(" ", -1, 20, 0, 0, 0, 0, 0),
                ),
                emptyMap(),
            )
        val actualFontId =
            RemoteBitmapFont(
                    listOf(
                        RemoteBitmapFont.Glyph("a", remoteImage, 1, 2, 3, 4, 10, 20),
                        RemoteBitmapFont.Glyph(" ", null, 20, 0, 0, 0, 0, 0),
                    )
                )
                .getIdForCreationState(actualState)

        assertThat(actualFontId).isEqualTo(expectedFontId)
        assertThat(actualState.document.buffer.buffer.cloneBytes())
            .isEqualTo(expectedState.document.buffer.buffer.cloneBytes())
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}

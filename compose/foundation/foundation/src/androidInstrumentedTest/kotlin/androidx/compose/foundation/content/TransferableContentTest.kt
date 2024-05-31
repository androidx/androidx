/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.content

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.toClipMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TransferableContentTest {

    @Test
    fun hasMediaType_genericNonText() {
        val transferableContent =
            TransferableContent(createClipData { addUri(mimeType = "image/png") })
        assertTrue(transferableContent.hasMediaType(MediaType.Image))
        assertTrue(transferableContent.hasMediaType(MediaType("image/png")))
        assertFalse(transferableContent.hasMediaType(MediaType("image/jpeg")))
    }

    @Test
    fun hasMediaType_genericText() {
        val transferableContent =
            TransferableContent(
                createClipData {
                    addText()
                    addUri(mimeType = "image/png")
                }
            )
        assertTrue(transferableContent.hasMediaType(MediaType.Text))
        assertTrue(transferableContent.hasMediaType(MediaType.PlainText))
        assertFalse(transferableContent.hasMediaType(MediaType.HtmlText))
        assertTrue(transferableContent.hasMediaType(MediaType.Image))
    }

    @Test
    fun readPlainText_concatsTextContent() {
        val transferableContent =
            TransferableContent(
                createClipData {
                    addText("a")
                    addText("b")
                    addText("c")
                }
            )

        assertThat(transferableContent.clipEntry.readPlainText()).isEqualTo("a\nb\nc")
    }

    @Test
    fun readPlainText_returnsNull_ifNoText() {
        val transferableContent =
            TransferableContent(
                createClipData {
                    addUri()
                    addUri()
                }
            )

        assertThat(transferableContent.clipEntry.readPlainText()).isNull()
    }

    @Test
    fun readPlainText_singleItem_doesNotHaveNewLine() {
        val transferableContent =
            TransferableContent(
                createClipData {
                    addText("abc")
                    addUri()
                    addIntent()
                }
            )

        assertThat(transferableContent.clipEntry.readPlainText()).isEqualTo("abc")
    }

    @Test
    fun consumeEach_returnsNull_ifEverythingIsConsumed() {
        val transferableContent = TransferableContent(createClipData())
        val remaining = transferableContent.consume { true }
        assertThat(remaining).isNull()
    }

    @Test
    fun consumeEach_returnsSameObject_ifNothingIsConsumed() {
        val transferableContent = TransferableContent(createClipData())
        val remaining = transferableContent.consume { false }
        assertThat(remaining).isSameInstanceAs(transferableContent)
    }

    @Test
    fun consumeEach_remainingMimeTypes_includeConsumedItems() {
        val transferableContent =
            TransferableContent(
                createClipData {
                    addText()
                    addUri(mimeType = "video/mp4")
                    addUri(mimeType = "image/gif")
                }
            )
        // only text would remain
        val remaining = transferableContent.consume { it.uri != null }
        assertThat(remaining?.clipEntry?.clipData?.itemCount).isEqualTo(1)
        assertThat(remaining?.clipEntry?.clipData?.getItemAt(0)?.uri).isNull()
        assertThat(remaining?.hasMediaType(MediaType("video/mp4"))).isTrue()
        assertThat(remaining?.hasMediaType(MediaType("image/gif"))).isTrue()
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun TransferableContent(clipData: ClipData): TransferableContent {
    return TransferableContent(
        clipEntry = clipData.toClipEntry(),
        clipMetadata = clipData.description.toClipMetadata(),
        source = TransferableContent.Source.Clipboard
    )
}

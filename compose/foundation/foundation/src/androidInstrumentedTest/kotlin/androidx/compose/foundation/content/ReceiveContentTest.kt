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
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.toClipMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class ReceiveContentTest {

    private val defaultLabel = "label"

    @Test
    fun consumeEach_separatesTransferableContent() {
        val clipData = createClipData()
        val transferableContent = TransferableContent(
            clipEntry = clipData.toClipEntry(),
            source = TransferableContent.Source.Keyboard,
            clipMetadata = clipData.description.toClipMetadata()
        )

        // only consume plain text
        val remaining = transferableContent.consumeEach {
            it.text != null && it.htmlText == null
        }

        assertThat(remaining).isNotNull()
        assertThat(remaining?.source).isEqualTo(TransferableContent.Source.Keyboard)
        assertClipDescription(remaining?.clipMetadata?.clipDescription)
            .isEqualToClipDescription(clipData.description)

        val remainingClipData = remaining?.clipEntry?.clipData
        assertThat(remainingClipData).isNotNull()
        assertThat(remainingClipData?.itemCount).isEqualTo(2)
        // the first item from original ClipData will be consumed
        assertThat(remainingClipData?.getItemAt(0)).isEqualTo(clipData.getItemAt(1))
        assertThat(remainingClipData?.getItemAt(1)).isEqualTo(clipData.getItemAt(2))
    }

    @Test
    fun consumingEverything_returnsNull() {
        val clipData = createClipData()
        val transferableContent = TransferableContent(
            clipEntry = clipData.toClipEntry(),
            source = TransferableContent.Source.Keyboard,
            clipMetadata = clipData.description.toClipMetadata()
        )

        // only consume plain text
        val remaining = transferableContent.consumeEach { true }

        assertThat(remaining).isNull()
    }

    @Test
    fun consumingOnlyItem_returnsNull() {
        val clipData = createClipData(itemCount = 1)
        val transferableContent = TransferableContent(
            clipEntry = clipData.toClipEntry(),
            source = TransferableContent.Source.Keyboard,
            clipMetadata = clipData.description.toClipMetadata()
        )

        // only consume plain text
        val remaining = transferableContent.consumeEach { true }

        assertThat(remaining).isNull()
    }

    @Test
    fun notConsumingAnything_returnsTheSameInstance() {
        val clipData = createClipData()
        val transferableContent = TransferableContent(
            clipEntry = clipData.toClipEntry(),
            source = TransferableContent.Source.Keyboard,
            clipMetadata = clipData.description.toClipMetadata()
        )

        // only consume plain text
        val remaining = transferableContent.consumeEach { false }

        assertThat(remaining).isSameInstanceAs(transferableContent)
    }

    @Test
    fun notConsumingOnlyItem_returnsTheSameInstance() {
        val clipData = createClipData(itemCount = 1)
        val transferableContent = TransferableContent(
            clipEntry = clipData.toClipEntry(),
            source = TransferableContent.Source.Keyboard,
            clipMetadata = clipData.description.toClipMetadata()
        )

        // only consume plain text
        val remaining = transferableContent.consumeEach { false }

        assertThat(remaining).isSameInstanceAs(transferableContent)
    }

    private fun createClipData(
        itemCount: Int = 3,
        label: String = defaultLabel
    ): ClipData {
        val chosenItems = sampleClipItems.take(itemCount)
        val mimeTypes = chosenItems.map { it.second }.toSet().toTypedArray()
        val clipDescription = ClipDescription(label, mimeTypes)
        val clipData = ClipData(clipDescription, chosenItems.first().first)
        for (i in 1 until itemCount) {
            clipData.addItem(chosenItems[i].first)
        }
        return clipData
    }

    companion object {

        private val sampleClipItems = listOf(
            ClipData.Item("plain text") to ClipDescription.MIMETYPE_TEXT_PLAIN,
            ClipData.Item(Uri.parse("content://com.example.app/image")) to "image/jpeg",
            ClipData.Item(
                "Html Content",
                "<p>Html Content</p>"
            ) to ClipDescription.MIMETYPE_TEXT_HTML,
            ClipData.Item(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://example.com")
                )
            ) to ClipDescription.MIMETYPE_TEXT_INTENT,
        )
    }
}

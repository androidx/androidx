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
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.toClipMetadata

/**
 * Android specific parts of [TransferableContent].
 *
 * @property linkUri Only supplied by InputConnection#commitContent.
 * @property extras Extras bundle that's passed by InputConnection#commitContent.
 */
@ExperimentalFoundationApi
actual class PlatformTransferableContent
internal constructor(val linkUri: Uri?, val extras: Bundle) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformTransferableContent) return false

        if (linkUri != other.linkUri) return false
        if (extras != other.extras) return false

        return true
    }

    override fun hashCode(): Int {
        var result = linkUri?.hashCode() ?: 0
        result = 31 * result + extras.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlatformTransferableContent(" + "linkUri=$linkUri, " + "extras=$extras)"
    }
}

/**
 * Helper function to consume parts of [TransferableContent] in Android by splitting it to
 * [ClipData.Item] parts. Use this function in [contentReceiver] modifier's `onReceive` callback to
 * easily separate remaining parts from incoming [TransferableContent].
 *
 * @sample androidx.compose.foundation.samples.ReceiveContentBasicSample
 * @param predicate Decides whether to consume or leave the given item out. Return true to indicate
 *   that this particular item was processed here, it shouldn't be passed further down the content
 *   receiver chain. Return false to keep it in the returned [TransferableContent].
 * @return Remaining parts of this [TransferableContent].
 */
@ExperimentalFoundationApi
fun TransferableContent.consume(predicate: (ClipData.Item) -> Boolean): TransferableContent? {
    val clipData = clipEntry.clipData
    return if (clipData.itemCount == 1) {
        // return this if the single item inside ClipData is not consumed, or null if it's consumed
        takeIf { !predicate(clipData.getItemAt(0)) }
    } else {
        // do not allocate a new list unnecessarily if every item ends up getting consumed.
        var remainingItems: MutableList<ClipData.Item>? = null

        for (i in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(i)
            if (!predicate(item)) {
                if (remainingItems == null) remainingItems = mutableListOf()
                remainingItems.add(item)
            }
        }
        // it cannot be non-null and empty at the same time but lets be safe
        if (remainingItems.isNullOrEmpty()) return null

        if (remainingItems.size == clipData.itemCount) return this

        val newClipDescription = ClipDescription(clipMetadata.clipDescription)
        // unfortunately ClipData ctor that takes multiple items is hidden from public API
        val newClipData = ClipData(newClipDescription, remainingItems.first())
        for (i in 1 until remainingItems.size) {
            newClipData.addItem(remainingItems[i])
        }
        return TransferableContent(
            newClipData.toClipEntry(),
            newClipDescription.toClipMetadata(),
            source,
            platformTransferableContent
        )
    }
}

@ExperimentalFoundationApi
actual fun TransferableContent.hasMediaType(mediaType: MediaType): Boolean {
    return clipMetadata.clipDescription.hasMimeType(mediaType.representation)
}

internal actual fun ClipEntry.readPlainText(): String? {
    var seenText = false
    for (i in 0 until clipData.itemCount) {
        seenText = seenText || (clipData.getItemAt(i).text != null)
    }
    return if (seenText) {
        // note: text may be null, ensure this is null-safe
        buildString {
            var seenFirstItem = false
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).text?.let { text ->
                    if (seenFirstItem) {
                        append("\n")
                    }
                    append(text)
                    seenFirstItem = true
                }
            }
        }
    } else {
        null
    }
}

/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.lang.IllegalStateException

internal actual class PlatformClipboardManager : ClipboardManager {
    internal val systemClipboard = try {
        Toolkit.getDefaultToolkit().getSystemClipboard()
    } catch (e: java.awt.HeadlessException) {
        null
    }

    actual override fun getText(): AnnotatedString? {
        return systemClipboard?.let {
            try {
                AnnotatedString(it.getData(DataFlavor.stringFlavor) as String)
            } catch (_: UnsupportedFlavorException) {
                null
            }
        }
    }

    actual override fun setText(annotatedString: AnnotatedString) {
        systemClipboard?.setContents(StringSelection(annotatedString.text), null)
    }

    override fun getClip(): ClipEntry? {
        return try {
            systemClipboard?.getContents(this)?.let(::ClipEntry)
        } catch (_: IllegalStateException) {
            null
        }
    }

    override fun getClipMetadata(): ClipMetadata? {
        return try {
            systemClipboard?.getContents(this)?.let(::ClipMetadata)
        } catch (_: IllegalStateException) {
            null
        }
    }

    override fun setClip(clipEntry: ClipEntry) {
        // Ignore clipDescription.
        systemClipboard?.setContents(clipEntry.transferable, null)
    }

    override fun hasClip(): Boolean {
        return try {
            systemClipboard?.availableDataFlavors?.isNotEmpty() ?: false
        } catch (_: IllegalStateException) {
            false
        }
    }

    /**
     * Returns the system clipboard.
     *
     * @throws java.awt.HeadlessException
     */
    override val nativeClipboard: NativeClipboard by lazy {
        // throw HeadlessException when native clipboard is accessed in a headless environment
        Toolkit.getDefaultToolkit().systemClipboard
    }
}

// Defining this class not as a typealias but a wrapper gives us flexibility in the future to
// add more functionality in it.
actual class ClipEntry(
    internal val transferable: Transferable
) {
    @Throws(UnsupportedFlavorException::class, IOException::class)
    fun getTransferData(flavor: DataFlavor): Any? {
        return transferable.getTransferData(flavor)
    }
}

// Defining this class not as a typealias but a wrapper gives us flexibility in the future to
// add more functionality in it.
actual class ClipMetadata(
    internal val transferable: Transferable
) {
    fun getTransferDataFlavors(): List<DataFlavor> {
        val dataFlavors = transferable.transferDataFlavors ?: return emptyList()
        return dataFlavors.filterNotNull()
    }

    fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return transferable.isDataFlavorSupported(flavor)
    }
}

actual typealias NativeClipboard = Clipboard

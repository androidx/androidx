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

package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

actual typealias NativeClipboard = Any

internal class AwtPlatformClipboard internal constructor() : Clipboard {

    private val systemClipboard by lazy {
        try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (_: HeadlessException) {
            null
        }
    }

    override suspend fun getClipEntry(): ClipEntry? {
        val transferable = systemClipboard?.getContents(null) ?: return null
        val flavors = transferable.transferDataFlavors
        if (flavors?.size == 0) return null
        return ClipEntry(transferable)
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val transferable = clipEntry?.asAwtTransferable
        systemClipboard?.setContents(
            /* contents = */ transferable ?: EmptyTransferable,
            /* owner = */ transferable as? ClipboardOwner,
        )
    }

    /**
     * Provides an instance of a platform clipboard.
     * The actual implementation may vary depending on the underlying GUI toolkit.
     * See [awtClipboard] to access [java.awt.datatransfer.Clipboard].
     */
    override val nativeClipboard: NativeClipboard
        get() = systemClipboard ?: error("systemClipboard is not available in headless mode")
}

/**
 * Returns [java.awt.datatransfer.Clipboard] instance if it's available, or null otherwise.
 * It might throw an exception when accessed in a headless mode.
 */
@ExperimentalComposeUiApi
val Clipboard.awtClipboard: java.awt.datatransfer.Clipboard?
    get() = nativeClipboard as? java.awt.datatransfer.Clipboard

/**
 * A wrapper for platform clip entry instance which can be used to access
 * or set the Clipboard content. The actual implementation may vary
 * depending on the underlying GUI toolkit and on the actual implementation
 * of Clipboard.nativeClipboard.
 *
 * See [asAwtTransferable] to access [Transferable].
 */
actual class ClipEntry
@ExperimentalComposeUiApi
constructor(
    @property:ExperimentalComposeUiApi
    val nativeClipEntry: Any
) {
    // TODO https://youtrack.jetbrains.com/issue/CMP-1260/ClipboardManager.-Implement-getClip-getClipMetadata-setClip
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")
}

/**
 * Returns a [Transferable] instance if the [ClipEntry.nativeClipEntry]
 * type is [Transferable]. Otherwise, it returns null.
 */
@ExperimentalComposeUiApi
val ClipEntry.asAwtTransferable: Transferable?
    get() = nativeClipEntry as? Transferable

internal actual fun createPlatformClipboard(): Clipboard {
    return AwtPlatformClipboard()
}

private object EmptyTransferable : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return emptyArray()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = false

    override fun getTransferData(flavor: DataFlavor?): Any {
        throw UnsupportedFlavorException(flavor)
    }
}

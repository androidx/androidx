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
import kotlin.js.Promise
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.files.Blob

actual typealias NativeClipboard = W3CTemporaryClipboard

class JsPlatformClipboard : Clipboard {

    private val browserClipboard by lazy {
        getW3CClipboard()
    }

    private val emptyClipboardItems = emptyArray<ClipboardItem>()

    override suspend fun getClipEntry(): ClipEntry? {
        val items = nativeClipboard.read().catch {
            // The most common reason is that the permission was denied
            println("Failed to read from Clipboard: $it")
            emptyClipboardItems
        }.await()
        return ClipEntry(items)
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) {
            nativeClipboard.write(emptyClipboardItems()).await<Any?>()
            return
        }
        nativeClipboard.write(clipEntry.clipboardItems).await<Any?>()
    }

    override val nativeClipboard: NativeClipboard
        get() = browserClipboard
}

private fun getW3CClipboard(): W3CTemporaryClipboard =
    js("window.navigator.clipboard")

internal actual fun createPlatformClipboard(): Clipboard {
    return if (isSecureContext()) {
        JsPlatformClipboard()
    } else {
        DumbInsecureContextClipboard()
    }
}

private fun isSecureContext(): Boolean =
    window.asDynamic().isSecureContext == true

actual class ClipEntry
@ExperimentalComposeUiApi
constructor(val clipboardItems: Array<ClipboardItem>) {

    // TODO https://youtrack.jetbrains.com/issue/CMP-1260/ClipboardManager.-Implement-getClip-getClipMetadata-setClip
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")

    companion object {
        fun withPlainText(text: String): ClipEntry {
            if (!isSecureContext()) {
                println("ClipboardItem is not available in insecure contexts.")
                return ClipEntry(emptyArray())
            }
            return ClipEntry(createClipboardItemWithPlainText(text))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun createClipboardItemWithPlainText(text: String): Array<ClipboardItem> =
    js("[new ClipboardItem({'text/plain': new Blob([text], { type: 'text/plain' })})]")

// Can't truly clear the clipboard, so setting the empty text
private fun emptyClipboardItems(): Array<ClipboardItem> =
    js("[new ClipboardItem({'text/plain': new Blob([''], { type: 'text/plain' })})]")

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Clipboard_API
 *
 * We declare this external interface temporary, because
 * the IDL in kotlinx-browser is incorrect:
 * https://github.com/Kotlin/kotlinx-browser/issues/14
 */
@ExperimentalComposeUiApi
@JsName("Clipboard")
external class W3CTemporaryClipboard {
    fun read(): Promise<Array<ClipboardItem>>
    fun write(data: Array<ClipboardItem>): Promise<Nothing>
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Clipboard_API
 *
 * We declare this external interface temporary, because
 * the IDL in kotlinx-browser is incorrect:
 * https://github.com/Kotlin/kotlinx-browser/issues/14
 */
@ExperimentalComposeUiApi
@JsName("ClipboardItem")
external interface ClipboardItem {
    val types: Array<String>
    fun getType(type: String): Promise<Blob>
}

/**
 * This Clipboard implementation is dumb.
 * It's created when window.isSecureContext != true.
 * See https://developer.mozilla.org/en-US/docs/Web/API/Clipboard
 */
private class DumbInsecureContextClipboard : Clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        println("Clipboard is not available in insecure contexts.")
        return null
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        println("Clipboard is not available in insecure contexts.")
    }

    private val browserClipboard by lazy {
        println("Clipboard is not available in insecure contexts.")
        getW3CClipboard()
    }

    override val nativeClipboard: NativeClipboard
        get() = browserClipboard
}
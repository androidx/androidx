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
import androidx.compose.ui.InternalComposeUiApi
import kotlin.getValue
import kotlin.js.Promise
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.files.Blob

actual typealias NativeClipboard = W3CTemporaryClipboard

private val isSecureContext: Boolean by lazy {
    isSecureContext()
}

// We don't expect the availability of browser APIs to change at runtime, so detect it and save
// It's necessary for https://youtrack.jetbrains.com/issue/CMP-8631
private val isFullClipboardApiSupported: Boolean by lazy {
    isSecureContext && isFullClipboardApiSupported()
}

private val isFallbackWriteTextApiAvailable: Boolean by lazy {
    isSecureContext && isFallbackWriteTextApiAvailable()
}

class WasmPlatformClipboard : Clipboard {

    private val browserClipboard by lazy {
        getW3CClipboard()
    }

    init {
        if (!isSecureContext) {
            warn("Clipboard API is not available in insecure contexts.")
        } else if (!isFallbackWriteTextApiAvailable) {
            warn("The browser doesn't support Clipboard.read(), Clipboard.write() and Clipboard.writeText()")
        } else if (!isFullClipboardApiSupported) {
            warn("The browser doesn't support Clipboard.read() and Clipboard.write()")
        }
    }

    private val emptyClipboardItems = emptyArray<ClipboardItem>().toJsArray()

    override suspend fun getClipEntry(): ClipEntry? {
        if (!isFullClipboardApiSupported) {
            warn("The browser doesn't support Clipboard.read()")
            return null
        }

        val items = nativeClipboard.read().catch {
            // The most common reason is that the permission was denied
            println("Failed to read from Clipboard: $it")
            emptyClipboardItems
        }.await<JsArray<ClipboardItem>>()
        return ClipEntry(items)
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        when {
            isFullClipboardApiSupported -> if (clipEntry == null) {
                // clear the clipboard
                nativeClipboard.write(emptyClipboardItems()).await<Any?>()
            } else {
                nativeClipboard.write(clipEntry.clipboardItems).await<Any?>()
            }
            isFallbackWriteTextApiAvailable -> {
                val text = clipEntry?.fallbackPlainText ?: ""
                nativeClipboard.writeText(text).await<Any?>()
            }
            else -> warn("The browser doesn't support Clipboard.write() and Clipboard.writeText()")
        }
    }

    override val nativeClipboard: NativeClipboard
        get() = browserClipboard
}

private val wasmPlatformClipboard: WasmPlatformClipboard by lazy { WasmPlatformClipboard() }

internal actual fun createPlatformClipboard(): Clipboard = wasmPlatformClipboard

actual class ClipEntry
@ExperimentalComposeUiApi
constructor(
    @property:ExperimentalComposeUiApi
    val clipboardItems: JsArray<ClipboardItem>
) {

    // TODO https://youtrack.jetbrains.com/issue/CMP-1260/ClipboardManager.-Implement-getClip-getClipMetadata-setClip
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")

    @InternalComposeUiApi
    var fallbackPlainText: String? = null

    companion object {
        fun withPlainText(text: String): ClipEntry {
            return when {
                isFullClipboardApiSupported -> ClipEntry(
                    if (isSecureContext) {
                        createClipboardItemWithPlainText(text)
                    } else {
                        emptyClipboardItems()
                    }
                )
                else -> ClipEntry(invalidClipboardItems())
                    .apply { fallbackPlainText = text }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun createClipboardItemWithPlainText(text: String): JsArray<ClipboardItem> =
    js("[new ClipboardItem({'text/plain': new Blob([text], { type: 'text/plain' })})]")

// Can't truly clear the clipboard, so setting the empty text
private fun emptyClipboardItems(): JsArray<ClipboardItem> =
    js("[new ClipboardItem({'text/plain': new Blob([''], { type: 'text/plain' })})]")

// We use it when we detect isSecureContext() != true,
// because we can't call ClipboardItem constructor - it's undefined.
private fun invalidClipboardItems(): JsArray<ClipboardItem> =
    js("[]")

private fun warn(text: String) {
    js("console.warn(text)")
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Clipboard_API
 *
 * We declare this external interface temporary because
 * the IDL in kotlinx-browser is incorrect:
 * https://github.com/Kotlin/kotlinx-browser/issues/14
 */
@ExperimentalComposeUiApi
@JsName("Clipboard")
external class W3CTemporaryClipboard {
    fun read(): Promise<JsArray<ClipboardItem>>
    fun write(data: JsArray<ClipboardItem>): Promise<Nothing>
    fun writeText(text: String): Promise<Nothing>
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Clipboard_API
 *
 * We declare this external interface temporary because
 * the IDL in kotlinx-browser is incorrect:
 * https://github.com/Kotlin/kotlinx-browser/issues/14
 */
@ExperimentalComposeUiApi
@JsName("ClipboardItem")
external interface ClipboardItem : JsAny {
    val types: JsArray<JsString>
    fun getType(type: JsString): Promise<Blob>
}
/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.mpp.demo.bugs.BugsScreen
import androidx.compose.mpp.demo.interops.HtmlInteropDemos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.rememberNavController
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlinx.browser.window
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asDeferred
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response

private const val notoColorEmoji = "./NotoColorEmoji.ttf"
private const val notoSansSC = "./NotoSansSC-Regular.ttf"

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalBrowserHistoryApi
fun main() {
    ComposeViewport(viewportContainerId = "composeApplication") {
        val navController = rememberNavController()
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val fontsLoaded = remember { mutableStateOf(false) }
        val app = remember { App(
            extraScreens = listOf(
                BugsScreen,
                Screen.Example("Web Clipboard API example") {
                    WebClipboardDemo()
                },
                HtmlInteropDemos
            )
        ) }

        if (fontsLoaded.value) {
            app.Content(navController)

            // TODO: possibly suboptimal workaround for https://youtrack.jetbrains.com/issue/CMP-7136/web-Its-non-trivial-to-bind-to-navigation-if-NavHost-is-called-asynchronously
            LaunchedEffect(Unit) {
                navController.bindToBrowserNavigation()
            }
        }

        LaunchedEffect(Unit) {
            val fontsDeferred = awaitAll(loadResAsync(notoColorEmoji), loadResAsync(notoSansSC)).zip(listOf(
                "NotoColorEmoji",
                "NotoSansSC"
            ))

            fontsDeferred.forEach { (font, name) ->
                val fontFamily = FontFamily(listOf(Font(name, font.toByteArray())))
                fontFamilyResolver.preload(fontFamily)
            }

            fontsLoaded.value = true
        }

    }
    setupBackingTextAreaDebugHints()
}

private suspend fun loadResAsync(url: String): Deferred<ArrayBuffer> {
    return window.fetch(url).await<Response>().arrayBuffer().asDeferred()
}

suspend fun loadRes(url: String): ArrayBuffer {
    return loadResAsync(url).await()
}

fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

private fun wasmExportsMemoryBuffer(): ArrayBuffer = js("wasmExports.memory.buffer")
private fun jsExportInt8ArrayToWasm(destination: ArrayBuffer, src: Int8Array, size: Int, dstAddr: Int) {
    val mem8 = Int8Array(destination, dstAddr, size)
    mem8.set(src)
}

internal fun jsInt8ArrayToKotlinByteArray(x: Int8Array): ByteArray {
    val size = x.length

    @OptIn(UnsafeWasmMemoryApi::class)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(wasmExportsMemoryBuffer(),  x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}
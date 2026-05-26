/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.UnresolvedSymbolsRegistry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.WebUnresolvedSymbolsRegistry
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@set:TestOnly
internal var defaultFallbackFontDownloader: FallbackFontDownloader = NotoFontDownloader()

internal interface FallbackFontDownloader {
    suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily>
}

internal class WebFallbackFontDownloader(
    private val downloader: FallbackFontDownloader,
    scope: CoroutineScope,
    private val onFontsLoaded: suspend (List<FontFamily>) -> Unit
) {
    private val channel = Channel<Set<Int>>(Channel.UNLIMITED)

    init {
        scope.launch {
            while (isActive) {
                val batch = awaitBatch()
                try {
                    val newFonts = downloader.downloadFallbackFont(batch)
                    drainChannel()
                    onFontsLoaded(newFonts)
                } catch (e: Exception) {
                    println("Failed to download fallback font: $e")
                }
            }
        }
    }

    fun submit(codepoints: Set<Int>) {
        channel.trySend(codepoints)
    }

    private suspend fun awaitBatch(): Set<Int> {
        val codepoints = mutableSetOf<Int>()
        codepoints += channel.receive()
        repeat(9) {
            codepoints += withTimeoutOrNull(60.milliseconds) {
                channel.receive()
            } ?: return codepoints
        }
        return codepoints
    }

    private fun drainChannel() {
        while (channel.tryReceive().isSuccess) { /* drain */ }
    }
}

@OptIn(InternalComposeApi::class)
@Composable
internal fun installFallbackFontDownloader() {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(fontFamilyResolver, coroutineScope) {
        val webDownloader = WebFallbackFontDownloader(
            downloader = defaultFallbackFontDownloader,
            scope = coroutineScope,
            onFontsLoaded = { fonts ->
                if (fonts.isNotEmpty()) {
                    for (font in fonts) {
                        fontFamilyResolver.preload(font)
                    }
                    WebUnresolvedSymbolsRegistry.onNewFontInstalled()
                }
            }
        )

        val listener = object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) {
                webDownloader.submit(codepoints)
            }
        }

        WebUnresolvedSymbolsRegistry.addListener(listener)
        onDispose {
            WebUnresolvedSymbolsRegistry.removeListener(listener)
        }
    }
}

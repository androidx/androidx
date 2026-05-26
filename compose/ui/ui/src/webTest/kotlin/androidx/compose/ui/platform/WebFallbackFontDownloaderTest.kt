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

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.text.font.FontFamily
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class, InternalComposeApi::class)
class WebFallbackFontDownloaderTest : OnCanvasTests {

    private class FakeDownloader : FallbackFontDownloader {
        val calls = mutableListOf<Set<Int>>()
        private var continuation: CancellableContinuation<Unit>? = null

        override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> {
            calls += codepoints.toSet()
            continuation?.resume(Unit)
            return emptyList()
        }

        suspend fun awaitNextDownloadCall(): Unit = suspendCancellableCoroutine {
            continuation = it
        }
    }

    // backgroundScope is cancelled automatically when the test ends without blocking test completion,
    // which is necessary because WebFallbackFontDownloader runs a while(isActive) loop.
    @Test
    fun singleSubmit_isForwardedToDownloader() = runTest {
        val fake = FakeDownloader()
        val downloader = WebFallbackFontDownloader(
            downloader = fake,
            scope = backgroundScope,
            onFontsLoaded = {}
        )
        downloader.submit(setOf(0x4E2D))
        advanceTimeBy(200)
        assertEquals(1, fake.calls.size)
        assertTrue(0x4E2D in fake.calls[0])
    }

    @Test
    fun multipleSubmitsAtOnce_areMergedIntoBatch() = runTest {
        val fake = FakeDownloader()
        val downloader = WebFallbackFontDownloader(
            downloader = fake,
            scope = backgroundScope,
            onFontsLoaded = {}
        )
        downloader.submit(setOf(1))
        downloader.submit(setOf(2))
        downloader.submit(setOf(3))
        advanceTimeBy(200)
        assertEquals(1, fake.calls.size)
        assertEquals(setOf(1, 2, 3), fake.calls[0])
    }

    @Test
    fun submitsAfterBatchTimeout_startNewBatch() = runTest {
        val fake = FakeDownloader()
        val downloader = WebFallbackFontDownloader(
            downloader = fake,
            scope = backgroundScope,
            onFontsLoaded = {}
        )
        downloader.submit(setOf(1))
        advanceTimeBy(200)
        assertEquals(1, fake.calls.size)

        downloader.submit(setOf(2))
        advanceTimeBy(200)
        assertEquals(2, fake.calls.size)
        assertEquals(setOf(2), fake.calls[1])
    }

    @Test
    fun moreThan10Submits_excessIsDroppedByDrain() = runTest {
        val fake = FakeDownloader()
        val downloader = WebFallbackFontDownloader(
            downloader = fake,
            scope = backgroundScope,
            onFontsLoaded = {}
        )
        // awaitBatch collects up to 10 sets, then drainChannel discards the rest
        for (i in 1..15) {
            downloader.submit(setOf(i))
        }
        advanceTimeBy(400)
        // Only one download call should happen; codepoints beyond the batch are drained
        assertEquals(1, fake.calls.size)
    }

    @Test
    fun exceptionInDownloader_doesNotCrashWorker() = runTest {
        var callCount = 0
        val throwingOnFirst = object : FallbackFontDownloader {
            override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> {
                callCount++
                if (callCount == 1) throw RuntimeException("download failed")
                return emptyList()
            }
        }
        val downloader = WebFallbackFontDownloader(
            downloader = throwingOnFirst,
            scope = backgroundScope,
            onFontsLoaded = {}
        )
        downloader.submit(setOf(1))
        advanceTimeBy(200)
        assertEquals(1, callCount)

        downloader.submit(setOf(2))
        advanceTimeBy(200)
        assertEquals(2, callCount, "Worker must survive exception and process next submit")
    }

    @Test
    fun onFontsLoaded_calledWithDownloadedFonts() = runTest {
        val fontFamily = FontFamily.Default
        val fakeWithResult = object : FallbackFontDownloader {
            override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> =
                listOf(fontFamily)
        }
        val loaded = mutableListOf<List<FontFamily>>()
        val downloader = WebFallbackFontDownloader(
            downloader = fakeWithResult,
            scope = backgroundScope,
            onFontsLoaded = { loaded += it }
        )
        downloader.submit(setOf(0x4E2D))
        advanceTimeBy(200)
        assertEquals(1, loaded.size)
        assertEquals(listOf(fontFamily), loaded[0])
    }

    @Test
    fun checkWebFontDownloaderIsConfiguredByDefault() = runTest {
        val fake = FakeDownloader()
        val unresolvedChar = '\uEE00'
        val codepoint = unresolvedChar.code

        val tmp = defaultFallbackFontDownloader
        defaultFallbackFontDownloader = fake

        createComposeWindow {
            BasicText(unresolvedChar.toString())
        }

        withContext(Dispatchers.Default) {
            withTimeout(200.milliseconds) {
                fake.awaitNextDownloadCall()
            }
        }

        defaultFallbackFontDownloader = tmp
        assertContentEquals(
            fake.calls.single(),
            listOf(codepoint),
            "FakeDownloader should have received codepoint 0x${codepoint.toString(16)}. Actual calls: ${fake.calls}"
        )
    }
}

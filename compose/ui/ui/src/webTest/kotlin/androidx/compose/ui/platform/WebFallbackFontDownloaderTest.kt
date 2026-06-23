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
    fun failedDownload_isRetriedWithSameCodepoints() = runTest {
        val font = FontFamily.Default
        val calls = mutableListOf<Set<Int>>()
        val flaky = object : FallbackFontDownloader {
            override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> {
                calls += codepoints.toSet()
                if (calls.size == 1) throw RuntimeException("transient network error")
                return listOf(font)
            }
        }
        val loaded = mutableListOf<List<FontFamily>>()
        val downloader = WebFallbackFontDownloader(
            downloader = flaky,
            scope = backgroundScope,
            onFontsLoaded = { loaded += it }
        )

        downloader.submit(setOf(0x4E2D, 0x6C34))
        advanceTimeBy(1000)

        assertEquals(2, calls.size, "Failed batch must be retried instead of being dropped")
        assertEquals(
            setOf(0x4E2D, 0x6C34),
            calls[1],
            "Retry must carry the same codepoints as the failed batch"
        )
        assertEquals(
            listOf(font),
            loaded.single(),
            "A successful retry must deliver the downloaded fonts"
        )
    }

    @Test
    fun consecutiveFailures_useGrowingBackoff() = runTest {
        var callCount = 0
        val alwaysFails = object : FallbackFontDownloader {
            override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> {
                callCount++
                throw RuntimeException("permanent failure")
            }
        }
        val downloader = WebFallbackFontDownloader(
            downloader = alwaysFails,
            scope = backgroundScope,
            onFontsLoaded = {}
        )

        downloader.submit(setOf(1))

        // First failure backs off by 0s, so the first retry happens (and fails) almost immediately.
        advanceTimeBy(500)
        assertEquals(2, callCount, "First failure must retry immediately (0s backoff)")

        // Second failure backs off by 5s — no further attempt before that elapses.
        advanceTimeBy(4000)
        assertEquals(2, callCount, "Third attempt must wait for the 5s backoff")

        // Cross the 5s boundary — the third attempt fires.
        advanceTimeBy(2000)
        assertEquals(3, callCount, "Third attempt must run once the 5s backoff elapsed")
    }

    @Test
    fun successResetsBackoff() = runTest {
        var callCount = 0
        // Fails on odd calls, succeeds on even ones, so every submit is "fail then retry-succeeds".
        val flaky = object : FallbackFontDownloader {
            override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> {
                callCount++
                if (callCount % 2 == 1) throw RuntimeException("transient")
                return emptyList()
            }
        }
        val downloader = WebFallbackFontDownloader(
            downloader = flaky,
            scope = backgroundScope,
            onFontsLoaded = {}
        )

        downloader.submit(setOf(1))
        advanceTimeBy(1000)
        assertEquals(2, callCount, "First batch fails then succeeds on the immediate retry")

        // The previous success must reset the backoff to 0, so this failure also retries immediately.
        // If the backoff were not reset, the retry would be delayed by 5s and callCount would stay 3.
        downloader.submit(setOf(2))
        advanceTimeBy(1000)
        assertEquals(4, callCount, "After a success, the next failure must retry immediately again")
    }

    @Test
    fun workerSurvivesFailures_andKeepsProcessingNewBatches() = runTest {
        val calls = mutableListOf<Set<Int>>()
        val downloader = WebFallbackFontDownloader(
            downloader = object : FallbackFontDownloader {
                override suspend fun downloadFallbackFont(codepoints: Set<Int>): List<FontFamily> {
                    calls += codepoints.toSet()
                    if (codepoints == setOf(1)) throw RuntimeException("always fails for 1")
                    return emptyList()
                }
            },
            scope = backgroundScope,
            onFontsLoaded = {}
        )

        downloader.submit(setOf(1))
        advanceTimeBy(300)

        downloader.submit(setOf(2))
        advanceTimeBy(300)

        assertTrue(
            calls.any { it == setOf(2) },
            "A continuously failing batch must not block the worker from processing new batches"
        )
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

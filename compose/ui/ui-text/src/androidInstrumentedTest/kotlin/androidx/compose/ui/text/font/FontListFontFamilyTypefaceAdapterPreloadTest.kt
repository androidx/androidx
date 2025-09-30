/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.graphics.Typeface
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FontListFontFamilyTypefaceAdapterPreloadTest {

    private lateinit var typefaceLoader: AsyncTestTypefaceLoader
    private lateinit var cache: AsyncTypefaceCache

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontLoader = AndroidFontLoader(context)

    @Before
    fun setup() {
        cache = AsyncTypefaceCache()
        typefaceLoader = AsyncTestTypefaceLoader()
    }

    @Test
    fun onPreload_onlyBlockingFonts_doesNotLoad() = runTest {
        val font = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = font.toFontFamily()
        subject().preload(fontFamily, fontLoader)
        // no styles matched, so no interactions
        assertThat(typefaceLoader.completedRequests()).isEmpty()
    }

    @Test
    fun onPreload_blockingAndAsyncFonts_matchesBlocking_doesLoad() = runTest {
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(blockingFont, asyncFont)
        subject().preload(fontFamily, fontLoader)
        // style matched, but blocking font is higher priority than async font
        assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont)
    }

    @Test
    fun onPreload_blockingAndAsyncFonts_matchesAsync_doesLoadForBoth() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(asyncFont, blockingFont)
        val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        testScheduler.runCurrent()
        assertThat(preloadJob.isActive).isFalse()
    }

    @Test
    fun onPreload_blockingAndAsyncFonts_matchesAsync_onlyLoadsFirstBlockingFallback() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val blockingFont2 = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(asyncFont, blockingFont, blockingFont2)
        val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        assertThat(typefaceLoader.completedRequests()).containsExactly(blockingFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        testScheduler.runCurrent()
        assertThat(preloadJob.isActive).isFalse()
    }

    @Test
    fun onPreload_blockingAndAsyncFonts_differentStyles_onlyLoadsAsync() = runTest {
        val blockingFont =
            BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE, weight = FontWeight.W100)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont, blockingFont)
        val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        testScheduler.runCurrent()
        assertThat(preloadJob.isActive).isFalse()
    }

    @Test
    fun onPreload_asyncFonts_differentStyles_loadsAll() = runTest {
        val asyncFont100 = AsyncFauxFont(typefaceLoader, weight = FontWeight.W100)
        val asyncFont400 = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(asyncFont400, asyncFont100)
        val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont400, asyncFont100)
        typefaceLoader.completeOne(asyncFont400, Typeface.SERIF)
        typefaceLoader.completeOne(asyncFont100, Typeface.SERIF)
        testScheduler.runCurrent()
        assertThat(preloadJob.isActive).isFalse()
    }

    @Test
    fun onPreload_fallbackAsyncFonts_sameStyle_loadsFirst() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val job = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        testScheduler.runCurrent()

        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont)
        assertThat(job.isActive).isFalse()
    }

    @Test(expected = IllegalStateException::class)
    fun onPreloadFail_timeout_throwsIllegalStateException() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val preloadJob = asyncAndRunCurrent { subject().preload(fontFamily, fontLoader) }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        testScheduler.apply {
            advanceTimeBy(Font.MaximumAsyncTimeoutMillis.milliseconds)
            runCurrent()
        }
        preloadJob.await()
    }

    @Test(expected = IllegalStateException::class)
    fun onPreload_whenFontLoadError_throwsIllegalStateException() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val deferred = asyncAndRunCurrent { subject().preload(fontFamily, fontLoader) }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.errorOne(asyncFont, RuntimeException("Failed to load"))
        deferred.await()
    }

    class MyFontException : RuntimeException()

    @Test(expected = IllegalStateException::class)
    fun onPreloadFails_exception_throwsIllegalStateException() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val asyncFontFallback = AsyncFauxFont(typefaceLoader, name = "AsyncFallbackFont")
        val fontFamily = FontFamily(asyncFont, asyncFontFallback)
        val deferred = asyncAndRunCurrent { subject().preload(fontFamily, fontLoader) }
        typefaceLoader.errorOne(asyncFont, MyFontException())
        deferred.await() // should throw
    }

    @Test
    fun onPreload_optionalAndAsyncFonts_matchesOptional_doesLoad() = runTest {
        val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val fontFamily = FontFamily(optionalFont, asyncFont)
        subject().preload(fontFamily, fontLoader)
        assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
    }

    @Test
    fun onPreload_optionalAndAsyncFonts_matchesAsync_doesLoadForBoth() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, null)
        val fontFamily = FontFamily(optionalFont, asyncFont)
        val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

        assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        testScheduler.runCurrent()
        assertThat(preloadJob.isActive).isFalse()
        assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont, optionalFont)
    }

    @Test
    fun onPreload_optionalAndAsyncAndBlockingFonts_matchesAsync_doesLoadForAll() = runTest {
        val asyncFont = AsyncFauxFont(typefaceLoader)
        val optionalFont = OptionalFauxFont(typefaceLoader, null)
        val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
        val fontFamily = FontFamily(optionalFont, asyncFont, blockingFont)
        val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }
        assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
        typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
        testScheduler.runCurrent()
        assertThat(preloadJob.isActive).isFalse()
        assertThat(typefaceLoader.completedRequests())
            .containsExactly(asyncFont, optionalFont, blockingFont)
    }

    @Test
    fun onPreload_optionalAndAsyncAndBlockingFonts_matchAsync_validOptional_doesNotLoadBlocking() =
        runTest {
            val asyncFont = AsyncFauxFont(typefaceLoader)
            val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.SANS_SERIF)
            val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
            // this is a weird order, but lets make sure it doesn't break :)
            val fontFamily = FontFamily(asyncFont, optionalFont, blockingFont)
            val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

            assertThat(typefaceLoader.pendingRequests()).containsExactly(asyncFont)
            typefaceLoader.completeOne(asyncFont, Typeface.SERIF)
            testScheduler.runCurrent()
            assertThat(preloadJob.isActive).isFalse()
            assertThat(typefaceLoader.completedRequests()).containsExactly(asyncFont, optionalFont)
        }

    @Test
    fun onPreload_optionalAndAsyncAndBlockingFonts_matchesOptional_doesNotLoadBlockingAsync() =
        runTest {
            val asyncFont = AsyncFauxFont(typefaceLoader)
            val optionalFont = OptionalFauxFont(typefaceLoader, Typeface.SANS_SERIF)
            val blockingFont = BlockingFauxFont(typefaceLoader, Typeface.MONOSPACE)
            // this is expected order
            val fontFamily = FontFamily(optionalFont, asyncFont, blockingFont)
            val preloadJob = launchAndRunCurrent { subject().preload(fontFamily, fontLoader) }

            assertThat(typefaceLoader.pendingRequests()).isEmpty()
            assertThat(preloadJob.isActive).isFalse()
            assertThat(typefaceLoader.completedRequests()).containsExactly(optionalFont)
        }

    private fun TestScope.subject() =
        FontListFontFamilyTypefaceAdapter(
            cache,
            backgroundScope.coroutineContext.minusKey(CoroutineExceptionHandler),
        )

    private fun TestScope.launchAndRunCurrent(block: suspend CoroutineScope.() -> Unit): Job =
        launch(block = block).also { testScheduler.runCurrent() }

    private fun <T> TestScope.asyncAndRunCurrent(
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> = async(block = block).also { testScheduler.runCurrent() }
}

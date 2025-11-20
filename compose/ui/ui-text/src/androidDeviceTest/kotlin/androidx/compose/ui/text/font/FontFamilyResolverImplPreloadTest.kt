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
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.font.testutils.AsyncFauxFont
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.testutils.OptionalFauxFont
import androidx.compose.ui.text.font.testutils.getImmutableResultFor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FontFamilyResolverImplPreloadTest {
    private lateinit var typefaceLoader: AsyncTestTypefaceLoader
    private lateinit var asyncTypefaceCache: AsyncTypefaceCache
    private lateinit var typefaceCache: TypefaceRequestCache
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val fontLoader = AndroidFontLoader(context)

    @Before
    fun setup() {
        asyncTypefaceCache = AsyncTypefaceCache()
        typefaceCache = TypefaceRequestCache()
        typefaceLoader = AsyncTestTypefaceLoader()
    }

    @Test
    fun preload_insertsTypefaceIntoCache() = runTest {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()
        subject().preload(fontFamily)
        assertThat(typefaceCache.size).isEqualTo(1)
        val cacheResult =
            typefaceCache.getImmutableResultFor(
                fontFamily,
                FontWeight.W100,
                fontLoader = fontLoader,
            )
        assertThat(cacheResult).isNotNull()
    }

    @Test
    fun preload_insertsTypefaceIntoCache_onlyForFontWeightAndStyle() = runTest {
        val fontFamily = FontTestData.FONT_100_REGULAR.toFontFamily()
        subject().preload(fontFamily)
        assertThat(typefaceCache.size).isEqualTo(1)
        val cacheResult =
            typefaceCache.getImmutableResultFor(
                fontFamily,
                FontWeight.W200,
                fontLoader = fontLoader,
            )
        assertThat(cacheResult).isNull()
    }

    @Test
    fun preload_insertsAllTypefaces_intoCache() = runTest {
        val fontFamily =
            FontFamily(
                FontTestData.FONT_100_REGULAR,
                FontTestData.FONT_200_REGULAR,
                FontTestData.FONT_300_REGULAR,
                FontTestData.FONT_400_REGULAR,
                FontTestData.FONT_500_REGULAR,
            )
        subject().preload(fontFamily)
        assertThat(typefaceCache.size).isEqualTo(5)
        for (weight in 100..500 step 100) {
            val cacheResult =
                typefaceCache.getImmutableResultFor(
                    fontFamily,
                    FontWeight(weight),
                    fontLoader = fontLoader,
                )
            assertThat(cacheResult).isNotNull()
        }
    }

    @Test
    fun preload_resolvesAsyncFonts() = runTest {
        val font = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)

        val fontFamily = font.toFontFamily()
        val preloadResult = asyncAndRunCurrent { subject().preload(fontFamily) }

        assertThat(typefaceLoader.pendingRequestsFor(font)).hasSize(1)
        // at this point, the request is out but font cache hasn't started
        assertThat(typefaceCache.size).isEqualTo(0)

        assertThat(preloadResult.isActive).isTrue()

        typefaceLoader.completeOne(font, Typeface.MONOSPACE)

        preloadResult.await()

        // at this point, result is back, and preload() has returned, so the main typeface
        // cache contains the result
        assertThat(typefaceCache.size).isEqualTo(1)

        val typefaceResult =
            typefaceCache.getImmutableResultFor(fontFamily, fontLoader = fontLoader)
        assertThat(typefaceResult).isNotNull()
    }

    @Test
    fun preload_onlyLoadsFirstAsyncFontInChain() = runTest {
        val font = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)

        val fontFamily = FontFamily(font, fallbackFont)
        val preloadResult = asyncAndRunCurrent { subject().preload(fontFamily) }

        typefaceLoader.completeOne(font, Typeface.MONOSPACE)

        preloadResult.await()

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
    }

    @Test(expected = IllegalStateException::class)
    fun preload_errorsOnTimeout() = runTest {
        val font = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)

        val fontFamily = FontFamily(font, fallbackFont)
        val deferred = asyncAndRunCurrent { subject().preload(fontFamily) }
        testScheduler.apply {
            advanceTimeBy(Font.MaximumAsyncTimeoutMillis.milliseconds)
            runCurrent()
        }
        assertThat(deferred.isCompleted).isTrue()
        deferred.await() // actually throw here
    }

    @Test
    fun whenOptionalFontFound_preload_doesNotResolveAsyncFont() = runTest {
        val optionalFont =
            OptionalFauxFont(typefaceLoader, Typeface.DEFAULT, FontWeight.Normal, FontStyle.Normal)
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)

        val fontFamily = FontFamily(optionalFont, fallbackFont)
        val preloadResult = asyncAndRunCurrent { subject().preload(fontFamily) }

        preloadResult.await()

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
        val typefaceResult =
            typefaceCache.getImmutableResultFor(fontFamily, fontLoader = fontLoader)
        assertThat(typefaceResult).isSameInstanceAs(Typeface.DEFAULT)
    }

    @Test
    fun whenOptionalFontNotFound_preload_doesResolveAsyncFont() = runTest {
        val optionalFont =
            OptionalFauxFont(typefaceLoader, null, FontWeight.Normal, FontStyle.Normal)
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Normal, FontStyle.Normal)
        val fontFamily = FontFamily(optionalFont, fallbackFont)
        val preloadResult = asyncAndRunCurrent { subject().preload(fontFamily) }
        testScheduler.runCurrent() // past yield on optionalFont
        typefaceLoader.completeOne(fallbackFont, Typeface.MONOSPACE)

        preloadResult.await()

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
        val typefaceResult =
            typefaceCache.getImmutableResultFor(fontFamily, fontLoader = fontLoader)
        assertThat(typefaceResult).isSameInstanceAs(Typeface.MONOSPACE)
    }

    @Test
    fun whenBlockingFont_neverResolvesAsync() = runTest {
        val blockingFont =
            BlockingFauxFont(
                typefaceLoader,
                Typeface.DEFAULT_BOLD,
                FontWeight.Bold,
                FontStyle.Normal,
            )
        val fallbackFont = AsyncFauxFont(typefaceLoader, FontWeight.Bold, FontStyle.Normal)

        val fontFamily = FontFamily(blockingFont, fallbackFont)
        val preloadResult = asyncAndRunCurrent { subject().preload(fontFamily) }

        preloadResult.await()

        assertThat(typefaceLoader.pendingRequestsFor(fallbackFont)).hasSize(0)
        val typefaceResult =
            typefaceCache.getImmutableResultFor(
                fontFamily,
                fontWeight = FontWeight.Bold,
                fontLoader = fontLoader,
            )
        assertThat(typefaceResult).isSameInstanceAs(Typeface.DEFAULT_BOLD)
    }

    // other font chain semantics are tested directly, preload is just checking that we don't
    // trigger async work when it's not necessary

    private fun TestScope.subject() =
        FontFamilyResolverImpl(
            fontLoader,
            typefaceRequestCache = typefaceCache,
            fontListFontFamilyTypefaceAdapter =
                FontListFontFamilyTypefaceAdapter(
                    asyncTypefaceCache,
                    backgroundScope.coroutineContext.minusKey(CoroutineExceptionHandler),
                ),
        )

    private fun <T> TestScope.asyncAndRunCurrent(
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> = async(block = block).also { testScheduler.runCurrent() }
}

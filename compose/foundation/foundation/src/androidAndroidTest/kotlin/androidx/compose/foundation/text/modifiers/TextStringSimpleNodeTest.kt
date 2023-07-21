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

package androidx.compose.foundation.text.modifiers

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextStringSimpleNodeTest {
    @get:Rule
    val rule = createComposeRule()
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    // TODO(b/279797016) re-enable this test, and add a path for AnnotatedString
    @Ignore("b/279797016 drawBehind is currently broken in tot")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun asyncTextResolution_causesRedraw() {
        val loadDeferred = CompletableDeferred<Unit>()
        val drawChannel = Channel<Unit>(capacity = Channel.UNLIMITED)
        val drawCount = AtomicInteger(0)
        val asyncFont = makeAsyncFont(loadDeferred)
        val subject = TextStringSimpleElement(
            "til",
            TextStyle.Default.copy(fontFamily = asyncFont.toFontFamily()),
            createFontFamilyResolver(context)
        )

        val modifier = Modifier.fillMaxSize() then subject then Modifier.drawBehind {
            drawRect(Color.Magenta, size = Size(100f, 100f))
            drawCount.incrementAndGet()
            drawChannel.trySend(Unit)
        }

        rule.setContent {
            Layout(modifier) { _, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {}
            }
        }
        rule.waitForIdle()
        runBlocking {
            // empty the draw channel here. sentContent already ensured that draw ran.
            // we just need this for sequencing AtomicInteger read/write/read later
            Truth.assertThat(drawChannel.isEmpty).isFalse()
            while (!drawChannel.isEmpty) {
                drawChannel.receive()
            }
        }
        val initialCount = drawCount.get()
        Truth.assertThat(initialCount).isGreaterThan(0)

        // this may take a while to make compose non-idle, so wait for drawChannel explicit sync
        loadDeferred.complete(Unit)
        runBlocking { withTimeout(1_000L) {
            drawChannel.receive()
        }
        }
        rule.waitForIdle()

        Truth.assertThat(drawCount.get()).isGreaterThan(initialCount)
    }

    private fun makeAsyncFont(loadDeferred: Deferred<Unit>): Font {

        val typefaceLoader = object : AndroidFont.TypefaceLoader {
            override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
                TODO("Not yet implemented")
            }

            override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
                loadDeferred.await()
                return Typeface.create("cursive", 0)
            }
        }
        return object : AndroidFont(
            FontLoadingStrategy.Async,
            typefaceLoader,
            FontVariation.Settings()
        ) {
            override val weight: FontWeight
                get() = FontWeight.Normal
            override val style: FontStyle
                get() = FontStyle.Normal
        }
    }
}
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail
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
    @get:Rule val rule = createComposeRule()
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun draw_whenNotAttached_doesNotCrash() {
        val subject =
            TextStringSimpleNode("text", TextStyle.Default, createFontFamilyResolver(context))
        rule.setContent {
            Canvas(Modifier.fillMaxSize()) {
                val contentDrawScope =
                    object : ContentDrawScope, DrawScope by this {
                        override fun drawContent() {
                            fail("Not used")
                        }
                    }
                        as ContentDrawScope
                with(subject) { contentDrawScope.draw() }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun exceedsMaxConstraintSize_doesNotCrash() {
        rule.setContent {
            val state = rememberScrollState()
            Column(Modifier.verticalScroll(state)) {
                BasicText(
                    text = "text\n".repeat(10_000),
                    style = TextStyle(fontSize = 50.sp),
                )
            }
        }
    }

    @Test
    fun movableContent_updateOnDetach_textIsUpdated() {
        var flag by mutableStateOf(false)

        rule.setContent {
            val content = remember {
                movableContentOf {
                    BoxWithConstraints {
                        BasicText(
                            text = if (!flag) "" else "LOADED",
                            modifier = Modifier.testTag("target")
                        )
                    }
                }
            }

            key(flag) { content() }
        }

        val textLayout1 = rule.onNodeWithTag("target").fetchTextLayoutResult()
        assertEquals(0, textLayout1.size.width)

        flag = true

        val textLayout2 = rule.onNodeWithTag("target").fetchTextLayoutResult()
        assertNotEquals(0, textLayout2.size.width)
    }

    @Ignore("b/346810857")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun asyncTextResolution_causesRedraw() {
        val loadDeferred = CompletableDeferred<Unit>()
        val drawChannel = Channel<Unit>(capacity = Channel.UNLIMITED)
        val drawCount = AtomicInteger(0)
        val asyncFont = makeAsyncFont(loadDeferred)
        val subject =
            TextStringSimpleElement(
                "til",
                TextStyle.Default.copy(fontFamily = asyncFont.toFontFamily()),
                createFontFamilyResolver(context)
            )

        val modifier =
            Modifier.fillMaxSize() then
                subject then
                Modifier.drawBehind {
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
        runBlocking { withTimeout(1_000L) { drawChannel.receive() } }
        rule.waitForIdle()

        Truth.assertThat(drawCount.get()).isGreaterThan(initialCount)
    }

    @Test
    fun setTextSubstitution_invalidatesDraw() {
        val drawCount = AtomicInteger(0)

        val subject =
            TextStringSimpleElement("til", TextStyle.Default, createFontFamilyResolver(context))

        val modifier =
            Modifier.fillMaxSize().drawBehind {
                drawRect(Color.Magenta, size = Size(100f, 100f))
                drawCount.incrementAndGet()
            } then subject

        rule.setContent { Box(modifier) }
        val initialCount = drawCount.get()
        rule.runOnIdle { Truth.assertThat(initialCount).isGreaterThan(0) }

        val node = rule.onNodeWithText("til").fetchSemanticsNode()

        rule.runOnIdle {
            node.config[SemanticsActions.SetTextSubstitution].action?.invoke(AnnotatedString("T"))
            node.config[SemanticsActions.ShowTextSubstitution].action?.invoke(true)
        }
        rule.waitForIdle()
        Truth.assertThat(drawCount.get()).isGreaterThan(initialCount)
    }

    @Test
    fun setTextSubstitution_setsSemantics() {
        val subject =
            TextStringSimpleElement("til", TextStyle.Default, createFontFamilyResolver(context))

        rule.setContent { Box(Modifier.fillMaxSize() then subject) }

        val node = rule.onNodeWithText("til").fetchSemanticsNode()

        rule.runOnIdle {
            node.config[SemanticsActions.SetTextSubstitution].action?.invoke(AnnotatedString("T"))
            node.config[SemanticsActions.ShowTextSubstitution].action?.invoke(true)
        }
        val replacedNode = rule.onNodeWithText("til").fetchSemanticsNode()
        rule.runOnIdle {
            Truth.assertThat(replacedNode.config[SemanticsProperties.TextSubstitution].text)
                .isEqualTo("T")
        }
    }

    private fun makeAsyncFont(loadDeferred: Deferred<Unit>): Font {

        val typefaceLoader =
            object : AndroidFont.TypefaceLoader {
                override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
                    TODO("Not yet implemented")
                }

                override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
                    loadDeferred.await()
                    return Typeface.create("cursive", 0)
                }
            }
        return object :
            AndroidFont(FontLoadingStrategy.Async, typefaceLoader, FontVariation.Settings()) {
            override val weight: FontWeight
                get() = FontWeight.Normal

            override val style: FontStyle
                get() = FontStyle.Normal
        }
    }
}

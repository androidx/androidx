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
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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
                BasicText(text = "text\n".repeat(10_000), style = TextStyle(fontSize = 50.sp))
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
                            modifier = Modifier.testTag("target"),
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
                createFontFamilyResolver(context),
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

    @Test
    fun measure_inLookaheadScope_avoidsTextReflowOnApproachFrames() {
        val subject =
            TextStringSimpleElement(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                TextStyle.Default.copy(fontSize = 20.sp),
                createFontFamilyResolver(context),
            )
        var approachWidth by mutableStateOf(100)

        rule.setContent {
            LookaheadScope {
                Box(
                    modifier =
                        subject.testTag("textTarget").layout { measurable, _ ->
                            val placeable =
                                if (isLookingAhead) {
                                    // Wide lookahead target where text fits on 1 line
                                    measurable.measure(
                                        Constraints(maxWidth = 1000, maxHeight = 1000)
                                    )
                                } else {
                                    // Narrow animated approach frame where text would wrap if
                                    // reflowed
                                    measurable.measure(Constraints.fixed(approachWidth, 100))
                                }
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                )
            }
        }
        rule.waitForIdle()

        // 1. Initial frame: text is laid out with lookahead target size
        val initialLayout = rule.onNodeWithTag("textTarget").fetchTextLayoutResult()
        Truth.assertThat(initialLayout.size.width).isGreaterThan(200)

        // 2. Animate approach width
        approachWidth = 150
        rule.waitForIdle()
        val secondLayout = rule.onNodeWithTag("textTarget").fetchTextLayoutResult()
        Truth.assertThat(secondLayout.size).isEqualTo(initialLayout.size)

        approachWidth = 200
        rule.waitForIdle()
        val thirdLayout = rule.onNodeWithTag("textTarget").fetchTextLayoutResult()
        Truth.assertThat(thirdLayout.size).isEqualTo(initialLayout.size)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextCenterOnApproachFrames() {
        runTestForAlignment(TextAlign.Center, LayoutDirection.Ltr, 80f, 120f)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextRightOnApproachFrames() {
        runTestForAlignment(TextAlign.Right, LayoutDirection.Ltr, 150f, 200f)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextLeftOnApproachFrames() {
        runTestForAlignment(TextAlign.Left, LayoutDirection.Ltr, 0f, 50f)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextStartRtlOnApproachFrames() {
        runTestForAlignment(TextAlign.Start, LayoutDirection.Rtl, 150f, 200f)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextEndRtlOnApproachFrames() {
        runTestForAlignment(TextAlign.End, LayoutDirection.Rtl, 0f, 50f)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextUnspecifiedRtlOnApproachFrames() {
        runTestForAlignment(null, LayoutDirection.Rtl, 150f, 200f)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun measure_inLookaheadScope_alignsTextUnspecifiedLtrOnApproachFrames() {
        runTestForAlignment(null, LayoutDirection.Ltr, 0f, 50f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runTestForAlignment(
        textAlign: TextAlign?,
        layoutDirection: LayoutDirection,
        expectedMin: Float,
        expectedMax: Float,
    ) {
        val fontSize = 20.sp
        val text = "I"
        val subject =
            TextStringSimpleElement(
                text,
                TextStyle.Default.copy(
                    fontSize = fontSize,
                    textAlign = textAlign ?: TextAlign.Unspecified,
                    color = Color.Black,
                ),
                createFontFamilyResolver(context),
            )

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    LookaheadScope {
                        Box(
                            modifier =
                                Modifier.layout { measurable, _ ->
                                        if (isLookingAhead) {
                                            // Wide lookahead target: 400px
                                            val placeable =
                                                measurable.measure(Constraints.fixed(400, 100))
                                            layout(400, 100) { placeable.place(0, 0) }
                                        } else {
                                            // Constrained approach: 200px
                                            val placeable =
                                                measurable.measure(Constraints.fixed(200, 100))
                                            layout(200, 100) { placeable.place(0, 0) }
                                        }
                                    }
                                    .then(subject)
                                    .testTag("textNode")
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        val bitmap = rule.onNodeWithTag("textNode").captureToImage().asAndroidBitmap()
        val averageX = calculateAverageXOfBlackPixels(bitmap)

        Truth.assertWithMessage(
                "Alignment $textAlign in $layoutDirection failed. Average X was $averageX, expected >= $expectedMin"
            )
            .that(averageX)
            .isAtLeast(expectedMin)
        Truth.assertWithMessage(
                "Alignment $textAlign in $layoutDirection failed. Average X was $averageX, expected <= $expectedMax"
            )
            .that(averageX)
            .isAtMost(expectedMax)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateAverageXOfBlackPixels(bitmap: Bitmap): Float {
        var sumX = 0f
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel != Color.White.toArgb()) {
                    sumX += x
                    count++
                }
            }
        }
        return if (count > 0) sumX / count else -1f
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

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

package androidx.glance.wear.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.Applier
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.Visibility
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.visibility
import androidx.glance.wear.tiles.curved.AnchorType
import androidx.glance.wear.tiles.curved.CurvedRow
import androidx.glance.wear.tiles.curved.CurvedTextStyle
import androidx.glance.wear.tiles.curved.RadialAlignment
import androidx.glance.wear.tiles.test.R
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.renderer.TileRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotTests {
    @get:Rule
    var screenshotRule = AndroidXScreenshotTestRule("glance/glance-wear-tiles")

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private lateinit var testBitmap: Bitmap

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
        testBitmap = getApplicationContext<Context>().getDrawable(R.drawable.oval)!!.toBitmap()
    }

    @Test
    fun basicBoxTest() = runSingleGoldenTest("basic-box") {
        Box(modifier = GlanceModifier.size(100.dp).background(Color.Green)) { }
    }

    @Test
    fun rowColumnGrid() = runSingleGoldenTest("row-column-grid") {
        Row {
            Column {
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Red)) {}
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Green)) {}
            }
            Column {
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Blue)) {}
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Cyan)) {}
            }
        }
    }

    @Test
    fun boxesWithBorder() = runSingleGoldenTest("boxes-with-border") {
        Row {
            Column {
                Box(modifier = GlanceModifier
                        .size(20.dp)
                        .background(Color.Red)
                        .border(width = 4.dp, color = ColorProvider(Color.Cyan))) {}
                Box(modifier = GlanceModifier
                        .size(20.dp)
                        .background(Color.Green)
                        .border(width = 4.dp, color = ColorProvider(Color.Blue))) {}
            }
            Column {
                Box(modifier = GlanceModifier
                        .size(20.dp)
                        .background(Color.Blue)
                        .border(
                            width = R.dimen.border_dimension,
                            color = ColorProvider(Color.Green)
                        )
                ) {}
                Box(modifier = GlanceModifier
                        .size(20.dp)
                        .background(Color.Cyan)
                        .border(
                            width = R.dimen.border_dimension,
                            color = ColorProvider(Color.Red)
                        )
                ) {}
            }
        }
    }

    @Test
    fun basicText() = runSingleGoldenTest("basic-text") {
        Column {
            Text(text = "Normal")
            Text(text = "Bold", style = TextStyle(fontWeight = FontWeight.Bold))
            Text(text = "Italic", style = TextStyle(fontStyle = FontStyle.Italic))
            Text(text = "Underline", style = TextStyle(textDecoration = TextDecoration.Underline))
            Text(
                text = "Everything",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
    }

    @Test
    fun textWithSizeAndAlignment() = runSingleGoldenTest("text-with-size-alignment") {
        Row {
            Column {
                Text(
                    text = "Hello World",
                    modifier = GlanceModifier.width(95.dp).height(60.dp).background(Color.Green)
                )
                Text(
                    text = "Hello World",
                    style = TextStyle(textAlign = TextAlign.Start),
                    modifier = GlanceModifier.width(95.dp).height(60.dp).background(Color.Blue)
                )

                Text(
                    text = "Hello World",
                    style = TextStyle(textAlign = TextAlign.End),
                    modifier = GlanceModifier.width(95.dp).height(60.dp).background(Color.Red)
                )
            }
            Column {
                Text(
                    text = "Hello World! This is a multiline test",
                    maxLines = 3,
                    modifier = GlanceModifier.width(100.dp).height(60.dp).background(Color.Red)
                )

                Text(
                    text = "Hello World! This is a multiline test",
                    maxLines = 3,
                    style = TextStyle(
                        textAlign = TextAlign.Start
                    ),
                    modifier = GlanceModifier.width(100.dp).height(60.dp).background(Color.Green)
                )

                Text(
                    text = "Hello World! This is a multiline test",
                    maxLines = 3,
                    style = TextStyle(
                        textAlign = TextAlign.End
                    ),
                    modifier = GlanceModifier.width(100.dp).height(60.dp).background(Color.Blue)
                )
            }
        }
    }

    @Test
    fun curvedText() = runSingleGoldenTest("curved-text") {
        CurvedRow(
            modifier = GlanceModifier.background(Color.Blue),
            radialAlignment = RadialAlignment.Center,
            anchorDegrees = -90f,
            anchorType = AnchorType.Center
        ) {
            CurvedText(text = "Hello World")
            CurvedText(text = "This is a test!", textStyle = CurvedTextStyle(fontSize = 24.sp))
        }
    }

    @Test
    fun curvedRowWithNormalElements() = runSingleGoldenTest("curved-row-with-normal-elements") {
        CurvedRow {
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Red)) {}
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Green)) {}
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Blue)) {}
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Cyan)) {}
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Magenta)) {}
        }
    }

    @Test
    fun spacersInGrid() = runSingleGoldenTest("spacers-in-grid") {
        Row {
            Column {
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Red)) {}
                Spacer(modifier = GlanceModifier.height(10.dp))
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Green)) {}
            }
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column {
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Blue)) {}
                Spacer(modifier = GlanceModifier.height(10.dp))
                Box(modifier = GlanceModifier.size(20.dp).background(Color.Cyan)) {}
            }
        }
    }

    @Test
    fun spacersInCurvedRow() = runSingleGoldenTest("spacers-in-curved-row") {
        CurvedRow {
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Red)) {}
            Spacer(modifier = GlanceModifier.width(10.dp))
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Green)) {}
            Spacer(modifier = GlanceModifier.width(10.dp))
            Box(modifier = GlanceModifier.size(30.dp).background(Color.Blue)) {}
        }
    }

    @Test
    fun imageScaleModes() = runSingleGoldenTest("image-scale-modes") {
        Column {
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "Oval-crop",
                modifier = GlanceModifier.size(50.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            Image(
                provider = ImageProvider(testBitmap),
                contentDescription = "Oval-fit",
                modifier = GlanceModifier.size(50.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            Image(
                provider = ImageProvider(R.drawable.oval),
                contentDescription = "Oval-fill-bounds",
                modifier = GlanceModifier.size(50.dp),
                contentScale = ContentScale.FillBounds
            )
        }
    }

    @Test
    fun visibility() = runSingleGoldenTest("visibility") {
        Column(modifier = GlanceModifier.fillMaxSize().background(Color.DarkGray)) {
            Text("First", style = TextStyle(color = ColorProvider(Color.Red)))
            Text("gone", modifier = GlanceModifier.visibility(Visibility.Gone))
            Row {
                Text("First",
                    modifier = GlanceModifier.visibility(Visibility.Invisible)
                        .background(ColorProvider(Color.Red))
                )
                Text("after")
            }
            Text("Third")
            Row(
                modifier = GlanceModifier.visibility(Visibility.Invisible).background(Color.Green)
            ) {
                Spacer(modifier = GlanceModifier.size(10.dp).background(Color.Red))
            }
            Text("Last")
        }
    }

    private suspend fun runComposition(content: @Composable () -> Unit) = coroutineScope {
        val root = EmittableBox()
        root.modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()
        root.contentAlignment = Alignment.Center

        val applier = Applier(root)
        val recomposer = Recomposer(currentCoroutineContext())
        val composition = Composition(applier, recomposer)
        val frameClock = BroadcastFrameClock()

        composition.setContent { content() }

        launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }

        recomposer.close()
        recomposer.join()

        root
    }

    private fun runSingleGoldenTest(
        expectedGolden: String,
        content: @Composable () -> Unit
    ) = fakeCoroutineScope.runBlockingTest {
        val context = getApplicationContext<Context>()
        val composition = runComposition(content)
        normalizeCompositionTree(context, composition)
        val translatedComposition = translateTopLevelComposition(context, composition)

        val renderer = TileRenderer(
            context,
            LayoutElementBuilders.Layout.Builder().setRoot(translatedComposition.layout).build(),
            translatedComposition.resources.build(),
            ContextCompat.getMainExecutor(getApplicationContext())
        ) {}

        val frame = FrameLayout(getApplicationContext())
        val firstChild = renderer.inflate(frame)

        requireNotNull(firstChild) {
            "Renderer did not inflate composition $composition"
        }

        val screenWidth =
            MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY)
        val screenHeight =
            MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, MeasureSpec.EXACTLY)

        frame.measure(screenWidth, screenHeight)
        frame.layout(
            0,
            0,
            SCREEN_WIDTH,
            SCREEN_HEIGHT
        )

        // Blit it to a bitmap for further testing.

        // Blit it to a bitmap for further testing.
        val bmp = Bitmap.createBitmap(
            SCREEN_WIDTH,
            SCREEN_HEIGHT,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        frame.draw(canvas)

        screenshotRule.assertBitmapAgainstGolden(bmp, expectedGolden, MSSIMMatcher())
    }

    companion object {
        private const val SCREEN_WIDTH = 390
        private const val SCREEN_HEIGHT = 390
    }
}
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

package androidx.glance.wear

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.core.content.ContextCompat
import androidx.glance.Applier
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle
import androidx.glance.layout.expandHeight
import androidx.glance.layout.expandWidth
import androidx.glance.layout.size
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import androidx.glance.wear.layout.AnchorType
import androidx.glance.wear.layout.CurvedRow
import androidx.glance.wear.layout.CurvedTextStyle
import androidx.glance.wear.layout.RadialAlignment
import androidx.glance.wear.layout.background
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
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

@OptIn(GlanceInternalApi::class, ExperimentalCoroutinesApi::class)
class ScreenshotTests {
    @get:Rule
    var screenshotRule = AndroidXScreenshotTestRule("glance/glance-wear")

    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun basicBoxTest() = runSingleGoldenTest("basic-box") {
        Box(modifier = Modifier.size(100.dp).background(Color.Green)) { }
    }

    @Test
    fun rowColumnGrid() = runSingleGoldenTest("row-column-grid") {
        Row {
            Column {
                Box(modifier = Modifier.size(20.dp).background(Color.Red)) {}
                Box(modifier = Modifier.size(20.dp).background(Color.Green)) {}
            }
            Column {
                Box(modifier = Modifier.size(20.dp).background(Color.Blue)) {}
                Box(modifier = Modifier.size(20.dp).background(Color.Cyan)) {}
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
    fun textWithSize() = runSingleGoldenTest("text-with-size") {
        Text(
            text = "Hello World! This is a test",
            modifier = Modifier.size(200.dp).background(Color.Red)
        )
    }

    @Test
    fun curvedText() = runSingleGoldenTest("curved-text") {
        CurvedRow(
            modifier = Modifier.background(Color.Blue),
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
            Box(modifier = Modifier.size(30.dp).background(Color.Red)) {}
            Box(modifier = Modifier.size(30.dp).background(Color.Green)) {}
            Box(modifier = Modifier.size(30.dp).background(Color.Blue)) {}
            Box(modifier = Modifier.size(30.dp).background(Color.Cyan)) {}
            Box(modifier = Modifier.size(30.dp).background(Color.Magenta)) {}
        }
    }

    private suspend fun runComposition(content: @Composable () -> Unit) = coroutineScope {
        val root = EmittableBox()
        root.modifier = Modifier.expandWidth().expandHeight()
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
        val translatedComposition = translateComposition(context, composition)

        val renderer = TileRenderer(
            context,
            LayoutElementBuilders.Layout.Builder().setRoot(translatedComposition).build(),
            ResourceBuilders.Resources.Builder().build(),
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
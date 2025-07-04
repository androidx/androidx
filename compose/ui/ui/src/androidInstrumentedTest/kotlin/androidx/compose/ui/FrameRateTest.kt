/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class FrameRateTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = rule.activity
        while (!activity.isDestroyed) {
            instrumentation.runOnMainSync {
                if (!activity.isDestroyed) {
                    activity.finish()
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun testSetFrameRateDefault() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            BasicUI(Float.NaN, Float.NaN)
        }
        rule.waitForIdle()
        rule.onNodeWithTag("frameRateTag").performClick()
        rule.waitForIdle()
        assertTrue(composeView.requestedFrameRate.isNaN())
        assertTrue(composeView.getChildAt(0).requestedFrameRate.isNaN())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun testSetFrameRate120() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            BasicUI(60f, 120f)
        }
        rule.waitForIdle()
        rule.onNodeWithTag("frameRateTag").performClick()
        rule.waitUntil(1000) { composeView.requestedFrameRate == 120f }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testFrameRateHigh() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            BasicUI(FrameRateCategory.High.value, FrameRateCategory.Normal.value)
        }
        rule.waitForIdle()
        rule.onNodeWithTag("frameRateTag").performClick()
        rule.waitForIdle()
        assertEquals(FrameRateCategory.High.value, composeView.getChildAt(0).requestedFrameRate)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testFrameRateCombined() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            BasicUI(FrameRateCategory.High.value, 80f)
        }
        rule.waitForIdle()

        rule.onNodeWithTag("frameRateTag").performClick()
        rule.waitUntil(1000) {
            FrameRateCategory.High.value == composeView.getChildAt(0).requestedFrameRate &&
                composeView.requestedFrameRate == 80f
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testFrameRateContentMoving() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            MovingContent()
        }
        rule.waitForIdle()
        rule.onNodeWithTag("frameRateTag").performClick()
        rule.waitUntil(1000) {
            FrameRateCategory.High.value == composeView.getChildAt(0).requestedFrameRate
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testFrameRateContentResizing() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        val frameRate = 30f
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            ResizingContent(frameRate)
        }
        rule.waitForIdle()
        rule.onNodeWithTag("ContentResizing").performClick()
        rule.waitUntil(1000) {
            FrameRateCategory.High.value == composeView.getChildAt(0).requestedFrameRate &&
                frameRate == composeView.requestedFrameRate
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testLazyColumnDemo() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        val frameRates = listOf(30f, 60f, 80f, 120f)
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            LazyColumnDemo()
        }
        rule.waitForIdle()
        // scroll to the 50th item
        rule.onNodeWithTag("scroll").performClick()
        for (frameRate in frameRates) {
            rule.onAllNodesWithText(frameRate.toString(), true).onFirst().performClick()
            rule.waitUntil(1000) { frameRate == composeView.requestedFrameRate }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testMovableContent() {
        assumeTrue(ComposeUiFlags.isAdaptiveRefreshRateEnabled)

        lateinit var composeView: AndroidComposeView
        val frameRates = listOf(30f, 60f)
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            MovableContent()
        }
        rule.waitForIdle()
        for (frameRate in frameRates) {
            rule.onAllNodesWithText(frameRate.toString(), true).onFirst().performClick()
            rule.waitUntil(1000) { frameRate == composeView.requestedFrameRate }
        }
        rule.onNodeWithText("toggle", true).performClick()
        rule.waitForIdle()
        for (frameRate in frameRates) {
            rule.onAllNodesWithText(frameRate.toString(), true).onFirst().performClick()
            rule.waitUntil(1000) { frameRate == composeView.requestedFrameRate }
        }
    }

    @Composable
    fun BasicUI(firstFrameRate: Float, secondFrameRate: Float) {
        var targetAlpha by remember { mutableFloatStateOf(1f) }
        val alpha by
            animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 100),
            )

        Button(
            onClick = { targetAlpha = if (targetAlpha == 1f) 0.2f else 1f },
            modifier =
                Modifier.testTag("frameRateTag")
                    .preferredFrameRate(secondFrameRate)
                    .background(LocalContentColor.current.copy(alpha = alpha)),
        ) {
            Text(
                text = "Click Me for alpha change $firstFrameRate",
                color = LocalContentColor.current.copy(alpha = alpha), // Adjust text alpha
                modifier = Modifier.preferredFrameRate(firstFrameRate),
            )
        }
    }

    @Composable
    fun LazyColumnDemo() {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var scrollToIndex by remember { mutableIntStateOf(0) }

        Button(
            modifier = Modifier.testTag("scroll").padding(20.dp),
            onClick = {
                scrollToIndex = 49
                coroutineScope.launch { listState.animateScrollToItem(scrollToIndex) }
            },
        ) {
            Text("Scroll to 50")
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(60.dp)) {
            // Add a single item
            items(100) { index ->
                when (index % 4) {
                    0 -> AlphaButton(30f)
                    1 -> AlphaButton(60f)
                    2 -> AlphaButton(80f)
                    3 -> AlphaButton(120f)
                    else -> {}
                }
            }
        }
    }

    @Composable
    private fun AlphaButton(frameRate: Float) {
        var targetAlpha by remember { mutableFloatStateOf(1f) }
        val alpha by
            animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 100),
            )

        Button(onClick = { targetAlpha = if (targetAlpha == 1f) 0.2f else 1f }) {
            Text(
                text = "Click for $frameRate fps",
                color = LocalContentColor.current.copy(alpha = alpha), // Adjust text alpha
                modifier = Modifier.preferredFrameRate(frameRate),
            )
        }
    }

    @Composable
    fun MovingContent() {
        val shortText = "Change position"
        var moved by remember { mutableStateOf(false) }
        // Animate Dp values (in this case, the offset)
        val offset by
            animateDpAsState(
                targetValue = if (moved) 100.dp else 0.dp,
                animationSpec = tween(durationMillis = 100),
                label = "offset",
            )

        Column(
            Modifier.height(250.dp)
                .padding(20.dp)
                .background(Color.Gray)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Button(
                onClick = { moved = !moved },
                modifier = Modifier.width(500.dp).testTag("frameRateTag"),
            ) {
                Text(shortText, modifier = Modifier.offset(x = offset))
            }
        }
    }

    @Composable
    fun ResizingContent(frameRate: Float) {
        Column(
            Modifier.height(250.dp)
                .padding(20.dp)
                .background(Color.Gray)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Text - text change animation", fontSize = 20.sp, color = Color.White)
            Spacer(Modifier.requiredHeight(20.dp))
            ResizingButtons(frameRate)
        }
    }

    @Composable
    fun ResizingButtons(frameRate: Float) {
        var expanded by remember { mutableStateOf(false) }
        val size by
            animateDpAsState(
                targetValue = if (expanded) 300.dp else 200.dp,
                animationSpec = tween(durationMillis = 100),
            )

        Button(
            onClick = { expanded = !expanded },
            modifier = Modifier.testTag("ContentResizing").preferredFrameRate(frameRate).width(size),
        ) {
            Text("Click Me for size change $frameRate")
        }
    }

    @Composable
    private fun MovableContent() {
        var isRow by remember { mutableStateOf(true) }

        val buttons = remember {
            movableContentOf {
                AlphaButton(30f)
                Spacer(Modifier.requiredSize(20.dp))
                AlphaButton(60f)
            }
        }

        Column(
            Modifier.height(300.dp)
                .padding(20.dp)
                .background(Color.Gray)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { isRow = !isRow }) { Text("toggle") }

            if (isRow) {
                Row(verticalAlignment = Alignment.CenterVertically) { buttons() }
            } else {
                Column(verticalArrangement = Arrangement.Center) { buttons() }
            }
        }
    }
}

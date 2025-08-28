/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.layout

import android.content.Context
import android.content.Intent
import android.graphics.Insets as FrameworkInsets
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets as AndroidWindowInsets
import android.view.WindowInsetsAnimation
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInsetsPaddingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var insetsView: InsetsView

    @Before
    fun setup() {
        WindowInsetsHolder.setUseTestInsets(true)
    }

    @After
    fun teardown() {
        WindowInsetsHolder.setUseTestInsets(false)
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

    @Test
    fun systemBarsPadding() {
        testInsetsPadding(WindowInsetsCompat.Type.systemBars(), Modifier.systemBarsPadding())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun displayCutoutPadding() {
        val coordinates = setInsetContent { Modifier.displayCutoutPadding() }

        val (width, height) = rule.runOnIdle { coordinates.boundsInRoot().bottomRight.round() }

        val insets = sendDisplayCutoutInsets(width, height)
        insets.assertIsConsumed(WindowInsetsCompat.Type.displayCutout())

        rule.runOnIdle {
            val expectedRect = Rect(10f, 11f, width - 12f, height - 13f)
            assertThat(coordinates.boundsInRoot()).isEqualTo(expectedRect)
        }
    }

    @Test
    fun recalculateWindowInsets() {
        var coordinates: LayoutCoordinates? = null

        var padding by mutableIntStateOf(10)

        setContent {
            val paddingDp = with(LocalDensity.current) { padding.toDp() }
            Box(Modifier.padding(paddingDp)) {
                Box(Modifier.fillMaxSize().recalculateWindowInsets().safeDrawingPadding()) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                }
            }
        }

        rule.waitUntil { coordinates != null }
        val coords = coordinates!!

        sendInsets(WindowInsetsCompat.Type.systemBars(), AndroidXInsets.of(11, 17, 23, 29))

        rule.waitUntil { // older devices animate the insets
            rule.runOnUiThread { coords.boundsInRoot().top > 16.99f }
        }

        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            val rootSize = coords.findRootCoordinates().size
            assertThat(bounds.left).isWithin(0.1f).of(11f)
            assertThat(bounds.top).isWithin(0.1f).of(17f)
            assertThat(bounds.right).isWithin(0.1f).of(rootSize.width - 23f)
            assertThat(bounds.bottom).isWithin(0.1f).of(rootSize.height - 29f)

            padding = 5
        }

        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            val rootSize = coords.findRootCoordinates().size
            assertThat(bounds.left).isWithin(0.1f).of(11f)
            assertThat(bounds.top).isWithin(0.1f).of(17f)
            assertThat(bounds.right).isWithin(0.1f).of(rootSize.width - 23f)
            assertThat(bounds.bottom).isWithin(0.1f).of(rootSize.height - 29f)

            padding = 20
        }

        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            val rootSize = coords.findRootCoordinates().size
            assertThat(bounds.left).isWithin(0.1f).of(20f)
            assertThat(bounds.top).isWithin(0.1f).of(20f)
            assertThat(bounds.right).isWithin(0.1f).of(rootSize.width - 23f)
            assertThat(bounds.bottom).isWithin(0.1f).of(rootSize.height - 29f)
        }
    }

    @Test
    fun recalculateWindowInsetsWithMovement() {
        var coordinates: LayoutCoordinates? = null

        var alignment by mutableStateOf(AbsoluteAlignment.TopLeft)
        setContent {
            Box(Modifier.background(Color.Blue)) {
                val sizeDp = with(LocalDensity.current) { 100.toDp() }
                Box(
                    Modifier.size(sizeDp)
                        .recalculateWindowInsets()
                        .safeDrawingPadding()
                        .align(alignment)
                        .background(Color.Yellow)
                        .onPlaced { coordinates = it }
                )
            }
        }

        rule.waitUntil { coordinates != null }
        val coords = coordinates!!

        sendInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(11, 17, 23, 29))
        rule.waitUntil { // older devices animate the insets
            rule.runOnUiThread { coords.boundsInRoot().top > 16.9f }
        }
        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            assertThat(bounds.left).isWithin(0.1f).of(11f)
            assertThat(bounds.top).isWithin(0.1f).of(17f)
            assertThat(bounds.right).isWithin(0.1f).of(100f)
            assertThat(bounds.bottom).isWithin(0.1f).of(100f)

            alignment = AbsoluteAlignment.BottomRight
        }

        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            val rootSize = coords.findRootCoordinates().size
            assertThat(bounds.left).isWithin(0.1f).of(rootSize.width - 100f)
            assertThat(bounds.top).isWithin(0.1f).of(rootSize.height - 100f)
            assertThat(bounds.right).isWithin(0.1f).of(rootSize.width - 23f)
            assertThat(bounds.bottom).isWithin(0.1f).of(rootSize.height - 29f)
        }
    }

    @Test
    fun recalculateWindowInsetsWithNestedMovement() {
        val coordinates = mutableStateOf<LayoutCoordinates?>(null)

        var alignment by mutableStateOf(AbsoluteAlignment.TopLeft)
        var size = 0f
        setContent {
            size = with(LocalDensity.current) { 100.dp.toPx() }
            Box(Modifier.background(Color.Blue)) {
                Box(Modifier.size(100.dp).align(alignment)) {
                    Box(Modifier.requiredSize(100.dp)) {
                        Box(
                            Modifier.size(100.dp)
                                .recalculateWindowInsets()
                                .safeDrawingPadding()
                                .background(Color.Yellow)
                                .onPlaced { coordinates.value = it }
                        )
                    }
                }
            }
        }

        rule.waitUntil { coordinates.value != null }
        val coords = coordinates.value!!

        sendInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(11, 17, 23, 29))
        rule.waitUntil { // older devices animate the insets
            rule.runOnUiThread { coords.boundsInRoot().top > 16.9f }
        }
        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            assertThat(bounds.left).isWithin(1f).of(11f)
            assertThat(bounds.top).isWithin(1f).of(17f)
            assertThat(bounds.right).isWithin(1f).of(size)
            assertThat(bounds.bottom).isWithin(1f).of(size)

            alignment = AbsoluteAlignment.BottomRight
        }

        rule.runOnIdle {
            val bounds = coords.boundsInRoot()
            val rootSize = coords.findRootCoordinates().size
            assertThat(bounds.left).isWithin(1f).of(rootSize.width - size)
            assertThat(bounds.top).isWithin(1f).of(rootSize.height - size)
            assertThat(bounds.right).isWithin(1f).of(rootSize.width - 23f)
            assertThat(bounds.bottom).isWithin(1f).of(rootSize.height - 29f)
        }
    }

    private fun sendDisplayCutoutInsets(width: Int, height: Int): WindowInsetsCompat {
        val centerWidth = width / 2
        val centerHeight = height / 2

        val left = AndroidRect(0, centerHeight, 10, centerHeight + 2)
        val top = AndroidRect(centerWidth, 0, centerWidth + 2, 11)
        val right = AndroidRect(width - 12, centerHeight, width, centerHeight + 2)
        val bottom = AndroidRect(centerWidth, height - 13, centerWidth + 2, height)
        val safeInsets = AndroidXInsets.of(10, 11, 12, 13)
        val windowInsets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 11, 0, 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), safeInsets)
                .setDisplayCutout(
                    DisplayCutoutCompat(
                        safeInsets,
                        left,
                        top,
                        right,
                        bottom,
                        AndroidXInsets.of(1, 2, 3, 4),
                    )
                )
                .build()
        return dispatchApplyWindowInsets(windowInsets)
    }

    @RequiresApi(33)
    private fun sendDisplayCutoutInsetsWithCutoutPath(width: Int, height: Int): WindowInsetsCompat {
        val centerWidth = width / 2
        val centerHeight = height / 2

        val left = AndroidRect(0, centerHeight, 10, centerHeight + 2)
        val top = AndroidRect(centerWidth, 0, centerWidth + 2, 11)
        val right = AndroidRect(width - 12, centerHeight, width, centerHeight + 2)
        val bottom = AndroidRect(centerWidth, height - 13, centerWidth + 2, height)
        val safeInsets = AndroidXInsets.of(10, 11, 12, 13)
        val windowInsets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 11, 0, 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), safeInsets)
                .setDisplayCutout(
                    DisplayCutoutCompat(
                        safeInsets,
                        left,
                        top,
                        right,
                        bottom,
                        AndroidXInsets.of(1, 2, 3, 4),
                        android.graphics.Path().apply {
                            addCircle(centerWidth + 1f, 5f, 1f, android.graphics.Path.Direction.CCW)
                        },
                    )
                )
                .build()
        return dispatchApplyWindowInsets(windowInsets)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun statusBarsPaddingApi21() {
        testInsetsPadding(WindowInsetsCompat.Type.statusBars(), Modifier.statusBarsPadding()) {
            width,
            height ->
            Rect(0f, 11f, width.toFloat(), height.toFloat())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun statusBarsPaddingApi30() {
        testInsetsPadding(WindowInsetsCompat.Type.statusBars(), Modifier.statusBarsPadding())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun captionBarPadding() {
        testInsetsPadding(WindowInsetsCompat.Type.captionBar(), Modifier.captionBarPadding())
    }

    @Test
    fun navigationBarsPaddingLeft() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding(),
            sentInsets = AndroidXInsets.of(10, 0, 0, 0),
        )
    }

    @Test
    fun navigationBarsPaddingRight() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding(),
            sentInsets = AndroidXInsets.of(0, 0, 12, 0),
        )
    }

    @Test
    fun navigationBarsPaddingBottom() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding(),
            sentInsets = AndroidXInsets.of(0, 0, 0, 13),
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun navigationBarsPaddingApi30() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding(),
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsPaddingIme() =
        testInsetsPadding(WindowInsetsCompat.Type.ime()) {
            Modifier.windowInsetsPadding(WindowInsets.ime)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsPaddingDisplayCutout() =
        testInsetsPadding(WindowInsetsCompat.Type.displayCutout()) {
            Modifier.windowInsetsPadding(WindowInsets.displayCutout)
        }

    @Test
    fun insetsPaddingStatusBarsTop() =
        testInsetsPadding(
            WindowInsetsCompat.Type.statusBars(),
            sentInsets = AndroidXInsets.of(0, 10, 0, 0),
            expected = { w, h -> Rect(0f, 10f, w.toFloat(), h.toFloat()) },
        ) {
            Modifier.windowInsetsPadding(WindowInsets.statusBars)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsPaddingStatusBarsApi30() =
        testInsetsPadding(WindowInsetsCompat.Type.statusBars()) {
            Modifier.windowInsetsPadding(WindowInsets.statusBars)
        }

    @Test
    fun insetsPaddingSystemBars() =
        testInsetsPadding(WindowInsetsCompat.Type.systemBars()) {
            Modifier.windowInsetsPadding(WindowInsets.systemBars)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun insetsPaddingTappableElement() =
        testInsetsPadding(WindowInsetsCompat.Type.tappableElement()) {
            Modifier.windowInsetsPadding(WindowInsets.tappableElement)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsPaddingCaptionBar() =
        testInsetsPadding(WindowInsetsCompat.Type.captionBar()) {
            Modifier.windowInsetsPadding(WindowInsets.captionBar)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun insetsPaddingMandatorySystemGestures() =
        testInsetsPadding(WindowInsetsCompat.Type.mandatorySystemGestures()) {
            Modifier.windowInsetsPadding(WindowInsets.mandatorySystemGestures)
        }

    @Test
    fun insetsPaddingNavigationBarsLeft() =
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            sentInsets = AndroidXInsets.of(10, 0, 0, 0),
            expected = { width, height -> Rect(10f, 0f, width.toFloat(), height.toFloat()) },
        ) {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        }

    @Test
    fun insetsPaddingNavigationBarsRight() =
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            sentInsets = AndroidXInsets.of(0, 0, 10, 0),
            expected = { width, height -> Rect(0f, 0f, width - 10f, height.toFloat()) },
        ) {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        }

    @Test
    fun insetsPaddingNavigationBarsBottom() =
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            sentInsets = AndroidXInsets.of(0, 0, 0, 10),
            expected = { width, height -> Rect(0f, 0f, width.toFloat(), height - 10f) },
        ) {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsPaddingNavigationBarsApi30() =
        testInsetsPadding(WindowInsetsCompat.Type.navigationBars()) {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsPaddingWaterfall() {
        val coordinates = setInsetContent { Modifier.windowInsetsPadding(WindowInsets.waterfall) }

        val (width, height) = rule.runOnIdle { coordinates.boundsInRoot().bottomRight.round() }

        val insets = sendDisplayCutoutInsets(width, height)
        insets.assertIsConsumed(WindowInsetsCompat.Type.displayCutout())

        rule.runOnIdle {
            val expectedRect = Rect(1f, 2f, width - 3f, height - 4f)
            assertThat(coordinates.boundsInRoot()).isEqualTo(expectedRect)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun cutoutPath() {
        var cutoutPath: Path? = null
        val coordinates = setInsetContent {
            cutoutPath = WindowInsets.cutoutPath
            Modifier.windowInsetsPadding(WindowInsets.displayCutout)
        }

        val (width, height) = rule.runOnIdle { coordinates.boundsInRoot().bottomRight.round() }

        val insets = sendDisplayCutoutInsetsWithCutoutPath(width, height)
        insets.assertIsConsumed(WindowInsetsCompat.Type.displayCutout())

        rule.runOnIdle {
            assertThat(
                    cutoutPath
                        ?.minus(Path().apply { addOval(Rect(Offset(width / 2 + 1f, 5f), 1f)) })
                        ?.isEmpty
                )
                .isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun insetsPaddingSystemGestures() =
        testInsetsPadding(WindowInsetsCompat.Type.systemGestures()) {
            Modifier.windowInsetsPadding(WindowInsets.systemGestures)
        }

    @Test
    fun mixedInsetsPadding() {
        val coordinates = setInsetContent {
            val windowInsets = WindowInsets
            val insets =
                windowInsets.navigationBars.union(windowInsets.statusBars).union(windowInsets.ime)
            Modifier.windowInsetsPadding(insets)
        }

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), AndroidXInsets.of(0, 0, 0, 15))
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 10, 0, 0))
                .setInsets(WindowInsetsCompat.Type.ime(), AndroidXInsets.of(0, 0, 0, 5))
                .build()

        dispatchApplyWindowInsets(insets)

        rule.waitUntil {
            val view = insetsView.findComposeView()
            val width = view.width
            val height = view.height
            coordinates.boundsInRoot() == Rect(0f, 10f, width.toFloat(), height - 15f)
        }
    }

    @Test
    fun consumedInsets() {
        lateinit var coordinates: LayoutCoordinates

        setContent {
            with(LocalDensity.current) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .padding(5.toDp(), 4.toDp(), 3.toDp(), 2.toDp())
                            .consumeWindowInsets(WindowInsets(5, 4, 3, 2))
                    ) {
                        Box(Modifier.fillMaxSize().systemBarsPadding()) {
                            Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                        }
                    }
                }
            }
        }

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), AndroidXInsets.of(10, 11, 12, 13))
                .build()

        dispatchApplyWindowInsets(insets)

        rule.waitUntil {
            val view = insetsView.findComposeView()
            val width = view.width
            val height = view.height
            coordinates.boundsInRoot() == Rect(10f, 11f, width - 12f, height - 13f)
        }
    }

    @Test
    fun consumedPadding() {
        lateinit var coordinates: LayoutCoordinates

        setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
            ) {
                Box(Modifier.statusBarsPadding()) {
                    Box(Modifier.systemBarsPadding()) {
                        Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                    }
                }
            }
        }

        // wait for layout
        rule.waitForIdle()

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 5, 0, 0))
                .setInsets(WindowInsetsCompat.Type.systemBars(), AndroidXInsets.of(10, 11, 12, 13))
                .build()

        dispatchApplyWindowInsets(insets)

        rule.waitUntil {
            val view = insetsView.findComposeView()
            val width = view.width
            val height = view.height
            coordinates.boundsInRoot() == Rect(10f, 11f, width - 12f, height - 13f)
        }
    }

    @Test
    fun withConsumedWindowInsets() {
        var top = 0
        var consumingModifier: Modifier by mutableStateOf(Modifier)
        setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
            ) {
                Box(consumingModifier) {
                    val density = LocalDensity.current
                    Box(
                        Modifier.fillMaxSize().onConsumedWindowInsetsChanged {
                            top = it.getTop(density)
                        }
                    )
                }
            }
        }

        // wait for layout
        rule.waitForIdle()

        assertThat(top).isEqualTo(0)

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 5, 0, 0))
                .build()

        dispatchApplyWindowInsets(insets)

        assertThat(top).isEqualTo(0)

        consumingModifier = Modifier.consumeWindowInsets(WindowInsets(0, 5, 0, 0))

        rule.waitForIdle()

        assertThat(top).isEqualTo(5)
    }

    private fun testInsetsPadding(
        type: Int,
        modifier: Modifier,
        sentInsets: AndroidXInsets = AndroidXInsets.of(10, 11, 12, 13),
        expected: (Int, Int) -> Rect = { width, height ->
            Rect(
                sentInsets.left.toFloat(),
                sentInsets.top.toFloat(),
                width - sentInsets.right.toFloat(),
                height - sentInsets.bottom.toFloat(),
            )
        },
    ) {
        testInsetsPadding(type, sentInsets, expected) { modifier }
    }

    private fun testInsetsPadding(
        type: Int,
        sentInsets: AndroidXInsets = AndroidXInsets.of(10, 11, 12, 13),
        expected: (Int, Int) -> Rect = { width, height ->
            Rect(10f, 11f, width - 12f, height - 13f)
        },
        modifier: @Composable () -> Modifier,
    ) {
        val coordinates = setInsetContent(modifier)

        val insets = sendInsets(type, sentInsets)
        insets.assertIsConsumed(type)

        rule.waitUntil {
            val view = insetsView.findComposeView()
            val width = view.width
            val height = view.height
            val expectedRect = expected(width, height)
            coordinates.boundsInRoot() == expectedRect
        }
    }

    // Removing the last Modifier handling insets should stop insets from being consumed
    @Test
    fun removeLastInsetsPadding() {
        var useStatusBarInsets by mutableStateOf(true)
        var useNavigationBarInsets by mutableStateOf(true)
        val coordinates = setInsetContent {
            (if (useStatusBarInsets) Modifier.statusBarsPadding() else Modifier).then(
                if (useNavigationBarInsets) Modifier.navigationBarsPadding() else Modifier
            )
        }

        rule.runOnIdle { useStatusBarInsets = false }

        sendInsets(WindowInsetsCompat.Type.systemBars())
            .assertIsConsumed(WindowInsetsCompat.Type.systemBars())

        rule.runOnIdle { useNavigationBarInsets = false }

        sendInsets(WindowInsetsCompat.Type.systemBars())
            .assertIsNotConsumed(WindowInsetsCompat.Type.systemBars())

        rule.runOnIdle {
            val view = insetsView.findComposeView()
            val width = view.width.toFloat()
            val height = view.height.toFloat()
            assertThat(coordinates.boundsInRoot()).isEqualTo(Rect(0f, 0f, width, height))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun animateImeInsets() {
        with(Api30Methods(rule)) {
            val coordinates = setInsetContent { Modifier.systemBarsPadding().imePadding() }

            sendInsets(WindowInsetsCompat.Type.systemBars())

            val view = insetsView.findComposeView()
            val animation =
                sendImeStart(
                    view,
                    AndroidXInsets.of(10, 11, 12, 13),
                    WindowInsetsCompat.Type.systemBars(),
                )

            val width = view.width
            val height = view.height

            animation.sendImeProgress(view, 0f)

            rule.runOnIdle {
                assertThat(coordinates.boundsInRoot())
                    .isEqualTo(Rect(10f, 11f, width - 12f, height - 13f))
            }

            animation.sendImeProgress(view, 0.75f)

            rule.runOnIdle {
                assertThat(coordinates.boundsInRoot())
                    .isEqualTo(Rect(10f, 11f, width - 12f, height - 15f))
            }

            animation.sendImeProgress(view, 1f)

            rule.runOnIdle {
                assertThat(coordinates.boundsInRoot())
                    .isEqualTo(Rect(10f, 11f, width - 12f, height - 20f))
            }

            animation.sendImeEnd(view)

            rule.runOnIdle {
                assertThat(coordinates.boundsInRoot())
                    .isEqualTo(Rect(10f, 11f, width - 12f, height - 20f))
            }
        }
    }

    @Test
    fun paddingValues() {
        lateinit var coordinates: LayoutCoordinates

        setContent {
            val padding = WindowInsets.systemBars.asPaddingValues()
            Box(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
            }
        }

        // wait for layout
        rule.waitForIdle()

        val insets = sendInsets(WindowInsetsCompat.Type.systemBars())
        insets.assertIsConsumed(WindowInsetsCompat.Type.systemBars())

        rule.waitUntil {
            val view = insetsView.findComposeView()
            val width = view.width
            val height = view.height
            val expectedRect = Rect(10f, 11f, width - 12f, height - 13f)
            coordinates.boundsInRoot() == expectedRect
        }
    }

    // Each level of the padding should consume some parts of the insets
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun consumeAtEachDepth() {
        lateinit var statusBar: LayoutCoordinates
        lateinit var navigationBar: LayoutCoordinates
        lateinit var ime: LayoutCoordinates
        setContent {
            Box(
                Modifier.fillMaxSize().statusBarsPadding().onGloballyPositioned { statusBar = it }
            ) {
                Box(Modifier.navigationBarsPadding().onGloballyPositioned { navigationBar = it }) {
                    Box(Modifier.imePadding().fillMaxSize().onGloballyPositioned { ime = it })
                }
            }
        }
        // wait for layout
        rule.waitForIdle()

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 10, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), AndroidXInsets.of(0, 0, 0, 11))
                .setInsets(WindowInsetsCompat.Type.ime(), AndroidXInsets.of(0, 10, 0, 20))
                .build()

        dispatchApplyWindowInsets(insets)

        rule.runOnIdle {
            val height = insetsView.findComposeView().height
            assertThat(statusBar.size.height).isEqualTo(height - 10)
            assertThat(navigationBar.size.height).isEqualTo(height - 21)
            assertThat(ime.size.height).isEqualTo(height - 30)
        }
    }

    // The consumedPaddingInsets() should remove the insets values so that they aren't consumed
    // further down the hierarchy.
    @Test
    fun consumedInsetsPadding() {
        lateinit var outer: LayoutCoordinates
        lateinit var middle: LayoutCoordinates
        lateinit var inner: LayoutCoordinates
        setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.fillMaxSize()
                        .consumeWindowInsets(PaddingValues(top = 1.toDp()))
                        .windowInsetsPadding(WindowInsets(top = 10))
                        .onGloballyPositioned { outer = it }
                ) {
                    Box(
                        Modifier.consumeWindowInsets(PaddingValues(top = 1.toDp()))
                            .windowInsetsPadding(WindowInsets(top = 20))
                            .onGloballyPositioned { middle = it }
                    ) {
                        Box(
                            Modifier.consumeWindowInsets(PaddingValues(top = 1.toDp()))
                                .windowInsetsPadding(WindowInsets(top = 30))
                                .fillMaxSize()
                                .onGloballyPositioned { inner = it }
                        )
                    }
                }
            }
        }
        // wait for layout
        rule.waitForIdle()

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 35, 0, 0))
                .build()

        dispatchApplyWindowInsets(insets)

        rule.runOnIdle {
            val height = insetsView.findComposeView().height
            assertThat(outer.size.height).isEqualTo(height - 9)
            assertThat(middle.size.height).isEqualTo(height - 18)
            assertThat(inner.size.height).isEqualTo(height - 27)
        }
    }

    // The consumedInsets() should remove only values that haven't been consumed.
    @Test
    fun consumedInsetsLimitedConsumption() {
        lateinit var outer: LayoutCoordinates
        lateinit var middle: LayoutCoordinates
        lateinit var inner: LayoutCoordinates
        setContent {
            Box(
                Modifier.fillMaxSize()
                    .consumeWindowInsets(WindowInsets(top = 1))
                    .windowInsetsPadding(WindowInsets(top = 10))
                    .onGloballyPositioned { outer = it }
            ) {
                Box(
                    Modifier.consumeWindowInsets(WindowInsets(top = 10))
                        .windowInsetsPadding(WindowInsets(top = 20))
                        .onGloballyPositioned { middle = it }
                ) {
                    Box(
                        Modifier.consumeWindowInsets(WindowInsets(top = 20))
                            .windowInsetsPadding(WindowInsets(top = 30))
                            .fillMaxSize()
                            .onGloballyPositioned { inner = it }
                    )
                }
            }
        }
        // wait for layout
        rule.waitForIdle()

        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 35, 0, 0))
                .build()

        dispatchApplyWindowInsets(insets)

        rule.runOnIdle {
            val height = insetsView.findComposeView().height
            assertThat(outer.size.height).isEqualTo(height - 9)
            assertThat(middle.size.height).isEqualTo(height - 19)
            assertThat(inner.size.height).isEqualTo(height - 29)
        }
    }

    // When the insets change, the layout should be redrawn.
    @Test
    fun newInsetsCausesLayout() {
        lateinit var coordinates: LayoutCoordinates
        var useMiddleInsets by mutableStateOf(true)

        setContent {
            Box(Modifier.fillMaxSize()) {
                val modifier =
                    if (useMiddleInsets) {
                        Modifier.consumeWindowInsets(WindowInsets(top = 1))
                    } else {
                        Modifier.consumeWindowInsets(WindowInsets(top = 2))
                    }
                with(LocalDensity.current) {
                    Box(modifier.size(50.toDp())) {
                        Box(
                            Modifier.windowInsetsPadding(WindowInsets(top = 10))
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates = it }
                        )
                    }
                }
            }
        }

        // wait for layout
        rule.waitForIdle()

        sendInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 20, 0, 0))

        rule.runOnIdle {
            assertThat(coordinates.size.height).isEqualTo(41)
            useMiddleInsets = false
        }

        rule.runOnIdle { assertThat(coordinates.size.height).isEqualTo(42) }
    }

    @Test
    fun disableConsuming() {
        setContent {
            AndroidView(
                factory = { context ->
                    ComposeView(context).also {
                        it.consumeWindowInsets = false
                        it.setContent { Box(Modifier.fillMaxSize().statusBarsPadding()) }
                    }
                }
            )
        }

        // wait for layout
        rule.waitForIdle()

        val remaining =
            sendInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 20, 0, 0))
        assertThat(remaining.getInsets(WindowInsetsCompat.Type.statusBars()).top).isEqualTo(20)
    }

    /** If we don't have setDecorFitsSystemWindows(false), there shouldn't be any insets */
    @Test
    fun noInsets() {
        var leftInset = -1
        var topInset = -1
        var rightInset = -1
        var bottomInset = -1

        setContent {
            val insets = WindowInsets.safeContent
            leftInset = insets.getLeft(LocalDensity.current, LocalLayoutDirection.current)
            topInset = insets.getTop(LocalDensity.current)
            rightInset = insets.getRight(LocalDensity.current, LocalLayoutDirection.current)
            bottomInset = insets.getBottom(LocalDensity.current)
        }

        val insets = WindowInsetsCompat.Builder().build()
        dispatchApplyWindowInsets(insets)
        rule.waitForIdle()
        assertThat(leftInset).isEqualTo(0)
        assertThat(topInset).isEqualTo(0)
        assertThat(rightInset).isEqualTo(0)
        assertThat(bottomInset).isEqualTo(0)
    }

    @Test
    fun reuseModifier() {
        var consumed1 = WindowInsets(0, 0, 0, 0)
        var consumed2 = WindowInsets(0, 0, 0, 0)
        setContent {
            with(LocalDensity.current) {
                val modifier = Modifier.consumeWindowInsets(PaddingValues(10.toDp()))
                Box(modifier.fillMaxSize().onConsumedWindowInsetsChanged { consumed1 = it }) {
                    Box(modifier.fillMaxSize().onConsumedWindowInsetsChanged { consumed2 = it })
                }
            }
        }

        rule.waitForIdle()
        sendInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 30, 0, 0))
        rule.runOnIdle {
            assertThat(consumed1.getTop(rule.density)).isEqualTo(10)
            assertThat(consumed2.getTop(rule.density)).isEqualTo(20)
        }
    }

    @Test
    fun removedPaddingModifierUpdatesChildren() {
        var useModifier by mutableStateOf(true)
        val bottomActivity = WindowInsetsActivity.topActivity
        rule.runOnUiThread {
            val intent = Intent(rule.activity, WindowInsetsNoActionBarActivity::class.java)
            rule.activity.startActivity(intent)
        }
        rule.waitUntil { WindowInsetsActivity.topActivity != bottomActivity }

        val activity = WindowInsetsActivity.topActivity!!
        lateinit var outsideCoordinates: LayoutCoordinates
        lateinit var coordinates: LayoutCoordinates
        lateinit var insideCoordinates: LayoutCoordinates
        rule.runOnUiThread {
            activity.enableEdgeToEdge()
            activity.setContent {
                val modifier =
                    if (useModifier) Modifier.statusBarsPadding() else Modifier.fillMaxSize()
                Box(
                    modifier
                        .onPlaced { outsideCoordinates = it }
                        .fillMaxSize()
                        .systemBarsPadding()
                        .onPlaced { coordinates = it }
                ) {
                    Box(Modifier.fillMaxSize().onPlaced { insideCoordinates = it })
                }
            }
        }
        rule.runOnUiThread {
            val window = activity.window
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.statusBars())
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.navigationBars())
        }
        // There could be an animation, so wait for the insets to appear
        rule.waitUntil {
            rule.runOnIdle {
                val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
                insets?.isVisible(WindowInsetsCompat.Type.statusBars()) == true &&
                    insets.isVisible(WindowInsetsCompat.Type.navigationBars())
            }
        }
        rule.waitUntil { coordinates.size != outsideCoordinates.size }
        rule.runOnIdle {
            assertThat(insideCoordinates.size).isEqualTo(coordinates.size)
            assertThat(outsideCoordinates.size).isNotEqualTo(coordinates.size)
        }
        val origOutsideSize = outsideCoordinates.size
        val origSize = coordinates.size

        // Make sure that when a modifier is removed, the child consumed values are updated.
        useModifier = false
        rule.waitUntil { outsideCoordinates.size != origOutsideSize }
        rule.runOnIdle {
            assertThat(coordinates.size).isEqualTo(origSize)
            assertThat(coordinates.positionInRoot().y).isGreaterThan(0)
            assertThat(outsideCoordinates.size.height).isGreaterThan(origOutsideSize.height)
            assertThat(outsideCoordinates.positionInRoot().y).isEqualTo(0)
        }
        rule.runOnUiThread { activity.finish() }
        // No crashing when the activity is destroyed. For example, traverseDescendants() on
        // a detached modifier doesn't cause a crash.
        rule.waitUntil { WindowInsetsActivity.topActivity == null }
    }

    private fun sendInsets(
        type: Int,
        sentInsets: AndroidXInsets = AndroidXInsets.of(10, 11, 12, 13),
    ): WindowInsetsCompat {
        val insets = WindowInsetsCompat.Builder().setInsets(type, sentInsets).build()
        return dispatchApplyWindowInsets(insets)
    }

    private fun dispatchApplyWindowInsets(insets: WindowInsetsCompat): WindowInsetsCompat {
        return rule.runOnIdle {
            val windowInsets = insets.toWindowInsets()!!
            val view = insetsView
            insetsView.myInsets = windowInsets
            val returnedInsets = view.findComposeView().dispatchApplyWindowInsets(windowInsets)
            WindowInsetsCompat.toWindowInsetsCompat(returnedInsets, view)
        }
    }

    private fun setInsetContent(insetsModifier: @Composable () -> Modifier): LayoutCoordinates {
        lateinit var coordinates: LayoutCoordinates

        setContent {
            (LocalView.current.parent as ComposeView).consumeWindowInsets = true
            Box(Modifier.fillMaxSize().background(Color.Blue).then(insetsModifier())) {
                Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
            }
        }

        // wait for layout
        rule.waitForIdle()
        return coordinates
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            AndroidView(
                factory = { context ->
                    val view = InsetsView(context)
                    insetsView = view
                    val composeView = ComposeView(rule.activity)
                    composeView.consumeWindowInsets = true
                    view.addView(
                        composeView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    composeView.setContent(content)
                    view
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun WindowInsetsCompat.assertIsConsumed(type: Int) {
        val insets = getInsets(type)
        assertThat(insets).isEqualTo(AndroidXInsets.of(0, 0, 0, 0))
    }

    private fun WindowInsetsCompat.assertIsNotConsumed(type: Int) {
        val insets = getInsets(type)
        assertThat(insets).isNotEqualTo(AndroidXInsets.of(0, 0, 0, 0))
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private class Api30Methods(
    val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
) {
    fun sendImeStart(view: View, otherInsets: AndroidXInsets, type: Int): WindowInsetsAnimation {
        return rule.runOnIdle {
            val animation =
                WindowInsetsAnimation(AndroidWindowInsets.Type.ime(), LinearInterpolator(), 100L)
            view.dispatchWindowInsetsAnimationPrepare(animation)

            val imeInsets = FrameworkInsets.of(0, 0, 0, 20)
            val bounds = WindowInsetsAnimation.Bounds(FrameworkInsets.NONE, imeInsets)
            view.dispatchWindowInsetsAnimationStart(animation, bounds)
            val targetInsets =
                android.view.WindowInsets.Builder()
                    .setInsets(android.view.WindowInsets.Type.ime(), imeInsets)
                    .setInsets(type, otherInsets.toPlatformInsets())
                    .build()
            view.dispatchApplyWindowInsets(targetInsets)
            animation
        }
    }

    fun WindowInsetsAnimation.sendImeProgress(view: View, progress: Float) {
        return rule.runOnIdle {
            val bottom = (20 * progress).roundToInt()
            val imeInsets = FrameworkInsets.of(0, 0, 0, bottom)
            val systemBarsInsets = FrameworkInsets.of(10, 11, 12, 13)
            val animatedInsets =
                AndroidWindowInsets.Builder()
                    .setInsets(AndroidWindowInsets.Type.systemBars(), systemBarsInsets)
                    .setInsets(AndroidWindowInsets.Type.ime(), imeInsets)
                    .build()

            val progressInsets =
                view.dispatchWindowInsetsAnimationProgress(animatedInsets, listOf(this))
            assertThat(progressInsets.isConsumed).isTrue()
        }
    }

    fun WindowInsetsAnimation.sendImeEnd(view: View) {
        rule.runOnIdle { view.dispatchWindowInsetsAnimationEnd(this) }
    }
}

/**
 * A View below the compose View that overrides the insets sent by the system. The compat
 * onApplyWindowInsets listener calls requestApplyInsets(), which results in the insets being sent
 * again. If we don't override the insets then the system insets (which are likely 0) will override
 * the insets that we set in the test.
 */
internal class InsetsView(context: Context) : FrameLayout(context) {
    var myInsets: AndroidWindowInsets? = null

    override fun dispatchApplyWindowInsets(insets: AndroidWindowInsets): AndroidWindowInsets {
        return super.dispatchApplyWindowInsets(myInsets ?: insets)
    }

    fun findComposeView(): View = findComposeView(this)!!

    private companion object {
        fun findComposeView(view: View): View? {
            if (view is ViewRootForTest) {
                return view
            } else if (view is ViewGroup) {
                view.forEach { child ->
                    val composeView = findComposeView(child)
                    if (composeView != null) {
                        return composeView
                    }
                }
            }
            return null
        }
    }
}

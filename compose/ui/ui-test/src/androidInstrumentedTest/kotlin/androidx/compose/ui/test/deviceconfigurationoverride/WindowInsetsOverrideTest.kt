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

package androidx.compose.ui.test.deviceconfigurationoverride

import android.graphics.Rect as AndroidRect
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.waterfall
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInsetsOverrideTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun systemBarsPadding() {
        testInsetsPadding(WindowInsetsCompat.Type.systemBars(), Modifier.systemBarsPadding())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun displayCutoutPadding() {
        var windowInsets by mutableStateOf(WindowInsetsCompat.Builder().build())

        lateinit var coordinates: LayoutCoordinates

        val insetsModifier = @Composable { Modifier.displayCutoutPadding() }

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(windowInsets)) {
                Box(Modifier.fillMaxSize().background(Color.Blue).then(insetsModifier())) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                }
            }
        }

        val (width, height) = rule.runOnIdle { coordinates.boundsInRoot().bottomRight.round() }

        windowInsets = createDisplayCutoutInsets(width, height)

        rule.runOnIdle {
            val expectedRect = Rect(10f, 11f, width - 12f, height - 13f)
            assertThat(coordinates.boundsInRoot()).isEqualTo(expectedRect)
        }
    }

    private fun createDisplayCutoutInsets(width: Int, height: Int): WindowInsetsCompat {
        val centerWidth = width / 2
        val centerHeight = height / 2

        val left = AndroidRect(0, centerHeight, 10, centerHeight + 2)
        val top = AndroidRect(centerWidth, 0, centerWidth + 2, 11)
        val right = AndroidRect(width - 12, centerHeight, width, centerHeight + 2)
        val bottom = AndroidRect(centerWidth, height - 13, centerWidth + 2, height)
        val safeInsets = AndroidXInsets.of(10, 11, 12, 13)
        return WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 11, 0, 0))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), safeInsets)
            .setDisplayCutout(
                DisplayCutoutCompat(
                    safeInsets,
                    left,
                    top,
                    right,
                    bottom,
                    AndroidXInsets.of(1, 2, 3, 4)
                )
            )
            .build()
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
            sentInsets = AndroidXInsets.of(10, 0, 0, 0)
        )
    }

    @Test
    fun navigationBarsPaddingRight() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding(),
            sentInsets = AndroidXInsets.of(0, 0, 12, 0)
        )
    }

    @Test
    fun navigationBarsPaddingBottom() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding(),
            sentInsets = AndroidXInsets.of(0, 0, 0, 13)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun navigationBarsPaddingApi30() {
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            Modifier.navigationBarsPadding()
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
            expected = { w, h -> Rect(0f, 10f, w.toFloat(), h.toFloat()) }
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
            expected = { width, height -> Rect(10f, 0f, width.toFloat(), height.toFloat()) }
        ) {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        }

    @Test
    fun insetsPaddingNavigationBarsRight() =
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            sentInsets = AndroidXInsets.of(0, 0, 10, 0),
            expected = { width, height -> Rect(0f, 0f, width - 10f, height.toFloat()) }
        ) {
            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        }

    @Test
    fun insetsPaddingNavigationBarsBottom() =
        testInsetsPadding(
            WindowInsetsCompat.Type.navigationBars(),
            sentInsets = AndroidXInsets.of(0, 0, 0, 10),
            expected = { width, height -> Rect(0f, 0f, width.toFloat(), height - 10f) }
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
        var windowInsets by mutableStateOf(WindowInsetsCompat.Builder().build())

        lateinit var coordinates: LayoutCoordinates

        val insetsModifier = @Composable { Modifier.windowInsetsPadding(WindowInsets.waterfall) }

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(windowInsets)) {
                Box(Modifier.fillMaxSize().background(Color.Blue).then(insetsModifier())) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                }
            }
        }

        val (width, height) = rule.runOnIdle { coordinates.boundsInRoot().bottomRight.round() }

        windowInsets = createDisplayCutoutInsets(width, height)

        rule.runOnIdle {
            val expectedRect = Rect(1f, 2f, width - 3f, height - 4f)
            assertThat(coordinates.boundsInRoot()).isEqualTo(expectedRect)
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
        val coordinates =
            setInsetContent(
                WindowInsetsCompat.Builder()
                    .setInsets(
                        WindowInsetsCompat.Type.navigationBars(),
                        AndroidXInsets.of(0, 0, 0, 15)
                    )
                    .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 10, 0, 0))
                    .setInsets(WindowInsetsCompat.Type.ime(), AndroidXInsets.of(0, 0, 0, 5))
                    .build()
            ) {
                val windowInsets = WindowInsets
                val insets =
                    windowInsets.navigationBars
                        .union(windowInsets.statusBars)
                        .union(windowInsets.ime)
                Modifier.windowInsetsPadding(insets)
            }

        rule.waitUntil {
            val size = coordinates.findRootCoordinates().size
            val width = size.width
            val height = size.height
            coordinates.boundsInRoot() == Rect(0f, 10f, width.toFloat(), height - 15f)
        }
    }

    @Test
    fun consumedInsets() {
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.systemBars(),
                            AndroidXInsets.of(10, 11, 12, 13)
                        )
                        .build()
                ) then DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
            ) {
                with(LocalDensity.current) {
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

        rule.waitUntil {
            val size = coordinates.findRootCoordinates().size
            val width = size.width
            val height = size.height
            coordinates.boundsInRoot() == Rect(10f, 11f, width - 12f, height - 13f)
        }
    }

    @Test
    fun consumedPadding() {
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            AndroidXInsets.of(0, 5, 0, 0)
                        )
                        .setInsets(
                            WindowInsetsCompat.Type.systemBars(),
                            AndroidXInsets.of(10, 11, 12, 13)
                        )
                        .build()
                ) then DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
            ) {
                Box(Modifier.statusBarsPadding()) {
                    Box(Modifier.systemBarsPadding()) {
                        Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                    }
                }
            }
        }

        rule.waitUntil {
            val size = coordinates.findRootCoordinates().size
            val width = size.width
            val height = size.height
            coordinates.boundsInRoot() == Rect(10f, 11f, width - 12f, height - 13f)
        }
    }

    @Test
    fun withConsumedWindowInsets() {
        var windowInsets by mutableStateOf(WindowInsetsCompat.Builder().build())
        var top = 0
        var consumingModifier: Modifier by mutableStateOf(Modifier)
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(windowInsets) then
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

        windowInsets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), AndroidXInsets.of(0, 5, 0, 0))
                .build()

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
                height - sentInsets.bottom.toFloat()
            )
        }
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
        val coordinates =
            setInsetContent(
                WindowInsetsCompat.Builder().setInsets(type, sentInsets).build(),
                modifier
            )

        rule.waitUntil {
            val size = coordinates.findRootCoordinates().size
            val width = size.width
            val height = size.height
            val expectedRect = expected(width, height)
            coordinates.boundsInRoot() == expectedRect
        }
    }

    @Test
    fun paddingValues() {
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.systemBars(),
                            AndroidXInsets.of(10, 11, 12, 13)
                        )
                        .build()
                )
            ) {
                val padding = WindowInsets.systemBars.asPaddingValues()
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
                }
            }
        }

        rule.waitUntil {
            val size = coordinates.findRootCoordinates().size
            val width = size.width
            val height = size.height
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

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            AndroidXInsets.of(0, 10, 0, 0)
                        )
                        .setInsets(
                            WindowInsetsCompat.Type.navigationBars(),
                            AndroidXInsets.of(0, 0, 0, 11)
                        )
                        .setInsets(WindowInsetsCompat.Type.ime(), AndroidXInsets.of(0, 10, 0, 20))
                        .build()
                )
            ) {
                Box(
                    Modifier.fillMaxSize().statusBarsPadding().onGloballyPositioned {
                        statusBar = it
                    }
                ) {
                    Box(
                        Modifier.navigationBarsPadding().onGloballyPositioned { navigationBar = it }
                    ) {
                        Box(Modifier.imePadding().fillMaxSize().onGloballyPositioned { ime = it })
                    }
                }
            }
        }

        rule.runOnIdle {
            val height = statusBar.findRootCoordinates().size.height
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
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            AndroidXInsets.of(0, 35, 0, 0)
                        )
                        .build()
                )
            ) {
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
        }
        // wait for layout
        rule.waitForIdle()

        rule.runOnIdle {
            val height = outer.findRootCoordinates().size.height
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
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            AndroidXInsets.of(0, 35, 0, 0)
                        )
                        .build()
                )
            ) {
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
        }

        rule.runOnIdle {
            val height = outer.findRootCoordinates().size.height
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

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            AndroidXInsets.of(0, 20, 0, 0)
                        )
                        .build()
                )
            ) {
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
        }

        rule.runOnIdle {
            assertThat(coordinates.size.height).isEqualTo(41)
            useMiddleInsets = false
        }

        rule.runOnIdle { assertThat(coordinates.size.height).isEqualTo(42) }
    }

    @Test
    fun reuseModifier() {
        var consumed1 = WindowInsets(0, 0, 0, 0)
        var consumed2 = WindowInsets(0, 0, 0, 0)
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            AndroidXInsets.of(0, 30, 0, 0)
                        )
                        .build()
                )
            ) {
                with(LocalDensity.current) {
                    val modifier = Modifier.consumeWindowInsets(PaddingValues(10.toDp()))
                    Box(modifier.fillMaxSize().onConsumedWindowInsetsChanged { consumed1 = it }) {
                        Box(modifier.fillMaxSize().onConsumedWindowInsetsChanged { consumed2 = it })
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(consumed1.getTop(rule.density)).isEqualTo(10)
            assertThat(consumed2.getTop(rule.density)).isEqualTo(20)
        }
    }

    private fun setInsetContent(
        windowInsets: WindowInsetsCompat,
        insetsModifier: @Composable () -> Modifier
    ): LayoutCoordinates {
        lateinit var coordinates: LayoutCoordinates

        setContent(windowInsets) {
            Box(Modifier.fillMaxSize().background(Color.Blue).then(insetsModifier())) {
                Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it })
            }
        }

        // wait for layout
        rule.waitForIdle()
        return coordinates
    }

    private fun setContent(
        windowInsets: WindowInsetsCompat,
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(windowInsets)) {
                content()
            }
        }
    }
}

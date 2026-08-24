/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.adaptive

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope.KeyboardKind
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.UiMediaScope.ViewingDistance
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.layout.WindowInsetsRulersProvider
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.areWindowInsetsRulersEnabled
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.InputDeviceBuilder
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowInputManager
import org.robolectric.shadows.ShadowPackageManager

@OptIn(
    ExperimentalMediaQueryApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalTestApi::class,
)
@RunWith(AndroidJUnit4::class)
class MediaQueryIntegrationTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var shadowPackageManager: ShadowPackageManager
    private lateinit var shadowInputManager: ShadowInputManager

    @Before
    fun setup() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        applicationContext = ApplicationProvider.getApplicationContext()
        shadowPackageManager = shadowOf(applicationContext.packageManager)

        val inputManager =
            applicationContext.getSystemService(Context.INPUT_SERVICE) as InputManager
        shadowInputManager = shadowOf(inputManager)
    }

    @After
    fun tearDown() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = false
    }

    @Test
    fun mediaQuery_windowDimensions_reflectsWindowInfoSize() {
        var result = false
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(400.dp, 800.dp))
            ) {
                result = mediaQuery { windowWidth == 400.dp && windowHeight == 800.dp }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_hasCamera_returnsTrueWhenFeaturePresent() {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_ANY, true)

        var result = false
        rule.setContent { result = mediaQuery { hasCamera } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_hasMicrophone_returnsTrueWhenFeaturePresent() {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_MICROPHONE, true)

        var result = false
        rule.setContent { result = mediaQuery { hasMicrophone } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_viewingDistance_returnsNearByDefault() {
        var result = false
        rule.setContent { result = mediaQuery { viewingDistance == ViewingDistance.Near } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_viewingDistance_returnsFarForTv() {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_LEANBACK, true)

        var result = false
        rule.setContent { result = mediaQuery { viewingDistance == ViewingDistance.Far } }
        assertTrue(result)
    }

    @Test
    @Suppress("DEPRECATION") // Simulating OS sticky broadcast for dock state
    fun mediaQuery_viewingDistance_returnsMediumWhenDocked() {
        val dockIntent =
            Intent(Intent.ACTION_DOCK_EVENT).apply {
                putExtra(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_DESK)
            }
        applicationContext.sendStickyBroadcast(dockIntent)

        var result = false
        rule.setContent { result = mediaQuery { viewingDistance == ViewingDistance.Medium } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_returnsFineForMouse() {
        addPointerDevice(id = 1, InputDevice.SOURCE_MOUSE)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.Fine } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_returnsCoarseForTouchscreen() {
        addPointerDevice(id = 1, InputDevice.SOURCE_TOUCHSCREEN)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.Coarse } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_prioritizesFineOverCoarse() {
        addPointerDevice(id = 1, InputDevice.SOURCE_TOUCHSCREEN)
        addPointerDevice(id = 2, InputDevice.SOURCE_MOUSE)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.Fine } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_ignoresDeviceWithoutMotionRange() {
        val fakeMouseDevice =
            InputDeviceBuilder.newBuilder().setId(1).setSources(InputDevice.SOURCE_MOUSE).build()
        shadowInputManager.addInputDevice(fakeMouseDevice)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.None } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_fallbackToFineForScrollAxes() {
        // Emulators and some virtual devices may use composite sources.
        val compositeSource = InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_TOUCHSCREEN

        val device =
            InputDeviceBuilder.newBuilder()
                .setId(1)
                .setSources(compositeSource)
                // Primary axis with composite source (triggers the fallback logic)
                .addMotionRange(MotionEvent.AXIS_X, compositeSource, 0f, 1000f, 0f, 0f, 1f)
                // Secondary fallback axis
                .addMotionRange(MotionEvent.AXIS_VSCROLL, compositeSource, -1f, 1f, 0f, 0f, 1f)
                .build()
        shadowInputManager.addInputDevice(device)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.Fine } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_fallbackToCoarseForTouchAxes() {
        val compositeSource = InputDevice.SOURCE_STYLUS or InputDevice.SOURCE_TOUCHSCREEN

        val device =
            InputDeviceBuilder.newBuilder()
                .setId(1)
                .setSources(compositeSource)
                // Primary axis with composite source
                .addMotionRange(MotionEvent.AXIS_X, compositeSource, 0f, 1000f, 0f, 0f, 1f)
                // Secondary fallback axis for touch devices
                .addMotionRange(MotionEvent.AXIS_TOUCH_MAJOR, compositeSource, 0f, 100f, 0f, 0f, 1f)
                .build()
        shadowInputManager.addInputDevice(device)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.Coarse } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_verifiedMotionRangeSourceOverridesFallback() {
        val compositeSource = InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_TOUCHSCREEN

        // Add a virtual/emulator device that triggers the Fine fallback
        val virtualDesktopDevice =
            InputDeviceBuilder.newBuilder()
                .setId(1)
                .setSources(compositeSource)
                .addMotionRange(MotionEvent.AXIS_X, compositeSource, 0f, 1000f, 0f, 0f, 1f)
                .addMotionRange(MotionEvent.AXIS_VSCROLL, compositeSource, -1f, 1f, 0f, 0f, 1f)
                .build()
        shadowInputManager.addInputDevice(virtualDesktopDevice)

        // Add a real, verified physical touchscreen (Exact source match)
        addPointerDevice(id = 2, InputDevice.SOURCE_TOUCHSCREEN)

        var result = false
        rule.setContent { result = mediaQuery { pointerPrecision == PointerPrecision.Coarse } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_keyboardKind_returnsPhysicalWhenConnected() {
        val physicalKeyboard =
            InputDeviceBuilder.newBuilder()
                .setId(1)
                .setKeyboardType(InputDevice.KEYBOARD_TYPE_ALPHABETIC)
                .build()
        shadowInputManager.addInputDevice(physicalKeyboard)

        var result = false
        rule.setContent { result = mediaQuery { keyboardKind == KeyboardKind.Physical } }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_keyboardKind_returnsVirtualWhenImeVisible() {
        var result = false
        lateinit var composeView: AndroidComposeView

        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            result = mediaQuery { keyboardKind == KeyboardKind.Virtual }
        }
        rule.waitForIdle()
        assertFalse(result)

        val insetsVisible =
            WindowInsetsCompat.Builder().setVisible(WindowInsetsCompat.Type.ime(), true).build()

        rule.runOnIdle {
            (composeView.insetsWatcher ?: composeView.insetsListener)?.onApplyWindowInsets(
                composeView,
                insetsVisible,
            )
        }
        rule.waitForIdle()
        assertTrue(result)

        val insetsHidden =
            WindowInsetsCompat.Builder().setVisible(WindowInsetsCompat.Type.ime(), false).build()

        rule.runOnIdle {
            (composeView.insetsWatcher ?: composeView.insetsListener)?.onApplyWindowInsets(
                composeView,
                insetsHidden,
            )
        }
        rule.waitForIdle()
        assertFalse(result)
    }

    @Test
    fun mediaQuery_keyboardKind_initiallyVirtualWhenImeVisible() {
        val inputManager =
            applicationContext.getSystemService(Context.INPUT_SERVICE) as InputManager
        lateinit var windowInfo: androidx.compose.ui.platform.WindowInfo
        rule.setContent { windowInfo = LocalWindowInfo.current }
        rule.waitForIdle()

        val scope =
            UiMediaScopeImpl(applicationContext, inputManager, windowInfo, imeVisibility = true)
        assertEquals(KeyboardKind.Virtual, scope.keyboardKind)
    }

    @Test
    fun mediaQuery_keyboardKind_fallbackUpdatesVisibilityWhenRulersDisabled() {
        var result = false
        lateinit var composeView: AndroidComposeView

        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            result = mediaQuery { keyboardKind == KeyboardKind.Virtual }
        }
        rule.waitForIdle()
        assertFalse(result)

        val insets =
            WindowInsetsCompat.Builder().setVisible(WindowInsetsCompat.Type.ime(), true).build()
        rule.runOnIdle {
            (composeView.insetsWatcher ?: composeView.insetsListener)?.onApplyWindowInsets(
                composeView,
                insets,
            )
        }
        rule.waitForIdle()
        assertTrue(result)

        areWindowInsetsRulersEnabled = false
        try {
            rule.runOnIdle { composeView.onGlobalLayout() }
            rule.waitForIdle()
            assertFalse(result)
        } finally {
            areWindowInsetsRulersEnabled = true
        }
    }

    @Test
    fun mediaQuery_isLazyInitialized_initiallyNull() {
        val shadowApp = shadowOf(applicationContext as Application)

        rule.setContent {
            LocalView.current as AndroidComposeView
            // No MediaQuery APIs used
        }
        rule.waitForIdle()

        // Verify the dock receiver is not registered eagerly on view attachment/composition
        val hasDockReceiver = shadowApp.hasReceiverForAction(Intent.ACTION_DOCK_EVENT)
        assertFalse("Dock receiver should not be registered eagerly", hasDockReceiver)
    }

    @Test
    fun mediaQuery_isLazyInitialized_instantiatedOnAccess() {
        val shadowApp = shadowOf(applicationContext as Application)
        var result = false
        rule.setContent { result = mediaQuery { viewingDistance == ViewingDistance.Near } }
        rule.waitForIdle()

        // Verify the dock receiver is registered lazily after the mediaQuery scope is read
        val hasDockReceiver = shadowApp.hasReceiverForAction(Intent.ACTION_DOCK_EVENT)
        assertTrue("Dock receiver should be registered lazily after read", hasDockReceiver)
        assertTrue(result)
    }

    @Test
    fun windowInsetsRulers_isRulerProvided_uninitialized() {
        lateinit var composeView: AndroidComposeView
        rule.setContent { composeView = LocalView.current as AndroidComposeView }
        rule.waitForIdle()

        val watcher = composeView.insetsWatcher
        if (watcher != null) {
            val provider = WindowInsetsRulersProvider(watcher)
            assertFalse(
                "Ruler should not be provided when insets are uninitialized",
                provider.isRulerProvided(WindowInsetsRulers.StatusBars.current.left),
            )
        }
    }

    private fun ShadowApplication.hasReceiverForAction(action: String): Boolean {
        return registeredReceivers.any { wrapper ->
            val actions = wrapper.intentFilter.actionsIterator() ?: return@any false
            var found = false
            while (actions.hasNext()) {
                if (actions.next() == action) {
                    found = true
                    break
                }
            }
            found
        }
    }

    private fun addPointerDevice(id: Int, source: Int) {
        val device =
            InputDeviceBuilder.newBuilder()
                .setId(id)
                .setSources(source)
                .addMotionRange(
                    MotionEvent.AXIS_X,
                    source,
                    /* min= */ 0f,
                    /* max= */ 1000f,
                    /* flat= */ 1f,
                    /* fuzz= */ 1f,
                    /* resolution= */ 1f,
                )
                .build()
        shadowInputManager.addInputDevice(device)
    }
}

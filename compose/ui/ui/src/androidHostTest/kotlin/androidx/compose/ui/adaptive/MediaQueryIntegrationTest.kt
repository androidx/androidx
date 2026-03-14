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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope.KeyboardKind
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.UiMediaScope.ViewingDistance
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.InputDeviceBuilder
import org.robolectric.shadows.ShadowInputManager
import org.robolectric.shadows.ShadowPackageManager

@OptIn(ExperimentalMediaQueryApi::class)
@RunWith(AndroidJUnit4::class)
class MediaQueryIntegrationTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var shadowPackageManager: ShadowPackageManager
    private lateinit var shadowInputManager: ShadowInputManager

    @Before
    fun setup() {
        applicationContext = ApplicationProvider.getApplicationContext()
        shadowPackageManager = shadowOf(applicationContext.packageManager)

        val inputManager =
            applicationContext.getSystemService(Context.INPUT_SERVICE) as InputManager
        shadowInputManager = shadowOf(inputManager)
    }

    @Test
    fun mediaQuery_windowDimensions_reflectsWindowInfoSize() {
        val mockWindowInfo =
            object : WindowInfo {
                override val isWindowFocused = true
                override val containerSize = IntSize.Zero
                override val containerDpSize = DpSize(width = 400.dp, height = 800.dp)
            }

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = mockWindowInfo,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { windowWidth == 400.dp && windowHeight == 800.dp }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_hasCamera_returnsTrueWhenFeaturePresent() {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_ANY, true)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { hasCamera }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_hasMicrophone_returnsTrueWhenFeaturePresent() {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_MICROPHONE, true)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { hasMicrophone }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_viewingDistance_returnsNearByDefault() {
        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { viewingDistance == ViewingDistance.Near }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_viewingDistance_returnsFarForTv() {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_LEANBACK, true)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { viewingDistance == ViewingDistance.Far }
            }
        }
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
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { viewingDistance == ViewingDistance.Medium }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_returnsFineForMouse() {
        addPointerDevice(id = 1, InputDevice.SOURCE_MOUSE)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { pointerPrecision == PointerPrecision.Fine }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_returnsCoarseForTouchscreen() {
        addPointerDevice(id = 1, InputDevice.SOURCE_TOUCHSCREEN)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { pointerPrecision == PointerPrecision.Coarse }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_prioritizesFineOverCoarse() {
        addPointerDevice(id = 1, InputDevice.SOURCE_TOUCHSCREEN)
        addPointerDevice(id = 2, InputDevice.SOURCE_MOUSE)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { pointerPrecision == PointerPrecision.Fine }
            }
        }
        assertTrue(result)
    }

    @Test
    fun mediaQuery_pointerPrecision_ignoresDeviceWithoutMotionRange() {
        val fakeMouseDevice =
            InputDeviceBuilder.newBuilder().setId(1).setSources(InputDevice.SOURCE_MOUSE).build()
        shadowInputManager.addInputDevice(fakeMouseDevice)

        var result = false
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { pointerPrecision == PointerPrecision.None }
            }
        }
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
        rule.setContent {
            val mediaScope =
                obtainUiMediaScope(
                    context = LocalContext.current,
                    view = LocalView.current,
                    windowInfo = LocalWindowInfo.current,
                )
            CompositionLocalProvider(LocalUiMediaScope provides mediaScope) {
                result = mediaQuery { keyboardKind == KeyboardKind.Physical }
            }
        }
        assertTrue(result)
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

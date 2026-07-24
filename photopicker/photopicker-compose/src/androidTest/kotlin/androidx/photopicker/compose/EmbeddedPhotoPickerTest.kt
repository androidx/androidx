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

package androidx.photopicker.compose

import android.content.res.Configuration
import android.os.Build
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.photopicker.testing.TestEmbeddedPhotoPickerProvider
import androidx.photopicker.testing.TestEmbeddedPhotoPickerSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class EmbeddedPhotoPickerTest {
    private val WAIT_TIMEOUT_DURATION_MILLIS = 5_000L
    private val TestEmbeddedPhotoPickerProvider.lastTestSession: TestEmbeddedPhotoPickerSession
        get() = sessions.last() as TestEmbeddedPhotoPickerSession

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        assumeFalse(
            "Test fails on cuttlefish b/460511933",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
    }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerProvidesSurfaceHostTokenToState() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState()
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                state.surfaceHostToken != null
            }
            assertThat(state.surfaceHostToken).isNotNull()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerProvidesSurfaceSizeToState() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            lateinit var state: EmbeddedPhotoPickerStateImpl

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState() as EmbeddedPhotoPickerStateImpl
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                state.surfaceSize != IntSize.Zero
            }
            assertThat(state.surfaceSize).isNotEqualTo(IntSize.Zero)
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerOpensSession() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()

            composeTestRule.setContent {
                EmbeddedPhotoPicker(
                    state = rememberEmbeddedPhotoPickerState(),
                    provider = testProvider,
                )
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession
            assertThat(session).isNotNull()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerClosesSessionOnDisposal() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()
            var showPicker by mutableStateOf(true)

            composeTestRule.setContent {
                if (showPicker) {
                    EmbeddedPhotoPicker(
                        state = rememberEmbeddedPhotoPickerState(),
                        provider = testProvider,
                    )
                }
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            assertThat(session.isClosed).isFalse()

            composeTestRule.runOnUiThread { showPicker = false }
            composeTestRule.waitForIdle()

            assertThat(session.isClosed).isTrue()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerResizingSync() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()

            val initialSize = IntSize(200, 400)
            val targetSize = IntSize(500, 800)
            var containerSize by mutableStateOf(initialSize)

            composeTestRule.setContent {
                SizingContainer(width = containerSize.width, height = containerSize.height) {
                    EmbeddedPhotoPicker(
                        state = rememberEmbeddedPhotoPickerState(),
                        provider = testProvider,
                    )
                }
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            composeTestRule.runOnUiThread { containerSize = targetSize }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                session.view.width == containerSize.width &&
                    session.view.height == containerSize.height
            }
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerConfigurationChangePropagation() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()

            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState()
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            val newConfig =
                Configuration().apply {
                    orientation = Configuration.ORIENTATION_LANDSCAPE
                    screenLayout = Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
                }

            state.notifyConfigurationChanged(newConfig)

            // Configuration.equals() is quite strict and checks incidental fields modified
            // internally. So we use specific fields (orientation and screenLayout) which are
            // reliable in integration tests.
            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                session.lastConfiguration?.orientation == newConfig.orientation &&
                    session.lastConfiguration?.screenLayout == newConfig.screenLayout
            }
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerProgrammaticExpansionSync() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()

            lateinit var state: EmbeddedPhotoPickerState

            composeTestRule.setContent {
                state = rememberEmbeddedPhotoPickerState(initialExpandedValue = false)
                EmbeddedPhotoPicker(state = state, provider = testProvider)
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession

            state.isExpanded = true

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                session.lastExpandedState == true
            }
            assertThat(state.isExpanded).isTrue()

            state.isExpanded = false
            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                session.lastExpandedState == false
            }
            assertThat(state.isExpanded).isFalse()
        }

    @Test
    @ExperimentalPhotoPickerComposeApi
    fun testEmbeddedPhotoPickerFeatureInfoPropagation() =
        runTest(composeTestRule.mainClock.scheduler) {
            val testProvider = TestEmbeddedPhotoPickerProvider.get()

            val maxSelectionLimit = 5
            val mimeTypes = listOf("image/jpeg", "video/mp4")
            val accentColor = 0xFF00FF00L
            val themeNightMode = Configuration.UI_MODE_NIGHT_YES
            val orderedSelection = true

            val customFeatureInfo =
                EmbeddedPhotoPickerFeatureInfo.Builder()
                    .setMaxSelectionLimit(maxSelectionLimit)
                    .setMimeTypes(mimeTypes)
                    .setAccentColor(accentColor)
                    .setThemeNightMode(themeNightMode)
                    .setOrderedSelection(orderedSelection)
                    .build()

            composeTestRule.setContent {
                EmbeddedPhotoPicker(
                    state = rememberEmbeddedPhotoPickerState(),
                    provider = testProvider,
                    embeddedPhotoPickerFeatureInfo = customFeatureInfo,
                )
            }

            composeTestRule.waitUntil(WAIT_TIMEOUT_DURATION_MILLIS) {
                testProvider.sessions.isNotEmpty()
            }
            val session = testProvider.lastTestSession
            val featureInfo = session.featureInfo

            assertThat(featureInfo.maxSelectionLimit).isEqualTo(maxSelectionLimit)
            assertThat(featureInfo.mimeTypes).containsExactlyElementsIn(mimeTypes)
            assertThat(featureInfo.accentColor).isEqualTo(accentColor)
            assertThat(featureInfo.themeNightMode).isEqualTo(themeNightMode)
            assertThat(featureInfo.isOrderedSelection).isEqualTo(orderedSelection)
        }
}

/**
 * A layout container that measures and constraints its child to an exact pixel size.
 *
 * This is used to test the resizing flow of [EmbeddedPhotoPicker]. Standard Compose sizing
 * modifiers (e.g. Modifier.size(dp)) define dimensions in density-independent pixels (Dp), which
 * Compose converts to physical pixels at runtime. This conversion is subject to floating-point
 * rounding errors and varies by device density, making exact assertions on the pixel values
 * received by [EmbeddedPhotoPickerState.notifyResized] fragile.
 *
 * By using this helper layout, we bypass Dp-to-Pixel conversion and force the child to be measured
 * with exact pixel constraints, allowing device-independent and stable pixel size assertions.
 */
@Composable
private fun SizingContainer(width: Int, height: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, _ ->
        // If the content composable is empty, layout a zero-sized block to avoid crashes.
        if (measurables.isEmpty()) {
            layout(width, height) {}
        } else {
            val placeable = measurables.first().measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.placeRelative(0, 0) }
        }
    }
}

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

package androidx.camera.compose

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import android.view.View
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationMode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplay
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class CameraDisplayRotationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val defaultDisplay: Display = ShadowDisplay.getDefaultDisplay()

    @Test
    fun returnsInitialDisplayRotation() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        var rotation = -1

        composeTestRule.setContent {
            rotation =
                rememberCameraDisplayRotation(
                    ImplementationMode.EXTERNAL,
                    TransformationMode.DEFERRED,
                )
        }

        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_90) }
    }

    @Test
    fun ninetyDegreeDisplayListener_doesNotJumpAheadOfConfigurationUpdate_thenTracks180Flip() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        var config by mutableStateOf(portraitConfiguration())
        var mode by mutableStateOf(ImplementationMode.EXTERNAL)
        var rotation = -1
        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides config) {
                // DEFERRED, so that switching from EXTERNAL to EMBEDDED stops following the default
                // display, while the view's display stays the same.
                rotation = rememberCameraDisplayRotation(mode, TransformationMode.DEFERRED)
            }
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_0) }

        // 90-degree DisplayListener callback before configuration update must not jump ahead, even
        // if rememberCameraDisplayRotation recomposes with a different parameter in the same epoch.
        composeTestRule.runOnUiThread { shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90) }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_0) }
        composeTestRule.runOnUiThread { mode = ImplementationMode.EMBEDDED }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_0) }

        // Configuration update applies the 90-degree rotation.
        composeTestRule.runOnUiThread {
            config = config.withOrientation(Configuration.ORIENTATION_LANDSCAPE)
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_90) }

        // Subsequent 180-degree flip and flip-back update without configuration changes.
        composeTestRule.runOnUiThread { shadowOf(defaultDisplay).setRotation(Surface.ROTATION_270) }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_270) }

        composeTestRule.runOnUiThread { shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90) }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_90) }

        // Flip 180 degrees to ROTATION_270, rotate 90 degrees to Portrait (ROTATION_0), then
        // rotate 90 degrees back to Landscape (ROTATION_90) with an equal Configuration and verify
        // the earlier ROTATION_270 override does not reactivate.
        val landscapeConfig = Configuration(config)
        composeTestRule.runOnUiThread { shadowOf(defaultDisplay).setRotation(Surface.ROTATION_270) }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_270) }

        composeTestRule.runOnUiThread {
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
            config = config.withOrientation(Configuration.ORIENTATION_PORTRAIT)
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_0) }

        composeTestRule.runOnUiThread {
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
            config = Configuration(landscapeConfig)
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_90) }
    }

    @Test
    fun externalDisplay_followsDefaultDisplayForExternalAndPreApplied_andViewDisplayOtherwise() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val externalDisplayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp")
        val externalDisplay = displayManager.getDisplay(externalDisplayId)
        shadowOf(externalDisplay).setRotation(Surface.ROTATION_0)
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)

        val externalView =
            object : View(context) {
                override fun getDisplay(): Display = externalDisplay

                override fun isAttachedToWindow(): Boolean = true
            }

        var externalRotation = -1
        var embeddedPreAppliedRotation = -1
        var embeddedDeferredRotation = -1
        composeTestRule.setContent {
            CompositionLocalProvider(LocalView provides externalView) {
                externalRotation =
                    rememberCameraDisplayRotation(
                        ImplementationMode.EXTERNAL,
                        TransformationMode.DEFERRED,
                    )
                embeddedPreAppliedRotation =
                    rememberCameraDisplayRotation(
                        ImplementationMode.EMBEDDED,
                        TransformationMode.PRE_APPLIED,
                    )
                // EMBEDDED DEFERRED follows the view's display to match the TextureView correction.
                embeddedDeferredRotation =
                    rememberCameraDisplayRotation(
                        ImplementationMode.EMBEDDED,
                        TransformationMode.DEFERRED,
                    )
            }
        }
        composeTestRule.runOnIdle {
            assertThat(externalRotation).isEqualTo(Surface.ROTATION_90)
            assertThat(embeddedPreAppliedRotation).isEqualTo(Surface.ROTATION_90)
            assertThat(embeddedDeferredRotation).isEqualTo(Surface.ROTATION_0)
        }

        // Rotating the primary phone by 90 degrees (90 -> 0) does not change the external
        // monitor's configuration, so DisplayListener must update immediately.
        composeTestRule.runOnUiThread { shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0) }
        composeTestRule.runOnIdle {
            assertThat(externalRotation).isEqualTo(Surface.ROTATION_0)
            assertThat(embeddedPreAppliedRotation).isEqualTo(Surface.ROTATION_0)
            assertThat(embeddedDeferredRotation).isEqualTo(Surface.ROTATION_0)
        }

        composeTestRule.runOnUiThread {
            shadowOf(externalDisplay).setRotation(Surface.ROTATION_180)
        }
        composeTestRule.runOnIdle {
            assertThat(externalRotation).isEqualTo(Surface.ROTATION_0)
            assertThat(embeddedPreAppliedRotation).isEqualTo(Surface.ROTATION_0)
            assertThat(embeddedDeferredRotation).isEqualTo(Surface.ROTATION_180)
        }
    }

    @Test
    fun layoutSizeChange_updatesNinetyDegreeRotationBeforeConfigurationChange() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        val portraitConfig = portraitConfiguration()
        val landscapeConfig = portraitConfig.withOrientation(Configuration.ORIENTATION_LANDSCAPE)
        var config by mutableStateOf(Configuration(portraitConfig))
        val trackingView =
            TrackingView(ApplicationProvider.getApplicationContext()).apply {
                layout(0, 0, 100, 200)
            }
        var rotation = -1

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalView provides trackingView,
                LocalConfiguration provides config,
            ) {
                rotation =
                    rememberCameraDisplayRotation(
                        ImplementationMode.EXTERNAL,
                        TransformationMode.DEFERRED,
                    )
            }
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_0) }

        // DisplayListener fires first without changing rotation; once layout bounds swap, the
        // layout listener publishes the 90-degree rotation in sync with the new bounds.
        composeTestRule.runOnUiThread {
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
            assertThat(rotation).isEqualTo(Surface.ROTATION_0)
            trackingView.layout(0, 0, 200, 100)
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_90) }

        composeTestRule.runOnUiThread { config = Configuration(landscapeConfig) }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_90) }

        composeTestRule.runOnUiThread {
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
            config = Configuration(portraitConfig)
            trackingView.layout(0, 0, 100, 200)
        }
        composeTestRule.runOnIdle { assertThat(rotation).isEqualTo(Surface.ROTATION_0) }
    }

    @Test
    fun stopsTrackingDisplayRotation_andUnregistersListeners_whenRemovedFromComposition() {
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        val trackingView = TrackingView(ApplicationProvider.getApplicationContext())
        var rotation = -1
        var isShown by mutableStateOf(true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalView provides trackingView) {
                if (isShown) {
                    rotation =
                        rememberCameraDisplayRotation(
                            ImplementationMode.EXTERNAL,
                            TransformationMode.DEFERRED,
                        )
                }
            }
        }
        composeTestRule.runOnIdle {
            assertThat(trackingView.attachListeners).hasSize(1)
            assertThat(trackingView.layoutListeners).hasSize(1)
            assertThat(rotation).isEqualTo(Surface.ROTATION_0)
        }

        composeTestRule.runOnUiThread {
            trackingView.getDisplayCallCount = 0
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_180)
        }
        composeTestRule.runOnIdle {
            assertThat(trackingView.getDisplayCallCount).isGreaterThan(0)
            assertThat(rotation).isEqualTo(Surface.ROTATION_180)
        }

        composeTestRule.runOnIdle { isShown = false }
        composeTestRule.runOnIdle {
            assertThat(trackingView.attachListeners).isEmpty()
            assertThat(trackingView.layoutListeners).isEmpty()
        }

        composeTestRule.runOnUiThread {
            trackingView.getDisplayCallCount = 0
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
            ShadowLooper.idleMainLooper()
        }
        composeTestRule.runOnIdle {
            assertThat(trackingView.getDisplayCallCount).isEqualTo(0)
            assertThat(rotation).isEqualTo(Surface.ROTATION_180)
        }
    }

    private fun portraitConfiguration(): Configuration =
        ApplicationProvider.getApplicationContext<Context>()
            .resources
            .configuration
            .withOrientation(Configuration.ORIENTATION_PORTRAIT)

    private fun Configuration.withOrientation(orientation: Int): Configuration =
        Configuration(this).also { it.orientation = orientation }

    private class TrackingView(context: Context) : View(context) {
        val attachListeners = mutableListOf<OnAttachStateChangeListener>()
        val layoutListeners = mutableListOf<OnLayoutChangeListener>()
        var getDisplayCallCount = 0

        override fun getDisplay(): Display {
            getDisplayCallCount++
            return super.getDisplay() ?: ShadowDisplay.getDefaultDisplay()
        }

        override fun isAttachedToWindow(): Boolean = true

        override fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
            super.addOnAttachStateChangeListener(listener)
            attachListeners.add(listener)
        }

        override fun removeOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
            super.removeOnAttachStateChangeListener(listener)
            attachListeners.remove(listener)
        }

        override fun addOnLayoutChangeListener(listener: OnLayoutChangeListener) {
            super.addOnLayoutChangeListener(listener)
            layoutListeners.add(listener)
        }

        override fun removeOnLayoutChangeListener(listener: OnLayoutChangeListener) {
            super.removeOnLayoutChangeListener(listener)
            layoutListeners.remove(listener)
        }
    }
}

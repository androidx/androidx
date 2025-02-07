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

package androidx.compose.material.ripple

import android.graphics.RenderNode
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [RippleHostView] */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RippleHostViewTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Test for b/377222399
     *
     * Note, without the corresponding fix this test would only fail on Samsung devices, unless
     * manually changing RippleDrawable.mRippleStyle.mRippleStyle to STYLE_SOLID through reflection.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun doesNotDrawWhileUnattached() {
        rule.runOnUiThread {
            val activity = rule.activity

            // View is explicitly not attached
            val rippleHostView = RippleHostView(activity)

            // Add a ripple while unattached
            rippleHostView.addRipple(
                PressInteraction.Press(Offset.Zero),
                true,
                Size(100f, 100f),
                radius = 10,
                color = Color.Red,
                alpha = 0.4f,
                onInvalidateRipple = {}
            )

            // Create a hardware backed canvas
            val canvas = RenderNode("RippleHostViewTest").beginRecording()

            // Should not crash
            rippleHostView.draw(canvas)
        }
    }
}

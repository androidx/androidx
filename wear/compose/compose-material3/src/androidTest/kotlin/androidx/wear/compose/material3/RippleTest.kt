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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class RippleTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun rippleConfiguration_disabled_dragged() {
        rippleConfiguration_dragged(false)
    }

    @Test
    fun rippleConfiguration_enabled_dragged() {
        rippleConfiguration_dragged(true)
    }

    private fun rippleConfiguration_dragged(isRippleEnabled: Boolean = true) {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        val boxContent: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RippleBoxWithBackground(interactionSource, ripple())
            }
        }

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                if (isRippleEnabled) {
                    boxContent()
                } else {
                    CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        boxContent()
                    }
                }
            }
        }

        rule.runOnIdle { scope!!.launch { interactionSource.emit(DragInteraction.Start()) } }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel =
                captureToImage().asAndroidBitmap().run { getPixel(width / 2, height / 2) }

            if (isRippleEnabled) {
                // Ripple should be showing and background should be different with Box's background
                Assert.assertNotEquals(RippleBoxBackgroundColor, Color(centerPixel))
            } else {
                // No ripple should be showing
                Assert.assertEquals(RippleBoxBackgroundColor, Color(centerPixel))
            }
        }
    }
}

/**
 * Generic Button like component with a border that allows injecting an [Indication], and has a
 * background with the same color around it - this makes the ripple contrast better and make it more
 * visible in screenshots.
 *
 * @param interactionSource the [MutableInteractionSource] that is used to drive the ripple state
 * @param ripple ripple [Indication] placed inside the surface
 */
@Composable
private fun RippleBoxWithBackground(
    interactionSource: MutableInteractionSource,
    ripple: Indication,
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        Box(Modifier.background(RippleBoxBackgroundColor).padding(25.dp)) {
            val shape = RoundedCornerShape(20)
            Box(
                Modifier.padding(25.dp)
                    .width(40.dp)
                    .height(40.dp)
                    .background(color = RippleBoxBackgroundColor, shape = shape)
                    .clip(shape)
                    .indication(interactionSource = interactionSource, indication = ripple)
            ) {}
        }
    }
}

private val RippleBoxBackgroundColor = Color.Blue

private const val Tag = "Ripple"

/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.constraintlayout.compose.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class MotionLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    /**
     * Tests that [MotionLayoutScope.motionFontSize] works as expected.
     *
     * See custom_text_size_scene.json5
     */
    @Test
    fun testCustomTextSize() {
        var animateToEnd by mutableStateOf(false)
        rule.setContent {
            val progress by animateFloatAsState(targetValue = if (animateToEnd) 1.0f else 0f)
            CustomTextSize(
                modifier = Modifier.size(200.dp),
                progress = progress
            )
        }
        rule.waitForIdle()

        var usernameSize = rule.onNodeWithTag("username").getUnclippedBoundsInRoot().size

        // TextSize is 18sp at the start. Since getting the resulting dimensions of the text is not
        // straightforward, the values were obtained by running the test
        assertEquals(55.dp.value, usernameSize.width.value, absoluteTolerance = 0.5f)
        assertEquals(25.dp.value, usernameSize.height.value, absoluteTolerance = 0.5f)

        animateToEnd = true
        rule.waitForIdle()

        usernameSize = rule.onNodeWithTag("username").getUnclippedBoundsInRoot().size

        // TextSize is 12sp at the end. Results in approx. 66% of the original text height
        assertEquals(35.dp.value, usernameSize.width.value, absoluteTolerance = 0.5f)
        assertEquals(17.dp.value, usernameSize.height.value, absoluteTolerance = 0.5f)
    }
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun CustomTextSize(modifier: Modifier, progress: Float) {
    val context = LocalContext.current
    CompositionLocalProvider(
        LocalDensity provides Density(1f, 1f),
        LocalTextStyle provides TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal
        )
    ) {
        MotionLayout(
            motionScene = MotionScene(
                content = context
                    .resources
                    .openRawResource(R.raw.custom_text_size_scene)
                    .readBytes()
                    .decodeToString()
            ),
            progress = progress,
            modifier = modifier
        ) {
            val profilePicProperties = motionProperties(id = "profile_pic")
            Box(
                modifier = Modifier
                    .layoutTestId("box")
                    .background(Color.DarkGray)
            )
            Image(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = profilePicProperties.value.color("background"),
                        shape = CircleShape
                    )
                    .layoutTestId("profile_pic")
            )
            Text(
                text = "Hello",
                fontSize = motionFontSize("username", "textSize"),
                modifier = Modifier.layoutTestId("username"),
                color = profilePicProperties.value.color("background")
            )
        }
    }
}
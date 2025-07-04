/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class AnimatedTextTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun testAnimatedText_hasTextSemantics() {
        rule.setContent {
            AnimatedText(
                "hey",
                fontRegistry =
                    rememberAnimatedTextFontRegistry(
                        FontVariation.Settings(),
                        FontVariation.Settings(),
                        startFontSize = 10.sp,
                        endFontSize = 10.sp,
                    ),
                progressFraction = { 0f },
            )
        }

        rule.onNodeWithText("hey").assertExists()
    }

    @Test
    fun testAnimatedText_hasSuppliedSemantics() {
        rule.setContent {
            AnimatedText(
                "hey",
                fontRegistry =
                    rememberAnimatedTextFontRegistry(
                        FontVariation.Settings(),
                        FontVariation.Settings(),
                        startFontSize = 10.sp,
                        endFontSize = 10.sp,
                    ),
                progressFraction = { 0f },
                modifier = Modifier.semantics { contentDescription = "test" },
            )
        }

        rule.onNodeWithText("hey").assertExists()
        rule.onNodeWithContentDescription("test").assertExists()
    }
}

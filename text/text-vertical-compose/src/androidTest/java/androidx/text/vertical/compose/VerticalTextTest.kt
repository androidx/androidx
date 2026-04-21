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

package androidx.text.vertical.compose

import android.os.Build
import android.text.SpannableString
import android.text.TextPaint
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class VerticalTextTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun exposesSemantics_correctly() = runComposeUiTest {
        val text = SpannableString("Hello Vertical")
        val paint = TextPaint().apply { textSize = 30f }

        setContent { VerticalText(text = text, paint = paint) }

        // Modern Compose testing relies on finding nodes by their semantic text
        onNodeWithText("Hello Vertical").assertExists().assertIsDisplayed()
    }
}

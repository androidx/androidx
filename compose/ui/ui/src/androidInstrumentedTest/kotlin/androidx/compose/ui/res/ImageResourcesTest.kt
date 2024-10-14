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

package androidx.compose.ui.res

import androidx.compose.testutils.assertPixels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ConfigChangeActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ImageResourcesTest {

    @get:Rule val rule = createAndroidComposeRule<ConfigChangeActivity>()

    @Test
    fun imageResourceTest() {
        rule.setContent {
            val image = ImageBitmap.imageResource(R.drawable.test_image)
            image.assertPixels { Color.Red }
        }
    }

    @Test
    fun imageResource_observesConfigChanges() {
        var image = ImageBitmap(1, 1)

        rule.activity.setDarkMode(false)
        rule.setContent { image = ImageBitmap.imageResource(R.drawable.test_image_day_night) }

        image.assertPixels { Color.Red }

        rule.activity.setDarkMode(true)
        rule.waitForIdle()
        image.assertPixels { Color.Blue }
    }
}

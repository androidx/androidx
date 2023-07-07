/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.translators

import android.content.res.Configuration
import androidx.glance.appwidget.ColorSubject.Companion.assertThat
import androidx.glance.appwidget.configurationContext
import androidx.glance.appwidget.test.R
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompoundButtonTranslatorTest {

    private val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    private val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }

    @Test
    fun resolveColorResource_0_shouldReturnFallback() {
        val colorProvider = ResourceCheckableColorProvider(
            resId = 0,
        )

        assertThat(colorProvider.getColor(lightContext, isChecked = true))
            .isSameColorAs(checkableColorProviderFallbackColor)
    }

    @Test
    fun resolveColorResource_invalid_shouldReturnFallback() {
        val colorProvider = ResourceCheckableColorProvider(
            resId = -1,
        )

        assertThat(colorProvider.getColor(lightContext, isChecked = true))
            .isSameColorAs(checkableColorProviderFallbackColor)
    }

    @Test
    fun resolveColorResource_valid_day_shouldReturnResolvedColor() {
        val colorProvider = ResourceCheckableColorProvider(
            resId = R.color.my_checkbox_colors,
        )

        assertThat(colorProvider.getColor(lightContext, isChecked = true))
            .isSameColorAs("#FF0000")
    }

    @Test
    fun resolveColorResource_valid_night_shouldReturnResolvedColor() {
        val colorProvider = ResourceCheckableColorProvider(
            resId = R.color.my_checkbox_colors,
        )

        assertThat(colorProvider.getColor(darkContext, isChecked = true))
            .isSameColorAs("#FFFF00")
    }
}

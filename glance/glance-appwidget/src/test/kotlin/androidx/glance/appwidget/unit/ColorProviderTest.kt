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

package androidx.glance.appwidget.unit

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.test.R
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider.Companion.createCheckableColorProvider
import androidx.glance.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import androidx.glance.appwidget.ColorSubject.Companion.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class ColorProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createCheckableColorProvider_checkedNull_uncheckedNull_shouldFallBackToSingleResource() {
        val provider = createCheckableColorProvider(
            source = "ColorProviderTest",
            checked = null,
            unchecked = null,
            fallback = R.color.my_checkbox_colors
        )

        assertIs<ResourceCheckableColorProvider>(provider)
        assertThat(provider.resId).isEqualTo(R.color.my_checkbox_colors)
    }

    @Test
    fun createCheckableColorProvider_checkedNull_uncheckedNotNull_shouldUseCheckedFromFallback() {
        val provider = createCheckableColorProvider(
            source = "ColorProviderTest",
            checked = null,
            unchecked = ColorProvider(day = Color.Red, night = Color.Yellow),
            fallback = R.color.my_checkbox_colors
        )

        assertIs<CheckedUncheckedColorProvider>(provider)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = true))
            .isSameColorAs("#FF0000")
        assertThat(provider.resolve(context, isNightMode = true, isChecked = true))
            .isSameColorAs("#FFFF00")
        assertThat(provider.resolve(context, isNightMode = false, isChecked = false))
            .isSameColorAs(Color.Red)
        assertThat(provider.resolve(context, isNightMode = true, isChecked = false))
            .isSameColorAs(Color.Yellow)
    }

    @Test
    fun createCheckableColorProvider_checkedNotNull_uncheckedNull_shouldUseUncheckedFromFallback() {
        val provider = createCheckableColorProvider(
            source = "ColorProviderTest",
            checked = ColorProvider(day = Color.Blue, night = Color.Green),
            unchecked = null,
            fallback = R.color.my_checkbox_colors
        )

        assertIs<CheckedUncheckedColorProvider>(provider)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = true))
            .isSameColorAs(Color.Blue)
        assertThat(provider.resolve(context, isNightMode = true, isChecked = true))
            .isSameColorAs(Color.Green)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = false))
            .isSameColorAs("#0000FF")
        assertThat(provider.resolve(context, isNightMode = true, isChecked = false))
            .isSameColorAs("#00FFFF")
    }

    @Test
    fun createCheckableColorProvider_checkedNotNull_uncheckedNotNull_shouldNotUseFallback() {
        val provider = createCheckableColorProvider(
            source = "ColorProviderTest",
            checked = ColorProvider(day = Color.Blue, night = Color.Green),
            unchecked = ColorProvider(day = Color.Red, night = Color.Yellow),
            fallback = R.color.my_checkbox_colors
        )

        assertIs<CheckedUncheckedColorProvider>(provider)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = true))
            .isSameColorAs(Color.Blue)
        assertThat(provider.resolve(context, isNightMode = true, isChecked = true))
            .isSameColorAs(Color.Green)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = false))
            .isSameColorAs(Color.Red)
        assertThat(provider.resolve(context, isNightMode = true, isChecked = false))
            .isSameColorAs(Color.Yellow)
    }

    @Test
    fun createCheckableColorProvider_fixedColors_shouldUseSameColorInDayAndNight() {
        val provider = createCheckableColorProvider(
            source = "ColorProviderTest",
            checked = ColorProvider(Color.Blue),
            unchecked = ColorProvider(Color.Red),
            fallback = R.color.my_checkbox_colors
        )

        assertIs<CheckedUncheckedColorProvider>(provider)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = true))
            .isSameColorAs(Color.Blue)
        assertThat(provider.resolve(context, isNightMode = true, isChecked = true))
            .isSameColorAs(Color.Blue)
        assertThat(provider.resolve(context, isNightMode = false, isChecked = false))
            .isSameColorAs(Color.Red)
        assertThat(provider.resolve(context, isNightMode = true, isChecked = false))
            .isSameColorAs(Color.Red)
    }

    @Test
    fun createCheckableColorProvider_uncheckedColorProvider_shouldThrow() {
        assertFailsWith<IllegalArgumentException> {
            createCheckableColorProvider(
                source = "ColorProviderTest",
                checked = ColorProvider(day = Color.Blue, night = Color.Green),
                unchecked = ColorProvider(R.color.my_checkbox_colors),
                fallback = R.color.my_checkbox_colors
            )
        }
    }

    @Test
    fun createCheckableColorProvider_checkedColorProvider_shouldThrow() {
        assertFailsWith<IllegalArgumentException> {
            createCheckableColorProvider(
                source = "ColorProviderTest",
                checked = ColorProvider(R.color.my_checkbox_colors),
                unchecked = ColorProvider(Color.Blue),
                fallback = R.color.my_checkbox_colors
            )
        }
    }

    @Test
    fun resolveColorProvider_FixedColorProvider() {
        assertThat(ColorProvider(Color.Blue).resolve(context)).isSameColorAs(Color.Blue)
    }

    @Test
    fun resolveColorProvider_ResourceColorProvider() {
        assertThat(ColorProvider(R.color.my_color).resolve(context))
            .isSameColorAs(Color(0xFFEEEEEE))
    }

    @Test
    @Config(qualifiers = "+night")
    fun resolveColorProvider_DayNightColorProvider() {
        assertThat(ColorProvider(Color.Blue, Color.Red).resolve(context))
            .isSameColorAs(Color.Red)
    }
}
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MinLinesConstrainerTest {
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun minConstrainer_from_new_onStyleChange() {
        val layoutDirection = LayoutDirection.Rtl

        val previous =
            MinLinesConstrainer.from(
                null,
                layoutDirection,
                TextStyle(color = Color.Green),
                density,
                fontFamilyResolver
            )

        val minMaxConstrainer =
            MinLinesConstrainer.from(
                null,
                layoutDirection,
                TextStyle(color = Color.Blue),
                density,
                fontFamilyResolver
            )

        assertThat(previous === minMaxConstrainer).isFalse()
    }

    @Test
    fun minConstrainer_from_reused() {
        val layoutDirection = LayoutDirection.Rtl
        // MinLinesConstrainer.from resolves the styling defaults passed to its ctor
        val constrainer =
            MinLinesConstrainer(
                layoutDirection,
                resolveDefaults(TextStyle(color = Color.Green), layoutDirection),
                density,
                fontFamilyResolver
            )

        val minMaxConstrainer =
            MinLinesConstrainer.from(
                constrainer,
                layoutDirection,
                TextStyle(color = Color.Green),
                density,
                fontFamilyResolver
            )

        assertThat(constrainer === minMaxConstrainer).isTrue()
    }

    @Test
    fun minConstrainer_from_cachedReused() {
        val layoutDirection = LayoutDirection.Rtl

        val previous =
            MinLinesConstrainer.from(
                null,
                layoutDirection,
                TextStyle(color = Color.Green),
                density,
                fontFamilyResolver
            )

        val minMaxConstrainer =
            MinLinesConstrainer.from(
                null,
                layoutDirection,
                TextStyle(color = Color.Green),
                density,
                fontFamilyResolver
            )

        assertThat(previous === minMaxConstrainer).isTrue()
    }
}

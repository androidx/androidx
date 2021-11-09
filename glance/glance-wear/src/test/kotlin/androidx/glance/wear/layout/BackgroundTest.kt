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

package androidx.glance.wear.layout

import androidx.glance.BackgroundModifier
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.findModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider
import androidx.glance.wear.test.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertIs

class BackgroundTest {
    @Test
    fun canUseBackgroundModifier() {
        val modifier = GlanceModifier.background(color = Color(0xFF223344))

        val addedModifier = requireNotNull(modifier.findModifier<BackgroundModifier>())

        val modifierColors = addedModifier.colorProvider
        assertIs<FixedColorProvider>(modifierColors)

        // Note: 0xFF223344 will coerce to a Long in Kotlin (making the equality test fail).
        // .toInt() is required here to make the tests work properly.
        assertThat(modifierColors.color.toArgb()).isEqualTo(0xFF223344.toInt())
    }

    @Test
    fun canUseBackgroundModifier_resId() {
        val modifier = GlanceModifier.background(color = R.color.color1)

        val addedModifier = requireNotNull(modifier.findModifier<BackgroundModifier>())

        val modifierColors = addedModifier.colorProvider
        assertIs<ResourceColorProvider>(modifierColors)
        assertThat(modifierColors.resId).isEqualTo(R.color.color1)
    }
}

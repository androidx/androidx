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

package androidx.glance.wear.tiles

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.unit.ColorProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class BorderTest {

    private val mockDisplayMetrics = DisplayMetrics().also {
        it.density = density
    }

    private val mockResources = mock<Resources>() {
        on { displayMetrics } doReturn mockDisplayMetrics
        on { getDimension(dimensionRes) } doReturn dimensionInDp * density
    }

    @Test
    fun buildBorderWithWidthInDp() {
        val modifiers = GlanceModifier.border(
            width = 5.dp,
            color = ColorProvider(Color.Red)
        )

        // Find the border modifier
        val borderModifier = checkNotNull(modifiers.findModifier<BorderModifier>())

        assertThat(borderModifier.color).isEqualTo(ColorProvider(Color.Red))
        assertThat(borderModifier.width).isEqualTo(BorderDimension(dp = 5.dp))
        assertThat(borderModifier.width.toDp(mockResources)).isEqualTo(5.dp)
    }

    @Test
    fun buildBorderWithWidthInDimenRes() {
        val modifiers = GlanceModifier.border(
            width = dimensionRes,
            color = ColorProvider(Color.Red)
        )

        // Find the border modifier
        val borderModifier = checkNotNull(modifiers.findModifier<BorderModifier>())

        assertThat(borderModifier.width).isEqualTo(BorderDimension(resourceId = dimensionRes))
        assertThat(borderModifier.width.toDp(mockResources).value).isEqualTo(dimensionInDp)
        assertThat(borderModifier.color).isEqualTo(ColorProvider(Color.Red))
    }

    private companion object {
        const val dimensionRes = 123
        const val density = 2f
        const val dimensionInDp = 3f
    }
}

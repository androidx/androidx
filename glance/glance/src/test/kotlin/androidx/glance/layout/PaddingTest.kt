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
package androidx.glance.layout

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.unit.dp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

class PaddingTest {

    private val mockDisplayMetrics = DisplayMetrics().also {
        it.density = density
    }

    private val mockResources = mock<Resources>() {
        on { displayMetrics } doReturn mockDisplayMetrics
        on { getDimension(dimensionRes1) } doReturn dimension1InDp * density
        on { getDimension(dimensionRes2) } doReturn dimension2InDp * density
    }

    @Test
    fun buildPadding() {
        val modifiers = GlanceModifier.padding(
            start = 1.dp,
            top = 2.dp,
            end = 3.dp,
            bottom = 4.dp
        )

        // Find the padding modifier...
        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(1.dp),
                top = PaddingDimension(2.dp),
                end = PaddingDimension(3.dp),
                bottom = PaddingDimension(4.dp),
            )
        )
    }

    @Test
    fun buildVerticalHorizontalPadding() {
        val modifiers = GlanceModifier.padding(vertical = 2.dp, horizontal = 4.dp)

        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(4.dp),
                top = PaddingDimension(2.dp),
                end = PaddingDimension(4.dp),
                bottom = PaddingDimension(2.dp),
            )
        )
    }

    @Test
    fun buildAllPadding() {
        val modifiers = GlanceModifier.padding(all = 5.dp)

        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(5.dp),
                top = PaddingDimension(5.dp),
                end = PaddingDimension(5.dp),
                bottom = PaddingDimension(5.dp),
            )
        )
    }

    @Test
    fun buildAbsolutePadding() {
        val modifiers = GlanceModifier.absolutePadding(
            left = 1.dp,
            top = 2.dp,
            right = 3.dp,
            bottom = 4.dp,
        )

        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                left = PaddingDimension(1.dp),
                top = PaddingDimension(2.dp),
                right = PaddingDimension(3.dp),
                bottom = PaddingDimension(4.dp),
            )
        )
    }

    @Test
    fun extractPadding_shouldReturnNull() {
        val modifiers = GlanceModifier.then(object : GlanceModifier.Element {})

        assertThat(modifiers.collectPadding()).isNull()
        assertThat(modifiers.collectPaddingInDp(mockResources)).isNull()
    }

    @Test
    fun mergePadding_noOrientation() {
        val modifiers = GlanceModifier.padding(horizontal = 15.dp).padding(vertical = dimensionRes1)

        val paddingModifier = checkNotNull(modifiers.collectPadding())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(15.dp),
                end = PaddingDimension(15.dp),
                top = PaddingDimension(dimensionRes1),
                bottom = PaddingDimension(dimensionRes1),
            )
        )

        val paddingInDp = checkNotNull(modifiers.collectPaddingInDp(mockResources))

        assertThat(paddingInDp).isEqualTo(
            PaddingInDp(
                start = 15.dp,
                end = 15.dp,
                top = dimension1InDp.dp,
                bottom = dimension1InDp.dp,
            )
        )
    }

    @Test
    fun mergePadding_resetWithAll() {
        val modifiers = GlanceModifier.padding(horizontal = 12.dp).padding(all = dimensionRes2)

        val paddingModifier = checkNotNull(modifiers.collectPadding())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(dp = 12.dp, resources = listOf(dimensionRes2)),
                end = PaddingDimension(dp = 12.dp, resources = listOf(dimensionRes2)),
                top = PaddingDimension(dimensionRes2),
                bottom = PaddingDimension(dimensionRes2),
            )
        )

        val paddingInDp = checkNotNull(modifiers.collectPaddingInDp(mockResources))

        assertThat(paddingInDp).isEqualTo(
            PaddingInDp(
                start = (12 + dimension2InDp).dp,
                end = (12 + dimension2InDp).dp,
                top = dimension2InDp.dp,
                bottom = dimension2InDp.dp,
            )
        )
    }

    @Test
    fun mergePadding_withRelativeOrientation() {
        val modifiers = GlanceModifier.padding(start = 15.dp, end = 12.dp, top = 20.dp)
            .padding(end = dimensionRes1)

        val paddingModifier = checkNotNull(modifiers.collectPadding())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(15.dp),
                end = PaddingDimension(dp = 12.dp, resources = listOf(dimensionRes1)),
                top = PaddingDimension(20.dp),
            )
        )
    }

    @Test
    fun mergePadding_withAbsoluteOrientation() {
        val modifiers = GlanceModifier.absolutePadding(left = 15.dp, right = 12.dp)
            .absolutePadding(left = dimensionRes1, bottom = dimensionRes2)

        val paddingModifier = checkNotNull(modifiers.collectPadding())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                left = PaddingDimension(dp = 15.dp, resources = listOf(dimensionRes1)),
                right = PaddingDimension(12.dp),
                bottom = PaddingDimension(dimensionRes2),
            )
        )
    }

    @Test
    fun mergePadding_setOrientationToRelative() {
        val modifiers = GlanceModifier.absolutePadding(left = 10.dp, right = 10.dp)
            .padding(start = dimensionRes1, end = dimensionRes2)

        val paddingModifier = checkNotNull(modifiers.collectPadding())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                start = PaddingDimension(dimensionRes1),
                end = PaddingDimension(dimensionRes2),
                left = PaddingDimension(10.dp),
                right = PaddingDimension(10.dp),
            )
        )
    }

    @Test
    fun mergePadding_setOrientationToAbsolute() {
        val modifiers = GlanceModifier.padding(start = dimensionRes1, end = dimensionRes2)
            .absolutePadding(left = 10.dp, right = 12.dp)

        val paddingModifier = checkNotNull(modifiers.collectPadding())

        assertThat(paddingModifier).isEqualTo(
            PaddingModifier(
                left = PaddingDimension(10.dp),
                right = PaddingDimension(12.dp),
                start = PaddingDimension(dimensionRes1),
                end = PaddingDimension(dimensionRes2),)
        )
    }

    @Test
    fun toRelative() {
        val paddingInDp = PaddingInDp(
            left = 1.dp,
            right = 2.dp,
            start = 10.dp,
            end = 20.dp,
            top = 50.dp,
            bottom = 100.dp,
        )

        assertThat(paddingInDp.toRelative(isRtl = true)).isEqualTo(
            PaddingInDp(
                start = 12.dp,
                end = 21.dp,
                top = 50.dp,
                bottom = 100.dp,
            )
        )

        assertThat(paddingInDp.toRelative(isRtl = false)).isEqualTo(
            PaddingInDp(
                start = 11.dp,
                end = 22.dp,
                top = 50.dp,
                bottom = 100.dp,
            )
        )
    }

    @Test
    fun toAbsolute() {
        val paddingInDp = PaddingInDp(
            left = 1.dp,
            right = 2.dp,
            start = 10.dp,
            end = 20.dp,
            top = 50.dp,
            bottom = 100.dp,
        )

        assertThat(paddingInDp.toAbsolute(isRtl = true)).isEqualTo(
            PaddingInDp(
                left = 21.dp,
                right = 12.dp,
                top = 50.dp,
                bottom = 100.dp,
            )
        )

        assertThat(paddingInDp.toAbsolute(isRtl = false)).isEqualTo(
            PaddingInDp(
                left = 11.dp,
                right = 22.dp,
                top = 50.dp,
                bottom = 100.dp,
            )
        )
    }

    private companion object {
        const val dimensionRes1 = 123
        const val dimensionRes2 = 321

        const val density = 2f

        const val dimension1InDp = 100f
        const val dimension2InDp = 200f
    }
}

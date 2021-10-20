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

import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertIs

class DimensionTest {
    @Test
    fun absoluteWidthModifier() {
        val modifier = GlanceModifier.width(5.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())

        val width = assertIs<Dimension.Dp>(widthModifier.width)
        assertThat(width.dp).isEqualTo(5.dp)
    }

    @Test
    fun wrapWidthModifier() {
        val modifier = GlanceModifier.wrapContentWidth()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Wrap)
    }

    @Test
    fun fillMaxWidthModifier() {
        val modifier = GlanceModifier.fillMaxWidth()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Fill)
    }

    @Test
    fun absoluteHeightModifier() {
        val modifier = GlanceModifier.height(5.dp)

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val height = assertIs<Dimension.Dp>(heightModifier.height)
        assertThat(height.dp).isEqualTo(5.dp)
    }

    @Test
    fun wrapHeightModifier() {
        val modifier = GlanceModifier.wrapContentHeight()

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isInstanceOf(Dimension.Wrap::class.java)
    }

    @Test
    fun fillMaxHeightModifier() {
        val modifier = GlanceModifier.fillMaxHeight()

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Fill)
    }

    @Test
    fun sizeModifier() {
        val modifier = GlanceModifier.size(1.dp, 2.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = assertIs<Dimension.Dp>(widthModifier.width)
        val height = assertIs<Dimension.Dp>(heightModifier.height)

        assertThat(width.dp).isEqualTo(1.dp)
        assertThat(height.dp).isEqualTo(2.dp)
    }

    @Test
    fun sizeModifierWithResources() {
        val modifier = GlanceModifier.size(123, 234)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = assertIs<Dimension.Resource>(widthModifier.width)
        val height = assertIs<Dimension.Resource>(heightModifier.height)

        assertThat(width.res).isEqualTo(123)
        assertThat(height.res).isEqualTo(234)
    }

    @Test
    fun combinedSizeModifier() {
        val modifier = GlanceModifier.size(10.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = assertIs<Dimension.Dp>(widthModifier.width)
        val height = assertIs<Dimension.Dp>(heightModifier.height)

        assertThat(width.dp).isEqualTo(10.dp)
        assertThat(height.dp).isEqualTo(10.dp)
    }

    @Test
    fun combinedSizeModifierWithResources() {
        val modifier = GlanceModifier.size(123)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = assertIs<Dimension.Resource>(widthModifier.width)
        val height = assertIs<Dimension.Resource>(heightModifier.height)

        assertThat(width.res).isEqualTo(123)
        assertThat(height.res).isEqualTo(123)
    }

    @Test
    fun fillMaxSizeModifier() {
        val modifier = GlanceModifier.fillMaxSize()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Fill)
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Fill)
    }

    @Test
    fun wrapContentSizeModifier() {
        val modifier = GlanceModifier.wrapContentSize()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Wrap)
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Wrap)
    }

    @Test
    fun resourceWidthModifier() {
        val modifier = GlanceModifier.width(123)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())

        val width = assertIs<Dimension.Resource>(widthModifier.width)
        assertThat(width.res).isEqualTo(123)
    }

    @Test
    fun resourceHeightModifier() {
        val modifier = GlanceModifier.height(123)

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val height = assertIs<Dimension.Resource>(heightModifier.height)
        assertThat(height.res).isEqualTo(123)
    }
}

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

import androidx.glance.Modifier
import androidx.glance.findModifier
import androidx.glance.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertIs

class DimensionTest {
    @Test
    fun absoluteWidthModifier() {
        val modifier = Modifier.width(5.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())

        val width = assertIs<Dimension.Dp>(widthModifier.width)
        assertThat(width.dp).isEqualTo(5.dp)
    }

    @Test
    fun wrapWidthModifier() {
        val modifier = Modifier.wrapContentWidth()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Wrap)
    }

    @Test
    fun fillMaxWidthModifier() {
        val modifier = Modifier.fillMaxWidth()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Fill)
    }

    @Test
    fun absoluteHeightModifier() {
        val modifier = Modifier.height(5.dp)

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val height = assertIs<Dimension.Dp>(heightModifier.height)
        assertThat(height.dp).isEqualTo(5.dp)
    }

    @Test
    fun wrapHeightModifier() {
        val modifier = Modifier.wrapContentHeight()

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isInstanceOf(Dimension.Wrap::class.java)
    }

    @Test
    fun fillMaxHeightModifier() {
        val modifier = Modifier.fillMaxHeight()

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Fill)
    }

    @Test
    fun sizeModifier() {
        val modifier = Modifier.size(1.dp, 2.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = assertIs<Dimension.Dp>(widthModifier.width)
        val height = assertIs<Dimension.Dp>(heightModifier.height)

        assertThat(width.dp).isEqualTo(1.dp)
        assertThat(height.dp).isEqualTo(2.dp)
    }

    @Test
    fun combinedSizeModifier() {
        val modifier = Modifier.size(10.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = assertIs<Dimension.Dp>(widthModifier.width)
        val height = assertIs<Dimension.Dp>(heightModifier.height)

        assertThat(width.dp).isEqualTo(10.dp)
        assertThat(height.dp).isEqualTo(10.dp)
    }

    @Test
    fun fillMaxSizeModifier() {
        val modifier = Modifier.fillMaxSize()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Fill)
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Fill)
    }

    @Test
    fun wrapContentSizeModifier() {
        val modifier = Modifier.wrapContentSize()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        assertThat(widthModifier.width).isSameInstanceAs(Dimension.Wrap)
        assertThat(heightModifier.height).isSameInstanceAs(Dimension.Wrap)
    }

    @Test
    fun resourceWidthModifier() {
        val modifier = Modifier.width(123)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())

        val width = assertIs<Dimension.Resource>(widthModifier.width)
        assertThat(width.res).isEqualTo(123)
    }

    @Test
    fun resourceHeightModifier() {
        val modifier = Modifier.height(123)

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val height = assertIs<Dimension.Resource>(heightModifier.height)
        assertThat(height.res).isEqualTo(123)
    }
}

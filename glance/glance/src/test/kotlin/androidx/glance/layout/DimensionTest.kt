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

class DimensionTest {
    @Test
    fun absoluteWidthModifier() {
        val modifier = Modifier.width(5.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())

        val width = widthModifier.width as Dimension.Dp
        assertThat(width.dp).isEqualTo(5.dp)
    }

    @Test
    fun wrapWidthModifier() {
        val modifier = Modifier.wrapWidth()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isInstanceOf(Dimension.Wrap::class.java)
    }

    @Test
    fun expandWidthModifier() {
        val modifier = Modifier.expandWidth()

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        assertThat(widthModifier.width).isInstanceOf(Dimension.Expand::class.java)
    }

    @Test
    fun absoluteHeightModifier() {
        val modifier = Modifier.height(5.dp)

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val height = heightModifier.height as Dimension.Dp
        assertThat(height.dp).isEqualTo(5.dp)
    }

    @Test
    fun wrapHeightModifier() {
        val modifier = Modifier.wrapHeight()

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isInstanceOf(Dimension.Wrap::class.java)
    }

    @Test
    fun expandHeightModifier() {
        val modifier = Modifier.expandHeight()

        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())
        assertThat(heightModifier.height).isInstanceOf(Dimension.Expand::class.java)
    }

    @Test
    fun sizeModifier() {
        val modifier = Modifier.size(1.dp, 2.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = widthModifier.width as Dimension.Dp
        val height = heightModifier.height as Dimension.Dp

        assertThat(width.dp).isEqualTo(1.dp)
        assertThat(height.dp).isEqualTo(2.dp)
    }

    @Test
    fun combinedSizeModifier() {
        val modifier = Modifier.size(10.dp)

        val widthModifier = checkNotNull(modifier.findModifier<WidthModifier>())
        val heightModifier = checkNotNull(modifier.findModifier<HeightModifier>())

        val width = widthModifier.width as Dimension.Dp
        val height = heightModifier.height as Dimension.Dp

        assertThat(width.dp).isEqualTo(10.dp)
        assertThat(height.dp).isEqualTo(10.dp)
    }
}

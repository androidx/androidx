/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class TabStyleTest {
    @Test
    fun setShape() {
        val shape = Shape.CORNER_MEDIUM
        val style = TabStyle.Builder().setShape(shape).build()
        assertThat(style.shape).isEqualTo(shape)
    }

    @Test
    fun setSelectedBackgroundColorAndTextColor() {
        val shape = Shape.CORNER_MEDIUM
        val bgColor = CarColor.BLUE
        val textColor = CarColor.RED
        val style =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(bgColor)
                .setTextColor(textColor)
                .build()

        assertThat(style.shape).isEqualTo(shape)
        assertThat(style.selectedBackgroundColor).isEqualTo(bgColor)
        assertThat(style.textColor).isEqualTo(textColor)
    }

    @Test
    fun builder_copyConstructor() {
        val shape = Shape.CORNER_MEDIUM
        val bgColor = CarColor.BLUE
        val textColor = CarColor.RED
        val style =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(bgColor)
                .setTextColor(textColor)
                .build()

        val copy = TabStyle.Builder(style).build()
        assertThat(copy).isEqualTo(style)
        assertThat(copy.selectedBackgroundColor).isEqualTo(bgColor)
        assertThat(copy.textColor).isEqualTo(textColor)
    }

    @Test
    fun builder_copyConstructor_clearProperties() {
        val shape = Shape.CORNER_MEDIUM
        val bgColor = CarColor.BLUE
        val textColor = CarColor.RED
        val style =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(bgColor)
                .setTextColor(textColor)
                .build()

        // Clear shape and textColor, keep bgColor
        val modified = TabStyle.Builder(style).setShape(null).setTextColor(null).build()
        assertThat(modified.shape).isNull()
        assertThat(modified.textColor).isNull()
        assertThat(modified.selectedBackgroundColor).isEqualTo(bgColor)

        // Clear everything -> throws IllegalStateException because no fields are set
        val emptyBuilder =
            TabStyle.Builder(style)
                .setShape(null)
                .setSelectedBackgroundColor(null)
                .setTextColor(null)
        assertThrows(IllegalStateException::class.java) { emptyBuilder.build() }
    }

    @Test
    fun builder_copyConstructor_modifyProperties() {
        val style =
            TabStyle.Builder()
                .setShape(Shape.CORNER_MEDIUM)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()

        val newShape = Shape.CORNER_EXTRA_LARGE
        val newBg = CarColor.GREEN
        val newText = CarColor.YELLOW
        val modified =
            TabStyle.Builder(style)
                .setShape(newShape)
                .setSelectedBackgroundColor(newBg)
                .setTextColor(newText)
                .build()

        assertThat(modified.shape).isEqualTo(newShape)
        assertThat(modified.selectedBackgroundColor).isEqualTo(newBg)
        assertThat(modified.textColor).isEqualTo(newText)
    }

    @Test
    fun build_onlyShapeSet() {
        val shape = Shape.CORNER_MEDIUM
        val style = TabStyle.Builder().setShape(shape).build()
        assertThat(style.shape).isEqualTo(shape)
        assertThat(style.selectedBackgroundColor).isNull()
        assertThat(style.textColor).isNull()
    }

    @Test
    fun build_onlySelectedBackgroundColorSet() {
        val bgColor = CarColor.BLUE
        val style = TabStyle.Builder().setSelectedBackgroundColor(bgColor).build()
        assertThat(style.shape).isNull()
        assertThat(style.selectedBackgroundColor).isEqualTo(bgColor)
        assertThat(style.textColor).isNull()
    }

    @Test
    fun build_onlyTextColorSet() {
        val textColor = CarColor.RED
        val style = TabStyle.Builder().setTextColor(textColor).build()
        assertThat(style.shape).isNull()
        assertThat(style.selectedBackgroundColor).isNull()
        assertThat(style.textColor).isEqualTo(textColor)
    }

    @Test
    fun build_noFieldsSet_throws() {
        assertThrows(IllegalStateException::class.java) { TabStyle.Builder().build() }
    }

    @Test
    fun equals() {
        val shape = Shape.CORNER_EXTRA_LARGE

        val style1 =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()
        val style2 =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()
        val style3 =
            TabStyle.Builder()
                .setShape(Shape.NONE)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()
        val style4 =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(CarColor.GREEN)
                .setTextColor(CarColor.RED)
                .build()
        val style5 =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.GREEN)
                .build()

        assertThat(style1).isEqualTo(style2)
        assertThat(style1).isNotEqualTo(style3)
        assertThat(style1).isNotEqualTo(style4)
        assertThat(style1).isNotEqualTo(style5)
    }

    @Test
    fun hashCode_match() {
        val shape = Shape.CORNER_EXTRA_LARGE

        val style1 =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()
        val style2 =
            TabStyle.Builder()
                .setShape(shape)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()
        val style3 =
            TabStyle.Builder()
                .setShape(Shape.NONE)
                .setSelectedBackgroundColor(CarColor.BLUE)
                .setTextColor(CarColor.RED)
                .build()

        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
        assertThat(style1.hashCode()).isNotEqualTo(style3.hashCode())
    }
}

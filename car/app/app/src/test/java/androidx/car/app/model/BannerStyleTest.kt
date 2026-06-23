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

import android.content.Context
import androidx.car.app.TestUtils
import androidx.core.graphics.drawable.IconCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [BannerStyle]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class BannerStyleTest {
    @Test
    fun defaultShapeIsNull() {
        val background = Background.Builder().setColor(CarColor.BLUE).build()
        val style = BannerStyle.Builder().setBackground(background).build()
        assertThat(style.shape).isNull()
    }

    @Test
    fun defaultBackgroundIsNull() {
        val shape = Shape.CORNER_MEDIUM
        val style = BannerStyle.Builder().setShape(shape).build()
        assertThat(style.background).isNull()
    }

    @Test
    fun setShape() {
        val shape = Shape.CORNER_MEDIUM
        val style = BannerStyle.Builder().setShape(shape).build()
        assertThat(style.shape).isEqualTo(shape)
    }

    @Test
    fun setBackground() {
        val background = Background.Builder().setColor(CarColor.BLUE).build()
        val style = BannerStyle.Builder().setBackground(background).build()
        assertThat(style.background).isEqualTo(background)
    }

    @Test
    fun setBackground_withImage_throws() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val iconRaw =
            IconCompat.createWithResource(
                context,
                TestUtils.getTestDrawableResId(context, "ic_test_1"),
            )
        val icon = CarIcon.Builder(iconRaw).build()
        val background = Background.Builder().setImage(icon).build()
        assertThrows(IllegalArgumentException::class.java) {
            BannerStyle.Builder().setBackground(background)
        }
    }

    @Test
    fun build_noFieldsSet_throws() {
        assertThrows(IllegalStateException::class.java) { BannerStyle.Builder().build() }
    }

    @Test
    fun equals() {
        val shape = Shape.CORNER_FULL
        val background = Background.Builder().setColor(CarColor.BLUE).build()

        val style1 = BannerStyle.Builder().setShape(shape).setBackground(background).build()
        val style2 = BannerStyle.Builder().setShape(shape).setBackground(background).build()
        val style3 = BannerStyle.Builder().setShape(Shape.NONE).setBackground(background).build()

        assertThat(style1).isEqualTo(style2)
        assertThat(style1).isNotEqualTo(style3)
    }

    @Test
    fun hashCode_match() {
        val shape = Shape.CORNER_FULL
        val background = Background.Builder().setColor(CarColor.BLUE).build()

        val style1 = BannerStyle.Builder().setShape(shape).setBackground(background).build()
        val style2 = BannerStyle.Builder().setShape(shape).setBackground(background).build()

        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
    }
}

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

import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [Background]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class BackgroundTest {
    @Test
    fun transparent() {
        assertThat(Background.TRANSPARENT.color)
            .isEqualTo(CarColor.createCustom(Color.TRANSPARENT, Color.TRANSPARENT))
    }

    @Test
    fun setColor() {
        val color = CarColor.BLUE
        val background = Background.Builder().setColor(color).build()
        assertThat(background.color).isEqualTo(color)
    }

    @Test
    fun setAndGetImage() {
        val image =
            CarIcon.Builder(IconCompat.createWithContentUri(Uri.parse("content://test"))).build()
        val background = Background.Builder().setImage(image).build()
        assertThat(background.image).isEqualTo(image)
    }

    @Test
    fun setImage_notCustom_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            Background.Builder().setImage(CarIcon.APP_ICON)
        }
    }

    @Test
    fun build_noFieldsSet_throwsIllegalStateException() {
        assertThrows(IllegalStateException::class.java) { Background.Builder().build() }
    }

    @Test
    fun build_bothFieldsSet_throwsIllegalStateException() {
        val image =
            CarIcon.Builder(IconCompat.createWithContentUri(Uri.parse("content://test"))).build()
        assertThrows(IllegalStateException::class.java) {
            Background.Builder().setColor(CarColor.BLUE).setImage(image).build()
        }
    }

    @Test
    fun equals() {
        val color = CarColor.BLUE
        val background1 = Background.Builder().setColor(color).build()
        val background2 = Background.Builder().setColor(color).build()
        val background3 = Background.Builder().setColor(CarColor.RED).build()

        assertThat(background1).isEqualTo(background2)
        assertThat(background1).isNotEqualTo(background3)

        val image =
            CarIcon.Builder(IconCompat.createWithContentUri(Uri.parse("content://test"))).build()
        val backgroundWithImage1 = Background.Builder().setImage(image).build()
        val backgroundWithImage2 = Background.Builder().setImage(image).build()
        assertThat(backgroundWithImage1).isEqualTo(backgroundWithImage2)
        assertThat(backgroundWithImage1).isNotEqualTo(background1)
    }

    @Test
    fun hashCode_match() {
        val background1 = Background.Builder().setColor(CarColor.BLUE).build()
        val background2 = Background.Builder().setColor(CarColor.BLUE).build()

        assertThat(background1.hashCode()).isEqualTo(background2.hashCode())
    }
}

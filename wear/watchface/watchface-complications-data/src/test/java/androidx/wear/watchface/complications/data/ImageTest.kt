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

package androidx.wear.watchface.complications.data

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.SharedRobolectricTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SharedRobolectricTestRunner::class)
public class ImageTest {
    @Test
    public fun monochromaticImage() {
        val icon = Icon.createWithContentUri("icon")
        val ambientIcon = Icon.createWithContentUri("icon")
        val image = MonochromaticImage.Builder(icon)
            .setAmbientImage(ambientIcon)
            .build()
        assertThat(image.image).isEqualTo(icon)
        assertThat(image.ambientImage).isEqualTo(ambientIcon)
    }

    @Test
    public fun smallImage() {
        val icon = Icon.createWithContentUri("icon")
        val ambientIcon = Icon.createWithContentUri("icon")
        val image = SmallImage.Builder(icon, SmallImageType.PHOTO)
            .setAmbientImage(ambientIcon)
            .build()
        assertThat(image.image).isEqualTo(icon)
        assertThat(image.type).isEqualTo(SmallImageType.PHOTO)
        assertThat(image.ambientImage).isEqualTo(ambientIcon)
    }
}

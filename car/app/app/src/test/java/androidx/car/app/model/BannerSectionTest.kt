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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class BannerSectionTest {
    private val mBanner = Banner.Builder().setTitle("Banner text").build()
    private val mBanner2 = Banner.Builder().setTitle("Banner text 2").build()

    @Test
    fun build_withExactlyOneBanner_succeeds() {
        val section = BannerSection.Builder().addItem(mBanner).build()

        assertThat(section.itemsDelegate.size).isEqualTo(1)
    }

    @Test
    fun build_withZeroBanners_throwsException() {
        val exception =
            assertThrows(IllegalStateException::class.java) { BannerSection.Builder().build() }
        assertThat(exception.message).contains("must contain exactly one")
    }

    @Test
    fun build_withMultipleBanners_throwsException() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                BannerSection.Builder().addItem(mBanner).addItem(mBanner2).build()
            }
        assertThat(exception.message).contains("must contain exactly one")
    }

    @Test
    fun equals_whenSectionsHaveTheSameContent_returnsTrue() {
        val section1 =
            BannerSection.Builder()
                .addItem(mBanner)
                .setTitle("Title")
                .setNoItemsMessage("Message")
                .build()
        val section2 =
            BannerSection.Builder()
                .addItem(mBanner)
                .setTitle("Title")
                .setNoItemsMessage("Message")
                .build()

        assertThat(section1).isEqualTo(section2)
    }

    @Test
    fun equals_whenNotEqual_returnsFalse() {
        val section1 = BannerSection.Builder().addItem(mBanner).setTitle("Title").build()
        val section2 = BannerSection.Builder().addItem(mBanner).setTitle("Different Title").build()

        assertThat(section1).isNotEqualTo(section2)
    }
}

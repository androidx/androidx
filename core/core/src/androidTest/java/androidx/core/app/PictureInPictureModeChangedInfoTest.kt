/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.app

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PictureInPictureModeChangedInfoTest {

    @Test
    fun isInPictureInPictureMode() {
        val info = PictureInPictureModeChangedInfo(true)
        assertThat(info.isInPictureInPictureMode).isTrue()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun newConfig() {
        val config = Configuration()
        val info = PictureInPictureModeChangedInfo(true, config)
        assertThat(info.newConfig).isSameInstanceAs(config)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun newConfigMissing() {
        val info = PictureInPictureModeChangedInfo(true)
        try {
            info.newConfig
            fail("Calling newConfig without passing one into PictureInPictureModeChangedInfo " +
                "should throw an IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }
}

/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.os.Build
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class ModeEnum3ATest {

    @Test
    fun iterateAfModes() {
        assertThat(AfMode.values().all { afMode -> afMode.value >= 0 }).isTrue()
    }

    @Test
    fun iterateAeModes() {
        assertThat(AeMode.values().all { aeMode -> aeMode.value >= 0 }).isTrue()
    }

    @Test
    fun iterateAwbModes() {
        assertThat(AwbMode.values().all { awbMode -> awbMode.value >= 0 }).isTrue()
    }
}
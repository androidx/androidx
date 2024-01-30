/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.processing

import android.os.Build
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.processing.TargetUtils.getHumanReadableName
import androidx.camera.core.processing.TargetUtils.isSuperset
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [TargetUtils].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TargetUtilsTest {

    @Test
    fun testIsSuperset() {
        assertThat(isSuperset(PREVIEW or VIDEO_CAPTURE, PREVIEW)).isTrue()
        assertThat(isSuperset(PREVIEW, PREVIEW or VIDEO_CAPTURE)).isFalse()
    }

    @Test
    fun verifyHumanReadableTargetsNames() {
        assertThat(getHumanReadableName(PREVIEW)).isEqualTo("PREVIEW")
        assertThat(getHumanReadableName(PREVIEW or VIDEO_CAPTURE))
            .isEqualTo("PREVIEW|VIDEO_CAPTURE")
        assertThat(getHumanReadableName(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE))
            .isEqualTo("IMAGE_CAPTURE|PREVIEW|VIDEO_CAPTURE")
    }
}

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

package androidx.camera.camera2.interop

import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.core.impl.CameraControlInternal
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCamera2Interop::class)
class Camera2CameraControlTest {

    @Test(expected = IllegalArgumentException::class)
    fun fromCameraControlThrows_whenNotCamera2Impl() {
        val wrongCameraControl =
            CameraControlInternal.DEFAULT_EMPTY_INSTANCE
        Camera2CameraControl.from(wrongCameraControl)
    }
}

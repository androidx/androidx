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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CrashWhenTakingPhotoWithAutoFlashAEModeQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureFailWithAutoFlashQuirk
import dagger.Module
import dagger.Provides

/**
 * A workaround to turn off the auto flash AE mode if device has the
 * [CrashWhenTakingPhotoWithAutoFlashAEModeQuirk] or [ImageCaptureFailWithAutoFlashQuirk].
 */
public interface AutoFlashAEModeDisabler {
    public fun getCorrectedAeMode(aeMode: Int): Int

    @Module
    public abstract class Bindings {
        public companion object {
            @Provides
            public fun provideAEModeDisabler(cameraQuirks: CameraQuirks): AutoFlashAEModeDisabler {
                val isFailWithAutoFlashQuirkEnabled =
                    cameraQuirks.quirks.contains(ImageCaptureFailWithAutoFlashQuirk::class.java)

                val isCrashWithAutoFlashQuirkEnabled =
                    DeviceQuirks[CrashWhenTakingPhotoWithAutoFlashAEModeQuirk::class.java] != null

                return if (isCrashWithAutoFlashQuirkEnabled || isFailWithAutoFlashQuirkEnabled)
                    AutoFlashAEModeDisablerImpl
                else NoOpAutoFlashAEModeDisabler
            }
        }
    }
}

public object AutoFlashAEModeDisablerImpl : AutoFlashAEModeDisabler {

    /**
     * Get AE mode corrected by the [CrashWhenTakingPhotoWithAutoFlashAEModeQuirk] and
     * [ImageCaptureFailWithAutoFlashQuirk].
     */
    override fun getCorrectedAeMode(aeMode: Int): Int {
        return if (aeMode == CONTROL_AE_MODE_ON_AUTO_FLASH) CONTROL_AE_MODE_ON else aeMode
    }
}

public object NoOpAutoFlashAEModeDisabler : AutoFlashAEModeDisabler {
    override fun getCorrectedAeMode(aeMode: Int): Int = aeMode
}

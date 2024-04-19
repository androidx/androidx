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

@file:RequiresApi(21) // TODO: b/200306659 - Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.TorchFlashRequiredFor3aUpdateQuirk
import dagger.Module
import dagger.Provides

/**
 * Workaround to use [CaptureRequest.FLASH_MODE_TORCH] for 3A operation.
 *
 * @see TorchFlashRequiredFor3aUpdateQuirk
 */
interface UseFlashModeTorchFor3aUpdate {
    fun shouldUseFlashModeTorch(): Boolean

    @Module
    abstract class Bindings {
        companion object {
            @Provides
            fun provideUseFlashModeTorchFor3aUpdate(
                cameraQuirks: CameraQuirks
            ): UseFlashModeTorchFor3aUpdate =
                if (cameraQuirks.quirks.contains(TorchFlashRequiredFor3aUpdateQuirk::class.java))
                    UseFlashModeTorchFor3aUpdateImpl
                else
                    NotUseFlashModeTorchFor3aUpdate
        }
    }
}

object UseFlashModeTorchFor3aUpdateImpl : UseFlashModeTorchFor3aUpdate {
    /** Returns true for torch should be used as flash. */
    override fun shouldUseFlashModeTorch() = true
}

object NotUseFlashModeTorchFor3aUpdate : UseFlashModeTorchFor3aUpdate {
    override fun shouldUseFlashModeTorch() = false
}

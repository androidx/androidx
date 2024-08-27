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

package androidx.camera.camera2.pipe.integration.compat

import androidx.camera.camera2.pipe.integration.compat.workaround.AutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.InactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.compat.workaround.Lock3ABehaviorWhenCaptureImage
import androidx.camera.camera2.pipe.integration.compat.workaround.MeteringRegionCorrection
import androidx.camera.camera2.pipe.integration.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.UseFlashModeTorchFor3aUpdate
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlash
import dagger.Module

/** Dependency bindings for adding camera compat related classes (e.g. workarounds, quirks etc.) */
@Module(
    includes =
        [
            AutoFlashAEModeDisabler.Bindings::class,
            InactiveSurfaceCloser.Bindings::class,
            MeteringRegionCorrection.Bindings::class,
            UseFlashModeTorchFor3aUpdate.Bindings::class,
            UseTorchAsFlash.Bindings::class,
            TemplateParamsOverride.Bindings::class,
            Lock3ABehaviorWhenCaptureImage.Bindings::class,
        ],
)
public abstract class CameraCompatModule

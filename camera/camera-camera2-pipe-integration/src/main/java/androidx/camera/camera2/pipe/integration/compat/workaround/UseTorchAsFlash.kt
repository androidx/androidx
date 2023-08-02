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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.compat.workaround

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.UseTorchAsFlashQuirk
import dagger.Module
import dagger.Provides

/**
 * Workaround to use torch as flash.
 *
 * @see UseTorchAsFlashQuirk
 */
interface UseTorchAsFlash {
    fun shouldUseTorchAsFlash(): Boolean

    @Module
    abstract class Bindings {
        companion object {
            @Provides
            fun provideUseTorchAsFlash(cameraQuirks: CameraQuirks): UseTorchAsFlash =
                if (cameraQuirks.quirks.contains(UseTorchAsFlashQuirk::class.java))
                    UseTorchAsFlashImpl
                else
                    NotUseTorchAsFlash
        }
    }
}

object UseTorchAsFlashImpl : UseTorchAsFlash {
    /** Returns true for torch should be used as flash. */
    override fun shouldUseTorchAsFlash() = true
}

object NotUseTorchAsFlash : UseTorchAsFlash {
    override fun shouldUseTorchAsFlash() = false
}

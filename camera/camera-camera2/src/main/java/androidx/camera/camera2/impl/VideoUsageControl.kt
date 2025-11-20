/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.impl

import androidx.camera.camera2.config.CameraScope
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.atomicfu.atomic

/** Tracks the video usage count of whether a camera is being used for a video output or not. */
@CameraScope
public class VideoUsageControl @Inject constructor() : UseCaseCameraControl {
    override var requestControl: UseCaseCameraRequestControl? = null

    /** An [atomic] for tracking the video usage count. */
    private val videoUsage = atomic(0)

    /** Increments usage count by 1. */
    public fun incrementUsage() {
        videoUsage.incrementAndGet().also {
            Camera2Logger.debug { "incrementUsage: videoUsage = $it" }
        }
    }

    /** Decrements usage count by 1. */
    public fun decrementUsage() {
        videoUsage.decrementAndGet().also {
            if (it < 0) {
                Camera2Logger.debug { "decrementUsage: videoUsage = $it, which is less than 0!" }
            } else {
                Camera2Logger.debug { "decrementUsage: videoUsage = $it" }
            }
        }
    }

    /** Resets the usage count to 0. */
    override fun reset() {
        videoUsage.value = 0
        Camera2Logger.debug { "reset: videoUsage = 0" }
    }

    public fun isInVideoUsage(): Boolean {
        return videoUsage.value.also {
            Camera2Logger.debug { "isInVideoUsage: videoUsage = $it" }
        } > 0
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(
            videoUsageControl: VideoUsageControl
        ): UseCaseCameraControl
    }
}

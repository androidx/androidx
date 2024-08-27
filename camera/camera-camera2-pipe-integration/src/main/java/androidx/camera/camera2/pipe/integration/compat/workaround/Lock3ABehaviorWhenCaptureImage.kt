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

package androidx.camera.camera2.pipe.integration.compat.workaround

import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.LockAeAndCaptureImageBreakCameraQuirk
import dagger.Module
import dagger.Provides

/**
 * Provides customized 3A lock behaviors before capturing an image.
 *
 * @property hasAeLockBehavior Indicates whether there is a specific AE (Auto Exposure) lock
 *   behavior defined. If true, the [aeLockBehavior] will be used; otherwise, the default AE
 *   behavior will be applied.
 * @property aeLockBehavior The specific AE lock behavior to apply if [hasAeLockBehavior] is true.
 *   If null and [hasAeLockBehavior] is true, AE lock will be effectively disabled.
 * @property hasAfLockBehavior Indicates whether there is a specific AF (Auto Focus) lock behavior
 *   defined. If true, the [afLockBehavior] will be used; otherwise, the default AF behavior will be
 *   applied.
 * @property afLockBehavior The specific AF lock behavior to apply if [hasAfLockBehavior] is true.
 *   If null and [hasAfLockBehavior] is true, AF lock will be effectively disabled.
 * @property hasAwbLockBehavior Indicates whether there is a specific AWB (Auto White Balance) lock
 *   behavior defined. If true, the [awbLockBehavior] will be used; otherwise, the default AWB
 *   behavior will be applied.
 * @property awbLockBehavior The specific AWB lock behavior to apply if [hasAwbLockBehavior] is
 *   true. If null and [hasAwbLockBehavior] is true, AWB lock will be effectively disabled.
 * @see LockAeAndCaptureImageBreakCameraQuirk
 */
public class Lock3ABehaviorWhenCaptureImage(
    private val hasAeLockBehavior: Boolean = false,
    private val aeLockBehavior: Lock3ABehavior? = null,
    private val hasAfLockBehavior: Boolean = false,
    private val afLockBehavior: Lock3ABehavior? = null,
    private val hasAwbLockBehavior: Boolean = false,
    private val awbLockBehavior: Lock3ABehavior? = null,
) {

    /**
     * Gets customized 3A lock behaviors, using provided defaults if no specific behavior is set.
     *
     * This method checks the `has*LockBehavior` properties to determine if a custom behavior is
     * defined for each 3A lock type (AE, AF, AWB). If a custom behavior is defined, it will be
     * returned; otherwise, the corresponding `default*Behavior` will be used.
     *
     * @param defaultAeBehavior Default AE lock behavior if none is specified.
     * @param defaultAfBehavior Default AF lock behavior if none is specified.
     * @param defaultAwbBehavior Default AWB lock behavior if none is specified.
     * @return A Triple containing the customized AE, AF, and AWB lock behaviors.
     */
    public fun getLock3ABehaviors(
        defaultAeBehavior: Lock3ABehavior? = null,
        defaultAfBehavior: Lock3ABehavior? = null,
        defaultAwbBehavior: Lock3ABehavior? = null
    ): Triple<Lock3ABehavior?, Lock3ABehavior?, Lock3ABehavior?> =
        Triple(
            if (hasAeLockBehavior) aeLockBehavior else defaultAeBehavior,
            if (hasAfLockBehavior) afLockBehavior else defaultAfBehavior,
            if (hasAwbLockBehavior) awbLockBehavior else defaultAwbBehavior
        )

    @Module
    public abstract class Bindings {
        public companion object {
            @Provides
            public fun provideLock3ABehaviorBeforeCaptureImage(
                cameraQuirks: CameraQuirks
            ): Lock3ABehaviorWhenCaptureImage =
                if (
                    cameraQuirks.quirks.contains(LockAeAndCaptureImageBreakCameraQuirk::class.java)
                ) {
                    doNotLockAe3ABehavior
                } else {
                    noCustomizedLock3ABehavior
                }
        }
    }

    public companion object {
        public val noCustomizedLock3ABehavior: Lock3ABehaviorWhenCaptureImage by lazy {
            Lock3ABehaviorWhenCaptureImage()
        }

        public val doNotLockAe3ABehavior: Lock3ABehaviorWhenCaptureImage by lazy {
            Lock3ABehaviorWhenCaptureImage(
                hasAeLockBehavior = true,
                aeLockBehavior = null // Explicitly disable AE lock
            )
        }
    }
}

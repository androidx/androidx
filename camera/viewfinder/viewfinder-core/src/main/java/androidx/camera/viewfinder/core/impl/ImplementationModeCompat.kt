/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.core.impl

import android.os.Build
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationMode
import androidx.camera.viewfinder.core.TransformationModeValue
import androidx.camera.viewfinder.core.impl.quirk.DeviceQuirks
import androidx.camera.viewfinder.core.impl.quirk.SurfaceViewNotCroppedByParentQuirk
import androidx.camera.viewfinder.core.impl.quirk.SurfaceViewStretchedQuirk

/**
 * Provides utility methods for selecting an appropriate [ImplementationMode] based on device
 * capabilities and API levels to work around compatibility issues.
 */
public class ImplementationModeCompat {
    public companion object {
        /**
         * Chooses a compatible [ImplementationMode] for the viewfinder.
         *
         * In general, this method returns [ImplementationMode.EXTERNAL] as it typically offers
         * higher performance. However, it returns [ImplementationMode.EMBEDDED] under the following
         * conditions:
         * - On API levels 24 (Android N) and below, due to compatibility quirks with the
         *   framework's implementation of external surfaces on these older API levels.
         * - On devices that exhibit specific quirky behavior with external surfaces, as identified
         *   by [DeviceQuirks].
         *
         * @return The chosen [ImplementationMode].
         */
        @JvmStatic
        public fun chooseCompatibleMode(): ImplementationMode =
            if (
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.N ||
                    DeviceQuirks.contains<SurfaceViewStretchedQuirk>() ||
                    DeviceQuirks.contains<SurfaceViewNotCroppedByParentQuirk>()
            ) {
                ImplementationMode.EMBEDDED
            } else {
                ImplementationMode.EXTERNAL
            }

        /**
         * Returns the [ImplementationMode] to use to display a source with the given
         * [transformationMode].
         *
         * When [implementationMode] is non-null, it is respected and returned unchanged. Otherwise,
         * a compatible default is chosen via [chooseCompatibleMode], additionally falling back to
         * [ImplementationMode.EMBEDDED] on API levels below 29 for [TransformationMode.PRE_APPLIED]
         * sources because [ImplementationMode.EXTERNAL] cannot rotate their content on those API
         * levels. The result only depends on the transformation mode, not on the current source
         * rotation, so that the implementation does not change when the source rotation changes.
         *
         * @param implementationMode The requested [ImplementationMode], or `null` to choose a
         *   compatible default.
         * @param transformationMode The [TransformationMode] of the source.
         * @return The [ImplementationMode] to use.
         */
        @JvmStatic
        public fun resolveImplementationMode(
            implementationMode: ImplementationMode?,
            @TransformationModeValue transformationMode: Int,
        ): ImplementationMode =
            implementationMode
                ?: if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        transformationMode == TransformationMode.PRE_APPLIED
                ) {
                    ImplementationMode.EMBEDDED
                } else {
                    chooseCompatibleMode()
                }
    }
}

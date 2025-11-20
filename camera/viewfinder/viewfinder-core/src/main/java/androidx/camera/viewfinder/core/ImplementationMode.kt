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

package androidx.camera.viewfinder.core

import androidx.annotation.RestrictTo
import androidx.camera.viewfinder.core.ImplementationMode.EMBEDDED
import androidx.camera.viewfinder.core.ImplementationMode.EXTERNAL

/**
 * The implementation mode of a Viewfinder.
 *
 * User preference on how the viewfinder should render the viewfinder. The viewfinder is displayed
 * with either a SurfaceView/AndroidExternalSurface or a TextureView/AndroidEmbeddedExternalSurface.
 * - [EXTERNAL] uses a SurfaceView/AndroidExternalSurface, it is generally better when it comes to
 *   certain key metrics, including power and latency.
 * - [EMBEDDED] uses a TextureView/AndroidEmbeddedExternalSurface it is better supported by a wider
 *   range of devices.
 *
 * The option is used to decide what is the best internal implementation given the device
 * capabilities and user configurations.
 */
enum class ImplementationMode(private val id: Int) {
    /**
     * Use a SurfaceView/AndroidExternalSurface for the Viewfinder when possible. It has somewhat
     * lower latency and less performance and power overhead. It offers more control on a single
     * drawing board, but does not support certain animations.
     */
    EXTERNAL(0),

    /** Use a TextureView/AndroidEmbeddedExternalSurface for the Viewfinder. */
    EMBEDDED(1);

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getId(): Int {
        return id
    }

    companion object {
        /**
         * Convert an Int id to ImplementationMode
         *
         * @throws IllegalArgumentException if id doesn't below to any ImplementationMode
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun fromId(id: Int): ImplementationMode {
            for (implementationMode in ImplementationMode.values()) {
                if (implementationMode.id == id) {
                    return implementationMode
                }
            }
            throw IllegalArgumentException("Unknown implementation mode id $id")
        }
    }
}

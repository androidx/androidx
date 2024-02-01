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

package androidx.camera.viewfinder.surface

import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RestrictTo

/**
 * The implementation mode of a viewfinder.
 *
 *
 *  User preference on how the viewfinder should render the viewfinder.
 * The viewfinder is displayed with either a [SurfaceView] or a
 * [TextureView]. A [SurfaceView] is generally better than a [TextureView]
 * when it comes to certain key metrics, including power and latency. On the other hand,
 * [TextureView] is better supported by a wider range of devices. The option is used to decide what
 * is the best internal implementation given the device capabilities and user configurations.
 */
enum class ImplementationMode(private val id: Int) {
    /**
     * Use a [SurfaceView] for the viewfinder when possible. A SurfaceView has somewhat
     * lower latency and less performance and power overhead than a TextureView. [SurfaceView]
     * offers more control on a single drawing board, but does not support certain animations.
     */
    PERFORMANCE(0),

    /**
     * Use a [TextureView] for the viewfinder.
     */
    COMPATIBLE(1);

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getId(): Int {
        return id
    }

    companion object {
        /**
         * Convert an Int id to ImplementationMode
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

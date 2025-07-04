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

/**
 * The request to get an [android.view.Surface] to display viewfinder input.
 *
 * This request contains requirements for the surface resolution and viewfinder input and output
 * information.
 *
 * @property width The requested surface width.
 * @property height The requested surface height.
 * @property implementationMode The [ImplementationMode] to apply to the viewfinder. Defaults to
 *   `null`, which will use the viewfinder's default implementation mode.
 * @property requestId An optional request ID to allow requests to be differentiated via [equals].
 *   Defaults to `null`.
 * @constructor Creates a new surface request with given resolution, and optional implementation
 *   mode request ID.
 */
class ViewfinderSurfaceRequest
@JvmOverloads
constructor(
    val width: Int,
    val height: Int,
    val implementationMode: ImplementationMode? = null,
    val requestId: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewfinderSurfaceRequest) return false

        if (width != other.width) return false
        if (height != other.height) return false
        if (implementationMode != other.implementationMode) return false
        if (requestId != other.requestId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = result * 31 + height.hashCode()
        result = result * 31 + implementationMode.hashCode()
        result = result * 31 + requestId.hashCode()

        return result
    }

    override fun toString(): String {
        return "ViewfinderSurfaceRequest(" +
            "width=$width, " +
            "height=$height, " +
            "implementationMode=$implementationMode, " +
            "requestId=$requestId" +
            ")"
    }

    /**
     * Creates a copy of this viewfinder surface request, allowing named properties to be altered
     * while keeping the rest unchanged.
     */
    @JvmSynthetic
    fun copy(
        width: Int = this.width,
        height: Int = this.height,
        implementationMode: ImplementationMode? = this.implementationMode,
        requestId: String? = this.requestId,
    ): ViewfinderSurfaceRequest =
        ViewfinderSurfaceRequest(width, height, implementationMode, requestId)
}

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

/**
 * Transformation information associated with the preview output.
 *
 * This information can be used to transform the Surface of a Viewfinder to be suitable to be
 * displayed.
 */
class TransformationInfo(

    /** Rotation of the source, relative to the device's natural rotation. */
    val sourceRotation: Int,

    /**
     * Indicates whether the source has been mirrored horizontally.
     *
     * This is common if the source comes from a camera that is front-facing.
     *
     * It is not common for both [isSourceMirroredHorizontally] and [isSourceMirroredVertically] to
     * be set to `true`. This is equivalent to [sourceRotation] being rotated by an additional 180
     * degrees.
     *
     * @see android.hardware.camera2.params.OutputConfiguration.MIRROR_MODE_AUTO
     * @see android.hardware.camera2.params.OutputConfiguration.MIRROR_MODE_H
     * @see androidx.camera.core.SurfaceRequest.TransformationInfo.isMirroring
     */
    val isSourceMirroredHorizontally: Boolean,

    /**
     * Indicates whether the source has been mirrored vertically.
     *
     * It is not common for a camera source to be mirror vertically, and typically
     * [isSourceMirroredHorizontally] will be the appropriate property.
     *
     * It is not common for both [isSourceMirroredHorizontally] and [isSourceMirroredVertically] to
     * be set to `true`. This is equivalent to [sourceRotation] being rotated by an additional 180
     * degrees.
     *
     * @see android.hardware.camera2.params.OutputConfiguration.MIRROR_MODE_V
     */
    val isSourceMirroredVertically: Boolean,

    /** Left offset of the cropRect in pixels */
    val cropRectLeft: Int,

    /** Top offset of the cropRect in pixels */
    val cropRectTop: Int,

    /** Right offset of the cropRect in pixels */
    val cropRectRight: Int,

    /** Bottom offset of the cropRect in pixels */
    val cropRectBottom: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformationInfo) return false

        if (sourceRotation != other.sourceRotation) return false
        if (isSourceMirroredHorizontally != other.isSourceMirroredHorizontally) return false
        if (isSourceMirroredVertically != other.isSourceMirroredVertically) return false
        if (cropRectLeft != other.cropRectLeft) return false
        if (cropRectTop != other.cropRectTop) return false
        if (cropRectRight != other.cropRectRight) return false
        if (cropRectBottom != other.cropRectBottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceRotation
        result = 31 * result + isSourceMirroredHorizontally.hashCode()
        result = 31 * result + isSourceMirroredVertically.hashCode()
        result = 31 * result + cropRectLeft
        result = 31 * result + cropRectTop
        result = 31 * result + cropRectRight
        result = 31 * result + cropRectBottom
        return result
    }
}

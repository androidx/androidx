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

import androidx.camera.viewfinder.core.TransformationInfo.Companion.CROP_NONE

/**
 * Transformation information associated with the preview output.
 *
 * This information can be used to transform the Surface of a Viewfinder to be suitable to be
 * displayed.
 */
class TransformationInfo
@JvmOverloads
constructor(
    /** Rotation of the source, relative to the device's natural rotation, in degrees. */
    val sourceRotation: Int = 0,

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
    val isSourceMirroredHorizontally: Boolean = false,

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
    val isSourceMirroredVertically: Boolean = false,

    /**
     * Left offset of the cropRect in pixels.
     *
     * The offset is in the coordinates of the original surface, before rotation or mirroring.
     *
     * If not set, this value will default to [CROP_NONE], which is equivalent to an offset of 0.
     */
    val cropRectLeft: Float = CROP_NONE,

    /**
     * Top offset of the cropRect in pixels
     *
     * The offset is in the coordinates of the original surface, before rotation or mirroring.
     *
     * If not set, this value will default to [CROP_NONE], which is equivalent to an offset of 0.
     */
    val cropRectTop: Float = CROP_NONE,

    /**
     * Right offset of the cropRect in pixels
     *
     * The offset is in the coordinates of the original surface, before rotation or mirroring.
     *
     * If not set, this value will default to [CROP_NONE], which is equivalent to an offset of the
     * width of the surface.
     */
    val cropRectRight: Float = CROP_NONE,

    /**
     * Bottom offset of the cropRect in pixels
     *
     * The offset is in the coordinates of the original surface, before rotation or mirroring.
     *
     * If not set, this value will default to [CROP_NONE], which is equivalent to an offset of the
     * height of the surface.
     */
    val cropRectBottom: Float = CROP_NONE,
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
        result = 31 * result + cropRectLeft.hashCode()
        result = 31 * result + cropRectTop.hashCode()
        result = 31 * result + cropRectRight.hashCode()
        result = 31 * result + cropRectBottom.hashCode()
        return result
    }

    override fun toString(): String {
        return "TransformationInfo(" +
            "sourceRotation=$sourceRotation, " +
            "isSourceMirroredHorizontally=$isSourceMirroredHorizontally, " +
            "isSourceMirroredVertically=$isSourceMirroredVertically, " +
            "cropRectLeft=$cropRectLeft, " +
            "cropRectTop=$cropRectTop, " +
            "cropRectRight=$cropRectRight, " +
            "cropRectBottom=$cropRectBottom" +
            ")"
    }

    companion object {
        /**
         * A crop value specifying no crop should be applied.
         *
         * When used as a value for [TransformationInfo.cropRectLeft],
         * [TransformationInfo.cropRectTop], [TransformationInfo.cropRectRight], or
         * [TransformationInfo.cropRectBottom], the crop rect dimension will be equivalent to the
         * resolution of the untransformed surface.
         */
        const val CROP_NONE: Float = Float.NaN

        /**
         * A [TransformationInfo] with default values.
         *
         * This transformation info instance has no source rotation, no mirroring, and no crop
         * rectangle.
         */
        @JvmField val DEFAULT: TransformationInfo = TransformationInfo()
    }
}

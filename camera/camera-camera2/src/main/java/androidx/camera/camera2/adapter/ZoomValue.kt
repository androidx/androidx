/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.adapter

import androidx.camera.camera2.internal.ZoomMath.getLinearZoomFromZoomRatio
import androidx.camera.camera2.internal.ZoomMath.getZoomRatioFromLinearZoom
import androidx.camera.core.CameraInfo
import androidx.camera.core.ZoomState

/**
 * Immutable adaptor to the [ZoomState] interface.
 *
 * @param zoomRatio The current zoom ratio.
 * @param minZoomRatio The minimum supported zoom ratio.
 * @param maxZoomRatio The maximum supported zoom ratio.
 * @param activeIntrinsicZoomRatio The intrinsic zoom ratio of the active physical lens relative to
 *   the default logical camera.
 */
@ConsistentCopyVisibility
public data class ZoomValue
internal constructor(
    private val zoomRatio: Float,
    private val minZoomRatio: Float,
    private val maxZoomRatio: Float,
    private val activeIntrinsicZoomRatio: Float = CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN,
    private val linearZoom: Float? = null,
) : ZoomState {
    public constructor(
        zoomRatio: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float,
        activeIntrinsicZoomRatio: Float = CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN,
    ) : this(zoomRatio, minZoomRatio, maxZoomRatio, activeIntrinsicZoomRatio, null)

    /**
     * ZoomValue should be created with either zoomRatio or linearZoom and the other value should be
     * calculated. If both are allowed to be set from outside, it becomes confusing regarding which
     * value to use if the values don't align with conversion values. Secondary constructor with a
     * LinearZoom value wrapper class is used for this purpose.
     */
    public data class LinearZoom(val value: Float)

    public constructor(
        linearZoom: LinearZoom,
        minZoomRatio: Float,
        maxZoomRatio: Float,
        activeIntrinsicZoomRatio: Float = CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN,
    ) : this(
        getZoomRatioFromLinearZoom(
            linearZoom = linearZoom.value,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
        ),
        minZoomRatio,
        maxZoomRatio,
        activeIntrinsicZoomRatio,
        linearZoom.value,
    )

    override fun getZoomRatio(): Float = zoomRatio

    override fun getMaxZoomRatio(): Float = maxZoomRatio

    override fun getMinZoomRatio(): Float = minZoomRatio

    override fun getLinearZoom(): Float =
        linearZoom
            ?: getLinearZoomFromZoomRatio(
                zoomRatio = zoomRatio,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio,
            )

    override fun getActiveIntrinsicZoomRatio(): Float = activeIntrinsicZoomRatio
}

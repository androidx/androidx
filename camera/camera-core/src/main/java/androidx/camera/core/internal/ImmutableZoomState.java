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

package androidx.camera.core.internal;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.ZoomState;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;

/** An implementation of {@link ZoomState} that is immutable. */
@AutoValue
public abstract class ImmutableZoomState implements ZoomState {
    /**
     * Creates an immutable instance of {@link ZoomState} with the default unknown active intrinsic
     * zoom ratio.
     */
    public static @NonNull ZoomState create(
            float zoomRatio, float maxZoomRatio, float minZoomRatio, float linearZoom) {
        return create(
                zoomRatio,
                maxZoomRatio,
                minZoomRatio,
                linearZoom,
                CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN);
    }

    /**
     * Creates an immutable instance of {@link ZoomState} with a specified active intrinsic zoom
     * ratio.
     */
    public static @NonNull ZoomState create(
            float zoomRatio,
            float maxZoomRatio,
            float minZoomRatio,
            float linearZoom,
            float activeIntrinsicZoomRatio) {
        return new AutoValue_ImmutableZoomState(
                zoomRatio, maxZoomRatio, minZoomRatio, linearZoom, activeIntrinsicZoomRatio);
    }

    /** Create an immutable instance of {@link ZoomState}. */
    public static @NonNull ZoomState create(@NonNull ZoomState zoomState) {
        return new AutoValue_ImmutableZoomState(
                zoomState.getZoomRatio(),
                zoomState.getMaxZoomRatio(),
                zoomState.getMinZoomRatio(),
                zoomState.getLinearZoom(),
                zoomState.getActiveIntrinsicZoomRatio());
    }

    @Override
    public abstract float getZoomRatio();

    @Override
    public abstract float getMaxZoomRatio();

    @Override
    public abstract float getMinZoomRatio();

    @Override
    public abstract float getLinearZoom();

    @Override
    public abstract float getActiveIntrinsicZoomRatio();
}

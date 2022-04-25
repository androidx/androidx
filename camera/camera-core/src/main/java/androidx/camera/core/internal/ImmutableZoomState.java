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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ZoomState;

import com.google.auto.value.AutoValue;

/**
 * An implementation of {@link ZoomState} that is immutable.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class ImmutableZoomState implements ZoomState {
    /** Create an immutable instance of {@link ZoomState}. */
    @NonNull
    public static ZoomState create(float zoomRatio, float maxZoomRatio, float minZoomRatio,
            float linearZoom) {
        return new AutoValue_ImmutableZoomState(zoomRatio, maxZoomRatio, minZoomRatio, linearZoom);
    }

    /** Create an immutable instance of {@link ZoomState}. */
    @NonNull
    public static ZoomState create(@NonNull ZoomState zoomState) {
        return new AutoValue_ImmutableZoomState(zoomState.getZoomRatio(),
                zoomState.getMaxZoomRatio(),
                zoomState.getMinZoomRatio(), zoomState.getLinearZoom());
    }

    @Override
    public abstract float getZoomRatio();

    @Override
    public abstract float getMaxZoomRatio();

    @Override
    public abstract float getMinZoomRatio();

    @Override
    public abstract float getLinearZoom();
}

/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface for injecting a {@link Surface}-based post-processing effect into CameraX.
 *
 * <p> TODO(b/233280438): update JavaDoc before going for API review.
 *
 * <p>Currently, this is only used by internal implementations such as "video cropping" and
 * "video recording during front/back camera switch". The interfaces themselves are also
 * placeholders and subject to change depending on API/design review. Moving forward, these
 * interfaces will be made public to developers and move to "androidx.camera.core".
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SurfaceEffect {

    /**
     * Bitmask option to indicate that this Surface will be used by CameraX as the output of the
     * {@link Preview} {@link UseCase}.
     */
    int PREVIEW = 1;

    /**
     * Bitmask option to indicate that this Surface will be used by CameraX as the output of
     * video capture {@link UseCase}.
     */
    int VIDEO_CAPTURE = 1 << 1;

    /**
     * Invoked when the upstream pipeline requires a {@link Surface} to write to.
     *
     * <p> The implementation is expected t o create a {@link Surface} backed
     * by{@link SurfaceTexture}, then listen for the
     * {@link SurfaceTexture#setOnFrameAvailableListener} to get the incoming upstream frames.
     *
     * @param request a request to provide {@link Surface} for input.
     */
    void onInputSurface(@NonNull SurfaceRequest request);

    /**
     * Invoked when the downstream pipeline provide Surface(s) to be written to.
     *
     * <p> The implementation is expected to draw processed frames to the {@link Surface}
     * acquired via {@link SurfaceOutput#getSurface} following specification defined in the said
     * {@link SurfaceOutput}.
     *
     * @param surfaceOutput a list of {@link SurfaceOutput}. For non stream sharing cases, the list
     *                      will only contain one element.
     */
    void onOutputSurface(@NonNull SurfaceOutput surfaceOutput);
}

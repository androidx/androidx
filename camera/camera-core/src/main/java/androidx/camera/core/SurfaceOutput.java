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
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A completable, single-use {@link Surface} for outputting processed camera frames.
 *
 * <p>Contains a {@link ListenableFuture<Surface>} and its characteristics along with methods
 * for notifying the end of life of the {@link Surface} and marking the {@link Surface} as no
 * longer in use.
 *
 * @hide
 * @see SurfaceEffect#onOutputSurface(SurfaceOutput)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SurfaceOutput {

    /**
     * Gets the output {@link Surface} for writing processed frames.
     *
     * <p> If there are multiple calls to the method, only the {@link OnCloseRequestedListener}
     * from the last call will be triggered.
     *
     * @param executor on which the listener should be invoked.
     * @param listener a listener to notify the implementation about the end-of-life of the
     *                 {@link SurfaceOutput}. The implementation should then invoke
     *                 {@link #close()} to mark the {@link Surface} as no longer in use.
     * @see OnCloseRequestedListener
     */
    @NonNull
    Surface getSurface(
            @NonNull Executor executor,
            @NonNull OnCloseRequestedListener listener);

    /**
     * This field indicates that what purpose the {@link Surface} will be used for.
     *
     * <ul>
     * <li>{@link SurfaceEffect#PREVIEW} if the {@link Surface} will be used for {@link Preview}.
     * <li>{@link SurfaceEffect#VIDEO_CAPTURE} if the {@link Surface} will be used for video
     * capture.
     * <li>{@link SurfaceEffect#PREVIEW} | {@link SurfaceEffect#VIDEO_CAPTURE} if the output
     * {@link Surface} will be used for sharing a single stream for both preview and video capture.
     * </ul>
     */
    @CameraEffect.Targets
    int getTargets();

    /**
     * Gets the size of the {@link Surface}.
     */
    @NonNull
    Size getSize();

    /**
     * Gets the format of the {@link Surface}.
     */
    int getFormat();

    /**
     * Call this method to mark the {@link Surface} provided via {@link #getSurface} as no longer in
     * use.
     *
     * <p>After this is called, the implementation should stop writing to the {@link Surface}.
     */
    void close();

    /**
     * Updates the 4 x 4 transformation matrix retrieved from {@link SurfaceTexture
     * #getTransformMatrix}.
     *
     * <p>This method applies an additional transformation on top of the value of
     * {@link SurfaceTexture#getTransformMatrix}. The result is matrix of the same format, which
     * is a transform matrix maps 2D homogeneous texture coordinates of the form (s, t, 0, 1)
     * with s and t in the inclusive range [0, 1] to the texture coordinate that should be used
     * to sample that location from the texture. The result should be used in the same way as
     * the original matrix. Please see the Javadoc of {@link SurfaceTexture#getTransformMatrix}.
     *
     * <p>The additional transformation is calculated based on the target rotation, target
     * resolution and the {@link ViewPort} configured by the app. The value could also include
     * workaround for device specific quirks.
     *
     * @param updated  the array into which the 4x4 matrix will be stored. The array must
     *                 have exactly 16 elements.
     * @param original the original value retrieved from
     *                 {@link SurfaceTexture#getTransformMatrix}. The array must have exactly 16
     *                 elements.
     * @see SurfaceTexture#getTransformMatrix(float[])
     */
    void updateTransformMatrix(@NonNull float[] updated, @NonNull float[] original);

    /**
     * A listener to notify the implementation about the end-of-life of the {@link Surface}.
     */
    interface OnCloseRequestedListener {

        /**
         * After this is invoked, the implementation should finish the current access to the
         * {@link Surface}, stop writing to the {@link Surface} and mark the
         * {@link SurfaceOutput} as closed by calling {@link SurfaceOutput#close()}.
         */
        void onCloseRequested();
    }
}

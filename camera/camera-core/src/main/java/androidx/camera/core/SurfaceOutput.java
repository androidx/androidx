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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * A {@link Surface} for drawing processed camera frames.
 *
 * <p>Contains a {@link Surface} and its characteristics along with methods to manage the
 * lifecycle of the {@link Surface}.
 *
 * @hide
 * @see SurfaceEffect#onOutputSurface(SurfaceOutput)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SurfaceOutput {

    /**
     * Gets the {@link Surface} for drawing processed frames.
     *
     * <p> If there are multiple calls to the method, only the {@link Consumer<Event>}
     * from the last call will be triggered.
     *
     * @param executor on which the listener should be invoked.
     * @param listener a listener to notify the implementation about the end-of-life of the
     *                 {@link SurfaceOutput}. The implementation should then invoke
     *                 {@link #close()} to mark the {@link Surface} as no longer in use.
     */
    @NonNull
    Surface getSurface(
            @NonNull Executor executor,
            @NonNull Consumer<Event> listener);

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
     * Get the rotation degrees.
     */
    int getRotationDegrees();

    /**
     * Call this method to mark the {@link Surface} as no longer in use.
     *
     * <p>After this is called, the implementation should stop writing to the {@link Surface}
     * provided via {@link #getSurface}
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
     * to sample that location from the texture. The matrix is stored in column-major order so that
     * it may be passed directly to OpenGL ES via the {@code glLoadMatrixf} or {@code
     * glUniformMatrix4fv} functions.
     *
     * <p>The additional transformation is calculated based on the target rotation, target
     * resolution and the {@link ViewPort} associated with the target {@link UseCase}. The value
     * could also include workarounds for device specific quirks.
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
     * Events of the {@link Surface} retrieved from
     * {@link SurfaceOutput#getSurface(Executor, Consumer)}.
     */
    @AutoValue
    abstract class Event {

        /**
         * Possible event codes.
         *
         * @hide
         */
        @IntDef({EVENT_REQUEST_CLOSE})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public @interface EventCode {
        }

        /**
         * The {@link Surface} provider is requesting to release the {@link Surface}.
         *
         * <p> Releasing a {@link Surface} while it's still being written into is not safe on
         * some devices. This is why the provider of the {@link Surface} will not release the
         * {@link Surface} without the CameraX's permission. Once this event is received, the
         * implementation should stop accessing the {@link Surface} as soon as possible, then
         * mark the {@link SurfaceOutput} as closed by calling {@link SurfaceOutput#close()}.
         * Once closed, CameraX will notify the {@link Surface} provider that it's safe to
         * release the {@link Surface}.
         */
        public static final int EVENT_REQUEST_CLOSE = 0;

        /**
         * Returns the event associated with the {@link SurfaceOutput}.
         */
        @EventCode
        public abstract int getEventCode();

        /**
         * Gets the {@link SurfaceOutput} associated with this event.
         */
        @NonNull
        public abstract SurfaceOutput getSurfaceOutput();

        /**
         * Creates a {@link Event} for sending to the implementation.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static SurfaceOutput.Event of(@EventCode int code,
                @NonNull SurfaceOutput surfaceOutput) {
            return new AutoValue_SurfaceOutput_Event(code, surfaceOutput);
        }
    }

    /** OpenGL transformation options for SurfaceOutput. */
    enum GlTransformOptions {
        /** Apply only the value of {@link SurfaceTexture#getTransformMatrix(float[])}. */
        USE_SURFACE_TEXTURE_TRANSFORM,

        /**
         * Discard the value of {@link SurfaceTexture#getTransformMatrix(float[])} and calculate
         * the transform based on crop rect, rotation degrees and mirroring.
         */
        APPLY_CROP_ROTATE_AND_MIRRORING,
    }
}

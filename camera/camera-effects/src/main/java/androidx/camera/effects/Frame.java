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

package androidx.camera.effects;

import static androidx.camera.effects.internal.Utils.lockCanvas;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;

import com.google.auto.value.AutoValue;

/**
 * Represents a frame that is about to be rendered.
 *
 * <p>Use this class to draw overlay on camera output. It contains a {@link Canvas} for the
 * drawing. It also provides metadata for positioning the overlay correctly, including
 * sensor-to-buffer transform, size, crop rect, rotation, mirroring, and timestamp.
 */
@AutoValue
public abstract class Frame {

    @NonNull
    private Surface mOverlaySurface;
    @Nullable
    private Canvas mOverlayCanvas;

    /**
     * Internal API to create a frame.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static Frame of(
            @NonNull Surface overlaySurface,
            long timestampNanos,
            @NonNull Size size,
            @NonNull SurfaceRequest.TransformationInfo transformationInfo) {
        Frame frame = new AutoValue_Frame(transformationInfo.getSensorToBufferTransform(), size,
                transformationInfo.getCropRect(), transformationInfo.getRotationDegrees(),
                transformationInfo.isMirroring(), timestampNanos);
        frame.mOverlaySurface = overlaySurface;
        return frame;
    }

    /**
     * Returns the sensor to image buffer transform matrix.
     *
     * <p>The value is a mapping from sensor coordinates to buffer coordinates, which is,
     * from the rect of the camera sensor to the rect defined by {@code (0, 0, #getSize()
     * #getWidth(), #getSize()#getHeight())}.
     *
     * <p>The value can be set on the {@link Canvas} using {@link Canvas#setMatrix} API. This
     * transforms the {@link Canvas} to the sensor coordinate system.
     *
     * @see SurfaceRequest.TransformationInfo#getSensorToBufferTransform()
     */
    @NonNull
    public abstract Matrix getSensorToBufferTransform();

    /**
     * Returns the resolution of the frame.
     *
     * <p>This is the size of the input {@link SurfaceTexture} created by the effect.
     *
     * @see SurfaceRequest#getResolution()
     */
    @NonNull
    public abstract Size getSize();

    /**
     * Returns the crop rect.
     *
     * <p>The value represents how CameraX intends to crop the input frame. The crop rect specifies
     * the region of valid pixels in the frame, using coordinates from (0, 0) to the (width,
     * height) of {@link #getSize()}. Only the overlay drawn within the bound of the crop rect
     * will be visible to the end users.
     *
     * <p>The crop rect is applied before the rotating and mirroring. The order of the operations
     * is as follows: 1) cropping, 2) rotating and 3) mirroring.
     *
     * @see SurfaceRequest.TransformationInfo#getCropRect()
     */
    @NonNull
    public abstract Rect getCropRect();

    /**
     * Returns the rotation degrees of the frame.
     *
     * <p>This is a clockwise rotation in degrees that needs to be applied to the frame. The
     * rotation will be determined by camera sensor orientation and UseCase configuration
     * such as {@link Preview#setTargetRotation}. The app must draw the overlay according to the
     * rotation degrees to ensure it is displayed correctly to the end users. For example, to
     * overlay a text, make sure the text's orientation is aligned with the rotation degrees.
     *
     * <p>The rotation is applied after the cropping but before the mirroring. The order of the
     * operations is as follows: 1) cropping, 2) rotating and 3) mirroring.
     *
     * @see SurfaceRequest.TransformationInfo#getRotationDegrees()
     */
    @IntRange(from = 0, to = 359)
    public abstract int getRotationDegrees();

    /**
     * Returns whether the buffer will be mirrored.
     *
     * <p>This flag indicates whether the buffer will be mirrored across the vertical
     * axis by the pipeline. For example, for front camera preview, the buffer is usually
     * mirrored before displayed to end users.
     *
     * <p>The mirroring is applied after the cropping and the rotating. The order of the
     * operations is as follows: 1) cropping, 2) rotating and 3) mirroring.
     *
     * @see SurfaceRequest.TransformationInfo#isMirroring()
     */
    public abstract boolean isMirroring();

    /**
     * Returns the timestamp of the frame in nanoseconds.
     *
     * <p>This value will match the frames from other streams. For example, for a
     * {@link ImageAnalysis} output that is originated from the same frame, this value will match
     * the value of {@link ImageInfo#getTimestamp()}.
     *
     * @see SurfaceTexture#getTimestamp()
     * @see ImageInfo#getTimestamp()
     */
    public abstract long getTimestampNanos();

    /**
     * Get the canvas for drawing the overlay.
     *
     * <p>Call this method to get the {@link Canvas} for drawing an overlay on top of the frame.
     * The {@link Canvas} is backed by a {@link SurfaceTexture} with a size equal to
     * {@link #getSize()}. To draw object in camera sensor coordinates, apply
     * {@link #getSensorToBufferTransform()} via {@link Canvas#setMatrix(Matrix)} before drawing.
     *
     * <p>Using this method introduces wait times to synchronize frame updates. The caller should
     * only invoke this method when it needs to draw overlay. For example, when an object is
     * detected in the frame.
     */
    @NonNull
    public Canvas getOverlayCanvas() {
        if (mOverlayCanvas == null) {
            mOverlayCanvas = lockCanvas(mOverlaySurface);
        }
        return mOverlayCanvas;
    }

    /**
     * Internal API to check whether the overlay canvas has been drawn into.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isOverlayDirty() {
        return mOverlayCanvas != null;
    }
}

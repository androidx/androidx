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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.SurfaceRequest;

import com.google.auto.value.AutoValue;

/**
 * Represents a frame that will be rendered next.
 *
 * <p>This class can be used to overlay graphics or data on camera output. It contains
 * information for drawing an overlay, including sensor-to-buffer transform, size, crop rect,
 * rotation, mirroring, and timestamp. It also provides a {@link Canvas} for the drawing.
 *
 * TODO(b/297509601): Make it public API in 1.4.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
@AutoValue
public abstract class Frame {

    private boolean mIsOverlayDirty = false;

    /**
     * Internal API to create a frame.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static Frame of(
            @NonNull Canvas overlayCanvas,
            long timestampNs,
            @NonNull Size size,
            @NonNull SurfaceRequest.TransformationInfo transformationInfo) {
        return new AutoValue_Frame(transformationInfo.getSensorToBufferTransform(), size,
                transformationInfo.getCropRect(), transformationInfo.getRotationDegrees(),
                transformationInfo.getMirroring(), timestampNs, overlayCanvas);
    }

    /**
     * Returns the sensor to image buffer transform matrix.
     *
     * <p>The value is a mapping from sensor coordinates to buffer coordinates, which is,
     * from the rect of {@link CameraCharacteristics#SENSOR_INFO_ACTIVE_ARRAY_SIZE} to the
     * rect defined by {@code (0, 0, #getSize()#getWidth(), #getSize()#getHeight())}.
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
     * @see SurfaceRequest#getResolution()
     */
    @NonNull
    public abstract Size getSize();

    /**
     * Returns the crop rect rectangle.
     *
     * <p>The value represents how the frame will be cropped by the CameraX pipeline. The crop
     * rectangle specifies the region of valid pixels in the frame, using coordinates from (0, 0)
     * to the (width, height) of {@link #getSize()}. Only the overlay drawn within the bound of
     * the crop rect will be visible to the end users.
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
     * rotation will be determined by {@link CameraCharacteristics} and UseCase configuration.
     * The app must draw the overlay according to the rotation degrees to ensure it is
     * displayed correctly to the end users.
     *
     * <p>The rotation is applied after the cropping but before the mirroring. The order of the
     * operations is as follows: 1) cropping, 2) rotating and 3) mirroring.
     *
     * @see SurfaceRequest.TransformationInfo#getRotationDegrees()
     */
    public abstract int getRotationDegrees();

    /**
     * Returns whether the buffer will be mirrored.
     *
     * <p>This flag indicates whether the buffer will be mirrored by the pipeline vertically. For
     * example, for front camera preview, the buffer is usually mirrored before displayed to end
     * users.
     *
     * <p>The mirroring is applied after the cropping and the rotating. The order of the
     * operations is as follows: 1) cropping, 2) rotating and 3) mirroring.
     *
     * @see SurfaceRequest.TransformationInfo#getMirroring()
     */
    public abstract boolean getMirroring();

    /**
     * Returns the timestamp of the frame in nanoseconds.
     *
     * @see SurfaceTexture#getTimestamp()
     * @see ImageInfo#getTimestamp()
     */
    public abstract long getTimestampNs();

    /**
     * Invalidates and returns the overlay canvas.
     *
     * <p>Call this method to get the {@link Canvas} for drawing an overlay on top of the frame.
     * The {@link Canvas} is backed by a {@link Bitmap} with the sizes equals {@link #getSize()} and
     * the format equals {@link Bitmap.Config#ARGB_8888}. To draw object in camera sensor
     * coordinates, apply {@link #getSensorToBufferTransform()} via
     * {@link Canvas#setMatrix(Matrix)} before drawing.
     *
     * <p>Only call this method if the caller needs to draw overlay on the frame. Calling this
     * method will upload the {@link Bitmap} to GPU for blending.
     */
    @NonNull
    public Canvas invalidateOverlayCanvas() {
        mIsOverlayDirty = true;
        return getOverlayCanvas();
    }

    /**
     * Internal API to check whether the overlay canvas is dirty.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isOverlayDirty() {
        return mIsOverlayDirty;
    }


    /**
     * Internal API to get the overlay canvas without invalidating it.
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract Canvas getOverlayCanvas();
}

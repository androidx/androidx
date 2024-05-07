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

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Handler;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ProcessingException;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.UseCase;
import androidx.camera.effects.internal.SurfaceProcessorImpl;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A {@link CameraEffect} for drawing overlay on top of the camera frames.
 *
 * <p>This class manages and processes camera frames with OpenGL. Upon arrival, frames are
 * enqueued into an array of GL textures for deferred rendering. Calling
 * {@link #drawFrameAsync(long)} dequeues frames and renders them to the output. Additionally, when
 * the texture queue reaches its capacity, the oldest frame is automatically dequeued and
 * rendered. The size of the texture queue can be defined in the constructor.
 *
 * <p>The queuing mechanism provides the flexibility to postpone frame rendering until analysis
 * results are available. For instance, to highlight on a QR code in a preview, one can apply a
 * QR code detection algorithm using {@link ImageAnalysis}. Once the frame's analysis result
 * is ready, invoke {@link #drawFrameAsync(long)} and pass in the
 * {@link ImageInfo#getTimestamp()} to release the frame and draw overlay. If the app
 * doesn't render real-time analysis results, set the queue depth to 0 to avoid unnecessary
 * buffer copies. For example, when laying over a static watermark.
 *
 * <p>Prior to rendering a frame, the {@link OverlayEffect} invokes the listener set in
 * {@link #setOnDrawListener(Function)}. This listener provides a {@link Frame} object, which
 * contains both a {@link Canvas} object for drawing the overlay and the metadata like crop rect,
 * rotation degrees, etc to calculate how the overlay should be positioned. Once the listener
 * returns, {@link OverlayEffect} updates the {@link SurfaceTexture} behind the {@link Canvas}
 * and blends it with the camera frame before rendering to the output.
 *
 * <p>This class is thread-safe. The methods can be invoked on any thread. The app provides a
 * {@link Handler} object in the constructor which is used for listening for Surface updates,
 * performing OpenGL operations and invoking app provided listeners.
 */
public class OverlayEffect extends CameraEffect implements AutoCloseable {

    /**
     * {@link #drawFrameAsync(long)} result code.
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            RESULT_SUCCESS,
            RESULT_FRAME_NOT_FOUND,
            RESULT_INVALID_SURFACE,
            RESULT_CANCELLED_BY_CALLER})
    public @interface DrawFrameResult {
    }

    /**
     * The {@link #drawFrameAsync(long)} call was successful. The frame with the exact timestamp was
     * drawn to the output surface.
     */
    public static final int RESULT_SUCCESS = 1;

    /**
     * The {@link #drawFrameAsync(long)} call failed because the frame with the exact timestamp was
     * not found in the queue. It could be one of the following reasons:
     *
     * <ul>
     * <li>the timestamp provided was incorrect, or
     * <li>the frame was removed because {@link #drawFrameAsync} had been called with a newer
     * timestamp, or
     * <li>the frame was removed due to the queue being full.
     * </ul>
     *
     * If it's the last case, the caller may avoid this issue by increasing the queue depth.
     */
    public static final int RESULT_FRAME_NOT_FOUND = 2;

    /**
     * The {@link #drawFrameAsync(long)} call failed because the output surface is missing, or the
     * output surface no longer matches the frame. It can happen when the output Surface is
     * replaced or disabled. For example, when the {@link UseCase} was unbound.
     */
    public static final int RESULT_INVALID_SURFACE = 3;

    /**
     * The {@link #drawFrameAsync(long)} call failed because the caller cancelled the drawing. This
     * happens when the listener in {@link #setOnDrawListener(Function)} returned false.
     */
    public static final int RESULT_CANCELLED_BY_CALLER = 4;

    /**
     * Creates an {@link OverlayEffect}.
     *
     * @param targets       The targets the effect applies to. For example,
     *                      {@link CameraEffect#PREVIEW} | {@link CameraEffect#VIDEO_CAPTURE}. See
     *                      {@link UseCaseGroup.Builder#addEffect} for supported targets
     *                      combinations.
     * @param queueDepth    The depth of the queue. This value indicates how many frames can be
     *                      queued before the oldest frame being automatically released.
     *                      {@link OverlayEffect} allocates an array of OpenGL 2D textures that
     *                      matches this size. The maximum value of the queueDepth depends on the
     *                      size of the image and the device capabilities. Set a larger value if
     *                      an ImageAnalysis processing takes a long time to produce a result to
     *                      be used for overlay, so the frame is not auto-released before the
     *                      result is ready. If the queue depth is 0, the input frames are
     *                      rendered immediately without queuing.
     * @param handler       The Handler for listening for the input Surface updates and for
     *                      performing OpenGL operations.
     * @param errorListener invoked if the effect runs into unrecoverable errors. The
     *                      {@link Throwable} will be the error thrown by this
     *                      {@link CameraEffect}. For example, {@link ProcessingException}.
     *                      This is invoked on the provided {@param Handler}.
     */
    public OverlayEffect(int targets, int queueDepth, @NonNull Handler handler,
            @NonNull Consumer<Throwable> errorListener) {
        this(targets, new SurfaceProcessorImpl(queueDepth, handler), errorListener);
    }

    private OverlayEffect(int targets, @NonNull SurfaceProcessorImpl surfaceProcessor,
            @NonNull Consumer<Throwable> errorListener) {
        this(targets, surfaceProcessor.getGlExecutor(), surfaceProcessor,
                errorListener);
    }

    private OverlayEffect(int targets, @NonNull Executor executor,
            @NonNull SurfaceProcessor surfaceProcessor,
            @NonNull Consumer<Throwable> errorListener) {
        super(targets, executor, surfaceProcessor, errorListener);
    }

    /**
     * Draws the queued frame with the given timestamp.
     *
     * <p>Once invoked, {@link OverlayEffect} retrieves the queued frame with the given timestamp
     * and draws it to the output Surface. If the frame is successfully drawn,
     * {@link ListenableFuture} completes with {@link #RESULT_SUCCESS}. Otherwise, it completes
     * with one of the following results: {@link #RESULT_FRAME_NOT_FOUND},
     * {@link #RESULT_INVALID_SURFACE} or {@link #RESULT_CANCELLED_BY_CALLER}. If this method is
     * called after the {@link OverlayEffect} is released, the {@link ListenableFuture} completes
     * with an {@link IllegalStateException}.
     *
     * <p>This method is thread safe. When calling from the {@link #getHandler()} thread, it's
     * executed right away; otherwise, it posts the execution on the {@link #getHandler()}. It's
     * recommended to call this method from the {@link #getHandler()} thread to avoid thread
     * hopping.
     */
    @NonNull
    public ListenableFuture<Integer> drawFrameAsync(long timestampNs) {
        return getSurfaceProcessorImpl().drawFrameAsync(timestampNs);
    }

    /**
     * Sets the listener for drawing overlay.
     *
     * <p>Each time before {@link OverlayEffect} draws a frame to the output, the listener
     * receives a {@link Frame} object, which contains the necessary APIs for drawing overlay.
     *
     * <p>To draw an overlay, first call {@link Frame#getOverlayCanvas()} ()} to get a
     * {@link Canvas} object. The {@link Canvas} object is backed by a {@link SurfaceTexture}
     * with the size of {@link Frame#getSize()}. {@link Frame#getSensorToBufferTransform()}
     * represents the mapping from camera sensor coordinates to the frame's coordinates. To draw
     * objects in the sensor coordinates, call {@link Canvas#setMatrix(Matrix)} with the value of
     * {@link Frame#getSensorToBufferTransform()}.
     *
     * <p>Once the drawing is done, the listener should return true for the {@link OverlayEffect}
     * to draw it to the output Surface. If it returns false, the frame will be dropped.
     *
     * <p>{@link OverlayEffect} invokes the listener on the {@link #getHandler()} thread.
     *
     * @see Frame
     */
    public void setOnDrawListener(@NonNull Function<Frame, Boolean> onDrawListener) {
        getSurfaceProcessorImpl().setOnDrawListener(onDrawListener);
    }

    /**
     * Clears the listener set in {@link #setOnDrawListener(Function)}.
     */
    public void clearOnDrawListener() {
        getSurfaceProcessorImpl().setOnDrawListener(null);
    }

    /**
     * Closes the {@link OverlayEffect}.
     *
     * <p>Once closed, the {@link OverlayEffect} can no longer be used.
     */
    @Override
    public void close() {
        getSurfaceProcessorImpl().release();
    }

    /**
     * Gets the depth of the queue set in the constructor.
     */
    public int getQueueDepth() {
        return getSurfaceProcessorImpl().getQueueDepth();
    }

    /**
     * Gets the {@link Handler} set in the constructor.
     */
    @NonNull
    public Handler getHandler() {
        return getSurfaceProcessorImpl().getGlHandler();
    }

    @NonNull
    private SurfaceProcessorImpl getSurfaceProcessorImpl() {
        return (SurfaceProcessorImpl) Objects.requireNonNull(getSurfaceProcessor());
    }
}

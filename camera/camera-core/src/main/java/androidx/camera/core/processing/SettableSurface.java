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

package androidx.camera.core.processing;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link DeferrableSurface} with another {@link DeferrableSurface} as its source.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SettableSurface extends DeferrableSurface {

    private final ListenableFuture<Surface> mSurfaceFuture;

    CallbackToFutureAdapter.Completer<Surface> mCompleter;

    private final Matrix mSensorToBufferTransform;
    private final boolean mHasEmbeddedTransform;
    private final Rect mCropRect;
    private final int mRotationDegrees;
    private final boolean mMirroring;
    private final int mTargets;

    private final AtomicBoolean mHasSource;

    /**
     * Please see the getters to understand the parameters.
     */
    public SettableSurface(
            int targets,
            @NonNull Size size,
            int format,
            @NonNull Matrix sensorToBufferTransform,
            boolean hasEmbeddedTransform,
            @NonNull Rect cropRect,
            int rotationDegrees,
            boolean mirroring) {
        super(size, format);
        mTargets = targets;
        mSensorToBufferTransform = sensorToBufferTransform;
        mHasEmbeddedTransform = hasEmbeddedTransform;
        mCropRect = cropRect;
        mRotationDegrees = rotationDegrees;
        mMirroring = mirroring;
        mHasSource = new AtomicBoolean(false);
        mSurfaceFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCompleter = completer;
                    return null;
                });
    }

    @NonNull
    @Override
    protected ListenableFuture<Surface> provideSurface() {
        return mSurfaceFuture;
    }

    /**
     * Sets the internal source of this {@link SettableSurface}.
     *
     * <p>This method is used to set a {@link DeferrableSurface} as the source of this
     * {@link SettableSurface}. This is for organizing the pipeline internally. For example, using
     * the output of one {@link UseCase} as the input of another {@link UseCase} for stream sharing.
     *
     * <p>It's also useful to link a new buffer request to an existing buffer, when the pipeline
     * is rebuilt partially. Example:
     *
     * <pre><code>
     * class NodeImplementation implements Node<SurfaceIn, SurfaceOut> {
     *   SurfaceOut apply(SurfaceIn surfaceIn) {
     *       if (canInputBeHandledByTheCurrentBuffer(surfaceIn)) {
     *           surfaceIn.getSurface().setSource(currentBuffer.getDeferrableSurface());
     *           // TODO(b/234174360): need to "unset" the existing link.
     *           return null;
     *       }
     *   }
     * }
     * </code></pre>
     *
     * <p> It throws {@link IllegalStateException} if the current {@link SettableSurface}
     * already has a source.
     */
    public void setSource(@NonNull DeferrableSurface source) {
        // TODO(b/234174360): propagate the #close() call to the source. Usually if the current
        //  DeferrableSurface is closed, the downstream one has to be closed as well. However, we
        //  should be able to "unset" the Surface too. e.g. when the pipeline is partially rebuilt
        //  during front/back camera switch VideoCapture, we don't want to propagate the close()
        //  call to the recording Surface.
        if (mHasSource.compareAndSet(false, true)) {
            Futures.propagate(source.getSurface(), mCompleter);
        } else {
            throw new IllegalStateException("The source has already been set.");
        }
    }

    /**
     * Creates a {@link SurfaceRequest} as the external source of this {@link SettableSurface}.
     *
     * <p>This method is used to request a {@link Surface} from an external source such as
     * {@code PreviewView}. The {@link Surface} provided via
     * {@link SurfaceRequest#provideSurface} will be used as the source of this
     * {@link SettableSurface}.
     *
     * <pre><code>
     * SurfaceOut surfaceOut = finalNode.apply(surfaceIn);
     * SurfaceRequest surfaceRequest = surfaceOut.getSurfaces().get(0)
     *     .createSurfaceRequestAsSource(camera, false);
     * mSurfaceProviderExecutor.execute(
     *     () -> surfaceProvider.onSurfaceRequested(surfaceRequest));
     * </code></pre>
     *
     * <p>It throws {@link IllegalStateException} if the current {@link SettableSurface}
     * already has a source.
     */
    @NonNull
    public SurfaceRequest createSurfaceRequestAsSource(@NonNull CameraInternal cameraInternal,
            boolean isRGBA8888Required) {
        SurfaceRequest surfaceRequest = new SurfaceRequest(getSize(), cameraInternal,
                isRGBA8888Required);
        setSource(surfaceRequest.getDeferrableSurface());
        return surfaceRequest;
    }

    /**
     * This field indicates that what purpose the {@link Surface} will be used for.
     */
    public int getTargets() {
        return mTargets;
    }

    /**
     * The allocated size of the {@link Surface}.
     */
    @NonNull
    public Size getSize() {
        return getPrescribedSize();
    }

    /**
     * The format of the {@link Surface}.
     */
    public int getFormat() {
        return getPrescribedStreamFormat();
    }

    /**
     * Gets the {@link Matrix} represents the transformation from camera sensor to the current
     * {@link Surface}.
     *
     * <p>This value represents the transformation from sensor coordinates to the current buffer
     * coordinates, which is required to transform coordinates between UseCases. For example, in
     * AR, transforming the coordinates of the detected face in ImageAnalysis to coordinates in
     * PreviewView.
     *
     * <p> If the {@link SettableSurface} is directly connected to a camera output and its
     * aspect ratio matches the aspect ratio of the sensor, this value is usually an identity
     * matrix, with the exception of device quirks. Each time a intermediate {@link Node}
     * transforms the image buffer, it has to append the same transformation to this
     * {@link Matrix} and pass it to the downstream {@link Node}.
     */
    @NonNull
    public Matrix getSensorToBufferTransform() {
        return mSensorToBufferTransform;
    }

    /**
     * Whether the current {@link Surface} has transformation info embedded.
     *
     * <p> Camera embeds transformation info into the {@link Surface}. The info is typically used by
     * {@link SurfaceView}/{@link TextureView} to correct the preview transformation. After the
     * buffer copy, the info is lost. The app (e.g. PreviewView) needs to handle the
     * transformation differently based on this flag.
     */
    public boolean hasEmbeddedTransform() {
        return mHasEmbeddedTransform;
    }

    // The following values represent the scenario that if this buffer is given directly to the
    // app, these are the additional transformation needs to be applied by the app. Every time we
    // make a change to the buffer, these values need to be updated as well.

    /**
     * Gets the crop rect based on {@link UseCase} config.
     */
    @NonNull
    public Rect getCropRect() {
        return mCropRect;
    }

    /**
     * Gets the clockwise rotation degrees based on {@link UseCase} config.
     */
    public int getRotationDegrees() {
        return mRotationDegrees;
    }

    /**
     * Gets whether the buffer needs to be horizontally mirrored based on {@link UseCase} config.
     */
    public boolean getMirroring() {
        return mMirroring;
    }
}

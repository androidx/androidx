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

import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ImageReader;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An extended {@link DeferrableSurface} that links to external sources.
 *
 * <p>This class is an extension of {@link DeferrableSurface} with additional info of the
 * surface such as crop rect and transformation. It also provides mechanisms to link to external
 * surface provider/consumer, and propagates Surface releasing/closure to linked provider/consumer.
 *
 * <p>An example of how {@link SettableSurface} connects an external surface provider to
 * an external surface consumer:
 * <pre>
 * {@code PreviewView}(surface provider) <--> {@link SurfaceRequest} <--> {@link SettableSurface}
 *     <--> {@link SurfaceOutput} --> {@link SurfaceEffect}(surface consumer)
 * </pre>
 *
 * <p>For the full workflow, please see {@code SettableSurfaceTest
 * #linkBothProviderAndConsumer_surfaceAndResultsArePropagatedE2E}
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
    @CameraEffect.Targets
    private final int mTargets;

    // Guarded by main thread.
    @Nullable
    private SurfaceOutputImpl mConsumerToNotify;
    // Guarded by main thread.
    private boolean mHasProvider = false;
    // Guarded by main thread.
    private boolean mHasConsumer = false;

    /**
     * Please see the getters to understand the parameters.
     */
    public SettableSurface(
            @CameraEffect.Targets int targets,
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
     * Sets a {@link ListenableFuture<Surface>} that provides the surface.
     *
     * <p>{@link SettableSurface} uses the surface provided by the provider. This method is for
     * organizing the pipeline internally, for example, using a {@link ImageReader} to provide
     * the surface.
     *
     * <p>It throws {@link IllegalStateException} if the current {@link SettableSurface}
     * already has a provider.
     */
    @MainThread
    public void setProvider(@NonNull ListenableFuture<Surface> surfaceFuture) {
        checkMainThread();
        Preconditions.checkState(!mHasProvider, "Provider can only be linked once.");
        mHasProvider = true;
        Futures.propagate(surfaceFuture, mCompleter);
    }

    /**
     * Sets the {@link DeferrableSurface} that provides the surface.
     *
     * <p> Once connected, the parent (this {@link SettableSurface}) and the provider should be
     * in sync on the following matters: 1) surface provision, 2) ref-counting, 3) closure and 4)
     * termination. See the list below for details:
     * <ul>
     * <li>Surface. the provider and the parent share the same Surface object.
     * <li>Ref-counting. The ref-count of the parent is managed by the surface consumer, which
     * indicates whether it's safe to release the surface. The ref-count of the provider
     * represents whether the parent is terminated. As long as the parent is not terminated, the
     * provider cannot release the surface because someone might be accessing the surface.
     * <li>Closure. When the parent is closed, if the surface is provided via {@link SurfaceOutput},
     * call {@link SurfaceOutputImpl#requestClose()} to decrease the ref-counter; if the
     * surface is used by the camera-camera2, wait for the ref-counter to go to zero on its own. For
     * the provider, closing after providing the surface has no effect; closing before
     * providing the surface propagates the exception upstream.
     * <li>Termination. On parent termination, close the provider and decrease the ref-count to
     * notify that the Surface can be safely released. The provider cannot be terminated before the
     * parent does.
     * </ul>
     *
     * <p>This method is for organizing the pipeline internally, for example, using the output of
     * one {@link UseCase} as the input of another {@link UseCase} for stream sharing.
     *
     * <p>It throws {@link IllegalStateException} if the current {@link SettableSurface}
     * already has a provider.
     *
     * @throws SurfaceClosedException when the provider is already closed. This should never
     *                                happen.
     */
    @MainThread
    public void setProvider(@NonNull DeferrableSurface provider) throws SurfaceClosedException {
        checkMainThread();
        setProvider(provider.getSurface());
        provider.incrementUseCount();
        getTerminationFuture().addListener(() -> {
            provider.decrementUseCount();
            provider.close();
        }, directExecutor());
    }

    /**
     * Creates a {@link SurfaceRequest} that is linked to this {@link SettableSurface}.
     *
     * <p>The {@link SurfaceRequest} is for requesting a {@link Surface} from an external source
     * such as {@code PreviewView} or {@code VideoCapture}. {@link SettableSurface} uses the
     * {@link Surface} provided by {@link SurfaceRequest#provideSurface} as its source. For how
     * the ref-counting works, please see the Javadoc of {@link #setProvider}.
     *
     * <p>It throws {@link IllegalStateException} if the current {@link SettableSurface}
     * already has a provider.
     */
    @MainThread
    @NonNull
    public SurfaceRequest createSurfaceRequest(@NonNull CameraInternal cameraInternal) {
        checkMainThread();
        // TODO(b/238230154) figure out how to support HDR.
        SurfaceRequest surfaceRequest = new SurfaceRequest(getSize(), cameraInternal, true);
        try {
            setProvider(surfaceRequest.getDeferrableSurface());
        } catch (SurfaceClosedException e) {
            // This should never happen. We just created the SurfaceRequest. It can't be closed.
            throw new AssertionError("Surface is somehow already closed", e);
        }
        return surfaceRequest;
    }

    /**
     * Creates a {@link SurfaceOutput} that is linked to this {@link SettableSurface}.
     *
     * <p>The {@link SurfaceOutput} is for providing a surface to an external target such
     * as {@link SurfaceEffect}.
     *
     * <p>This method returns a {@link ListenableFuture} that completes when the
     * {@link SettableSurface#getSurface()} completes. The {@link SurfaceOutput} contains the
     * surface and ref-counts the {@link SettableSurface}.
     *
     * <p>Do not provide the {@link SurfaceOutput} to external target if the
     * {@link ListenableFuture} fails.
     */
    @MainThread
    @NonNull
    public ListenableFuture<SurfaceOutput> createSurfaceOutputFuture(
            @NonNull float[] glTransformation) {
        checkMainThread();
        Preconditions.checkState(!mHasConsumer, "Consumer can only be linked once.");
        mHasConsumer = true;
        return Futures.transformAsync(getSurface(),
                surface -> {
                    Preconditions.checkNotNull(surface);
                    try {
                        incrementUseCount();
                    } catch (SurfaceClosedException e) {
                        return Futures.immediateFailedFuture(e);
                    }
                    SurfaceOutputImpl surfaceOutputImpl = new SurfaceOutputImpl(
                            surface, getTargets(), getFormat(), getSize(), glTransformation);
                    surfaceOutputImpl.getCloseFuture().addListener(this::decrementUseCount,
                            directExecutor());
                    mConsumerToNotify = surfaceOutputImpl;
                    return Futures.immediateFuture(surfaceOutputImpl);
                }, mainThreadExecutor());
    }

    /**
     * Closes the {@link DeferrableSurface} and notifies linked objects for the closure.
     */
    @MainThread
    @Override
    public final void close() {
        checkMainThread();
        super.close();
        if (mConsumerToNotify != null) {
            mConsumerToNotify.requestClose();
            mConsumerToNotify = null;
        }
    }

    /**
     * This field indicates that what purpose the {@link Surface} will be used for.
     */
    @CameraEffect.Targets
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

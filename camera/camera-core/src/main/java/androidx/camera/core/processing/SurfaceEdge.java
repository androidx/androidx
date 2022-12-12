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

import static androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture;
import static androidx.camera.core.impl.utils.futures.Futures.immediateFuture;
import static androidx.camera.core.impl.utils.futures.Futures.transformAsync;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.SurfaceRequest.TransformationInfo;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashSet;
import java.util.Set;

/**
 * An edge between two {@link Node} that is based on a {@link DeferrableSurface}.
 *
 * <p>This class contains a single {@link DeferrableSurface} with additional info such as size,
 * crop rect and transformation. It also connects the downstream {@link DeferrableSurface} or
 * {@link SurfaceRequest} that provides the {@link Surface}.
 *
 * <p>To set up a connection, configure both downstream/upstream nodes. Both downstream/upstream
 * nodes can only be configured once for each connection. Trying to configure them again throws
 * {@link IllegalStateException}.
 *
 * <p>To connect a downstream node(Surface provider):
 * <ul>
 * <li>For external source, call {@link #createSurfaceRequest} and send the
 * {@link SurfaceRequest} to the app. For example, sending the {@link SurfaceRequest} to
 * PreviewView or Recorder.
 * <li>For internal source, call {@link #setProvider} with the {@link DeferrableSurface}
 * from another {@link UseCase}. For example, when sharing one stream to two use cases.
 * </ul>
 *
 * <p>To connect a upstream node(surface consumer):
 * <ul>
 * <li>For external source, call {@link #createSurfaceOutputFuture} and send the
 * {@link SurfaceOutput} to the app. For example, sending the {@link SurfaceOutput} to
 * {@link SurfaceProcessor}.
 * <li>For internal source, call {@link #getDeferrableSurface()} and set the
 * {@link DeferrableSurface} on {@link SessionConfig}.
 * </ul>
 *
 * <p>The connection ends when the {@link #close()} or {@link #invalidate()} is called.
 * The difference is that {@link #close()} only notifies the upstream pipeline that the
 * {@link Surface} should no longer be used, and {@link #invalidate()} cleans the current
 * connection so it can be connected again.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SurfaceEdge {

    private final Matrix mSensorToBufferTransform;
    private final boolean mHasCameraTransform;
    private final Rect mCropRect;
    private final boolean mMirroring;
    @CameraEffect.Targets
    private final int mTargets;
    private final Size mSize;
    // Guarded by main thread.
    private int mRotationDegrees;

    // Guarded by main thread.
    @Nullable
    private SurfaceOutputImpl mConsumerToNotify;
    // Guarded by main thread.
    private boolean mHasConsumer = false;

    // Guarded by main thread.
    @Nullable
    private SurfaceRequest mProviderSurfaceRequest;

    // Guarded by main thread.
    @NonNull
    private SettableSurface mSettableSurface;

    // Guarded by main thread.
    @NonNull
    private final Set<Runnable> mOnInvalidatedListeners = new HashSet<>();

    /**
     * Please see the getters to understand the parameters.
     */
    public SurfaceEdge(
            @CameraEffect.Targets int targets,
            @NonNull Size size,
            @NonNull Matrix sensorToBufferTransform,
            boolean hasCameraTransform,
            @NonNull Rect cropRect,
            int rotationDegrees,
            boolean mirroring) {
        mTargets = targets;
        mSize = size;
        mSensorToBufferTransform = sensorToBufferTransform;
        mHasCameraTransform = hasCameraTransform;
        mCropRect = cropRect;
        mRotationDegrees = rotationDegrees;
        mMirroring = mirroring;
        mSettableSurface = new SettableSurface(size);
    }

    /**
     * Adds a Runnable that gets invoked when the downstream pipeline is invalidated.
     *
     * <p>The added listeners are invoked when the downstream pipeline wants to replace the
     * previously provided {@link Surface}. For example, when {@link SurfaceRequest#invalidate()}
     * is called. When that happens, the edge should notify the upstream pipeline to get the new
     * Surface.
     */
    @MainThread
    public void addOnInvalidatedListener(@NonNull Runnable onInvalidated) {
        checkMainThread();
        mOnInvalidatedListeners.add(onInvalidated);
    }

    /**
     * Gets the {@link DeferrableSurface} for upstream nodes.
     *
     * <p>This method throws {@link IllegalStateException} if the current {@link SurfaceEdge}
     * already has a Surface consumer. To remove the current Surface consumer, call
     * {@link #invalidate()} to reset the connection.
     */
    @NonNull
    @MainThread
    public DeferrableSurface getDeferrableSurface() {
        checkMainThread();
        checkAndSetHasConsumer();
        return mSettableSurface;
    }

    /**
     * Sets the downstream {@link DeferrableSurface}.
     *
     * <p>Once connected, the value {@link #getDeferrableSurface()} and the provider will be
     * in sync on the following matters: 1) surface provision, 2) ref-counting, 3) closure and 4)
     * termination. See the list below for details:
     * <ul>
     * <li>Surface. the provider and the parent share the same Surface object.
     * <li>Ref-counting. The ref-count of the {@link #getDeferrableSurface()} represents whether
     * it's safe to release the Surface. The ref-count of the provider represents whether the
     * {@link #getDeferrableSurface()} is terminated. As long as the parent is not terminated, the
     * provider cannot release the surface because someone might be accessing the surface.
     * <li>Closure. When {@link #getDeferrableSurface()} is closed, if the surface is provided
     * via {@link SurfaceOutput}, it will invoke {@link SurfaceOutputImpl#requestClose()} to
     * decrease the ref-counter; if the surface is used by the camera-camera2, wait for the
     * ref-counter to go to zero on its own. For the provider, closing after providing the
     * surface has no effect; closing before providing the surface propagates the exception
     * upstream.
     * <li>Termination. On {@link #getDeferrableSurface()} termination, close the provider and
     * decrease the ref-count to notify that the Surface can be safely released. The provider
     * cannot be terminated before the {@link #getDeferrableSurface()} does.
     * </ul>
     *
     * <p>This method is for organizing the pipeline internally. For example, using the output of
     * one {@link UseCase} as the input of another {@link UseCase} for stream sharing.
     *
     * <p>This method is idempotent. Calling it with the same provider no-ops. Calling it with a
     * different provider throws {@link IllegalStateException}.
     *
     * @throws DeferrableSurface.SurfaceClosedException when the provider is already closed.
     */
    @MainThread
    public void setProvider(@NonNull DeferrableSurface provider)
            throws DeferrableSurface.SurfaceClosedException {
        checkMainThread();
        mSettableSurface.setProvider(provider);
    }

    /**
     * Creates a {@link SurfaceRequest} that is linked to this {@link SurfaceEdge}.
     *
     * <p>The {@link SurfaceRequest} is for requesting a {@link Surface} from an external source
     * such as {@code PreviewView} or {@code VideoCapture}. {@link SurfaceEdge} uses the
     * {@link Surface} provided by {@link SurfaceRequest#provideSurface} as its source. For how
     * the ref-counting works, please see the Javadoc of {@link #setProvider}.
     *
     * <p>It throws {@link IllegalStateException} if the current {@link SurfaceEdge}
     * already has a provider.
     */
    @MainThread
    @NonNull
    public SurfaceRequest createSurfaceRequest(@NonNull CameraInternal cameraInternal) {
        return createSurfaceRequest(cameraInternal, null);
    }

    /**
     * Creates a {@link SurfaceRequest} that is linked to this {@link SurfaceEdge}.
     *
     * <p>The {@link SurfaceRequest} is for requesting a {@link Surface} from an external source
     * such as {@code PreviewView} or {@code VideoCapture}. {@link SurfaceEdge} uses the
     * {@link Surface} provided by {@link SurfaceRequest#provideSurface} as its source. For how
     * the ref-counting works, please see the Javadoc of {@link #setProvider}.
     *
     * <p>It throws {@link IllegalStateException} if the current {@link SurfaceEdge}
     * already has a provider.
     *
     * <p>This overload optionally allows allows specifying the expected frame rate range in which
     * the surface should operate.
     */
    @MainThread
    @NonNull
    public SurfaceRequest createSurfaceRequest(@NonNull CameraInternal cameraInternal,
            @Nullable Range<Integer> expectedFpsRange) {
        checkMainThread();
        // TODO(b/238230154) figure out how to support HDR.
        SurfaceRequest surfaceRequest = new SurfaceRequest(getSize(), cameraInternal,
                expectedFpsRange, () -> mainThreadExecutor().execute(this::invalidate));
        try {
            DeferrableSurface deferrableSurface = surfaceRequest.getDeferrableSurface();
            if (mSettableSurface.setProvider(deferrableSurface)) {
                mSettableSurface.getTerminationFuture().addListener(deferrableSurface::close,
                        directExecutor());
            }
        } catch (DeferrableSurface.SurfaceClosedException e) {
            // This should never happen. We just created the SurfaceRequest. It can't be closed.
            throw new AssertionError("Surface is somehow already closed", e);
        } catch (RuntimeException e) {
            // This should never happen. It indicates a bug in CameraX code. Close the
            // SurfaceRequest just to be safe.
            surfaceRequest.willNotProvideSurface();
            throw e;
        }
        mProviderSurfaceRequest = surfaceRequest;
        notifyTransformationInfoUpdate();
        return surfaceRequest;
    }

    /**
     * Creates a {@link SurfaceOutput} that is linked to this {@link SurfaceEdge}.
     *
     * <p>The {@link SurfaceOutput} is for providing a surface to an external target such
     * as {@link SurfaceProcessor}.
     *
     * <p>This method returns a {@link ListenableFuture<SurfaceOutput>} that completes when the
     * {@link #getDeferrableSurface()} completes. The {@link SurfaceOutput} contains the surface
     * and ref-counts the {@link SurfaceEdge}.
     *
     * <p>Do not provide the {@link SurfaceOutput} to external target if the
     * {@link ListenableFuture} fails.
     *
     * <p>This method throws {@link IllegalStateException} if the current {@link SurfaceEdge}
     * already has a Surface consumer. To remove the current Surface consumer, call
     * {@link #invalidate()} to reset the connection.
     *
     * @param inputSize       resolution of input image buffer
     * @param cropRect        crop rect of input image buffer
     * @param rotationDegrees expected rotation to the input image buffer
     * @param mirroring       expected mirroring to the input image buffer
     */
    @MainThread
    @NonNull
    public ListenableFuture<SurfaceOutput> createSurfaceOutputFuture(@NonNull Size inputSize,
            @NonNull Rect cropRect, int rotationDegrees, boolean mirroring) {
        checkMainThread();
        checkAndSetHasConsumer();
        SettableSurface settableSurface = mSettableSurface;
        return transformAsync(mSettableSurface.getSurface(),
                surface -> {
                    checkNotNull(surface);
                    try {
                        settableSurface.incrementUseCount();
                    } catch (DeferrableSurface.SurfaceClosedException e) {
                        return immediateFailedFuture(e);
                    }
                    SurfaceOutputImpl surfaceOutputImpl = new SurfaceOutputImpl(surface,
                            getTargets(), getSize(), inputSize, cropRect, rotationDegrees,
                            mirroring);
                    surfaceOutputImpl.getCloseFuture().addListener(
                            settableSurface::decrementUseCount,
                            directExecutor());
                    mConsumerToNotify = surfaceOutputImpl;
                    return immediateFuture(surfaceOutputImpl);
                }, mainThreadExecutor());
    }

    /**
     * Closes the current connection and notifies that a new connection is ready.
     *
     * <p>Call this method to notify that the {@link Surface} previously provided via
     * {@link #createSurfaceRequest} or {@link #setProvider} should no longer be used. The
     * upstream pipeline should call {@link #getDeferrableSurface()} or
     * {@link #createSurfaceOutputFuture} to get the new {@link Surface}.
     *
     * <p>Only call this method when the surface provider is ready to provide a new {@link Surface}.
     * For example, when {@link SurfaceRequest#invalidate()} is invoked or when a downstream
     * {@link UseCase} resets.
     *
     * @see #close()
     */
    @MainThread
    public void invalidate() {
        checkMainThread();
        close();
        mHasConsumer = false;
        mSettableSurface = new SettableSurface(mSize);
        for (Runnable onInvalidated : mOnInvalidatedListeners) {
            onInvalidated.run();
        }
    }

    /**
     * Closes the current connection.
     *
     * <p>This method uses the mechanism in {@link DeferrableSurface} and/or
     * {@link SurfaceOutputImpl} to notify the upstream pipeline that the {@link Surface}
     * previously provided via {@link #createSurfaceRequest} or {@link #setProvider} should no
     * longer be used. The upstream pipeline will stops writing to the {@link Surface}, and the
     * downstream pipeline can choose to release the {@link Surface} once the writing stops.
     *
     * @see DeferrableSurface#close().
     * @see #invalidate()
     */
    @MainThread
    public final void close() {
        checkMainThread();
        mSettableSurface.close();
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
        return mSize;
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
     * <p> If the {@link SurfaceEdge} is directly connected to a camera output and its
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
     * Whether the current {@link Surface} contains the camera transformation info.
     *
     * <p>Camera2 writes the camera transform to the {@link Surface}. The info is typically used by
     * {@link SurfaceView}/{@link TextureView} to correct the preview. Once it's buffer copied by
     * post-processing, the info is lost. The app (e.g. PreviewView) needs to handle the
     * transformation differently based on this flag.
     */
    public boolean hasCameraTransform() {
        return mHasCameraTransform;
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
     * Sets the rotation degrees.
     *
     * <p>If the surface provider is created via {@link #createSurfaceRequest(CameraInternal)}, the
     * returned SurfaceRequest will receive the rotation update by
     * {@link SurfaceRequest.TransformationInfoListener}.
     */
    @MainThread
    public void setRotationDegrees(int rotationDegrees) {
        checkMainThread();
        if (mRotationDegrees == rotationDegrees) {
            return;
        }
        mRotationDegrees = rotationDegrees;
        notifyTransformationInfoUpdate();
    }

    @MainThread
    private void notifyTransformationInfoUpdate() {
        checkMainThread();
        if (mProviderSurfaceRequest != null) {
            mProviderSurfaceRequest.updateTransformationInfo(
                    TransformationInfo.of(mCropRect, mRotationDegrees, ROTATION_NOT_SPECIFIED,
                            hasCameraTransform()));
        }
    }

    /**
     * Check the edge only has one consumer defensively.
     */
    private void checkAndSetHasConsumer() {
        checkState(!mHasConsumer, "Consumer can only be linked once.");
        mHasConsumer = true;
    }

    /**
     * Gets whether the buffer needs to be horizontally mirrored based on {@link UseCase} config.
     */
    public boolean getMirroring() {
        return mMirroring;
    }

    /**
     * A {@link DeferrableSurface} that sets another {@link DeferrableSurface} as the source.
     *
     * <p>This class provides mechanisms to link an {@link DeferrableSurface}, and propagates
     * Surface releasing/closure to the {@link DeferrableSurface}.
     */
    static class SettableSurface extends DeferrableSurface {

        final ListenableFuture<Surface> mSurfaceFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCompleter = completer;
                    return "SettableFuture hashCode: " + hashCode();
                });

        CallbackToFutureAdapter.Completer<Surface> mCompleter;

        private DeferrableSurface mProvider;

        SettableSurface(@NonNull Size size) {
            super(size, ImageFormat.PRIVATE);
        }

        @NonNull
        @Override
        protected ListenableFuture<Surface> provideSurface() {
            return mSurfaceFuture;
        }

        /**
         * Sets the {@link DeferrableSurface} that provides the surface.
         *
         * <p>This method is idempotent. Calling it with the same provider no-ops.
         *
         * @return true if the provider is set; false if the same provider has already been set.
         * @throws IllegalStateException    if the provider has already been set.
         * @throws IllegalArgumentException if the provider's size is different than the size of
         *                                  this {@link SettableSurface}.
         * @throws SurfaceClosedException   if the provider is already closed.
         * @see SurfaceEdge#setProvider(DeferrableSurface)
         */
        @MainThread
        public boolean setProvider(@NonNull DeferrableSurface provider)
                throws SurfaceClosedException {
            checkMainThread();
            checkNotNull(provider);
            if (mProvider == provider) {
                // Same provider has already been set. Ignore.
                return false;
            }
            checkState(mProvider == null, "A different provider has been set. To change the "
                    + "provider, call SurfaceEdge#invalidate before calling "
                    + "SurfaceEdge#setProvider");
            checkArgument(getPrescribedSize().equals(provider.getPrescribedSize()),
                    "The provider's size must match the parent");
            checkState(!isClosed(), "The parent is closed. Call SurfaceEdge#invalidate() before "
                    + "setting a new provider.");
            mProvider = provider;
            Futures.propagate(provider.getSurface(), mCompleter);
            provider.incrementUseCount();
            getTerminationFuture().addListener(provider::decrementUseCount, directExecutor());
            return true;
        }
    }
}

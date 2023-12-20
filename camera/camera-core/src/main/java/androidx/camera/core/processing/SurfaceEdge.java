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
import static androidx.camera.core.impl.utils.Threads.runOnMain;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture;
import static androidx.camera.core.impl.utils.futures.Futures.immediateFuture;
import static androidx.camera.core.impl.utils.futures.Futures.transformAsync;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.SurfaceRequest.TransformationInfo;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.streamsharing.StreamSharing;
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

    private final int mFormat;
    private final Matrix mSensorToBufferTransform;
    private final boolean mHasCameraTransform;
    private final Rect mCropRect;
    private final boolean mMirroring;
    @CameraEffect.Targets
    private final int mTargets;
    private final StreamSpec mStreamSpec;

    // Guarded by main thread.
    @ImageOutputConfig.OptionalRotationValue
    private int mTargetRotation;

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

    // Guarded by main thread.
    // Tombstone flag indicates whether the edge has been closed. Once closed, the edge should
    // never be used again.
    private boolean mIsClosed = false;

    /**
     * Please see the getters to understand the parameters.
     */
    public SurfaceEdge(
            @CameraEffect.Targets int targets,
            @CameraEffect.Formats int format,
            @NonNull StreamSpec streamSpec,
            @NonNull Matrix sensorToBufferTransform,
            boolean hasCameraTransform,
            @NonNull Rect cropRect,
            int rotationDegrees,
            @ImageOutputConfig.OptionalRotationValue int targetRotation,
            boolean mirroring) {
        mTargets = targets;
        mFormat = format;
        mStreamSpec = streamSpec;
        mSensorToBufferTransform = sensorToBufferTransform;
        mHasCameraTransform = hasCameraTransform;
        mCropRect = cropRect;
        mRotationDegrees = rotationDegrees;
        mTargetRotation = targetRotation;
        mMirroring = mirroring;
        mSettableSurface = new SettableSurface(streamSpec.getResolution(), mFormat);
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
        checkNotClosed();
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
        checkNotClosed();
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
        checkNotClosed();
        mSettableSurface.setProvider(provider, this::disconnectWithoutCheckingClosed);
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
        checkMainThread();
        checkNotClosed();
        // TODO(b/238230154) figure out how to support HDR.
        SurfaceRequest surfaceRequest = new SurfaceRequest(
                mStreamSpec.getResolution(),
                cameraInternal,
                mStreamSpec.getDynamicRange(),
                mStreamSpec.getExpectedFrameRateRange(),
                () -> mainThreadExecutor().execute(() -> {
                    if (!mIsClosed) {
                        invalidate();
                    }
                }));
        try {
            DeferrableSurface deferrableSurface = surfaceRequest.getDeferrableSurface();
            if (mSettableSurface.setProvider(deferrableSurface,
                    this::disconnectWithoutCheckingClosed)) {
                // TODO(b/286817690): consider close the deferrableSurface directly when the
                //  SettableSurface is closed. The delay might cause issues on legacy devices.
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
            @CameraEffect.Formats int format, @NonNull Rect cropRect, int rotationDegrees,
            boolean mirroring, @Nullable CameraInternal cameraInternal) {
        checkMainThread();
        checkNotClosed();
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
                            getTargets(), format, mStreamSpec.getResolution(), inputSize, cropRect,
                            rotationDegrees, mirroring, cameraInternal, mSensorToBufferTransform);
                    surfaceOutputImpl.getCloseFuture().addListener(
                            settableSurface::decrementUseCount,
                            directExecutor());
                    mConsumerToNotify = surfaceOutputImpl;
                    return immediateFuture(surfaceOutputImpl);
                }, mainThreadExecutor());
    }

    /**
     * Resets connection and notifies that a new connection is ready.
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
        checkNotClosed();
        if (mSettableSurface.canSetProvider()) {
            // If the edge is still connectable, no-ops.
            return;
        }
        disconnectWithoutCheckingClosed();
        mHasConsumer = false;
        mSettableSurface = new SettableSurface(mStreamSpec.getResolution(), mFormat);
        for (Runnable onInvalidated : mOnInvalidatedListeners) {
            onInvalidated.run();
        }
    }

    /**
     * Closes the edge.
     *
     * <p> Disconnects the edge and sets a tombstone so it will never be used again. This method
     * is idempotent.
     *
     * @see #disconnect()
     */
    @MainThread
    public final void close() {
        checkMainThread();
        disconnectWithoutCheckingClosed();
        mIsClosed = true;
    }

    /**
     * Disconnects the edge.
     *
     * <p> Once disconnected, upstream should stop sending images to the edge, and downstream
     * should stop expecting images from the edge.
     *
     * <p> This method notifies the upstream via {@link SettableSurface#close()}/
     * {@link SurfaceOutputImpl}. By calling {@link SettableSurface#close()}, it also decrements the
     * ref-count on downstream Surfaces so they can be released.
     *
     * @see DeferrableSurface#close().
     * @see #invalidate()
     */
    @MainThread
    public final void disconnect() {
        checkMainThread();
        checkNotClosed();
        disconnectWithoutCheckingClosed();
    }

    private void disconnectWithoutCheckingClosed() {
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
     * Gets the buffer format of this edge.
     */
    @CameraEffect.Formats
    public int getFormat() {
        return mFormat;
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
     * @see #updateTransformation(int, int)
     */
    public void updateTransformation(int rotationDegrees) {
        updateTransformation(rotationDegrees, ROTATION_NOT_SPECIFIED);
    }

    /**
     * Updates the transformation info.
     *
     * <p>If the surface provider is created via {@link #createSurfaceRequest(CameraInternal)}, the
     * returned SurfaceRequest will receive the rotation update by
     * {@link SurfaceRequest.TransformationInfoListener}.
     *
     * @param rotationDegrees the suggested clockwise rotation degrees of the buffer.
     * @param targetRotation  the UseCase target rotation configured by the app. This value is
     *                        needed if the SurfaceProvider is a TextureView without GL processing.
     *                        TextureView will combine this value and the value in
     *                        {@link SurfaceTexture#getTransformMatrix} to correct the output.
     * @ TODO(b/284336967): allow setting the crop rect and propagate it to the SurfaceProcessor.
     */
    public void updateTransformation(
            int rotationDegrees,
            @ImageOutputConfig.OptionalRotationValue int targetRotation) {
        // This method is not limited to the main thread because UseCase#setTargetRotation calls
        // this method and can be called from a background thread.
        runOnMain(() -> {
            boolean isDirty = false;
            if (mRotationDegrees != rotationDegrees) {
                isDirty = true;
                mRotationDegrees = rotationDegrees;
            }
            if (mTargetRotation != targetRotation) {
                isDirty = true;
                mTargetRotation = targetRotation;
            }
            if (isDirty) {
                notifyTransformationInfoUpdate();
            }
        });
    }

    @MainThread
    private void notifyTransformationInfoUpdate() {
        checkMainThread();
        if (mProviderSurfaceRequest != null) {
            mProviderSurfaceRequest.updateTransformationInfo(TransformationInfo.of(
                    mCropRect, mRotationDegrees, mTargetRotation, hasCameraTransform(),
                    mSensorToBufferTransform, mMirroring));
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
     * Returns {@link StreamSpec} associated with this edge.
     */
    @NonNull
    public StreamSpec getStreamSpec() {
        return mStreamSpec;
    }

    private void checkNotClosed() {
        checkState(!mIsClosed, "Edge is already closed.");
    }

    @VisibleForTesting
    @NonNull
    public DeferrableSurface getDeferrableSurfaceForTesting() {
        return mSettableSurface;
    }

    @VisibleForTesting
    public boolean isClosed() {
        return mIsClosed;
    }

    /**
     * @return true if this edge is connected to a Surface provider.
     */
    @VisibleForTesting
    public boolean hasProvider() {
        return mSettableSurface.hasProvider();
    }

    /**
     * A {@link DeferrableSurface} that sets another {@link DeferrableSurface} as the source.
     *
     * <p>This class provides mechanisms to link an {@link DeferrableSurface}, and propagates
     * Surface releasing/closure to the {@link DeferrableSurface}.
     *
     * <p>Closing the parent {@link SettableSurface} does not close the linked
     * {@link DeferrableSurface}. This is by design. The lifecycle of the child
     * {@link DeferrableSurface} will be managed by the owner of the child. For example, the
     * parent could be {@link StreamSharing} and the child could be a {@link Preview}.
     */
    static class SettableSurface extends DeferrableSurface {

        final ListenableFuture<Surface> mSurfaceFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCompleter = completer;
                    return "SettableFuture hashCode: " + hashCode();
                });

        CallbackToFutureAdapter.Completer<Surface> mCompleter;

        private DeferrableSurface mProvider;

        SettableSurface(@NonNull Size size, @CameraEffect.Formats int format) {
            super(size, format);
        }

        @NonNull
        @Override
        protected ListenableFuture<Surface> provideSurface() {
            return mSurfaceFuture;
        }

        @MainThread
        boolean canSetProvider() {
            checkMainThread();
            return mProvider == null && !isClosed();
        }

        @VisibleForTesting
        boolean hasProvider() {
            return mProvider != null;
        }

        /**
         * Sets the {@link DeferrableSurface} that provides the surface.
         *
         * <p>This method is idempotent. Calling it with the same provider no-ops.
         *
         * @param provider         the provider to link.
         * @param onProviderClosed a callback to be invoked when the provider is closed. The
         *                         callback will be invoked on the main thread.
         * @return true if the provider is set; false if the same provider has already been set.
         * @throws IllegalStateException    if the provider has already been set.
         * @throws IllegalArgumentException if the provider's size is different than the size of
         *                                  this {@link SettableSurface}.
         * @throws SurfaceClosedException   if the provider is already closed.
         * @see SurfaceEdge#setProvider(DeferrableSurface)
         */
        @MainThread
        public boolean setProvider(@NonNull DeferrableSurface provider,
                @NonNull Runnable onProviderClosed)
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
                    String.format("The provider's size(%s) must match the parent(%s)",
                            getPrescribedSize(), provider.getPrescribedSize()));
            checkArgument(getPrescribedStreamFormat() == provider.getPrescribedStreamFormat(),
                    String.format("The provider's format(%s) must match the parent(%s)",
                            getPrescribedStreamFormat(), provider.getPrescribedStreamFormat()));
            checkState(!isClosed(), "The parent is closed. Call SurfaceEdge#invalidate() before "
                    + "setting a new provider.");
            mProvider = provider;
            Futures.propagate(provider.getSurface(), mCompleter);
            provider.incrementUseCount();
            getTerminationFuture().addListener(provider::decrementUseCount, directExecutor());
            // When the child is closed, close the parent too to stop rendering.
            provider.getCloseFuture().addListener(onProviderClosed, mainThreadExecutor());
            return true;
        }
    }
}

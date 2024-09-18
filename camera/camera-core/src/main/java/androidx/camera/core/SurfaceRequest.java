/*
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Range;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A completable, single-use request of a {@link Surface}.
 *
 * <p>Contains requirements for surface characteristics along with methods for completing the
 * request and listening for request cancellation.
 *
 * <p>Acts as a bridge between the surface provider and the surface requester. The diagram below
 * describes how it works:
 * <ol>
 * <li>The surface provider gives a reference to surface requester for providing {@link Surface}
 * (e.g. {@link Preview#setSurfaceProvider(Preview.SurfaceProvider)}).
 * <li>The surface requester uses the reference to send a {@code SurfaceRequest} to get a
 * {@link Surface} (e.g. {@link Preview.SurfaceProvider#onSurfaceRequested(SurfaceRequest)}).
 * <li>The surface provider can use {@link #provideSurface(Surface, Executor, Consumer)} to provide
 * a {@link Surface} or inform the surface requester no {@link Surface} will be provided with
 * {@link #willNotProvideSurface()}. If a {@link Surface} is provided, the connection between
 * surface provider and surface requester is established.
 * <li>If the connection is established, the surface requester can get the {@link Surface} through
 * {@link #getDeferrableSurface()} and start to send frame data.
 * <li>If for some reason the provided {@link Surface} is no longer valid (e.g. when the
 * SurfaceView destroys its surface due to page being slid out in ViewPager2), the surface
 * provider can use {@link #invalidate()} method to inform the surface requester and the
 * established connection will be closed.
 * <li>The surface requester will re-send a new {@code SurfaceRequest} to establish a new
 * connection.
 * </ol>
 *
 * <img src="/images/reference/androidx/camera/camera-core/surface_request_work_flow.svg"/>
 */
public final class SurfaceRequest {

    /**
     * A frame rate range with no specified lower or upper bound.
     *
     * @see SurfaceRequest#getExpectedFrameRate()
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Range<Integer> FRAME_RATE_RANGE_UNSPECIFIED =
            StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;

    private final Object mLock = new Object();

    private final Size mResolution;

    @NonNull
    private final DynamicRange mDynamicRange;

    private final Range<Integer> mExpectedFrameRate;
    private final CameraInternal mCamera;
    private final boolean mIsPrimary;

    // For the camera to retrieve the surface from the user
    @SuppressWarnings("WeakerAccess") /*synthetic accessor */
    final ListenableFuture<Surface> mSurfaceFuture;
    private final CallbackToFutureAdapter.Completer<Surface> mSurfaceCompleter;

    // For the user to wait for the camera to be finished with the surface and retrieve errors
    // from the camera.
    private final ListenableFuture<Void> mSessionStatusFuture;

    // For notification of surface recreated.
    @NonNull
    private final CallbackToFutureAdapter.Completer<Void> mSurfaceRecreationCompleter;

    // For notification of surface request cancellation. Should only be used to register
    // cancellation listeners.
    private final CallbackToFutureAdapter.Completer<Void> mRequestCancellationCompleter;

    private final DeferrableSurface mInternalDeferrableSurface;

    @GuardedBy("mLock")
    @Nullable
    private TransformationInfo mTransformationInfo;
    @GuardedBy("mLock")
    @Nullable
    private TransformationInfoListener mTransformationInfoListener;
    // Executor for calling TransformationUpdateListener.
    @GuardedBy("mLock")
    @Nullable
    private Executor mTransformationInfoExecutor;

    /**
     * Creates a new surface request with the given resolution and {@link Camera}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SurfaceRequest(
            @NonNull Size resolution,
            @NonNull CameraInternal camera,
            @NonNull Runnable onInvalidated) {
        this(resolution, camera, DynamicRange.SDR, FRAME_RATE_RANGE_UNSPECIFIED, onInvalidated);
    }

    /**
     * Creates a new surface request with the given resolution, {@link Camera}, dynamic range, and
     * expected frame rate.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SurfaceRequest(
            @NonNull Size resolution,
            @NonNull CameraInternal camera,
            @NonNull DynamicRange dynamicRange,
            @NonNull Range<Integer> expectedFrameRate,
            @NonNull Runnable onInvalidated) {
        this(resolution, camera, true, dynamicRange,
                expectedFrameRate, onInvalidated);
    }

    /**
     * Creates a new surface request with the given resolution, {@link Camera}, dynamic range, and
     * expected frame rate and whether it's primary or secondary camera.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SurfaceRequest(
            @NonNull Size resolution,
            @NonNull CameraInternal camera,
            boolean isPrimary,
            @NonNull DynamicRange dynamicRange,
            @NonNull Range<Integer> expectedFrameRate,
            @NonNull Runnable onInvalidated) {
        super();
        mResolution = resolution;
        mCamera = camera;
        mIsPrimary = isPrimary;
        Preconditions.checkArgument(dynamicRange.isFullySpecified(),
                "SurfaceRequest's DynamicRange must always be fully specified.");
        mDynamicRange = dynamicRange;
        mExpectedFrameRate = expectedFrameRate;

        // To ensure concurrency and ordering, operations are chained. Completion can only be
        // triggered externally by the top-level completer (mSurfaceCompleter). The other future
        // completers are only completed by callbacks set up within the constructor of this class
        // to ensure correct ordering of events.

        // Cancellation listener must be called last to ensure the result can be retrieved from
        // the session listener.
        String surfaceRequestString =
                "SurfaceRequest[size: " + resolution + ", id: " + this.hashCode() + "]";
        AtomicReference<CallbackToFutureAdapter.Completer<Void>> cancellationCompleterRef =
                new AtomicReference<>(null);
        ListenableFuture<Void> requestCancellationFuture =
                CallbackToFutureAdapter.getFuture(completer -> {
                    cancellationCompleterRef.set(completer);
                    return surfaceRequestString + "-cancellation";
                });
        CallbackToFutureAdapter.Completer<Void> requestCancellationCompleter =
                Preconditions.checkNotNull(cancellationCompleterRef.get());
        mRequestCancellationCompleter = requestCancellationCompleter;

        // Surface session status future completes and is responsible for finishing the
        // cancellation listener.
        AtomicReference<CallbackToFutureAdapter.Completer<Void>> sessionStatusCompleterRef =
                new AtomicReference<>(null);
        mSessionStatusFuture = CallbackToFutureAdapter.getFuture(completer -> {
            sessionStatusCompleterRef.set(completer);
            return surfaceRequestString + "-status";
        });

        Futures.addCallback(mSessionStatusFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                // Cancellation didn't occur, so complete the cancellation future. There
                // shouldn't ever be any standard listeners on this future, so nothing should be
                // invoked.
                Preconditions.checkState(requestCancellationCompleter.set(null));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (t instanceof RequestCancelledException) {
                    // Cancellation occurred. Notify listeners.
                    Preconditions.checkState(requestCancellationFuture.cancel(false));
                } else {
                    // Cancellation didn't occur, complete the future so cancellation listeners
                    // are not invoked.
                    Preconditions.checkState(requestCancellationCompleter.set(null));
                }
            }
        }, CameraXExecutors.directExecutor());

        // Create the surface future/completer. This will be used to complete the rest of the
        // future chain and can be set externally via SurfaceRequest methods.
        CallbackToFutureAdapter.Completer<Void> sessionStatusCompleter =
                Preconditions.checkNotNull(sessionStatusCompleterRef.get());
        AtomicReference<CallbackToFutureAdapter.Completer<Surface>> surfaceCompleterRef =
                new AtomicReference<>(null);
        mSurfaceFuture = CallbackToFutureAdapter.getFuture(completer -> {
            surfaceCompleterRef.set(completer);
            return surfaceRequestString + "-Surface";
        });
        mSurfaceCompleter = Preconditions.checkNotNull(surfaceCompleterRef.get());

        // Create the deferrable surface which will be used for communicating when the
        // camera and consumer are done using the surface. Note this anonymous inner class holds
        // an implicit reference to the SurfaceRequest. This is by design, and ensures the
        // SurfaceRequest and all contained future completers will not be garbage collected as
        // long as the DeferrableSurface is referenced externally (via getDeferrableSurface()).
        mInternalDeferrableSurface = new DeferrableSurface(resolution,
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
            @NonNull
            @Override
            protected ListenableFuture<Surface> provideSurface() {
                return mSurfaceFuture;
            }
        };
        ListenableFuture<Void> terminationFuture =
                mInternalDeferrableSurface.getTerminationFuture();

        // Propagate surface completion to the session future.
        Futures.addCallback(mSurfaceFuture, new FutureCallback<Surface>() {
            @Override
            public void onSuccess(@Nullable Surface result) {
                // On successful setting of a surface, defer completion of the session future to
                // the DeferrableSurface termination future. Once that future completes, then it
                // is safe to release the Surface and associated resources.
                Futures.propagate(terminationFuture, sessionStatusCompleter);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                // Translate cancellation into a SurfaceRequestCancelledException. Other
                // exceptions mean either the request was completed via willNotProvideSurface() or a
                // programming error occurred. In either case, the user will never see the
                // session future (an immediate future will be returned instead), so complete the
                // future so cancellation listeners are never called.
                if (t instanceof CancellationException) {
                    Preconditions.checkState(sessionStatusCompleter.setException(
                            new RequestCancelledException(
                                    surfaceRequestString + " cancelled.", t)));
                } else {
                    sessionStatusCompleter.set(null);
                }
            }
        }, CameraXExecutors.directExecutor());

        // If the deferrable surface is terminated, there are two cases:
        // 1. The surface has not yet been provided to the camera (or marked as 'will not
        //    complete'). Treat this as if the surface request has been cancelled.
        // 2. The surface was already provided to the camera. In this case the camera is now
        //    finished with the surface, so cancelling the surface future below will be a no-op.
        terminationFuture.addListener(() -> mSurfaceFuture.cancel(true),
                CameraXExecutors.directExecutor());

        mSurfaceRecreationCompleter = initialSurfaceRecreationCompleter(
                CameraXExecutors.directExecutor(), onInvalidated);
    }

    /**
     * Returns the {@link DeferrableSurface} instance used to track usage of the surface that
     * fulfills this request.
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public DeferrableSurface getDeferrableSurface() {
        return mInternalDeferrableSurface;
    }

    /**
     * Returns whether this surface request has been serviced.
     *
     * <p>A surface request is considered serviced if
     * {@link #provideSurface(Surface, Executor, Consumer)} or {@link #willNotProvideSurface()}
     * has been called.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isServiced() {
        return mSurfaceFuture.isDone();
    }

    /**
     * Returns the resolution of the requested {@link Surface}.
     *
     * <p>The surface which fulfills this request must have the resolution specified here in
     * order to fulfill the resource requirements of the camera. Fulfillment of the request
     * with a surface of a different resolution may cause the {@code resultListener} passed to
     * {@link #provideSurface(Surface, Executor, Consumer)} to be invoked with a {@link Result}
     * containing {@link Result#RESULT_INVALID_SURFACE}.
     *
     * @return The guaranteed supported resolution.
     * @see SurfaceTexture#setDefaultBufferSize(int, int)
     */
    @NonNull
    public Size getResolution() {
        return mResolution;
    }

    /**
     * Returns the dynamic range expected to be used with the requested surface.
     *
     * <p>The dynamic range may have implications for which surface type is returned. Special
     * care should be taken to ensure the provided surface can support the requested dynamic
     * range. For example, if the returned dynamic range has {@link DynamicRange#getBitDepth()}
     * equal to {@link DynamicRange#BIT_DEPTH_10_BIT}, then the surface provided to
     * {@link #provideSurface(Surface, Executor, Consumer)} should use an
     * {@link android.graphics.ImageFormat} that can support ten bits of dynamic range, such as
     * {@link android.graphics.ImageFormat#PRIVATE} or
     * {@link android.graphics.ImageFormat#YCBCR_P010}.
     *
     * <p>The dynamic range returned here will always be fully specified. That is, it will never
     * have an {@link DynamicRange#getEncoding() encoding} of
     * {@link DynamicRange#ENCODING_UNSPECIFIED} or {@link DynamicRange#ENCODING_HDR_UNSPECIFIED}
     * and will never have {@link DynamicRange#getBitDepth() bit depth} of
     * {@link DynamicRange#BIT_DEPTH_UNSPECIFIED}.
     */
    @NonNull
    public DynamicRange getDynamicRange() {
        return mDynamicRange;
    }

    /**
     * Returns the expected rate at which frames will be produced into the provided {@link Surface}.
     *
     * <p>This information can be used to configure components that can be optimized by knowing
     * the frame rate. For example, {@link android.media.MediaCodec} can be configured with the
     * correct bitrate calculated from the frame rate.
     *
     * <p>The range may represent a variable frame rate, in which case {@link Range#getUpper()}
     * and {@link Range#getLower()} will be different values. This is commonly used by
     * auto-exposure algorithms to ensure a scene is exposed correctly in varying lighting
     * conditions. The frame rate may also be fixed, in which case {@link Range#getUpper()} will
     * be equivalent to {@link Range#getLower()}.
     *
     * <p>This method may also return {@link #FRAME_RATE_RANGE_UNSPECIFIED} if no information about
     * the frame rate can be determined. In this case, no assumptions should be made about what
     * the actual frame rate will be.
     *
     * @return The expected frame rate range or {@link #FRAME_RATE_RANGE_UNSPECIFIED} if no frame
     * rate information is available.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Range<Integer> getExpectedFrameRate() {
        return mExpectedFrameRate;
    }

    /**
     * Returns the {@link Camera} which is requesting a {@link Surface}.
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public CameraInternal getCamera() {
        return mCamera;
    }

    /**
     * Returns whether the {@link Camera} is primary or secondary in dual camera case.
     * The value is always true for single camera case.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isPrimary() {
        return mIsPrimary;
    }

    /**
     * Completes the request for a {@link Surface} if it has not already been
     * completed or cancelled.
     *
     * <p>Once the camera no longer needs the provided surface, the {@code resultListener} will be
     * invoked with a {@link Result} containing {@link Result#RESULT_SURFACE_USED_SUCCESSFULLY}.
     * At this point it is safe to release the surface and any underlying resources. Releasing
     * the surface before receiving this signal may cause undesired behavior on lower API levels.
     *
     * <p>If the request is cancelled by the camera before successfully attaching the
     * provided surface to the camera, then the {@code resultListener} will be invoked with a
     * {@link Result} containing {@link Result#RESULT_REQUEST_CANCELLED}. In addition, any
     * cancellation listeners provided to
     * {@link #addRequestCancellationListener(Executor, Runnable)} will be invoked.
     *
     * <p>If the request was previously completed via {@link #willNotProvideSurface()}, then
     * {@code resultListener} will be invoked with a {@link Result} containing
     * {@link Result#RESULT_WILL_NOT_PROVIDE_SURFACE}.
     *
     * <p>Upon returning from this method, the surface request is guaranteed to be complete.
     * However, only the {@code resultListener} provided to the first invocation of this method
     * should be used to track when the provided {@link Surface} is no longer in use by the
     * camera, as subsequent invocations will always invoke the {@code resultListener} with a
     * {@link Result} containing {@link Result#RESULT_SURFACE_ALREADY_PROVIDED}.
     *
     * @param surface        The surface which will complete the request.
     * @param executor       Executor used to execute the {@code resultListener}.
     * @param resultListener Listener used to track how the surface is used by the camera in
     *                       response to being provided by this method.
     */
    public void provideSurface(@NonNull Surface surface, @NonNull Executor executor,
            @NonNull Consumer<Result> resultListener) {
        if (mSurfaceCompleter.set(surface) || mSurfaceFuture.isCancelled()) {
            // Session will be pending completion (or surface request was cancelled). Return the
            // session future.
            Futures.addCallback(mSessionStatusFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    resultListener.accept(Result.of(Result.RESULT_SURFACE_USED_SUCCESSFULLY,
                            surface));
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Preconditions.checkState(t instanceof RequestCancelledException, "Camera "
                            + "surface session should only fail with request "
                            + "cancellation. Instead failed due to:\n" + t);
                    resultListener.accept(Result.of(Result.RESULT_REQUEST_CANCELLED, surface));
                }
            }, executor);
        } else {
            // Surface request is already complete
            Preconditions.checkState(mSurfaceFuture.isDone());
            try {
                mSurfaceFuture.get();
                // Getting this far means the surface was already provided.
                executor.execute(
                        () -> resultListener.accept(
                                Result.of(Result.RESULT_SURFACE_ALREADY_PROVIDED, surface)));
            } catch (InterruptedException | ExecutionException e) {
                executor.execute(
                        () -> resultListener.accept(
                                Result.of(Result.RESULT_WILL_NOT_PROVIDE_SURFACE, surface)));
            }

        }
    }

    /**
     * Signals that the request will never be fulfilled.
     *
     * <p>This may be called in the case where the application may be shutting down and a
     * surface will never be produced to fulfill the request.
     *
     * <p>This should always be called as soon as it is known that the request will not
     * be fulfilled. Failure to complete the SurfaceRequest via {@code willNotProvideSurface()}
     * or {@link #provideSurface(Surface, Executor, Consumer)} may cause long delays in shutting
     * down the camera.
     *
     * <p>Upon returning from this method, the request is guaranteed to be complete, regardless
     * of the return value. If the request was previously successfully completed by
     * {@link #provideSurface(Surface, Executor, Consumer)}, invoking this method will return
     * {@code false}, and will have no effect on how the surface is used by the camera.
     *
     * @return {@code true} if this call to {@code willNotProvideSurface()} successfully
     * completes the request, i.e., the request has not already been completed via
     * {@link #provideSurface(Surface, Executor, Consumer)} or by a previous call to
     * {@code willNotProvideSurface()} and has not already been cancelled by the camera.
     */
    public boolean willNotProvideSurface() {
        return mSurfaceCompleter.setException(
                new DeferrableSurface.SurfaceUnavailableException("Surface request "
                        + "will not complete."));
    }

    /**
     * Sets a {@link Runnable} that handles the situation where {@link Surface} is no longer valid
     * and triggers the process to request a new {@link Surface}.
     *
     * @param executor Executor used to execute the {@code runnable}.
     * @param runnable The code which will be run when {@link Surface} is no longer valid.
     */
    private CallbackToFutureAdapter.Completer<Void> initialSurfaceRecreationCompleter(
            @NonNull Executor executor, @NonNull Runnable runnable) {
        AtomicReference<CallbackToFutureAdapter.Completer<Void>> completerRef =
                new AtomicReference<>(null);
        final ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(completer -> {
            completerRef.set(completer);
            return "SurfaceRequest-surface-recreation(" + SurfaceRequest.this.hashCode() + ")";
        });

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                runnable.run();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                // Do nothing
            }
        }, executor);

        return Preconditions.checkNotNull(completerRef.get());
    }

    /**
     * Invalidates the previously provided {@link Surface} to provide a new {@link Surface}.
     *
     * <p>Call this method to inform the surface requester that the previously provided
     * {@link Surface} is no longer valid (e.g. when the SurfaceView destroys its surface due to
     * page being slid out in ViewPager2) and should re-send a {@link SurfaceRequest} to obtain a
     * new {@link Surface}.
     *
     * <p>Calling this method will cause the camera to be reconfigured. The app should call this
     * method when the surface provider is ready to provide a new {@link Surface}. (e.g. a
     * SurfaceView's surface is created when its window is visible.)
     *
     * <p>If the provided {@link Surface} was already invalidated, invoking this method will return
     * {@code false}, and will have no effect. The surface requester will not be notified again, so
     * there will not be another {@link SurfaceRequest}.
     *
     * <p>Calling this method without {@link #provideSurface(Surface, Executor, Consumer)}
     * (regardless of whether @link #willNotProvideSurface()} has been called) will still trigger
     * the surface requester to re-send a {@link SurfaceRequest}.
     *
     * <p>Since calling this method also means that the {@link SurfaceRequest} will not be
     * fulfilled, if the {@link SurfaceRequest} has not responded, it will respond as if calling
     * {@link #willNotProvideSurface()}.
     *
     * @return true if the provided {@link Surface} is invalidated or false if it was already
     * invalidated.
     */
    public boolean invalidate() {
        willNotProvideSurface();
        return mSurfaceRecreationCompleter.set(null);
    }

    /**
     * Adds a listener to be informed when the camera cancels the surface request.
     *
     * <p>A surface request may be cancelled by the camera if the surface is no longer required.
     * Examples of why cancellation may occur include (1) a {@link UseCase} being unbound from the
     * camera, (2) surface requirements, such as {@linkplain #getResolution() resolution}, changing
     * due to newly bound use cases, or (3) the camera requiring a new {@link Surface}
     * object on lower API levels to work around compatibility issues.
     *
     * <p>When a request is cancelled, the {@link Runnable Runnables} provided here will be
     * invoked on the {@link Executor} they are added with, and can be used as a signal to stop any
     * work that may be in progress to fulfill the surface request.
     *
     * <p>Once a surface request has been cancelled by the camera,
     * {@link #willNotProvideSurface()} will have no effect and will return {@code false}.
     * Attempting to complete the request via {@link #provideSurface(Surface, Executor, Consumer)}
     * will also have no effect, and any {@code resultListener} passed to
     * {@link #provideSurface(Surface, Executor, Consumer)} will be invoked with a {@link Result}
     * containing {@link Result#RESULT_REQUEST_CANCELLED}.
     *
     * <p>Note that due to the asynchronous nature of this listener, it is not guaranteed
     * that the listener will be called before an attempt to complete the request with
     * {@link #provideSurface(Surface, Executor, Consumer)} or {@link #willNotProvideSurface()}, so
     * the return values of these methods can always be checked if your workflow for producing a
     * surface expects them to complete successfully.
     *
     * @param executor The executor used to notify the listener.
     * @param listener The listener which will be run upon request cancellation.
     */
    // Since registered listeners will only be called once, and the lifetime of a SurfaceRequest
    // is relatively short, there is no need for a 'removeRequestCancellationListener()' method.
    // References to listeners are also automatically freed up upon completion of the request due
    // to the implementation of CallbackToFutureAdapter.Completer.
    @SuppressLint("PairedRegistration")
    public void addRequestCancellationListener(@NonNull Executor executor,
            @NonNull Runnable listener) {
        mRequestCancellationCompleter.addCancellationListener(listener, executor);
    }

    /**
     * Updates the {@link TransformationInfo} associated with this {@link SurfaceRequest}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void updateTransformationInfo(@NonNull TransformationInfo transformationInfo) {
        TransformationInfoListener listener;
        Executor executor;
        synchronized (mLock) {
            mTransformationInfo = transformationInfo;
            listener = mTransformationInfoListener;
            executor = mTransformationInfoExecutor;
        }
        if (listener != null && executor != null) {
            executor.execute(() -> listener.onTransformationInfoUpdate(transformationInfo));
        }
    }

    /**
     * Sets a listener to receive updates on transformation info.
     *
     * <p> Sets a listener to receive the transformation info associated with this
     * {@link SurfaceRequest} when it changes or becomes available. The listener is called
     * immediately if transformation info is available at the time of setting.
     *
     * @param executor The executor used to notify the listener.
     * @param listener the listener which will be called when transformation info changes.
     * @see TransformationInfoListener
     * @see TransformationInfo
     */
    public void setTransformationInfoListener(@NonNull Executor executor,
            @NonNull TransformationInfoListener listener) {
        TransformationInfo transformationInfo;
        synchronized (mLock) {
            mTransformationInfoListener = listener;
            mTransformationInfoExecutor = executor;
            transformationInfo = mTransformationInfo;
        }
        if (transformationInfo != null) {
            executor.execute(() -> listener.onTransformationInfoUpdate(transformationInfo));
        }
    }

    /**
     * Clears the {@link TransformationInfoListener} set via {@link #setTransformationInfoListener}.
     */
    public void clearTransformationInfoListener() {
        synchronized (mLock) {
            mTransformationInfoListener = null;
            mTransformationInfoExecutor = null;
        }
    }

    /**
     * An exception used to signal that the camera has cancelled a request for a {@link Surface}.
     *
     * <p>This may be set on the {@link ListenableFuture} returned by
     * {@link SurfaceRequest#provideSurface(Surface, Executor, Consumer) when the camera is
     * shutting down or when the surface resolution requirements have changed in order to
     * accommodate newly bound use cases, in which case the {@linkplain Runnable runnables}
     * provided to {@link #addRequestCancellationListener(Executor, Runnable)} will also be
     * invoked on their respective {@link Executor Executors}.
     */
    private static final class RequestCancelledException extends RuntimeException {
        RequestCancelledException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Listener that receives updates of the {@link TransformationInfo} associated with the
     * {@link SurfaceRequest}.
     */
    public interface TransformationInfoListener {

        /**
         * Called when the {@link TransformationInfo} is updated.
         *
         * <p> This is called when the transformation info becomes available or is updated.
         * The rotation degrees is updated after calling {@link Preview#setTargetRotation}, and the
         * crop rect is updated after changing the {@link ViewPort} associated with the
         * {@link Preview}.
         *
         * @param transformationInfo apply the transformation info to transform {@link Preview}
         * @see TransformationInfo
         * @see Preview#setTargetRotation(int)
         * @see Preview.Builder#setTargetRotation(int)
         * @see CameraCharacteristics#SENSOR_ORIENTATION
         * @see ViewPort
         */
        void onTransformationInfoUpdate(@NonNull TransformationInfo transformationInfo);
    }

    /**
     * Result of providing a surface to a {@link SurfaceRequest} via
     * {@link #provideSurface(Surface, Executor, Consumer)}.
     */
    @AutoValue
    public abstract static class Result {

        /**
         * Possible result codes.
         */
        @IntDef({RESULT_SURFACE_USED_SUCCESSFULLY, RESULT_REQUEST_CANCELLED, RESULT_INVALID_SURFACE,
                RESULT_SURFACE_ALREADY_PROVIDED, RESULT_WILL_NOT_PROVIDE_SURFACE})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public @interface ResultCode {
        }

        /**
         * Provided surface was successfully used by the camera and eventually detached once no
         * longer needed by the camera.
         *
         * <p>This result denotes that it is safe to release the {@link Surface} and any underlying
         * resources.
         *
         * <p>For compatibility reasons, the {@link Surface} object should not be reused by
         * future {@link SurfaceRequest SurfaceRequests}, and a new surface should be
         * created instead.
         */
        public static final int RESULT_SURFACE_USED_SUCCESSFULLY = 0;

        /**
         * Provided surface was never attached to the camera due to the {@link SurfaceRequest} being
         * cancelled by the camera.
         *
         * <p>It is safe to release or reuse {@link Surface}, assuming it was not previously
         * attached to a camera via {@link #provideSurface(Surface, Executor, Consumer)}. If
         * reusing the surface for a future surface request, it should be verified that the
         * surface still matches the resolution specified by {@link SurfaceRequest#getResolution()}.
         */
        public static final int RESULT_REQUEST_CANCELLED = 1;

        /**
         * Provided surface could not be used by the camera.
         *
         * <p>This is likely due to the {@link Surface} being closed prematurely or the resolution
         * of the surface not matching the resolution specified by
         * {@link SurfaceRequest#getResolution()}.
         */
        public static final int RESULT_INVALID_SURFACE = 2;

        /**
         * Surface was not attached to the camera through this invocation of
         * {@link #provideSurface(Surface, Executor, Consumer)} due to the {@link SurfaceRequest}
         * already being complete with a surface.
         *
         * <p>The {@link SurfaceRequest} has already been completed by a previous invocation
         * of {@link #provideSurface(Surface, Executor, Consumer)}.
         *
         * <p>It is safe to release or reuse the {@link Surface}, assuming it was not previously
         * attached to a camera via {@link #provideSurface(Surface, Executor, Consumer)}.
         */
        public static final int RESULT_SURFACE_ALREADY_PROVIDED = 3;

        /**
         * Surface was not attached to the camera through this invocation of
         * {@link #provideSurface(Surface, Executor, Consumer)} due to the {@link SurfaceRequest}
         * already being marked as "will not provide surface".
         *
         * <p>The {@link SurfaceRequest} has already been marked as 'will not provide surface' by a
         * previous invocation of {@link #willNotProvideSurface()}.
         *
         * <p>It is safe to release or reuse the {@link Surface}, assuming it was not previously
         * attached to a camera via {@link #provideSurface(Surface, Executor, Consumer)}.
         */
        public static final int RESULT_WILL_NOT_PROVIDE_SURFACE = 4;

        /**
         * Creates a result from the given result code and surface.
         *
         * <p>Can be used to compare to results returned to {@code resultListener} in
         * {@link #provideSurface(Surface, Executor, Consumer)}.
         *
         * @param code    One of {@link #RESULT_SURFACE_USED_SUCCESSFULLY},
         *                {@link #RESULT_REQUEST_CANCELLED}, {@link #RESULT_INVALID_SURFACE},
         *                {@link #RESULT_SURFACE_ALREADY_PROVIDED}, or
         *                {@link #RESULT_WILL_NOT_PROVIDE_SURFACE}.
         * @param surface The {@link Surface} used to complete the {@link SurfaceRequest}.
         */
        @NonNull
        static Result of(@ResultCode int code, @NonNull Surface surface) {
            return new AutoValue_SurfaceRequest_Result(code, surface);
        }

        /**
         * Returns the result of invoking {@link #provideSurface(Surface, Executor, Consumer)}
         * with the surface from {@link #getSurface()}.
         *
         * @return One of {@link #RESULT_SURFACE_USED_SUCCESSFULLY},
         * {@link #RESULT_REQUEST_CANCELLED}, {@link #RESULT_INVALID_SURFACE}, or
         * {@link #RESULT_SURFACE_ALREADY_PROVIDED}, {@link #RESULT_WILL_NOT_PROVIDE_SURFACE}.
         */
        @ResultCode
        public abstract int getResultCode();

        /**
         * The surface used to complete a {@link SurfaceRequest} with
         * {@link #provideSurface(Surface, Executor, Consumer)}.
         *
         * @return the surface.
         */
        @NonNull
        public abstract Surface getSurface();

        // Ensure Result can't be subclassed outside the package
        Result() {
        }
    }

    /**
     * Transformation associated the preview output.
     *
     * <p> The {@link TransformationInfo} can be used transform the {@link Surface} provided via
     * {@link SurfaceRequest#provideSurface}. The info is based on the camera sensor rotation,
     * preview target rotation and the {@link ViewPort} associated with the {@link Preview}. The
     * application of the info depends on the source of the {@link Surface}. For detailed example,
     * please check out the source code of PreviewView in androidx.camera.view artifact.
     *
     * <p> The info is also needed to transform coordinates across use cases. In a face detection
     * example, one common scenario is running a face detection algorithm against a
     * {@link ImageAnalysis} use case, and highlight the detected face in the preview. Below is
     * a code sample to get the transformation {@link Matrix} based on the {@link ImageProxy} from
     * {@link ImageAnalysis} and the {@link TransformationInfo} from {@link Preview}:
     *
     * <pre><code>
     *     // Get rotation transformation.
     *     val transformation = Matrix()
     *     transformation.setRotate(info.getRotationDegrees())
     *
     *     // Get rotated crop rect and cropping transformation.
     *     val rotatedRect = new RectF()
     *     rotation.mapRect(rotatedRect, RectF(imageProxy.getCropRect()))
     *     rotatedRect.sort()
     *     val cropTransformation = Matrix()
     *     cropTransformation.setRectToRect(
     *          RectF(imageProxy.getCropRect()), rotatedRect, ScaleToFit.FILL)
     *
     *     // Concatenate the rotation and cropping transformations.
     *     transformation.postConcat(cropTransformation)
     * </code></pre>
     *
     * @see Preview#setTargetRotation(int)
     * @see Preview.Builder#setTargetRotation(int)
     * @see CameraCharacteristics#SENSOR_ORIENTATION
     * @see ViewPort
     */
    @AutoValue
    public abstract static class TransformationInfo {

        /**
         * Returns the crop rect rectangle.
         *
         * <p> The returned value dictates how the {@link Surface} provided in
         * {@link SurfaceRequest#provideSurface} should be displayed. The crop
         * rectangle specifies the region of valid pixels in the buffer, using coordinates from (0,
         * 0) to the (width, height) of {@link SurfaceRequest#getResolution}. The caller should
         * arrange the UI so that only the valid region is visible to app users.
         *
         * <p> If {@link Preview} is configured with a {@link ViewPort}, this value is calculated
         * based on the configuration of {@link ViewPort}; if not, it returns the full rect of the
         * buffer. For code sample on how to apply the crop rect, please see {@link ViewPort#FIT}.
         *
         * @see ViewPort
         */
        @NonNull
        public abstract Rect getCropRect();

        /**
         * Returns the rotation needed to transform the output from sensor to the target
         * rotation.
         *
         * <p> This is a clockwise rotation in degrees that needs to be applied to the sensor
         * buffer. The rotation will be determined by {@link CameraCharacteristics},
         * {@link Preview#setTargetRotation(int)} and
         * {@link Preview.Builder#setTargetRotation(int)}. This value is useful for transforming
         * coordinates across use cases.
         *
         * <p> This value is most useful for transforming coordinates across use cases, e.g. in
         * preview, highlighting a pattern detected in image analysis. For correcting
         * the preview itself, usually the source of the {@link Surface} handles the rotation
         * without needing this value. For {@link SurfaceView}, it automatically corrects the
         * preview to match the display rotation. For {@link TextureView}, the only additional
         * rotation needed is the display rotation. For detailed example, please check out the
         * source code of PreviewView in androidx.camera .view artifact.
         *
         * @return The rotation in degrees which will be a value in {0, 90, 180, 270}.
         * @see Preview#setTargetRotation(int)
         * @see Preview#getTargetRotation()
         * @see ViewPort
         */
        @ImageOutputConfig.RotationDegreesValue
        public abstract int getRotationDegrees();

        /**
         * The {@linkplain Preview#getTargetRotation() target rotation} of the {@link Preview}.
         *
         * <p>Used to correct preview for {@link TextureView}.
         * {@link #getRotationDegrees()} is a function of 1)
         * {@link CameraCharacteristics#SENSOR_ORIENTATION}, 2) camera lens facing direction and 3)
         * target rotation. {@link TextureView} handles 1) & 2) automatically,
         * while still needs the target rotation to correct the display.This is used when apps
         * need to rotate the preview to non-display orientation.
         *
         * <p>The API is internal for PreviewView to use. For external users, the value
         * is usually {@link Display#getRotation()} in practice. If that's not the case, they can
         * always obtain the value from {@link Preview#getTargetRotation()}.
         *
         * <p>Please note that if the value is {@link ImageOutputConfig#ROTATION_NOT_SPECIFIED}
         * which means targetRotation is not specified for Preview, the user should always get
         * up-to-date display rotation and re-calculate the rotationDegrees to correct the display.
         *
         * @see CameraCharacteristics#SENSOR_ORIENTATION
         */
        @ImageOutputConfig.OptionalRotationValue
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public abstract int getTargetRotation();

        /**
         * Whether the {@link Surface} contains the camera transform.
         *
         * <p>When the Surface is connected to the camera directly, camera writes the
         * camera orientation value to the Surface. For example, the value can be retrieved via
         * {@link SurfaceTexture#getTransformMatrix(float[])}. Android components such
         * as {@link TextureView} and {@link SurfaceView} use the value to transform the output.
         * When the Surface is not connect to the camera directly, for example, when it was
         * copied with OpenGL, the Surface will not contain the camera orientation value.
         *
         * <p>The app may need to transform the UI differently based on this flag. If this value
         * is true, the app only needs to apply the Surface transformation; otherwise, the app
         * needs to apply the value of {@link #getRotationDegrees()}. For example, if the preview
         * is displayed in a {@link TextureView}:
         *
         * <pre><code>
         * int rotationDegrees;
         * if (surfaceRequest.hasCameraTransform()) {
         *   switch (textureView.getDisplay().getRotation()) {
         *     case Surface.ROTATION_0:
         *       rotationDegrees = 0;
         *       break;
         *     case Surface.ROTATION_90:
         *       rotationDegrees = 90;
         *       break;
         *     case Surface.ROTATION_180:
         *       rotationDegrees = 180;
         *       break;
         *     case Surface.ROTATION_270:
         *       rotationDegrees = 270;
         *       break;
         *     }
         * } else {
         *   rotationDegrees = transformationInfo.getRotationDegrees();
         * }
         * Matrix textureViewTransform = new Matrix();
         * textureViewTransform.postRotate(rotationDegrees);
         * textureView.setTransform(textureViewTransform);
         * </code></pre>
         *
         * @return true if the {@link Surface} contains the camera transformation.
         */
        public abstract boolean hasCameraTransform();

        /**
         * Returns the sensor to image buffer transform matrix.
         *
         * <p>The value is a mapping from sensor coordinates to buffer coordinates, which is,
         * from the rect of {@link CameraCharacteristics#SENSOR_INFO_ACTIVE_ARRAY_SIZE} to the
         * rect defined by {@code (0, 0, #getResolution#getWidth(), #getResolution#getHeight())}.
         * The matrix can be used to map the coordinates from one {@link UseCase} to another. For
         * example, detecting face with {@link ImageAnalysis}, and then highlighting the face in
         * {@link Preview}.
         *
         * <p>Code sample
         * <code><pre>
         *  // Get the transformation from sensor to effect input.
         *  Matrix sensorToEffect = surfaceRequest.getSensorToBufferTransform();
         *  // Get the transformation from sensor to ImageAnalysis.
         *  Matrix sensorToAnalysis = imageProxy.getSensorToBufferTransform();
         *  // Concatenate the two matrices to get the transformation from ImageAnalysis to effect.
         *  Matrix analysisToEffect = Matrix()
         *  sensorToAnalysis.invert(analysisToEffect);
         *  analysisToEffect.postConcat(sensorToEffect);
         * </pre></code>
         */
        @NonNull
        public abstract Matrix getSensorToBufferTransform();

        /**
         * Returns whether the buffer should be mirrored.
         *
         * <p>This flag indicates whether the buffer needs to be mirrored across the vertical
         * axis. For example, for front camera preview, the buffer should usually be mirrored. The
         * mirroring should be applied after the {@link #getRotationDegrees()} is applied.
         */
        public abstract boolean isMirroring();

        /**
         * Creates new {@link TransformationInfo}
         *
         * <p> Internally public to be used in view artifact tests.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static TransformationInfo of(@NonNull Rect cropRect,
                @ImageOutputConfig.RotationDegreesValue int rotationDegrees,
                @ImageOutputConfig.OptionalRotationValue int targetRotation,
                boolean hasCameraTransform, @NonNull Matrix sensorToBufferTransform,
                boolean mirroring) {
            return new AutoValue_SurfaceRequest_TransformationInfo(cropRect, rotationDegrees,
                    targetRotation, hasCameraTransform, sensorToBufferTransform, mirroring);
        }

        // Hides public constructor.
        TransformationInfo() {
        }
    }
}

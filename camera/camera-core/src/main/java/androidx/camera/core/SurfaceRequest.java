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
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.DeferrableSurface;
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
 * @see Preview.SurfaceProvider#onSurfaceRequested(SurfaceRequest)
 */
public final class SurfaceRequest {

    private final Size mResolution;
    private final Camera mCamera;
    private final Rect mViewPortRect;

    // For the camera to retrieve the surface from the user
    @SuppressWarnings("WeakerAccess") /*synthetic accessor */
    final ListenableFuture<Surface> mSurfaceFuture;
    private final CallbackToFutureAdapter.Completer<Surface> mSurfaceCompleter;

    // For the user to wait for the camera to be finished with the surface and retrieve errors
    // from the camera.
    private final ListenableFuture<Void> mSessionStatusFuture;

    // For notification of surface request cancellation. Should only be used to register
    // cancellation listeners.
    private final CallbackToFutureAdapter.Completer<Void> mRequestCancellationCompleter;

    /**
     * Creates a new surface request with the given resolution.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SurfaceRequest(@NonNull Size resolution, @NonNull Camera camera) {
        this(resolution, camera, null);
    }

    private DeferrableSurface mInternalDeferrableSurface;

    /**
     * Creates a new surface request with the given resolution and {@link Camera}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SurfaceRequest(
            @NonNull Size resolution,
            @NonNull Camera camera,
            @Nullable Rect viewPortRect) {
        super();
        mResolution = resolution;
        mCamera = camera;
        // Use full surface rect if viewPortRect is null.
        mViewPortRect = viewPortRect != null ? viewPortRect : new Rect(0, 0, resolution.getWidth(),
                resolution.getHeight());

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
            public void onFailure(Throwable t) {
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
        // camera and consumer are done using the surface.
        mInternalDeferrableSurface = new DeferrableSurface() {
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
            public void onFailure(Throwable t) {
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
    }

    /**
     * Returns the {@link DeferrableSurface} instance used to track usage of the surface that
     * fulfills this request.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public DeferrableSurface getDeferrableSurface() {
        return mInternalDeferrableSurface;
    }

    /**
     * Returns the resolution of the requested {@link Surface}.
     *
     * The surface which fulfills this request must have the resolution specified here in
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
     * Returns the {@link Camera} which is requesting a {@link Surface}.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Camera getCamera() {
        return mCamera;
    }


    /**
     * Return the crop rect rectangle.
     *
     * <p> The returned value dictates how the {@link Surface} provided in
     * {@link #provideSurface(Surface, Executor, Consumer)} should be displayed.
     *
     * <p> The {@link Rect} is based on the Surface coordinates. The caller should update the UI
     * so that only the area defined by the {@link Rect} is visible to app users.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Rect getViewPortRect() {
        return mViewPortRect;
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
                public void onFailure(Throwable t) {
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
     * Result of providing a surface to a {@link SurfaceRequest} via
     * {@link #provideSurface(Surface, Executor, Consumer)}.
     */
    @AutoValue
    public abstract static class Result {

        /**
         * Possible result codes.
         *
         * @hide
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
}

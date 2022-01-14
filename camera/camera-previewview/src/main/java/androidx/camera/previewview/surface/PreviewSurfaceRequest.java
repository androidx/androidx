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

package androidx.camera.previewview.surface;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A completable, single-use request of a {@link Surface}.
 *
 * <p>Contains requirements for surface characteristics along with methods for completing the
 * request and listening for request cancellation.
 */
@RequiresApi(21)
public class PreviewSurfaceRequest {

    private final boolean mIsFrontCamera;
    @NonNull
    private final Size mResolution;
    @NonNull
    private final PreviewSurface mInternalPreviewSurface;

    @SuppressWarnings("WeakerAccess") /*synthetic accessor */
    final ListenableFuture<Surface> mSurfaceFuture;

    @SuppressWarnings("unused")
    @Nullable
    private Callback mCallback;

    /**
     * Creates a new surface request with the given resolution, the {@link Callback}.
     */
    public PreviewSurfaceRequest(
            boolean isFrontCamera,
            @NonNull Size resolution) {
        super();
        mResolution = resolution;
        mIsFrontCamera = isFrontCamera;

        // To ensure concurrency and ordering, operations are chained. Completion can only be
        // triggered externally by the top-level completer (mSurfaceCompleter). The other future
        // completers are only completed by callbacks set up within the constructor of this class
        // to ensure correct ordering of events.

        // Cancellation listener must be called last to ensure the result can be retrieved from
        // the session listener.
        String surfaceRequestString =
                "SurfaceRequest[size: " + resolution + ", id: " + this.hashCode() + "]";
        AtomicReference<CallbackToFutureAdapter.Completer<Surface>> surfaceCompleterRef =
                new AtomicReference<>(null);
        mSurfaceFuture = CallbackToFutureAdapter.getFuture(completer -> {
            surfaceCompleterRef.set(completer);
            return surfaceRequestString + "-Surface";
        });
        mInternalPreviewSurface = new PreviewSurface() {
            @SuppressLint("SyntheticAccessor")
            @NonNull
            @Override
            protected ListenableFuture<Surface> provideSurfaceAsync() {
                return mSurfaceFuture;
            }
        };
    }

    /**
     * Returns the resolution of the requested {@link Surface}.
     *
     * The surface which fulfills this request must have the resolution specified here in
     * order to fulfill the resource requirements of the camera.
     *
     * @return The guaranteed supported resolution.
     * @see SurfaceTexture#setDefaultBufferSize(int, int)
     */
    @NonNull
    public Size getResolution() {
        return mResolution;
    }

    /**
     * Returns the status of camera lens facing.
     *
     * @return true if front camera, otherwise false.
     */
    public boolean isFrontCamera() {
        return mIsFrontCamera;
    }

    /**
     * Set surface request callback.
     *
     * @param callback {@link Callback}.
     */
    @SuppressLint("ExecutorRegistration")
    public void setCallback(@NonNull Callback callback) {
        mCallback = callback;
    }

    /**
     * Clear surface request callback.
     */
    public void clearCallback() {
        mCallback = null;
    }

    /**
     * Returns the {@link PreviewSurface}.
     * @return The surface returned for the request.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public PreviewSurface getPreviewSurface() {
        return mInternalPreviewSurface;
    }

    /**
     * Closes the {@link PreviewSurface} for safe release.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void closePreviewSurface() {
        mInternalPreviewSurface.close();
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
     * {@link Result} containing {@link Result#RESULT_REQUEST_CANCELLED}.
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
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void provideSurface(
            @NonNull Surface surface,
            @NonNull Executor executor,
            @NonNull Consumer<Result> resultListener) {
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
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public boolean willNotProvideSurface() {
        return true;
    }

    /**
     * Result of providing a surface to a {@link PreviewSurfaceRequest} via
     * {@link #provideSurface(Surface, Executor, Consumer)}.
     *
     * @hide
     */
    @AutoValue
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract static class Result {

        /**
         * Possible result codes.
         *
         * @hide
         */
        @IntDef({RESULT_SURFACE_USED_SUCCESSFULLY, RESULT_REQUEST_CANCELLED, RESULT_INVALID_SURFACE,
                RESULT_SURFACE_ALREADY_PROVIDED, RESULT_WILL_NOT_PROVIDE_SURFACE})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(Scope.LIBRARY_GROUP)
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
         * future {@link PreviewSurfaceRequest SurfaceRequests}, and a new surface should be
         * created instead.
         */
        public static final int RESULT_SURFACE_USED_SUCCESSFULLY = 0;

        /**
         * Provided surface was never attached to the camera due to the
         * {@link PreviewSurfaceRequest} being cancelled by the camera.
         *
         * <p>It is safe to release or reuse {@link Surface}, assuming it was not previously
         * attached to a camera via {@link #provideSurface(Surface, Executor, Consumer)}. If
         * reusing the surface for a future surface request, it should be verified that the
         * surface still matches the resolution specified by
         * {@link PreviewSurfaceRequest#getResolution()}.
         */
        public static final int RESULT_REQUEST_CANCELLED = 1;

        /**
         * Provided surface could not be used by the camera.
         *
         * <p>This is likely due to the {@link Surface} being closed prematurely or the resolution
         * of the surface not matching the resolution specified by
         * {@link PreviewSurfaceRequest#getResolution()}.
         */
        public static final int RESULT_INVALID_SURFACE = 2;

        /**
         * Surface was not attached to the camera through this invocation of
         * {@link #provideSurface(Surface, Executor, Consumer)} due to the {@link PreviewSurfaceRequest}
         * already being complete with a surface.
         *
         * <p>The {@link PreviewSurfaceRequest} has already been completed by a previous invocation
         * of {@link #provideSurface(Surface, Executor, Consumer)}.
         *
         * <p>It is safe to release or reuse the {@link Surface}, assuming it was not previously
         * attached to a camera via {@link #provideSurface(Surface, Executor, Consumer)}.
         */
        public static final int RESULT_SURFACE_ALREADY_PROVIDED = 3;

        /**
         * Surface was not attached to the camera through this invocation of
         * {@link #provideSurface(Surface, Executor, Consumer)} due to the
         * {@link PreviewSurfaceRequest} already being marked as "will not provide surface".
         *
         * <p>The {@link PreviewSurfaceRequest} has already been marked as 'will not provide
         * surface' by a previous invocation of {@link #willNotProvideSurface()}.
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
         * @param surface The {@link Surface} used to complete the {@link PreviewSurfaceRequest}.
         */
        @NonNull
        static Result of(@ResultCode int code, @NonNull Surface surface) {
            return new AutoValue_PreviewSurfaceRequest_Result(code, surface);
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
         * The surface used to complete a {@link PreviewSurfaceRequest} with
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
     * Callback for surface request.
     */
    public interface Callback {

        /**
         * Callback for success.
         * @param surface surface.
         */
        void onSuccess(@NonNull Surface surface);

        /**
         * Callback for failure.
         * @param t throwable.
         */
        void onFailure(@NonNull Throwable t);
    }
}

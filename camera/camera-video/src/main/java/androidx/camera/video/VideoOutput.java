/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.ConstantObservable;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.Timebase;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
 * A class that will produce video data from a {@link Surface}.
 *
 * <p>Implementations will provide a {@link Surface} to a video frame producer via the
 * {@link SurfaceRequest} sent to {@link #onSurfaceRequested(SurfaceRequest)}.
 *
 * <p>The type of video data produced by a video output and API for saving or communicating that
 * data is left to the implementation. An implementation commonly used for local video saving is
 * {@link Recorder}. This interface is usually only needs to be implemented by applications for
 * advanced use cases.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface VideoOutput {
    /**
     * A state which represents whether the video frame producer is producing frames to the
     * provided {@link Surface}.
     */
    @RestrictTo(Scope.LIBRARY)
    enum SourceState {
        /** The video frame producer is active and is producing frames. */
        ACTIVE_STREAMING,
        /** The video frame producer is active but is not producing frames. */
        ACTIVE_NON_STREAMING,
        /** The video frame producer is inactive. */
        INACTIVE
    }

    /**
     * Called when a new {@link Surface} has been requested by a video frame producer.
     *
     * <p>Users of this class should not call this method directly. It will be called by the
     * video frame producer. Implementors of this class should be aware that this method is
     * called when a video frame producer is ready to receive a surface that it can use to send
     * video frames to the video output. The video frame producer may repeatedly request a
     * surface more than once, but only the latest {@link SurfaceRequest} should be considered
     * active. All previous surface requests will complete by sending a
     * {@link androidx.camera.core.SurfaceRequest.Result} to the consumer passed to
     * {@link SurfaceRequest#provideSurface(Surface, Executor, Consumer)}.
     *
     * <p>A request is considered active until it is
     * {@linkplain SurfaceRequest#provideSurface(Surface, Executor, androidx.core.util.Consumer)
     * fulfilled}, {@linkplain SurfaceRequest#willNotProvideSurface() marked as 'will not
     * complete'}, or
     * {@linkplain SurfaceRequest#addRequestCancellationListener(Executor, Runnable) cancelled
     * by the video frame producer}. After one of these conditions occurs, a request is considered
     * completed.
     *
     * <p>Once a request is successfully completed, it is guaranteed that if a new request is
     * made, the {@link Surface} used to fulfill the previous request will be detached from the
     * video frame producer and the {@code resultListener} provided in
     * {@link SurfaceRequest#provideSurface(Surface, Executor, Consumer)}
     * will be invoked with a {@link androidx.camera.core.SurfaceRequest.Result} containing
     * {@link androidx.camera.core.SurfaceRequest.Result#RESULT_SURFACE_USED_SUCCESSFULLY}.
     *
     * @param request the request for a surface which contains the requirements of the
     *                surface and methods for completing the request.
     */
    void onSurfaceRequested(@NonNull SurfaceRequest request);

    /**
     * Called when a new {@link Surface} has been requested by a video frame producer.
     *
     * @param timebase the video source timebase
     */
    @RestrictTo(Scope.LIBRARY)
    default void onSurfaceRequested(@NonNull SurfaceRequest request, @NonNull Timebase timebase) {
        onSurfaceRequested(request);
    }

    /**
     * Returns an observable {@link StreamInfo} which contains the information of the
     * {@link VideoOutput}.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY)
    default Observable<StreamInfo> getStreamInfo() {
        return StreamInfo.ALWAYS_ACTIVE_OBSERVABLE;
    }

    /**
     * Returns an observable {@link MediaSpec} which contains hints about what kind of input the
     * {@link VideoOutput} is expecting.
     *
     * <p>All values contained in the media specification are considered hints and may be ignored
     * by the video frame producer. The {@link VideoOutput} should always respect the surface
     * requirements given in the {@link SurfaceRequest} in
     * {@link #onSurfaceRequested(SurfaceRequest)}, or the video frame producer may not be able
     * to produce frames.
     *
     * <p>Implementations should be careful about updating the {@link MediaSpec} too often, as
     * changes may not come for free and may require the video frame producer to re-initialize,
     * which could cause a new {@link SurfaceRequest} to be sent to
     * {@link #onSurfaceRequested(SurfaceRequest)}.
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    default Observable<MediaSpec> getMediaSpec() {
        return ConstantObservable.withValue(null);
    }

    /**
     * Called when the state of the video frame producer is changed.
     */
    @RestrictTo(Scope.LIBRARY)
    default void onSourceStateChanged(@NonNull SourceState sourceState) {

    }

    // TODO(b/278170231): wraps getMediaSpec and getMediaCapabilities for increased scalability and
    //  easier retrieval of initial specs and capabilities.
    /**
     * Returns the {@link VideoCapabilities} information of the {@link VideoOutput}.
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    default VideoCapabilities getMediaCapabilities(@NonNull CameraInfo cameraInfo) {
        return VideoCapabilities.EMPTY;
    }
}

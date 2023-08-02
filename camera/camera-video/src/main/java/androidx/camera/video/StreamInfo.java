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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.ConstantObservable;
import androidx.camera.core.impl.Observable;

import com.google.auto.value.AutoValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that contains the information of an video output stream.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY)
@AutoValue
public abstract class StreamInfo {

    /** The stream hasn't been setup. */
    static final int STREAM_ID_ANY = 0;

    /** The stream setup fails. */
    static final int STREAM_ID_ERROR = -1;

    static final StreamInfo STREAM_INFO_ANY_INACTIVE = StreamInfo.of(STREAM_ID_ANY,
            StreamState.INACTIVE);

    static final Set<Integer> NON_SURFACE_STREAM_ID = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(STREAM_ID_ANY, STREAM_ID_ERROR)));

    static final Observable<StreamInfo> ALWAYS_ACTIVE_OBSERVABLE =
            ConstantObservable.withValue(StreamInfo.of(STREAM_ID_ANY, StreamState.ACTIVE));
    /**
     * A state which represents whether the video output is ready for frame streaming.
     *
     * <p>This is used in the observable returned by {@link #getStreamState()} to inform
     * producers that they can start or stop producing frames.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    enum StreamState {
        /** The video output is active and ready to receive frames. */
        ACTIVE,
        /** The video output is inactive and any frames sent will be discarded. */
        INACTIVE;
    }

    StreamInfo() {

    }

    @NonNull
    static StreamInfo of(int id, @NonNull StreamState streamState) {
        return new AutoValue_StreamInfo(id, streamState, null);
    }

    @NonNull
    static StreamInfo of(int id, @NonNull StreamState streamState, @Nullable
            SurfaceRequest.TransformationInfo inProgressTransformationInfo) {
        return new AutoValue_StreamInfo(id, streamState, inProgressTransformationInfo);
    }

    /**
     * Gets the ID of the video output stream.
     *
     * <p>The ID will be changed if the {@link Surface} provided to
     * {@link VideoOutput#onSurfaceRequested} becomes invalid by the {@link VideoOutput}. A new
     * {@link SurfaceRequest} has to be issued in order to obtain a new {@link Surface} to
     * continue drawing frames to the {@link VideoOutput}.
     *
     * <p>The ID will be {@link #STREAM_ID_ANY} if the stream hasn't been setup and the ID will be
     * {@link #STREAM_ID_ERROR} if the stream setup fails.
     */
    public abstract int getId();

    /**
     * Gets the stream state which can be used to determine if the video output is ready for
     * streaming.
     *
     * <p>When the StreamState is ACTIVE, the {@link Surface} provided to
     * {@link VideoOutput#onSurfaceRequested} should be ready to consume frames.
     *
     * <p>When the StreamState is INACTIVE, any frames drawn to the {@link Surface} may be
     * discarded.
     *
     * <p>This can be used by video producers to determine when frames should be drawn to the
     * {@link Surface} to ensure they are not doing excess work.
     *
     * <p>Implementers of the VideoOutput interface should consider overriding this method
     * as a performance improvement. The default implementation returns an {@link Observable}
     * which is always {@link StreamState#ACTIVE}.
     */
    @NonNull
    public abstract StreamState getStreamState();

    /**
     * Returns the existing transformation information if there's an in-processing surface.
     *
     * @return the in-progress transformation information, or {@code null} if there's no
     * in-processing surface.
     */
    @Nullable
    public abstract SurfaceRequest.TransformationInfo getInProgressTransformationInfo();
}

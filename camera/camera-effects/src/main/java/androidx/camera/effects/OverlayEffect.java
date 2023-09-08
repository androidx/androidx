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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.UseCase;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@link CameraEffect} for drawing overlay on top of the camera frames.
 * TODO(b/297509601): Make it public API in 1.4.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public class OverlayEffect {

    /**
     * {@link #drawFrame(long)} result code
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
     * The {@link #drawFrame(long)} call was successful. The frame with the exact timestamp was
     * drawn to the output surface.
     */
    public static final int RESULT_SUCCESS = 1;

    /**
     * The {@link #drawFrame(long)} call failed because the frame with the exact timestamp was
     * not found in the queue. It could be one of the following reasons:
     *
     * <ul>
     * <li>the timestamp was incorrect, or
     * <li>the frame was not yet available, or
     * <li>the frame was removed because {@link #drawFrame} had been called with a newer
     * timestamp, or
     * <li>the frame was removed due to the queue is full.
     * </ul>
     *
     * If it's the last case, the caller may avoid this issue by increasing the queue depth.
     */
    public static final int RESULT_FRAME_NOT_FOUND = 2;

    /**
     * The {@link #drawFrame(long)} call failed because the output surface is missing, or the
     * output surface no longer matches the frame. It could be because the {@link UseCase}
     * was unbound, causing the original surface to be replaced or disabled.
     */
    public static final int RESULT_INVALID_SURFACE = 3;

    /**
     * The {@link #drawFrame(long)} call failed because the caller cancelled the drawing. This
     * happens when the listener provided via {@link #setOnDrawListener(Function)} returned false.
     */
    public static final int RESULT_CANCELLED_BY_CALLER = 4;

    /**
     * TODO(b/297509601): add JavaDoc
     */
    @NonNull
    public ListenableFuture<Integer> drawFrame(long timestampNs) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * TODO(b/297509601): add JavaDoc
     */
    public void setOnDrawListener(@NonNull Function<Frame, Boolean> onDrawListener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

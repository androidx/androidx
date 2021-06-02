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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Provides basic information for a recording to work.
 */
abstract class Recording {

    protected static final String TAG = "Recording";

    private final Recorder mRecorder;
    private final OutputOptions mOutputOptions;
    private Consumer<VideoRecordEvent> mEventListener;
    private Executor mCallbackExecutor;

    Recording(@NonNull Recorder recorder, @NonNull OutputOptions options,
            @Nullable Executor callbackExecutor, @Nullable Consumer<VideoRecordEvent> listener) {
        mRecorder = recorder;
        mOutputOptions = options;
        mCallbackExecutor = callbackExecutor;
        mEventListener = listener;
    }

    @NonNull
    Recorder getRecorder() {
        return mRecorder;
    }

    @NonNull
    OutputOptions getOutputOptions() {
        return mOutputOptions;
    }

    @Nullable
    Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    void setCallbackExecutor(@Nullable Executor callbackExecutor) {
        mCallbackExecutor = callbackExecutor;
    }

    @Nullable
    Consumer<VideoRecordEvent> getEventListener() {
        return mEventListener;
    }

    void setEventListener(@Nullable Consumer<VideoRecordEvent> eventListener) {
        mEventListener = eventListener;
    }

    /**
     * Updates the recording status and callback to users.
     */
    void updateVideoRecordEvent(@NonNull VideoRecordEvent event) {
        if (getCallbackExecutor() != null && getEventListener() != null) {
            try {
                getCallbackExecutor().execute(() -> getEventListener().accept(event));
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "The callback executor is invalid.", e);
            }
        }
    }
}

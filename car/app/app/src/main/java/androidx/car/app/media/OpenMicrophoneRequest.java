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

package androidx.car.app.media;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The request for a call to open microphone.
 *
 * <p>This is a host-side interface for handling audio callbacks. To record the microphone, use
 * {@link CarAudioRecord}.
 */
@CarProtocol
@RequiresCarApi(5)
@KeepFields
public final class OpenMicrophoneRequest {
    private final @Nullable CarAudioCallbackDelegate mCarAudioCallbackDelegate;

    OpenMicrophoneRequest(@NonNull Builder builder) {
        mCarAudioCallbackDelegate = builder.mCarAudioCallbackDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private OpenMicrophoneRequest() {
        mCarAudioCallbackDelegate = null;
    }

    public @NonNull CarAudioCallbackDelegate getCarAudioCallbackDelegate() {
        return requireNonNull(mCarAudioCallbackDelegate);
    }

    /**
     * A builder for {@link OpenMicrophoneRequest}.
     */
    public static final class Builder {
        final @NonNull CarAudioCallbackDelegate mCarAudioCallbackDelegate;

        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull CarAudioCallback callback) {
            mCarAudioCallbackDelegate = CarAudioCallbackDelegate.create(requireNonNull(callback));
        }

        /**
         * Builds the {@link OpenMicrophoneRequest} for this builder.
         */
        public @NonNull OpenMicrophoneRequest build() {
            return new OpenMicrophoneRequest(this);
        }
    }
}

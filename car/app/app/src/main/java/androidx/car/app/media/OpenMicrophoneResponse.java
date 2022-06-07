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
import android.os.ParcelFileDescriptor;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;

import java.io.IOException;
import java.io.InputStream;

/**
 * The response for a call to open microphone.
 *
 * <p>This is a host-side interface for handling audio callbacks. To record the microphone, use
 * {@link CarAudioRecord}.
 */
@CarProtocol
@RequiresCarApi(5)
public final class OpenMicrophoneResponse {
    @Keep
    @Nullable
    private final CarAudioCallbackDelegate mCarAudioCallbackDelegate;

    @Keep
    @Nullable
    private final ParcelFileDescriptor mCarMicrophoneDescriptor;

    OpenMicrophoneResponse(@NonNull Builder builder) {
        mCarAudioCallbackDelegate = builder.mCarAudioCallbackDelegate;
        mCarMicrophoneDescriptor = builder.mCarMicrophoneDescriptor;
    }

    /** Constructs an empty instance, used by serialization code. */
    private OpenMicrophoneResponse() {
        mCarMicrophoneDescriptor = null;
        mCarAudioCallbackDelegate = null;
    }

    /**
     * Returns the callback to use to communicate recording state.
     */
    @NonNull
    public CarAudioCallbackDelegate getCarAudioCallback() {
        return requireNonNull(mCarAudioCallbackDelegate);
    }

    /**
     * Returns an {@link InputStream} to read the microphone bytes from.
     */
    @NonNull
    public InputStream getCarMicrophoneInputStream() {
        ParcelFileDescriptor pfd = mCarMicrophoneDescriptor;
        if (pfd == null) {
            try {
                // For no pfd return a closed pfd.
                ParcelFileDescriptor[] pfds = ParcelFileDescriptor.createReliablePipe();
                pfds[1].close();
                pfd = pfds[0];
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
    }

    /**
     * A builder for {@link OpenMicrophoneResponse}s.
     */
    public static final class Builder {
        @NonNull
        final CarAudioCallbackDelegate mCarAudioCallbackDelegate;

        @Nullable
        ParcelFileDescriptor mCarMicrophoneDescriptor;

        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull CarAudioCallback callback) {
            mCarAudioCallbackDelegate = CarAudioCallbackDelegate.create(requireNonNull(callback));

        }

        /**
         * Sets the {@link ParcelFileDescriptor} that is to be used to read microphone bytes from
         * the car.
         *
         * <p>This is only needed in projected case, where the car microphone comes from the host.
         */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setCarMicrophoneDescriptor(
                @NonNull ParcelFileDescriptor carMicrophoneDescriptor) {
            mCarMicrophoneDescriptor = requireNonNull(carMicrophoneDescriptor);
            return this;
        }

        /**
         * Builds the {@link OpenMicrophoneResponse} for this builder.
         */
        @NonNull
        public OpenMicrophoneResponse build() {
            return new OpenMicrophoneResponse(this);
        }
    }
}

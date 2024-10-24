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
import android.os.RemoteException;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An internal delegate for performing callbacks related to car audio between library and host.
 *
 * <p>This is a host-side interface for handling audio callbacks. To record the microphone, use
 * {@link CarAudioRecord}.
 */
@CarProtocol
@RequiresCarApi(5)
@KeepFields
public class CarAudioCallbackDelegate {
    private final @Nullable ICarAudioCallback mCallback;

    /**
     * Signifies to stop processing audio input.
     */
    public void onStopRecording() {
        try {
            requireNonNull(mCallback).onStopRecording();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    // This callback relates to a background process and is owned by the library
    @SuppressLint("ExecutorRegistration")
    static @NonNull CarAudioCallbackDelegate create(@NonNull CarAudioCallback callback) {
        return new CarAudioCallbackDelegate(callback);
    }

    private CarAudioCallbackDelegate(@NonNull CarAudioCallback callback) {
        mCallback = new CarAudioCallbackStub(callback);
    }

    /** For serialization. */
    private CarAudioCallbackDelegate() {
        mCallback = null;
    }

    // We need to keep these stub for Bundler serialization logic.
    @CarProtocol
    @KeepFields
    private static class CarAudioCallbackStub extends ICarAudioCallback.Stub {
        private final @Nullable CarAudioCallback mCarAudioCallback;

        CarAudioCallbackStub(@NonNull CarAudioCallback callback) {
            mCarAudioCallback = callback;
        }

        /** For serialization. */
        CarAudioCallbackStub() {
            mCarAudioCallback = null;
        }

        @Override
        public void onStopRecording() {
            requireNonNull(mCarAudioCallback).onStopRecording();
        }
    }
}

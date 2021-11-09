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

package androidx.camera.video.internal.encoder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * The encoder callback event.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface EncoderCallback {

    /** The method called before the first encoded data. */
    void onEncodeStart();

    /** The method called after the last encoded data. */
    void onEncodeStop();

    /**
     * The method called when encoder is currently started and {@link Encoder#pause()} is invoked.
     *
     * <p>The callback is only triggered when the encoder is completely paused. Successive calls
     * to {@link Encoder#pause()} and {@link Encoder#start()} may not trigger the callback.
     * {@link #onEncodedData} will not be triggered after this callback.
     */
    default void onEncodePaused() {
    }

    /**
     * The method called when error occurs while encoding.
     *
     * <p>After that, {@link #onEncodedData} and {@link #onEncodeStop} will not be triggered.
     *
     * @param e the encode error
     */
    void onEncodeError(@NonNull EncodeException e);

    /**
     * The method called when a new encoded data comes.
     *
     * @param encodedData the encoded data
     */
    void onEncodedData(@NonNull EncodedData encodedData);

    /**
     * The method called when encoder gets a new output config.
     *
     * @param outputConfig the output config
     */
    void onOutputConfigUpdate(@NonNull OutputConfig outputConfig);

    /** An empty implementation. */
    EncoderCallback EMPTY = new EncoderCallback() {
        /** {@inheritDoc} */
        @Override
        public void onEncodeStart() {
        }

        /** {@inheritDoc} */
        @Override
        public void onEncodeStop() {
        }

        /** {@inheritDoc} */
        @Override
        public void onEncodeError(@NonNull EncodeException e) {
        }

        /** {@inheritDoc} */
        @Override
        public void onEncodedData(@NonNull EncodedData encodedData) {
        }

        /** {@inheritDoc} */
        @Override
        public void onOutputConfigUpdate(@NonNull OutputConfig outputConfig) {
        }
    };
}

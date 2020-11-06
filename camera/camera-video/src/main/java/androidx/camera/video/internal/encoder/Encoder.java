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

import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * The encoder interface.
 *
 * <p>An encoder could be either a video encoder or an audio encoder. The interface defines the
 * common APIs to communicate with an encoder.
 */
public interface Encoder {

    /** Returns the encoder's input instance. */
    @NonNull
    EncoderInput getInput();

    /**
     * Starts the encoder.
     *
     * <p>If the encoder is not started yet, it will first trigger
     * {@link EncoderCallback#onEncodeStart}. Then continually invoke the
     * {@link EncoderCallback#onEncodedData} callback until the encoder is paused, stopped or
     * released. It can call {@link #pause} to pause the encoding after started. If the encoder is
     * in paused state, then calling this method will resume the encoding.
     */
    void start();

    /**
     * Stops the encoder.
     *
     * <p>It will trigger {@link EncoderCallback#onEncodeStop} after the last encoded data. It can
     * call {@link #start} to start again.
     */
    void stop();

    /**
     * Pauses the encoder.
     *
     * <p>{@link #pause} only work between {@link #start} and {@link #stop}.
     * Once the encoder is paused, it will drop the input data until {@link #start} is invoked
     * again.
     */
    void pause();

    /**
     * Releases the encoder.
     *
     * <p>Once the encoder is released, it cannot be used anymore. Any other method call after
     * the encoder is released will get {@link IllegalStateException}.
     */
    void release();

    /**
     * Sets callback to encoder.
     *
     * @param encoderCallback the encoder callback
     * @param executor the callback executor
     */
    void setEncoderCallback(@NonNull EncoderCallback encoderCallback, @NonNull Executor executor);

    /** The encoder's input. */
    interface EncoderInput {
    }

    /**
     * A SurfaceInput provides a {@link Surface} as the interface to receive video raw data.
     *
     * <p>SurfaceInput is only available for video encoder. It has to set
     * {@link #setOnSurfaceUpdateListener} to obtain the {@link Surface} update. It is the caller's
     * responsibility to release the updated {@link Surface}.
     */
    interface SurfaceInput extends EncoderInput {

        void setOnSurfaceUpdateListener(@NonNull Executor executor,
                @NonNull OnSurfaceUpdateListener listener);

        /**
         * An interface for receiving the update event of the input {@link Surface} of the encoder.
         */
        interface OnSurfaceUpdateListener {
            /**
             * Notifies the surface is updated.
             *
             * @param surface the updated surface
             */
            void onSurfaceUpdate(@NonNull Surface surface);
        }
    }

    /** A ByteBufferInput provides {@link #putByteBuffer} method to send raw data. */
    interface ByteBufferInput extends EncoderInput {

        /**
         * Puts an input raw {@link ByteBuffer} to the encoder.
         *
         * <p>The input {@code ByteBuffer} must be put when encoder is in started and not paused
         * state, otherwise the {@code ByteBuffer} will be dropped directly. Then the encoded data
         * will be sent via {@link EncoderCallback#onEncodedData} callback.
         *
         * @param byteBuffer the input byte buffer
         */
        void putByteBuffer(@NonNull ByteBuffer byteBuffer);
    }
}

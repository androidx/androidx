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

import androidx.camera.video.internal.BufferProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * The encoder interface.
 *
 * <p>An encoder could be either a video encoder or an audio encoder. The interface defines the
 * common APIs to communicate with an encoder.
 */
public interface Encoder {

    /**
     * A stop timestamp that represents no preference on the stop timestamp.
     *
     * <p>The encoder will decide a proper stop timestamp internally. Usually it will be the time
     * when {@link #stop()} is called.
     */
    long NO_TIMESTAMP = -1;

    /** Returns the encoder's input instance. */
    @NonNull EncoderInput getInput();

    /** Returns the EncoderInfo which provides encoder's information and capabilities. */
    @NonNull EncoderInfo getEncoderInfo();

    /** Returns target bitrate configured on the encoder */
    int getConfiguredBitrate();

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
     * <p>It will trigger {@link EncoderCallback#onEncodeStop} after receiving the encoded data
     * that has a timestamp as close as to the time this method is called. It's guaranteed that
     * there won't be more encoded data being produced after
     * {@link EncoderCallback#onEncodeStop()} is sent. {@link #start} can be called to start
     * another encoding session.
     */
    void stop();

    /**
     * Stops the encoder with an expected stop time.
     *
     * <p>It will trigger {@link EncoderCallback#onEncodeStop} after receiving the encoded data
     * that has a timestamp as close as to the given stop timestamp. It's guaranteed that there
     * won't be more encoded data being produced after {@link EncoderCallback#onEncodeStop()} is
     * sent.{@link #start} can be called to start another encoding session.
     *
     * <p>Use {@link #stop()} to stop the encoder without specifying expected stop time and let
     * the Encoder to decide.
     *
     * @param expectedStopTimeUs The desired stop time.
     */
    void stop(long expectedStopTimeUs);

    /**
     * Pauses the encoder.
     *
     * <p>{@code pause} only work between {@link #start} and {@link #stop}.
     * Once the encoder is paused, it will drop the input data until {@link #start} is invoked
     * again.
     */
    void pause();

    /**
     * Releases the encoder.
     *
     * <p>Once the encoder is released, it cannot be used anymore. Any other method call after
     * the encoder is released will get {@link IllegalStateException}.
     *
     * <p>If this encoder takes {@link SurfaceInput}, this method will release the {@link Surface}.
     * So this method should only be called when the frame producer is finished with the surface.
     */
    void release();

    /**
     * Returns a ListenableFuture to represent the release status.
     *
     * <p>Cancellation on the returned ListenableFuture takes no effect.
     */
    @NonNull ListenableFuture<Void> getReleasedFuture();

    /**
     * Sets callback to encoder.
     *
     * @param encoderCallback the encoder callback
     * @param executor the callback executor
     */
    void setEncoderCallback(@NonNull EncoderCallback encoderCallback, @NonNull Executor executor);

    /**
     * Request a key frame.
     *
     * <p>Only take effect when the encoder is a video encoder and encoder is started.
     */
    void requestKeyFrame();

    /** The encoder's input. */
    interface EncoderInput {
    }

    /**
     * A SurfaceInput provides a {@link Surface} as the interface to receive video raw data.
     *
     * <p>SurfaceInput is only available for video encoder. For the returned surface, the
     * receiver should rely on {@link Encoder#release()} to release it.
     */
    interface SurfaceInput extends EncoderInput {

        /** Returns the surface. */
        @NonNull
        Surface getSurface();
    }

    /**
     * A ByteBufferInput is a {@link BufferProvider} implementation and provides
     * {@link InputBuffer} to write input data to the encoder.
     *
     * @see BufferProvider
     * @see InputBuffer
     */
    interface ByteBufferInput extends EncoderInput, BufferProvider<InputBuffer> {
    }
}

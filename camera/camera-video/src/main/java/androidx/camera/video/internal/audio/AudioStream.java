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

package androidx.camera.video.internal.audio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/** Represents an audio stream. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface AudioStream {

    /**
     * Starts the audio stream.
     *
     * @throws AudioStreamException  if there is an error when starting the audio stream.
     * @throws IllegalStateException if the stream has been released.
     */
    void start() throws AudioStreamException, IllegalStateException;

    /**
     * Stops the audio stream.
     *
     * <p>Stream can be started again via {@link #start()} after this method is called.
     *
     * @throws IllegalStateException if the stream has been released.
     */
    void stop() throws IllegalStateException;

    /**
     * Releases the audio stream.
     *
     * <p>Once the stream is released, it is no longer usable.</p>
     */
    void release();

    /**
     * Reads data from the audio stream.
     *
     * <p>Data is written to {@link ByteBuffer#position()} and the position on this buffer is
     * unchanged after a call to this method. The representation of the data in the buffer will
     * depend on the format specified in the {@link AudioSettings}.
     *
     * @param byteBuffer the buffer to which the audio data is written.
     * @return the retrieved information by this read operation.
     * @throws IllegalStateException if the stream has not been started or has been released.
     */
    @NonNull
    PacketInfo read(@NonNull ByteBuffer byteBuffer);

    /**
     * Sets callback to monitor the stream state.
     *
     * <p>An existing callback can be removed by setting {@code null} callback.
     *
     * <p>The callback must be set when audio stream is not started and not released.
     *
     * @throws IllegalArgumentException if {@code callback} is not null but {@code executor} is
     *                                  null.
     * @throws IllegalStateException    if the callback is started or released.
     */
    void setCallback(@Nullable AudioStreamCallback callback, @Nullable Executor executor);

    /** The information of a read operation. */
    @AutoValue
    abstract class PacketInfo {

        /**
         * Creates a PacketInfo.
         *
         * @param sizeInBytes the total size in bytes.
         * @param timestampNs the timestamp in nanoseconds.
         */
        @NonNull
        public static PacketInfo of(int sizeInBytes, long timestampNs) {
            return new AutoValue_AudioStream_PacketInfo(sizeInBytes, timestampNs);
        }

        /** Total data size in bytes. */
        public abstract int getSizeInBytes();

        /** Get the beginning timestamp. */
        public abstract long getTimestampNs();
    }

    /** The callback to monitor the stream state. */
    interface AudioStreamCallback {
        /** Calls when silence state is changed. */
        default void onSilenceStateChanged(boolean isSilenced) {
        }
    }

    /** Represents any failure when accessing {@link AudioStream}. */
    @SuppressWarnings("unused")
    class AudioStreamException extends Exception {
        /** Constructs the exception. */
        public AudioStreamException() {
        }

        /**
         * Constructs the exception.
         *
         * @param message the exception message.
         */
        public AudioStreamException(@NonNull String message) {
            super(message);
        }

        /**
         * Constructs the exception.
         *
         * @param message the exception message.
         * @param cause   the cause of this exception.
         */
        public AudioStreamException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs the exception.
         *
         * @param cause the cause of this exception.
         */
        public AudioStreamException(@NonNull Throwable cause) {
            super(cause);
        }
    }
}

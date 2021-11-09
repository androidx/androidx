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

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;

/**
 * InputBuffer is provided by the {@link Encoder} and used to feed data into the {@link Encoder}.
 *
 * <p>Once {@link InputBuffer} is complete or no longer needed, {@link #submit} or
 * {@link #cancel} must be called to return the request to the encoder, otherwise, it will cause
 * leakage or failure.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface InputBuffer {

    /**
     * Gets the {@link ByteBuffer} of the input buffer.
     *
     * <p>Before submitting the InputBuffer, the internal position of the ByteBuffer must be set
     * to prepare it for reading, e.g. the {@link ByteBuffer#position} is the beginning of the
     * data, usually 0; the {@link ByteBuffer#limit} is the end of the data. Usually
     * {@link ByteBuffer#flip} is used after writing data.
     *
     * <p>Getting ByteBuffer multiple times won't reset its internal position and data.
     *
     * @throws {@link IllegalStateException} if InputBuffer is submitted or canceled.
     */
    @NonNull
    ByteBuffer getByteBuffer();

    /**
     * Sets the timestamp of the input buffer in microseconds.
     *
     * @throws {@link IllegalStateException} if InputBuffer is submitted or canceled.
     */
    void setPresentationTimeUs(long presentationTimeUs);

    /**
     * Denotes the input buffer is the end of the data stream.
     *
     * @throws {@link IllegalStateException} if InputBuffer is submitted or canceled.
     */
    void setEndOfStream(boolean isEndOfStream);

    /**
     * Submits the input buffer.
     *
     * <p>The data will be written to encoder only when {@link #submit} is called.
     *
     * @return {@code true} if submit successfully; {@code false} if already submitted, failed or
     * has been canceled.
     */
    boolean submit();

    /**
     * Returns the request to encoder without taking any effect.
     *
     * @return {@code true} if cancel successfully; {@code false} if already submitted, failed or
     * has been canceled.
     */
    boolean cancel();

    /**
     * The {@link ListenableFuture} that is complete when {@link #submit} or {@link #cancel} is
     * called.
     */
    @NonNull
    ListenableFuture<Void> getTerminationFuture();
}

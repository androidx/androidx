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

package androidx.camera.effects.internal;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.effects.opengl.GlRenderer;

/**
 * A buffer of {@link TextureFrame}.
 *
 * <p>This class is not thread safe. It is expected to be called from a single GL thread.
 */
@RequiresApi(21)
class TextureFrameBuffer {

    @NonNull
    private final TextureFrame[] mFrames;

    /**
     * Creates a buffer of frames backed by texture IDs.
     *
     * @param textureIds @D textures allocated by {@link GlRenderer#createBufferTextureIds}.
     */
    TextureFrameBuffer(int[] textureIds) {
        mFrames = new TextureFrame[textureIds.length];
        for (int i = 0; i < textureIds.length; i++) {
            mFrames[i] = new TextureFrame(textureIds[i]);
        }
    }

    /**
     * Gets the number of total frames in the buffer.
     */
    int getLength() {
        return mFrames.length;
    }

    /**
     * Gets a filled frame with the exact timestamp.
     *
     * <p>This is used when the caller wants to render a frame with a specific timestamp. It
     * returns null if the frame no longer exists in the queue. e.g. it might be removed
     * because the queue is full.
     *
     * <p>This method also empties frames that are older than the given timestamp. This is based
     * on the assumption that the output frame are always rendered in order.
     *
     * <p>Once the returned frame is rendered, it should be marked empty by the caller.
     */
    @Nullable
    TextureFrame getFrameToRender(long timestampNs) {
        TextureFrame frameToReturn = null;
        for (TextureFrame frame : mFrames) {
            if (frame.isEmpty()) {
                continue;
            }
            if (frame.getTimestampNs() == timestampNs) {
                frameToReturn = frame;
            } else if (frame.getTimestampNs() < timestampNs) {
                frame.markEmpty();
            }
        }
        return frameToReturn;
    }

    /**
     * Gets the next empty frame, or the oldest frame if the buffer is full.
     *
     * <p>This is called when a new frame is available from the camera. The new frame will be
     * filled to this position. If there is no empty frame, the oldest frame will be overwritten.
     */
    @NonNull
    TextureFrame getFrameToFill() {
        long minTimestampNs = Long.MAX_VALUE;
        TextureFrame oldestFrame = null;
        for (TextureFrame frame : mFrames) {
            if (frame.isEmpty()) {
                return frame;
            } else if (frame.getTimestampNs() < minTimestampNs) {
                minTimestampNs = frame.getTimestampNs();
                oldestFrame = frame;
            }
        }
        return requireNonNull(oldestFrame);
    }

    @VisibleForTesting
    @NonNull
    TextureFrame[] getFrames() {
        return mFrames;
    }
}

/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.player;

import androidx.annotation.NonNull;

/**
 * Immutable class for describing video size.
 */
// TODO: Remove this class and use androidx.media2.common.VideoSize instead
public final class VideoSize {
    private final androidx.media2.common.VideoSize mInternal;

    /**
     * Creates a new immutable VideoSize instance.
     *
     * @param width The width of the video
     * @param height The height of the video
     */
    public VideoSize(int width, int height) {
        mInternal = new androidx.media2.common.VideoSize(width, height);
    }

    VideoSize(@NonNull androidx.media2.common.VideoSize internal) {
        mInternal = internal;
    }

    /**
     * Returns the width of the video.
     */
    public int getWidth() {
        return mInternal.getWidth();
    }

    /**
     * Returns the height of the video.
     */
    public int getHeight() {
        return mInternal.getHeight();
    }

    /**
     * Checks if this video size is equal to another video size.
     * <p>
     * Two video sizes are equal if and only if both their widths and heights are
     * equal.
     * <p>
     * A video size object is never equal to any other type of object.
     *
     * @return {@code true} if the objects were equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof VideoSize) {
            VideoSize other = (VideoSize) obj;
            return mInternal.equals(other.mInternal);
        }
        return false;
    }

    /**
     * Return the video size represented as a string with the format {@code "WxH"}
     *
     * @return string representation of the video size
     */
    @Override
    public String toString() {
        return mInternal.toString();
    }

    @Override
    public int hashCode() {
        return mInternal.hashCode();
    }
}

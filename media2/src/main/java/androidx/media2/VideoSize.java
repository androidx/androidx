/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

/**
 * Immutable class for describing video size.
 */
public final class VideoSize {
    /**
     * Creates a new immutable VideoSize instance.
     *
     * @param width The width of the video
     * @param height The height of the video
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public VideoSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the width of the video.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the height of the video.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Checks if this video size is equal to another video size.
     * <p>
     * Two video sizes are equal if and only if both their widths and heights are
     * equal.
     * </p>
     * <p>
     * A video size object is never equal to any other type of object.
     * </p>
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
            return mWidth == other.mWidth && mHeight == other.mHeight;
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
        return mWidth + "x" + mHeight;
    }

    @Override
    public int hashCode() {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return mHeight ^ ((mWidth << (Integer.SIZE / 2)) | (mWidth >>> (Integer.SIZE / 2)));
    }

    private final int mWidth;
    private final int mHeight;
}

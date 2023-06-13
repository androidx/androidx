/*
 * Copyright 2022 Google Inc. All rights reserved.
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

package androidx.heifwriter;

import static android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class writes one or more still images (of the same dimensions) into
 * an AVIF file.
 *
 * It currently supports three input modes: {@link #INPUT_MODE_BUFFER},
 * {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
 *
 * The general sequence (in pseudo-code) to write a avif file using this class is as follows:
 *
 * 1) Construct the writer:
 * AvifWriter avifwriter = new AvifWriter(...);
 *
 * 2) If using surface input mode, obtain the input surface:
 * Surface surface = avifwriter.getInputSurface();
 *
 * 3) Call start:
 * avifwriter.start();
 *
 * 4) Depending on the chosen input mode, add one or more images using one of these methods:
 * avifwriter.addYuvBuffer(...);   Or
 * avifwriter.addBitmap(...);   Or
 * render to the previously obtained surface
 *
 * 5) Call stop:
 * avifwriter.stop(...);
 *
 * 6) Close the writer:
 * avifwriter.close();
 *
 * Please refer to the documentations on individual methods for the exact usage.
 */
@SuppressWarnings("HiddenSuperclass")
public final class AvifWriter extends WriterBase {

    private static final String TAG = "AvifWriter";
    private static final boolean DEBUG = false;

    /**
     * The input mode where the client adds input buffers with YUV data.
     *
     * @see #addYuvBuffer(int, byte[])
     */
    public static final int INPUT_MODE_BUFFER = WriterBase.INPUT_MODE_BUFFER;

    /**
     * The input mode where the client renders the images to an input Surface created by the writer.
     *
     * The input surface operates in single buffer mode. As a result, for use case where camera
     * directly outputs to the input surface, this mode will not work because camera framework
     * requires multiple buffers to operate in a pipeline fashion.
     *
     * @see #getInputSurface()
     */
    public static final int INPUT_MODE_SURFACE = WriterBase.INPUT_MODE_SURFACE;

    /**
     * The input mode where the client adds bitmaps.
     *
     * @see #addBitmap(Bitmap)
     */
    public static final int INPUT_MODE_BITMAP = WriterBase.INPUT_MODE_BITMAP;

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        INPUT_MODE_BUFFER, INPUT_MODE_SURFACE, INPUT_MODE_BITMAP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InputMode {

    }

    /**
     * Builder class for constructing a AvifWriter object from specified parameters.
     */
    public static final class Builder {
        private final String mPath;
        private final FileDescriptor mFd;
        private final int mWidth;
        private final int mHeight;
        private final @InputMode int mInputMode;
        private boolean mGridEnabled = true;
        private int mQuality = 100;
        private int mMaxImages = 1;
        private int mPrimaryIndex = 0;
        private int mRotation = 0;
        private Handler mHandler;
        private boolean mHighBitDepthEnabled = false;

        /**
         * Construct a Builder with output specified by its path.
         *
         * @param path Path of the file to be written.
         * @param width Width of the image in number of pixels.
         * @param height Height of the image in number of pixels.
         * @param inputMode Input mode for this writer, must be one of {@link #INPUT_MODE_BUFFER},
         *                  {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
         */
        public Builder(@NonNull String path,
            @IntRange(from = 1) int width,
            @IntRange(from = 1) int height,
            @InputMode int inputMode) {
            this(path, null, width, height, inputMode);
        }

        /**
         * Construct a Builder with output specified by its file descriptor.
         *
         * @param fd File descriptor of the file to be written.
         * @param width Width of the image in number of pixels.
         * @param height Height of the image in number of pixels.
         * @param inputMode Input mode for this writer, must be one of {@link #INPUT_MODE_BUFFER},
         *                  {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
         */
        public Builder(@NonNull FileDescriptor fd,
            @IntRange(from = 1) int width,
            @IntRange(from = 1) int height,
            @InputMode int inputMode) {
            this(null, fd, width, height, inputMode);
        }

        private Builder(String path, FileDescriptor fd,
            @IntRange(from = 1) int width,
            @IntRange(from = 1) int height,
            @InputMode int inputMode) {
            mPath = path;
            mFd = fd;
            mWidth = width;
            mHeight = height;
            mInputMode = inputMode;
        }

        /**
         * Set the image rotation in degrees.
         *
         * @param rotation Rotation angle in degrees (clockwise) of the image, must be 0, 90,
         *                 180 or 270. Default is 0.
         * @return this Builder object.
         */
        public @NonNull Builder setRotation(@IntRange(from = 0) int rotation) {
            if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
                throw new IllegalArgumentException("Invalid rotation angle: " + rotation);
            }
            mRotation = rotation;
            return this;
        }

        /**
         * Set whether to enable grid option.
         *
         * @param gridEnabled Whether to enable grid option. If enabled, the tile size will be
         *                    automatically chosen. Default is to enable.
         * @return this Builder object.
         */
        public @NonNull Builder setGridEnabled(boolean gridEnabled) {
            mGridEnabled = gridEnabled;
            return this;
        }

        /**
         * Set the quality for encoding images.
         *
         * @param quality A number between 0 and 100 (inclusive), with 100 indicating the best
         *                quality supported by this implementation. Default is 100.
         * @return this Builder object.
         */
        public @NonNull Builder setQuality(@IntRange(from = 0, to = 100) int quality) {
            if (quality < 0 || quality > 100) {
                throw new IllegalArgumentException("Invalid quality: " + quality);
            }
            mQuality = quality;
            return this;
        }

        /**
         * Set the maximum number of images to write.
         *
         * @param maxImages Max number of images to write. Frames exceeding this number will not be
         *                  written to file. The writing can be stopped earlier before this number
         *                  of images are written by {@link #stop(long)}, except for the input mode
         *                  of {@link #INPUT_MODE_SURFACE}, where the EOS timestamp must be
         *                  specified (via {@link #setInputEndOfStreamTimestamp(long)} and reached.
         *                  Default is 1.
         * @return this Builder object.
         */
        public @NonNull Builder setMaxImages(@IntRange(from = 1) int maxImages) {
            if (maxImages <= 0) {
                throw new IllegalArgumentException("Invalid maxImage: " + maxImages);
            }
            mMaxImages = maxImages;
            return this;
        }

        /**
         * Set the primary image index.
         *
         * @param primaryIndex Index of the image that should be marked as primary, must be within
         *                     range [0, maxImages - 1] inclusive. Default is 0.
         * @return this Builder object.
         */
        public @NonNull Builder setPrimaryIndex(@IntRange(from = 0) int primaryIndex) {
            mPrimaryIndex = primaryIndex;
            return this;
        }

        /**
         * Provide a handler for the AvifWriter to use.
         *
         * @param handler If not null, client will receive all callbacks on the handler's looper.
         *                Otherwise, client will receive callbacks on a looper created by the
         *                writer. Default is null.
         * @return this Builder object.
         */
        public @NonNull Builder setHandler(@Nullable Handler handler) {
            mHandler = handler;
            return this;
        }

        /**
         * Provide a setting for the AvifWriter to use high bit-depth or not.
         *
         * @param highBitDepthEnabled Whether to enable high bit-depth mode. Default is false, if
         *                            true, AvifWriter will encode with high bit-depth.
         * @return this Builder object.
         */
        public @NonNull Builder setHighBitDepthEnabled(boolean highBitDepthEnabled) {
            mHighBitDepthEnabled = highBitDepthEnabled;
            return this;
        }

        /**
         * Build a AvifWriter object.
         *
         * @return a AvifWriter object built according to the specifications.
         * @throws IOException if failed to create the writer, possibly due to failure to create
         *                     {@link android.media.MediaMuxer} or {@link android.media.MediaCodec}.
         */
        public @NonNull AvifWriter build() throws IOException {
            return new AvifWriter(mPath, mFd, mWidth, mHeight, mRotation, mGridEnabled, mQuality,
                mMaxImages, mPrimaryIndex, mInputMode, mHandler, mHighBitDepthEnabled);
        }
    }

    @SuppressLint("WrongConstant")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    AvifWriter(@NonNull String path,
        @NonNull FileDescriptor fd,
        int width,
        int height,
        int rotation,
        boolean gridEnabled,
        int quality,
        int maxImages,
        int primaryIndex,
        @InputMode int inputMode,
        @Nullable Handler handler,
        boolean highBitDepthEnabled) throws IOException {
        super(rotation, inputMode, maxImages, primaryIndex, gridEnabled, quality,
            handler, highBitDepthEnabled);

        if (DEBUG) {
            Log.d(TAG, "width: " + width
                + ", height: " + height
                + ", rotation: " + rotation
                + ", gridEnabled: " + gridEnabled
                + ", quality: " + quality
                + ", maxImages: " + maxImages
                + ", primaryIndex: " + primaryIndex
                + ", inputMode: " + inputMode);
        }

        // set to 1 initially, and wait for output format to know for sure
        mNumTiles = 1;

        mMuxer = (path != null) ? new MediaMuxer(path, MUXER_OUTPUT_HEIF)
            : new MediaMuxer(fd, MUXER_OUTPUT_HEIF);

        mEncoder = new AvifEncoder(width, height, gridEnabled, quality,
            mInputMode, mHandler, new WriterCallback(), highBitDepthEnabled);
    }
}
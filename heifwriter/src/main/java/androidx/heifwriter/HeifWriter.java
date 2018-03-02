/*
 * Copyright 2018 Google Inc. All rights reserved.
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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import static android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF;

/**
 * This class writes one or more still images (of the same dimensions) into
 * a heif file.
 *
 * It currently supports three input modes: {@link #INPUT_MODE_BUFFER},
 * {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
 *
 * The general sequence (in pseudo-code) to write a heif file using this class is as follows:
 *
 * 1) Construct the writer:
 * HeifWriter heifwriter = new HeifWriter(...);
 *
 * 2) If using surface input mode, obtain the input surface:
 * Surface surface = heifwriter.getInputSurface();
 *
 * 3) Call start:
 * heifwriter.start();
 *
 * 4) Depending on the chosen input mode, add one or more images using one of these methods:
 * heifwriter.addYuvBuffer(...);   Or
 * heifwriter.addBitmap(...);   Or
 * render to the previously obtained surface
 *
 * 5) Call stop:
 * heifwriter.stop(...);
 *
 * 6) Close the writer:
 * heifwriter.close();
 *
 * Please refer to the documentations on individual methods for the exact usage.
 */
public final class HeifWriter implements AutoCloseable {
    private static final String TAG = "HeifWriter";
    private static final boolean DEBUG = false;

    private final @InputMode int mInputMode;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private int mNumTiles;
    private final int mNumImages;
    private final int mPrimaryIndex;
    private final ResultWaiter mResultWaiter = new ResultWaiter();

    private MediaMuxer mMuxer;
    private HeifEncoder mHeifEncoder;
    private int[] mTrackIndexArray;
    private int mOutputIndex;
    private boolean mStarted;

    /**
     * The input mode where the client adds input buffers with YUV data.
     *
     * @see #addYuvBuffer(int, byte[])
     */
    public static final int INPUT_MODE_BUFFER = 0;

    /**
     * The input mode where the client renders the images to an input Surface
     * created by the writer.
     *
     * @see #getInputSurface()
     */
    public static final int INPUT_MODE_SURFACE = 1;

    /**
     * The input mode where the client adds bitmaps.
     *
     * @see #addBitmap(Bitmap)
     */
    public static final int INPUT_MODE_BITMAP = 2;

    /** @hide */
    @IntDef({
            INPUT_MODE_BUFFER, INPUT_MODE_SURFACE, INPUT_MODE_BITMAP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InputMode {}

    /**
     * Construct a heif writer that writes to a file specified by its path.
     *
     * @param path Path of the file to be written.
     * @param width Width of the image.
     * @param height Height of the image.
     * @param useGrid Whether to encode image into tiles. If enabled, the tile size will be
     *                automatically chosen.
     * @param quality A number between 0 and 100 (inclusive), with 100 indicating the best quality
     *                supported by this implementation (which often results in larger file size).
     * @param numImages Max number of images to write. Frames exceeding this number will not be
     *                  written to file. The writing can be stopped earlier before this number of
     *                  images are written by {@link #stop(long)}, except for the input mode of
     *                  {@link #INPUT_MODE_SURFACE}, where the EOS timestamp must be specified (via
     *                 {@link #setInputEndOfStreamTimestamp(long)} and reached.
     * @param primaryIndex Index of the image that should be marked as primary, must be within range
     *                     [0, numImages - 1] inclusive.
     * @param inputMode Input mode for this writer, must be one of {@link #INPUT_MODE_BUFFER},
     *                  {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
     * @param handler If not null, client will receive all callbacks on the handler's looper.
     *                Otherwise, client will receive callbacks on a looper created by the writer.
     *
     * @throws IOException if failed to construct MediaMuxer or HeifEncoder.
     */
    @SuppressLint("WrongConstant")
    public HeifWriter(@NonNull String path,
                      int width, int height, boolean useGrid,
                      int quality, int numImages, int primaryIndex,
                      @InputMode int inputMode,
                      @Nullable Handler handler) throws IOException {
        this(width, height, useGrid, quality, numImages, primaryIndex, inputMode, handler,
                new MediaMuxer(path, MUXER_OUTPUT_HEIF));
    }

    /**
     * Construct a heif writer that writes to a file specified by file descriptor.
     *
     * @param fd File descriptor of the file to be written.
     * @param width Width of the image.
     * @param height Height of the image.
     * @param useGrid Whether to encode image into tiles. If enabled, the tile size will be
     *                automatically chosen.
     * @param quality A number between 0 and 100 (inclusive), with 100 indicating the best quality
     *                supported by this implementation (which often results in larger file size).
     * @param numImages Max number of images to write. Frames exceeding this number will not be
     *                  written to file. The writing can be stopped earlier before this number of
     *                  images are written by {@link #stop(long)}, except for the input mode of
     *                  {@link #INPUT_MODE_SURFACE}, where the EOS timestamp must be specified (via
     *                 {@link #setInputEndOfStreamTimestamp(long)} and reached.
     * @param primaryIndex Index of the image that should be marked as primary, must be within range
     *                     [0, numImages - 1] inclusive.
     * @param inputMode Input mode for this writer, must be one of {@link #INPUT_MODE_BUFFER},
     *                  {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
     * @param handler If not null, client will receive all callbacks on the handler's looper.
     *                Otherwise, client will receive callbacks on a looper created by the writer.
     *
     * @throws IOException if failed to construct MediaMuxer or HeifEncoder.
     */
    @SuppressLint("WrongConstant")
    public HeifWriter(@NonNull FileDescriptor fd,
                      int width, int height, boolean useGrid,
                      int quality, int numImages, int primaryIndex,
                      @InputMode int inputMode,
                      @Nullable Handler handler) throws IOException {
        this(width, height, useGrid, quality, numImages, primaryIndex, inputMode, handler,
                new MediaMuxer(fd, MUXER_OUTPUT_HEIF));
    }

    private HeifWriter(int width, int height, boolean useGrid,
                       int quality, int numImages, int primaryIndex,
                       @InputMode int inputMode,
                       @Nullable Handler handler,
                       @NonNull MediaMuxer muxer) throws IOException {
        if (numImages <= 0 || primaryIndex < 0 || primaryIndex >= numImages) {
            throw new IllegalArgumentException(
                    "Invalid numImages (" + numImages + ") or primaryIndex (" + primaryIndex + ")");
        }

        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC, width, height);

        if (DEBUG) {
            Log.d(TAG, "format: " + format + ", inputMode: " + inputMode +
                    ", numImage: " + numImages + ", primaryIndex: " + primaryIndex);
        }

        // set to 1 initially, and wait for output format to know for sure
        mNumTiles = 1;

        mInputMode = inputMode;
        mNumImages = numImages;
        mPrimaryIndex = primaryIndex;

        Looper looper = (handler != null) ? handler.getLooper() : null;
        if (looper == null) {
            mHandlerThread = new HandlerThread("HeifEncoderThread",
                    Process.THREAD_PRIORITY_FOREGROUND);
            mHandlerThread.start();
            looper = mHandlerThread.getLooper();
        } else {
            mHandlerThread = null;
        }
        mHandler = new Handler(looper);

        mMuxer = muxer;

        mHeifEncoder = new HeifEncoder(width, height, useGrid, quality,
                mInputMode, mHandler, new HeifCallback());
    }

    /**
     * Start the heif writer. Can only be called once.
     *
     * @throws IllegalStateException if called more than once.
     */
    public void start() {
        checkStarted(false);
        mStarted = true;
        mHeifEncoder.start();
    }

    /**
     * Add one YUV buffer to the heif file.
     *
     * @param format The YUV format as defined in {@link android.graphics.ImageFormat}, currently
     *               only support YUV_420_888.
     *
     * @param data byte array containing the YUV data. If the format has more than one planes,
     *             they must be concatenated.
     *
     * @throws IllegalStateException if not started or not configured to use buffer input.
     */
    public void addYuvBuffer(int format, @NonNull byte[] data) {
        checkStartedAndMode(INPUT_MODE_BUFFER);
        synchronized (this) {
            if (mHeifEncoder != null) {
                mHeifEncoder.addYuvBuffer(format, data);
            }
        }
    }

    /**
     * Retrieves the input surface for encoding.
     *
     * @return the input surface if configured to use surface input.
     *
     * @throws IllegalStateException if called after start or not configured to use surface input.
     */
    public @NonNull Surface getInputSurface() {
        checkStarted(false);
        checkMode(INPUT_MODE_SURFACE);
        return mHeifEncoder.getInputSurface();
    }

    /**
     * Set the timestamp (in nano seconds) of the last input frame to encode.
     *
     * This call is only valid for surface input. Client can use this to stop the heif writer
     * earlier before the maximum number of images are written. If not called, the writer will
     * only stop when the maximum number of images are written.
     *
     * @param timestampNs timestamp (in nano seconds) of the last frame that will be written to the
     *                    heif file. Frames with timestamps larger than the specified value will not
     *                    be written. However, if a frame already started encoding when this is set,
     *                    all tiles within that frame will be encoded.
     *
     * @throws IllegalStateException if not started or not configured to use surface input.
     */
    public void setInputEndOfStreamTimestamp(long timestampNs) {
        checkStartedAndMode(INPUT_MODE_SURFACE);
        synchronized (this) {
            if (mHeifEncoder != null) {
                mHeifEncoder.setEndOfInputStreamTimestamp(timestampNs);
            }
        }
    }

    /**
     * Add one bitmap to the heif file.
     *
     * @param bitmap the bitmap to be added to the file.
     * @throws IllegalStateException if not started or not configured to use bitmap input.
     */
    public void addBitmap(@NonNull Bitmap bitmap) {
        checkStartedAndMode(INPUT_MODE_BITMAP);
        synchronized (this) {
            if (mHeifEncoder != null) {
                mHeifEncoder.addBitmap(bitmap);
            }
        }
    }

    /**
     * Stop the heif writer synchronously. Throws exception if the writer didn't finish writing
     * successfully. Upon a success return:
     *
     * - For buffer and bitmap inputs, all images sent before stop will be written.
     *
     * - For surface input, images with timestamp on or before that specified in
     *   {@link #setInputEndOfStreamTimestamp(long)} will be written. In case where
     *   {@link #setInputEndOfStreamTimestamp(long)} was never called, stop will block
     *   until maximum number of images are received.
     *
     * @param timeoutMs Maximum time (in microsec) to wait for the writer to complete, with zero
     *                  indicating waiting indefinitely.
     * @see #setInputEndOfStreamTimestamp(long)
     * @throws Exception if encountered error, in which case the output file may not be valid. In
     *                   particular, {@link TimeoutException} is thrown when timed out, and {@link
     *                   MediaCodec.CodecException} is thrown when encountered codec error.
     */
    public void stop(long timeoutMs) throws Exception {
        checkStarted(true);
        synchronized (this) {
            if (mHeifEncoder != null) {
                mHeifEncoder.stopAsync();
            }
        }
        mResultWaiter.waitForResult(timeoutMs);
    }

    private void checkStarted(boolean requiredStarted) {
        if (mStarted != requiredStarted) {
            throw new IllegalStateException("Already started");
        }
    }

    private void checkMode(@InputMode int requiredMode) {
        if (mInputMode != requiredMode) {
            throw new IllegalStateException("Not valid in input mode " + mInputMode);
        }
    }

    private void checkStartedAndMode(@InputMode int requiredMode) {
        checkStarted(true);
        checkMode(requiredMode);
    }

    /**
     * Routine to stop and release writer, must be called on the same looper
     * that receives heif encoder callbacks.
     */
    private void closeInternal() {
        if (DEBUG) Log.d(TAG, "closeInternal");

        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }

        if (mHeifEncoder != null) {
            mHeifEncoder.close();
            synchronized (this) {
                mHeifEncoder = null;
            }
        }
    }

    /**
     * Callback from the heif encoder.
     */
    private class HeifCallback extends HeifEncoder.Callback {
        /**
         * Upon receiving output format from the encoder, add the requested number of
         * image tracks to the muxer and start the muxer.
         */
        @Override
        public void onOutputFormatChanged(
                @NonNull HeifEncoder encoder, @NonNull MediaFormat format) {
            if (encoder != mHeifEncoder) return;

            if (DEBUG) {
                Log.d(TAG, "onOutputFormatChanged: " + format);
            }
            if (mTrackIndexArray != null) {
                stopAndNotify(new IllegalStateException(
                        "Output format changed after muxer started"));
                return;
            }

            try {
                int gridRows = format.getInteger(MediaFormat.KEY_GRID_ROWS);
                int gridCols = format.getInteger(MediaFormat.KEY_GRID_COLS);
                mNumTiles = gridRows * gridCols;
            } catch (NullPointerException | ClassCastException  e) {
                mNumTiles = 1;
            }

            // add mNumImages image tracks of the same format
            mTrackIndexArray = new int[mNumImages];
            for (int i = 0; i < mTrackIndexArray.length; i++) {
                // mark primary
                if (i == mPrimaryIndex) {
                    format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
                }
                mTrackIndexArray[i] = mMuxer.addTrack(format);
            }
            mMuxer.start();
        }

        /**
         * Upon receiving an output buffer from the encoder (which is one image when
         * grid is not used, or one tile if grid is used), add that sample to the muxer.
         */
        @Override
        public void onDrainOutputBuffer(
                @NonNull HeifEncoder encoder, @NonNull ByteBuffer byteBuffer) {
            if (encoder != mHeifEncoder) return;

            if (DEBUG) {
                Log.d(TAG, "onDrainOutputBuffer: " + mOutputIndex);
            }
            if (mTrackIndexArray == null) {
                stopAndNotify(new IllegalStateException(
                        "Output buffer received before format info"));
                return;
            }

            if (mOutputIndex < mNumImages * mNumTiles) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.set(byteBuffer.position(), byteBuffer.remaining(), 0, 0);
                mMuxer.writeSampleData(
                        mTrackIndexArray[mOutputIndex / mNumTiles], byteBuffer, info);
            }

            mOutputIndex++;

            // post EOS if reached max number of images allowed.
            if (mOutputIndex == mNumImages * mNumTiles) {
                stopAndNotify(null);
            }
        }

        @Override
        public void onComplete(@NonNull HeifEncoder encoder) {
            if (encoder != mHeifEncoder) return;

            stopAndNotify(null);
        }

        @Override
        public void onError(@NonNull HeifEncoder encoder, @NonNull MediaCodec.CodecException e) {
            if (encoder != mHeifEncoder) return;

            stopAndNotify(e);
        }

        private void stopAndNotify(@Nullable Exception error) {
            try {
                closeInternal();
            } catch (Exception e) {
                // if there is an error during muxer stop, that must be propagated,
                // unless error exists already.
                if (error == null) {
                    error = e;
                }
            }
            mResultWaiter.signalResult(error);
        }
    }

    private static class ResultWaiter {
        private boolean mDone;
        private Exception mException;

        synchronized void waitForResult(long timeoutMs) throws Exception {
            if (timeoutMs < 0) {
                throw new IllegalArgumentException("timeoutMs is negative");
            }
            if (timeoutMs == 0) {
                while (!mDone) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {}
                }
            } else {
                final long startTimeMs = System.currentTimeMillis();
                long remainingWaitTimeMs = timeoutMs;
                // avoid early termination by "spurious" wakeup.
                while (!mDone && remainingWaitTimeMs > 0) {
                    try {
                        wait(remainingWaitTimeMs);
                    } catch (InterruptedException ex) {}
                    remainingWaitTimeMs -= (System.currentTimeMillis() - startTimeMs);
                }
            }
            if (!mDone) {
                mDone = true;
                mException = new TimeoutException("timed out waiting for result");
            }
            if (mException != null) {
                throw mException;
            }
        }

        synchronized void signalResult(@Nullable Exception e) {
            if (!mDone) {
                mDone = true;
                mException = e;
                notifyAll();
            }
        }
    }

    @Override
    public void close() {
        mHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    closeInternal();
                } catch (Exception e) {
                    // If the client called stop() properly, any errors would have been
                    // reported there. We don't want to crash when closing.
                }
            }
        });
    }
}

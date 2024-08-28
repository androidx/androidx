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
 * This class holds common utliities for {@link HeifWriter} and {@link AvifWriter}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WriterBase implements AutoCloseable {
    private static final String TAG = "WriterBase";
    private static final boolean DEBUG = false;
    private static final int MUXER_DATA_FLAG = 16;

    /**
     * The input mode where the client adds input buffers with YUV data.
     *
     * @see #addYuvBuffer(int, byte[])
     */
    protected static final int INPUT_MODE_BUFFER = 0;

    /**
     * The input mode where the client renders the images to an input Surface
     * created by the writer.
     *
     * The input surface operates in single buffer mode. As a result, for use case
     * where camera directly outputs to the input surface, this mode will not work
     * because camera framework requires multiple buffers to operate in a pipeline
     * fashion.
     *
     * @see #getInputSurface()
     */
    protected static final int INPUT_MODE_SURFACE = 1;

    /**
     * The input mode where the client adds bitmaps.
     *
     * @see #addBitmap(Bitmap)
     */
    protected static final int INPUT_MODE_BITMAP = 2;

    /** */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        INPUT_MODE_BUFFER, INPUT_MODE_SURFACE, INPUT_MODE_BITMAP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InputMode {}

    protected final @InputMode int mInputMode;
    protected final boolean mHighBitDepthEnabled;
    protected final HandlerThread mHandlerThread;
    protected final Handler mHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    protected int mNumTiles;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    protected final int mRotation;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    protected final int mMaxImages;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    protected final int mPrimaryIndex;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ResultWaiter mResultWaiter = new ResultWaiter();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull protected MediaMuxer mMuxer;
    @NonNull protected EncoderBase mEncoder;
    final AtomicBoolean mMuxerStarted = new AtomicBoolean(false);
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int[] mTrackIndexArray;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mOutputIndex;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mGridEnabled;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mQuality;
    private boolean mStarted;

    private final List<Pair<Integer, ByteBuffer>> mExifList = new ArrayList<>();

    protected WriterBase(int rotation,
        @InputMode int inputMode,
        int maxImages,
        int primaryIndex,
        boolean gridEnabled,
        int quality,
        @Nullable Handler handler,
        boolean highBitDepthEnabled) throws IOException {
        if (primaryIndex >= maxImages) {
            throw new IllegalArgumentException(
                "Invalid maxImages (" + maxImages + ") or primaryIndex (" + primaryIndex + ")");
        }

        mRotation = rotation;
        mInputMode = inputMode;
        mMaxImages = maxImages;
        mPrimaryIndex = primaryIndex;
        mGridEnabled = gridEnabled;
        mQuality = quality;
        mHighBitDepthEnabled = highBitDepthEnabled;

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
    }

    /**
     * Start the heif writer. Can only be called once.
     *
     * @throws IllegalStateException if called more than once.
     */
    void start() {
        checkStarted(false);
        mStarted = true;
        mEncoder.start();
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
    void addYuvBuffer(int format, @NonNull byte[] data) {
        checkStartedAndMode(INPUT_MODE_BUFFER);
        synchronized (this) {
            if (mEncoder != null) {
                mEncoder.addYuvBuffer(format, data);
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
    @NonNull Surface getInputSurface() {
        checkStarted(false);
        checkMode(INPUT_MODE_SURFACE);
        return mEncoder.getInputSurface();
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
    void setInputEndOfStreamTimestamp(@IntRange(from = 0) long timestampNs) {
        checkStartedAndMode(INPUT_MODE_SURFACE);
        synchronized (this) {
            if (mEncoder != null) {
                mEncoder.setEndOfInputStreamTimestamp(timestampNs);
            }
        }
    }

    /**
     * Add one bitmap to the heif file.
     *
     * @param bitmap the bitmap to be added to the file.
     * @throws IllegalStateException if not started or not configured to use bitmap input.
     */
    void addBitmap(@NonNull Bitmap bitmap) {
        checkStartedAndMode(INPUT_MODE_BITMAP);
        synchronized (this) {
            if (mEncoder != null) {
                mEncoder.addBitmap(bitmap);
            }
        }
    }

    /**
     * Add Exif data for the specified image. The data must be a valid Exif data block,
     * starting with "Exif\0\0" followed by the TIFF header (See JEITA CP-3451C Section 4.5.2.)
     *
     * @param imageIndex index of the image, must be a valid index for the max number of image
     *                   specified by {@link Builder#setMaxImages(int)}.
     * @param exifData byte buffer containing a Exif data block.
     * @param offset offset of the Exif data block within exifData.
     * @param length length of the Exif data block.
     */
    void addExifData(int imageIndex, @NonNull byte[] exifData, int offset, int length) {
        checkStarted(true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(length);
        buffer.put(exifData, offset, length);
        buffer.flip();
        // Put it in a queue, as we might not be able to process it at this time.
        synchronized (mExifList) {
            mExifList.add(new Pair<Integer, ByteBuffer>(imageIndex, buffer));
        }
        processExifData();
    }

    @SuppressLint("WrongConstant")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void processExifData() {
        if (!mMuxerStarted.get()) {
            return;
        }

        while (true) {
            Pair<Integer, ByteBuffer> entry;
            synchronized (mExifList) {
                if (mExifList.isEmpty()) {
                    return;
                }
                entry = mExifList.remove(0);
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            info.set(entry.second.position(), entry.second.remaining(), 0, MUXER_DATA_FLAG);
            mMuxer.writeSampleData(mTrackIndexArray[entry.first], entry.second, info);
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
    void stop(@IntRange(from = 0) long timeoutMs) throws Exception {
        checkStarted(true);
        synchronized (this) {
            if (mEncoder != null) {
                mEncoder.stopAsync();
            }
        }
        mResultWaiter.waitForResult(timeoutMs);
        processExifData();
        closeInternal();
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
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void closeInternal() {
        if (DEBUG) Log.d(TAG, "closeInternal");
        // We don't want to crash when closing, catch all exceptions.
        try {
            // Muxer could throw exceptions if stop is called without samples.
            // Don't crash in that case.
            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
            }
        } catch (Exception e) {
        } finally {
            mMuxer = null;
        }
        try {
            if (mEncoder != null) {
                mEncoder.close();
            }
        } catch (Exception e) {
        } finally {
            synchronized (this) {
                mEncoder = null;
            }
        }
    }

    /**
     * Callback from the encoder.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    protected class WriterCallback extends EncoderBase.Callback {
        private boolean mEncoderStopped;
        /**
         * Upon receiving output format from the encoder, add the requested number of
         * image tracks to the muxer and start the muxer.
         */
        @Override
        public void onOutputFormatChanged(
            @NonNull EncoderBase encoder, @NonNull MediaFormat format) {
            if (mEncoderStopped) return;

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
                int gridCols = format.getInteger(MediaFormat.KEY_GRID_COLUMNS);
                mNumTiles = gridRows * gridCols;
            } catch (NullPointerException | ClassCastException  e) {
                mNumTiles = 1;
            }

            // add mMaxImages image tracks of the same format
            mTrackIndexArray = new int[mMaxImages];

            // set rotation angle
            if (mRotation > 0) {
                Log.d(TAG, "setting rotation: " + mRotation);
                mMuxer.setOrientationHint(mRotation);
            }
            for (int i = 0; i < mTrackIndexArray.length; i++) {
                // mark primary
                format.setInteger(MediaFormat.KEY_IS_DEFAULT, (i == mPrimaryIndex) ? 1 : 0);
                mTrackIndexArray[i] = mMuxer.addTrack(format);
            }
            mMuxer.start();
            mMuxerStarted.set(true);
            processExifData();
        }

        /**
         * Upon receiving an output buffer from the encoder (which is one image when
         * grid is not used, or one tile if grid is used), add that sample to the muxer.
         */
        @Override
        public void onDrainOutputBuffer(
            @NonNull EncoderBase encoder, @NonNull ByteBuffer byteBuffer) {
            if (mEncoderStopped) return;

            if (DEBUG) {
                Log.d(TAG, "onDrainOutputBuffer: " + mOutputIndex);
            }
            if (mTrackIndexArray == null) {
                stopAndNotify(new IllegalStateException(
                    "Output buffer received before format info"));
                return;
            }

            if (mOutputIndex < mMaxImages * mNumTiles) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.set(byteBuffer.position(), byteBuffer.remaining(), 0, 0);
                mMuxer.writeSampleData(
                    mTrackIndexArray[mOutputIndex / mNumTiles], byteBuffer, info);
            }

            mOutputIndex++;

            // post EOS if reached max number of images allowed.
            if (mOutputIndex == mMaxImages * mNumTiles) {
                stopAndNotify(null);
            }
        }

        @Override
        public void onComplete(@NonNull EncoderBase encoder) {
            stopAndNotify(null);
        }

        @Override
        public void onError(@NonNull EncoderBase encoder, @NonNull MediaCodec.CodecException e) {
            stopAndNotify(e);
        }

        private void stopAndNotify(@Nullable Exception error) {
            if (mEncoderStopped) return;

            mEncoderStopped = true;
            mResultWaiter.signalResult(error);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static class ResultWaiter {
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

    /*
     * Gets rotation.
     */
    public int getRotation() {
        return mRotation;
    }

    /*
     * Returns true if grid is enabled.
     */
    public boolean isGridEnabled() {
        return mGridEnabled;
    }

    /*
     * Gets configured quality.
     */
    public int getQuality() {
        return mQuality;
    }

    /*
     * Gets number of maximum images.
     */
    public int getMaxImages() {
        return mMaxImages;
    }

    /*
     * Gets index of the primary image.
     */
    public int getPrimaryIndex() {
        return mPrimaryIndex;
    }

    /*
     * Gets handler.
     *
     * The result is the same as clients' input from setHandler() method.
     * If not null, client will receive all callbacks on the handler's looper.
     * Otherwise, client will receive callbacks on the current looper.
     */
    public @Nullable Handler getHandler() {
        return mHandler;
    }

    /*
     * Returns true if high bit-depth is enabled.
     */
    public boolean isHighBitDepthEnabled() {
        return mHighBitDepthEnabled;
    }
}
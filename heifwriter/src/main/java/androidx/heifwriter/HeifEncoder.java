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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * This class encodes images into HEIF-compatible samples using HEVC encoder.
 *
 * It currently supports three input modes: {@link #INPUT_MODE_BUFFER},
 * {@link #INPUT_MODE_SURFACE}, or {@link #INPUT_MODE_BITMAP}.
 *
 * The output format and samples are sent back in {@link
 * Callback#onOutputFormatChanged(HeifEncoder, MediaFormat)} and {@link
 * Callback#onDrainOutputBuffer(HeifEncoder, ByteBuffer)}. If the client
 * requests to use grid, each tile will be sent back individually.
 *
 * HeifEncoder is made a separate class from {@link HeifWriter}, as some more
 * advanced use cases might want to build solutions on top of the HeifEncoder directly.
 * (eg. mux still images and video tracks into a single container).
 *
 * @hide
 */
public final class HeifEncoder implements AutoCloseable,
        SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "HeifEncoder";
    private static final boolean DEBUG = false;

    private static final int GRID_WIDTH = 512;
    private static final int GRID_HEIGHT = 512;
    private static final double MAX_COMPRESS_RATIO = 0.25f;
    private static final int INPUT_BUFFER_POOL_SIZE = 2;

    private MediaCodec mEncoder;

    private final Callback mCallback;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final @InputMode int mInputMode;

    private final int mWidth;
    private final int mHeight;
    private final int mGridWidth;
    private final int mGridHeight;
    private final int mGridRows;
    private final int mGridCols;
    private final int mNumTiles;

    private int mInputIndex;
    private boolean mInputEOS;
    private final Rect mSrcRect;
    private final Rect mDstRect;
    private ByteBuffer mCurrentBuffer;
    private final ArrayList<ByteBuffer> mEmptyBuffers = new ArrayList<>();
    private final ArrayList<ByteBuffer> mFilledBuffers = new ArrayList<>();
    private final ArrayList<Integer> mCodecInputBuffers = new ArrayList<>();

    // Helper for tracking EOS when surface is used
    private SurfaceEOSTracker mEOSTracker;

    // Below variables are to handle GL copy from client's surface
    // to encoder surface when tiles are used.
    private SurfaceTexture mInputTexture;
    private Surface mInputSurface;
    private Surface mEncoderSurface;
    private EglWindowSurface mEncoderEglSurface;
    private EglRectBlt mRectBlt;
    private int mTextureId;
    private final float[] mTmpMatrix = new float[16];

    public static final int INPUT_MODE_BUFFER = HeifWriter.INPUT_MODE_BUFFER;
    public static final int INPUT_MODE_SURFACE = HeifWriter.INPUT_MODE_SURFACE;
    public static final int INPUT_MODE_BITMAP = HeifWriter.INPUT_MODE_BITMAP;
    @IntDef({
        INPUT_MODE_BUFFER,
        INPUT_MODE_SURFACE,
        INPUT_MODE_BITMAP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InputMode {}

    public static abstract class Callback {
        /**
         * Called when the output format has changed.
         *
         * @param encoder The HeifEncoder object.
         * @param format The new output format.
         */
        public abstract void onOutputFormatChanged(
                @NonNull HeifEncoder encoder, @NonNull MediaFormat format);

        /**
         * Called when an output buffer becomes available.
         *
         * @param encoder The HeifEncoder object.
         * @param byteBuffer the available output buffer.
         */
        public abstract void onDrainOutputBuffer(
                @NonNull HeifEncoder encoder, @NonNull ByteBuffer byteBuffer);

        /**
         * Called when encoding reached the end of stream without error.
         *
         * @param encoder The HeifEncoder object.
         */
        public abstract void onComplete(@NonNull HeifEncoder encoder);

        /**
         * Called when encoding hits an error.
         *
         * @param encoder The HeifEncoder object.
         * @param e The exception that the codec reported.
         */
        public abstract void onError(@NonNull HeifEncoder encoder, @NonNull CodecException e);
    }

    /**
     * Configure the heif encoding session. Should only be called once.
     *
     * @param width Width of the image.
     * @param height Height of the image.
     * @param useGrid Whether to encode image into tiles. If enabled, tile size will be
     *                automatically chosen.
     * @param quality A number between 0 and 100 (inclusive), with 100 indicating the best quality
     *                supported by this implementation (which often results in larger file size).
     * @param inputMode The input type of this encoding session.
     * @param handler If not null, client will receive all callbacks on the handler's looper.
     *                Otherwise, client will receive callbacks on a looper created by us.
     * @param cb The callback to receive various messages from the heif encoder.
     */
    public HeifEncoder(int width, int height, boolean useGrid,
                       int quality, @InputMode int inputMode,
                       @Nullable Handler handler, @NonNull Callback cb) throws IOException {
        if (DEBUG) Log.d(TAG, "width: " + width + ", height: " + height +
                ", useGrid: " + useGrid + ", quality: " + quality + ", inputMode: " + inputMode);

        if (width < 0 || height < 0 || quality < 0 || quality > 100) {
            throw new IllegalArgumentException("invalid encoder inputs");
        }

        // Disable grid if the image is too small
        useGrid &= (width > GRID_WIDTH || height > GRID_HEIGHT);

        boolean useHeicEncoder = false;
        MediaCodecInfo.CodecCapabilities caps = null;
        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC);
            caps = mEncoder.getCodecInfo().getCapabilitiesForType(
                    MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC);
            // If the HEIC encoder can't support the size, fall back to HEVC encoder.
            if (!caps.getVideoCapabilities().isSizeSupported(width, height)) {
                mEncoder.release();
                mEncoder = null;
                throw new Exception();
            }
            useHeicEncoder = true;
        } catch (Exception e) {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            caps = mEncoder.getCodecInfo().getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            // Always enable grid if the size is too large for the HEVC encoder
            useGrid |= !caps.getVideoCapabilities().isSizeSupported(width, height);
        }

        mInputMode = inputMode;

        mCallback = cb;

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
        boolean useSurfaceInternally =
                (inputMode == INPUT_MODE_SURFACE) || (inputMode == INPUT_MODE_BITMAP);
        int colorFormat = useSurfaceInternally ? CodecCapabilities.COLOR_FormatSurface :
                CodecCapabilities.COLOR_FormatYUV420Flexible;

        mWidth = width;
        mHeight = height;

        int gridWidth, gridHeight, gridRows, gridCols;

        if (useGrid) {
            gridWidth = GRID_WIDTH;
            gridHeight = GRID_HEIGHT;
            gridRows = (height + GRID_HEIGHT - 1) / GRID_HEIGHT;
            gridCols = (width + GRID_WIDTH - 1) / GRID_WIDTH;
        } else {
            gridWidth = mWidth;
            gridHeight = mHeight;
            gridRows = 1;
            gridCols = 1;
        }

        MediaFormat codecFormat;
        if (useHeicEncoder) {
            codecFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC, mWidth, mHeight);
        } else {
            codecFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_HEVC, gridWidth, gridHeight);
        }

        if (useGrid) {
            codecFormat.setInteger(MediaFormat.KEY_TILE_WIDTH, gridWidth);
            codecFormat.setInteger(MediaFormat.KEY_TILE_HEIGHT, gridHeight);
            codecFormat.setInteger(MediaFormat.KEY_GRID_COLUMNS, gridCols);
            codecFormat.setInteger(MediaFormat.KEY_GRID_ROWS, gridRows);
        }

        if (useHeicEncoder) {
            mGridWidth = width;
            mGridHeight = height;
            mGridRows = 1;
            mGridCols = 1;
        } else {
            mGridWidth = gridWidth;
            mGridHeight = gridHeight;
            mGridRows = gridRows;
            mGridCols = gridCols;
        }
        mNumTiles = mGridRows * mGridCols;

        codecFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        codecFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        codecFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mNumTiles);
        codecFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, mNumTiles * 30);

        MediaCodecInfo.EncoderCapabilities encoderCaps = caps.getEncoderCapabilities();

        if (encoderCaps.isBitrateModeSupported(
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)) {
            Log.d(TAG, "Setting bitrate mode to constant quality");
            Range<Integer> qualityRange = encoderCaps.getQualityRange();
            Log.d(TAG, "Quality range: " + qualityRange);
            codecFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
            codecFormat.setInteger(MediaFormat.KEY_QUALITY, (int) (qualityRange.getLower() +
                            (qualityRange.getUpper() - qualityRange.getLower()) * quality / 100.0));
        } else {
            if (encoderCaps.isBitrateModeSupported(
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)) {
                Log.d(TAG, "Setting bitrate mode to constant bitrate");
                codecFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            } else { // assume VBR
                Log.d(TAG, "Setting bitrate mode to variable bitrate");
                codecFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            }
            // Calculate the bitrate based on image dimension, max compression ratio and quality.
            // Note that we set the frame rate to the number of tiles, so the bitrate would be the
            // intended bits for one image.
            int bitrate = (int) (width * height * 1.5 * 8 * MAX_COMPRESS_RATIO * quality / 100.0f);
            codecFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        }

        mEncoder.setCallback(new EncoderCallback(), mHandler);
        mEncoder.configure(codecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        if (useSurfaceInternally) {
            mEncoderSurface = mEncoder.createInputSurface();

            boolean copyTiles = (mNumTiles > 1);
            mEOSTracker = new SurfaceEOSTracker(copyTiles);

            if (inputMode == INPUT_MODE_SURFACE) {
                if (copyTiles) {
                    mEncoderEglSurface = new EglWindowSurface(mEncoderSurface);
                    mEncoderEglSurface.makeCurrent();

                    mRectBlt = new EglRectBlt(
                            new Texture2dProgram((inputMode == INPUT_MODE_BITMAP)
                                    ? Texture2dProgram.TEXTURE_2D
                                    : Texture2dProgram.TEXTURE_EXT),
                            mWidth, mHeight);

                    mTextureId = mRectBlt.createTextureObject();

                    if (inputMode == INPUT_MODE_SURFACE) {
                        // use single buffer mode to block on input
                        mInputTexture = new SurfaceTexture(mTextureId, true);
                        mInputTexture.setOnFrameAvailableListener(this);
                        mInputTexture.setDefaultBufferSize(mWidth, mHeight);
                        mInputSurface = new Surface(mInputTexture);
                    }

                    // make uncurrent since onFrameAvailable could be called on arbituray thread.
                    // making the context current on a different thread will cause error.
                    mEncoderEglSurface.makeUnCurrent();
                } else {
                    mInputSurface = mEncoderSurface;
                }
            }
        } else {
            for (int i = 0; i < INPUT_BUFFER_POOL_SIZE; i++) {
                mEmptyBuffers.add(ByteBuffer.allocateDirect(mWidth * mHeight * 3 / 2));
            }
        }

        mDstRect = new Rect(0, 0, mGridWidth, mGridHeight);
        mSrcRect = new Rect();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            if (mEncoderEglSurface == null) {
                return;
            }

            mEncoderEglSurface.makeCurrent();

            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(mTmpMatrix);

            long timestampNs = surfaceTexture.getTimestamp();

            if (DEBUG) Log.d(TAG, "onFrameAvailable: timestampUs " + (timestampNs / 1000));

            boolean takeFrame = mEOSTracker.updateLastInputAndEncoderTime(timestampNs,
                    computePresentationTime(mInputIndex + mNumTiles - 1));

            if (takeFrame) {
                // Copies from surface texture to encoder inputs using GL.
                GLES20.glViewport(0, 0, mGridWidth, mGridHeight);

                for (int row = 0; row < mGridRows; row++) {
                    for (int col = 0; col < mGridCols; col++) {
                        int left = col * mGridWidth;
                        int top = row * mGridHeight;
                        mSrcRect.set(left, top, left + mGridWidth, top + mGridHeight);
                        mRectBlt.copyRect(mTextureId, mTmpMatrix, mSrcRect);
                        mEncoderEglSurface.setPresentationTime(
                                1000 * computePresentationTime(mInputIndex++));
                        mEncoderEglSurface.swapBuffers();
                    }
                }
            }

            surfaceTexture.releaseTexImage();

            // make uncurrent since the onFrameAvailable could be called on arbituray thread.
            // making the context current on a different thread will cause error.
            mEncoderEglSurface.makeUnCurrent();
        }
    }

    /**
     * Start the encoding process.
     */
    public void start() {
        mEncoder.start();
    }

    /**
     * Add one YUV buffer to be encoded. This might block if the encoder can't process the input
     * buffers fast enough.
     *
     * After the call returns, the client can reuse the data array.
     *
     * @param format The YUV format as defined in {@link android.graphics.ImageFormat}, currently
     *               only support YUV_420_888.
     *
     * @param data byte array containing the YUV data. If the format has more than one planes,
     *             they must be concatenated.
     */
    public void addYuvBuffer(int format, @NonNull byte[] data) {
        if (mInputMode != INPUT_MODE_BUFFER) {
            throw new IllegalStateException(
                    "addYuvBuffer is only allowed in buffer input mode");
        }
        if (data == null || data.length != mWidth * mHeight * 3 / 2) {
            throw new IllegalArgumentException("invalid data");
        }
        addYuvBufferInternal(data);
    }

    /**
     * Retrieves the input surface for encoding.
     *
     * Will only return valid value if configured to use surface input.
     */
    public @NonNull Surface getInputSurface() {
        if (mInputMode != INPUT_MODE_SURFACE) {
            throw new IllegalStateException(
                    "getInputSurface is only allowed in surface input mode");
        }
        return mInputSurface;
    }

    /**
     * Sets the timestamp (in nano seconds) of the last input frame to encode. Frames with
     * timestamps larger than the specified value will not be encoded. However, if a frame
     * already started encoding when this is set, all tiles within that frame will be encoded.
     *
     * This method only applies when surface is used.
     */
    public void setEndOfInputStreamTimestamp(long timestampNs) {
        if (mInputMode != INPUT_MODE_SURFACE) {
            throw new IllegalStateException(
                    "setEndOfInputStreamTimestamp is only allowed in surface input mode");
        }
        if (mEOSTracker != null) {
            mEOSTracker.updateInputEOSTime(timestampNs);
        }
    }

    /**
     * Adds one bitmap to be encoded.
     */
    public void addBitmap(@NonNull Bitmap bitmap) {
        if (mInputMode != INPUT_MODE_BITMAP) {
            throw new IllegalStateException("addBitmap is only allowed in bitmap input mode");
        }

        boolean takeFrame = mEOSTracker.updateLastInputAndEncoderTime(
                computePresentationTime(mInputIndex),
                computePresentationTime(mInputIndex + mNumTiles - 1));

        if (!takeFrame) return;

        synchronized (this) {
            for (int row = 0; row < mGridRows; row++) {
                for (int col = 0; col < mGridCols; col++) {
                    int left = col * mGridWidth;
                    int top = row * mGridHeight;
                    mSrcRect.set(left, top, left + mGridWidth, top + mGridHeight);
                    Canvas canvas = mEncoderSurface.lockCanvas(null);
                    canvas.drawBitmap(bitmap, mSrcRect, mDstRect, null);
                    mEncoderSurface.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    /**
     * Sends input EOS to the encoder. Result will be notified asynchronously via
     * {@link Callback#onComplete(HeifEncoder)} if encoder reaches EOS without error, or
     * {@link Callback#onError(HeifEncoder, CodecException)} otherwise.
     */
    public void stopAsync() {
        if (mInputMode == INPUT_MODE_BITMAP) {
            // here we simply set the EOS timestamp to 0, so that the cut off will be the last
            // bitmap ever added.
            mEOSTracker.updateInputEOSTime(0);
        } else if (mInputMode == INPUT_MODE_BUFFER) {
            addYuvBufferInternal(null);
        }
    }

    /**
     * Generates the presentation time for input frame N, in microseconds.
     * The timestamp advances 1 sec for every whole frame.
     */
    private long computePresentationTime(int frameIndex) {
        return 132 + (long)frameIndex * 1000000 / mNumTiles;
    }

    /**
     * Obtains one empty input buffer and copies the data into it. Before input
     * EOS is sent, this would block until the data is copied. After input EOS
     * is sent, this would return immediately.
     */
    private void addYuvBufferInternal(@Nullable byte[] data) {
        ByteBuffer buffer = acquireEmptyBuffer();
        if (buffer == null) {
            return;
        }
        buffer.clear();
        if (data != null) {
            buffer.put(data);
        }
        buffer.flip();
        synchronized (mFilledBuffers) {
            mFilledBuffers.add(buffer);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                maybeCopyOneTileYUV();
            }
        });
    }

    /**
     * Routine to copy one tile if we have both input and codec buffer available.
     *
     * Must be called on the handler looper that also handles the MediaCodec callback.
     */
    private void maybeCopyOneTileYUV() {
        ByteBuffer currentBuffer;
        while ((currentBuffer = getCurrentBuffer()) != null && !mCodecInputBuffers.isEmpty()) {
            int index = mCodecInputBuffers.remove(0);

            // 0-length input means EOS.
            boolean inputEOS = (mInputIndex % mNumTiles == 0) && (currentBuffer.remaining() == 0);

            if (!inputEOS) {
                Image image = mEncoder.getInputImage(index);
                int left = mGridWidth * (mInputIndex % mGridCols);
                int top = mGridHeight * (mInputIndex / mGridCols % mGridRows);
                mSrcRect.set(left, top, left + mGridWidth, top + mGridHeight);
                copyOneTileYUV(currentBuffer, image, mWidth, mHeight, mSrcRect, mDstRect);
            }

            mEncoder.queueInputBuffer(index, 0,
                    inputEOS ? 0 : mEncoder.getInputBuffer(index).capacity(),
                    computePresentationTime(mInputIndex++),
                    inputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

            if (inputEOS || mInputIndex % mNumTiles == 0) {
                returnEmptyBufferAndNotify(inputEOS);
            }
        }
    }

    /**
     * Copies from a rect from src buffer to dst image.
     * TOOD: This will be replaced by JNI.
     */
    private static void copyOneTileYUV(
            ByteBuffer srcBuffer, Image dstImage,
            int srcWidth, int srcHeight,
            Rect srcRect, Rect dstRect) {
        if (srcRect.width() != dstRect.width() || srcRect.height() != dstRect.height()) {
            throw new IllegalArgumentException("src and dst rect size are different!");
        }
        if (srcWidth % 2 != 0      || srcHeight % 2 != 0      ||
                srcRect.left % 2 != 0  || srcRect.top % 2 != 0    ||
                srcRect.right % 2 != 0 || srcRect.bottom % 2 != 0 ||
                dstRect.left % 2 != 0  || dstRect.top % 2 != 0    ||
                dstRect.right % 2 != 0 || dstRect.bottom % 2 != 0) {
            throw new IllegalArgumentException("src or dst are not aligned!");
        }

        Image.Plane[] planes = dstImage.getPlanes();
        for (int n = 0; n < planes.length; n++) {
            ByteBuffer dstBuffer = planes[n].getBuffer();
            int colStride = planes[n].getPixelStride();
            int copyWidth = Math.min(srcRect.width(), srcWidth - srcRect.left);
            int copyHeight = Math.min(srcRect.height(), srcHeight - srcRect.top);
            int srcPlanePos = 0, div = 1;
            if (n > 0) {
                div = 2;
                srcPlanePos = srcWidth * srcHeight * (n + 3) / 4;
            }
            for (int i = 0; i < copyHeight / div; i++) {
                srcBuffer.position(srcPlanePos +
                        (i + srcRect.top / div) * srcWidth / div + srcRect.left / div);
                dstBuffer.position((i + dstRect.top / div) * planes[n].getRowStride()
                        + dstRect.left * colStride / div);

                for (int j = 0; j < copyWidth / div; j++) {
                    dstBuffer.put(srcBuffer.get());
                    if (colStride > 1 && j != copyWidth / div - 1) {
                        dstBuffer.position(dstBuffer.position() + colStride - 1);
                    }
                }
            }
        }
    }

    private ByteBuffer acquireEmptyBuffer() {
        synchronized (mEmptyBuffers) {
            // wait for an empty input buffer first
            while (!mInputEOS && mEmptyBuffers.isEmpty()) {
                try {
                    mEmptyBuffers.wait();
                } catch (InterruptedException e) {}
            }

            // if already EOS, return null to stop further encoding.
            return mInputEOS ? null : mEmptyBuffers.remove(0);
        }
    }

    /**
     * Routine to get the current input buffer to copy from.
     * Only called on callback handler thread.
     */
    private ByteBuffer getCurrentBuffer() {
        if (!mInputEOS && mCurrentBuffer == null) {
            synchronized (mFilledBuffers) {
                mCurrentBuffer = mFilledBuffers.isEmpty() ?
                        null : mFilledBuffers.remove(0);
            }
        }
        return mInputEOS ? null : mCurrentBuffer;
    }

    /**
     * Routine to put the consumed input buffer back into the empty buffer pool.
     * Only called on callback handler thread.
     */
    private void returnEmptyBufferAndNotify(boolean inputEOS) {
        synchronized (mEmptyBuffers) {
            mInputEOS |= inputEOS;
            mEmptyBuffers.add(mCurrentBuffer);
            mEmptyBuffers.notifyAll();
        }
        mCurrentBuffer = null;
    }

    /**
     * Routine to release all resources. Must be run on the same looper that
     * handles the MediaCodec callbacks.
     */
    private void stopInternal() {
        if (DEBUG) Log.d(TAG, "stopInternal");

        // after start, mEncoder is only accessed on handler, so no need to sync
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }

        // unblock the addBuffer() if we're tearing down before EOS is sent.
        synchronized (mEmptyBuffers) {
            mInputEOS = true;
            mEmptyBuffers.notifyAll();
        }

        synchronized(this) {
            if (mRectBlt != null) {
                mRectBlt.release(false);
                mRectBlt = null;
            }

            if (mEncoderEglSurface != null) {
                // Note that this frees mEncoderSurface too. If mEncoderEglSurface is not
                // there, client is responsible to release the input surface it got from us,
                // we don't release mEncoderSurface here.
                mEncoderEglSurface.release();
                mEncoderEglSurface = null;
            }

            if (mInputTexture != null) {
                mInputTexture.release();
                mInputTexture = null;
            }
        }
    }

    /**
     * This class handles EOS for surface or bitmap inputs.
     *
     * When encoding from surface or bitmap, we can't call {@link MediaCodec#signalEndOfInputStream()}
     * immediately after input is drawn, since this could drop all pending frames in the
     * buffer queue. When there are tiles, this could leave us a partially encoded image.
     *
     * So here we track the EOS status by timestamps, and only signal EOS to the encoder
     * when we collected all images we need.
     *
     * Since this is updated from multiple threads ({@link #setEndOfInputStreamTimestamp(long)},
     * {@link EncoderCallback#onOutputBufferAvailable(MediaCodec, int, BufferInfo)},
     * {@link #addBitmap(Bitmap)} and {@link #onFrameAvailable(SurfaceTexture)}), it must be fully
     * synchronized.
     *
     * Note that when buffer input is used, the EOS flag is set in
     * {@link EncoderCallback#onInputBufferAvailable(MediaCodec, int)} and this class is not used.
     */
    private class SurfaceEOSTracker {
        private static final boolean DEBUG_EOS = false;

        final boolean mCopyTiles;
        long mInputEOSTimeNs = -1;
        long mLastInputTimeNs = -1;
        long mEncoderEOSTimeUs = -1;
        long mLastEncoderTimeUs = -1;
        long mLastOutputTimeUs = -1;
        boolean mSignaled;

        SurfaceEOSTracker(boolean copyTiles) {
            mCopyTiles = copyTiles;
        }

        synchronized void updateInputEOSTime(long timestampNs) {
            if (DEBUG_EOS) Log.d(TAG, "updateInputEOSTime: " + timestampNs);

            if (mCopyTiles) {
                if (mInputEOSTimeNs < 0) {
                    mInputEOSTimeNs = timestampNs;
                }
            } else {
                if (mEncoderEOSTimeUs < 0) {
                    mEncoderEOSTimeUs = timestampNs / 1000;
                }
            }
            updateEOSLocked();
        }

        synchronized boolean updateLastInputAndEncoderTime(long inputTimeNs, long encoderTimeUs) {
            if (DEBUG_EOS) Log.d(TAG,
                    "updateLastInputAndEncoderTime: " + inputTimeNs + ", " + encoderTimeUs);

            boolean shouldTakeFrame = mInputEOSTimeNs < 0 || inputTimeNs <= mInputEOSTimeNs;
            if (shouldTakeFrame) {
                mLastEncoderTimeUs = encoderTimeUs;
            }
            mLastInputTimeNs = inputTimeNs;
            updateEOSLocked();
            return shouldTakeFrame;
        }

        synchronized void updateLastOutputTime(long outputTimeUs) {
            if (DEBUG_EOS) Log.d(TAG, "updateLastOutputTime: " + outputTimeUs);

            mLastOutputTimeUs = outputTimeUs;
            updateEOSLocked();
        }

        private void updateEOSLocked() {
            if (mSignaled) {
                return;
            }
            if (mEncoderEOSTimeUs < 0) {
                if (mInputEOSTimeNs >= 0 && mLastInputTimeNs >= mInputEOSTimeNs) {
                    if (mLastEncoderTimeUs < 0) {
                        doSignalEOSLocked();
                        return;
                    }
                    // mEncoderEOSTimeUs tracks the timestamp of the last output buffer we
                    // will wait for. When that buffer arrives, encoder will be signalled EOS.
                    mEncoderEOSTimeUs = mLastEncoderTimeUs;
                    if (DEBUG_EOS) Log.d(TAG,
                            "updateEOSLocked: mEncoderEOSTimeUs " + mEncoderEOSTimeUs);
                }
            }
            if (mEncoderEOSTimeUs >= 0 && mEncoderEOSTimeUs <= mLastOutputTimeUs) {
                doSignalEOSLocked();
            }
        }

        private void doSignalEOSLocked() {
            if (DEBUG_EOS) Log.d(TAG, "doSignalEOSLocked");

            mHandler.post(new Runnable() {
                @Override public void run() {
                    if (mEncoder != null) {
                        mEncoder.signalEndOfInputStream();
                    }
                }
            });

            mSignaled = true;
        }
    }

    /**
     * MediaCodec callback for HEVC encoding.
     */
    private class EncoderCallback extends MediaCodec.Callback {
        private boolean mOutputEOS;

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            if (codec != mEncoder) return;

            if (DEBUG) Log.d(TAG, "onOutputFormatChanged: " + format);

            if (!MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC.equals(
                    format.getString(MediaFormat.KEY_MIME))) {
                format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC);
                format.setInteger(MediaFormat.KEY_WIDTH, mWidth);
                format.setInteger(MediaFormat.KEY_HEIGHT, mHeight);

                if (mNumTiles > 1) {
                    format.setInteger(MediaFormat.KEY_TILE_WIDTH, mGridWidth);
                    format.setInteger(MediaFormat.KEY_TILE_HEIGHT, mGridHeight);
                    format.setInteger(MediaFormat.KEY_GRID_ROWS, mGridRows);
                    format.setInteger(MediaFormat.KEY_GRID_COLUMNS, mGridCols);
                }
            }

            mCallback.onOutputFormatChanged(HeifEncoder.this, format);
        }

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            if (codec != mEncoder || mInputEOS) return;

            if (DEBUG) Log.d(TAG, "onInputBufferAvailable: " + index);
            mCodecInputBuffers.add(index);
            maybeCopyOneTileYUV();
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, BufferInfo info) {
            if (codec != mEncoder || mOutputEOS) return;

            if (DEBUG) {
                Log.d(TAG, "onOutputBufferAvailable: " + index
                        + ", time " + info.presentationTimeUs
                        + ", size " + info.size
                        + ", flags " + info.flags);
            }

            if ((info.size > 0) && ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0)) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(index);

                // reset position as addBuffer() modifies it
                outputBuffer.position(info.offset);
                outputBuffer.limit(info.offset + info.size);

                if (mEOSTracker != null) {
                    mEOSTracker.updateLastOutputTime(info.presentationTimeUs);
                }

                mCallback.onDrainOutputBuffer(HeifEncoder.this, outputBuffer);
            }

            mOutputEOS |= ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);

            codec.releaseOutputBuffer(index, false);

            if (mOutputEOS) {
                stopAndNotify(null);
            }
        }

        @Override
        public void onError(MediaCodec codec, CodecException e) {
            if (codec != mEncoder) return;

            Log.e(TAG, "onError: " + e);
            stopAndNotify(e);
        }

        private void stopAndNotify(@Nullable CodecException e) {
            stopInternal();
            if (e == null) {
                mCallback.onComplete(HeifEncoder.this);
            } else {
                mCallback.onError(HeifEncoder.this, e);
            }
        }
    }

    @Override
    public void close() {
        // unblock the addBuffer() if we're tearing down before EOS is sent.
        synchronized (mEmptyBuffers) {
            mInputEOS = true;
            mEmptyBuffers.notifyAll();
        }

        mHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                stopInternal();
            }
        });
    }
}
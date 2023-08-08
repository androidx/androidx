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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.IOException;

/**
 * This class encodes images into HEIF-compatible samples using AV1 encoder.
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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class AvifEncoder extends EncoderBase {
    private static final String TAG = "AvifEncoder";
    private static final boolean DEBUG = false;

    protected static final int GRID_WIDTH = 512;
    protected static final int GRID_HEIGHT = 512;
    // Block size for AV1 encoder
    protected static final int ENCODING_BLOCK_SIZE = 64;
    protected static final double MAX_COMPRESS_RATIO = 0.25f;

    private static final MediaCodecList sMCL =
        new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    /**
     * Configure the avif encoding session. Should only be called once.
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
     * @param cb The callback to receive various messages from the avif encoder.
     */
    public AvifEncoder(int width, int height, boolean useGrid,
            int quality, @InputMode int inputMode,
            @Nullable Handler handler, @NonNull Callback cb,
            boolean useBitDepth10) throws IOException {
        super("AVIF", width, height, useGrid, quality, inputMode, handler, cb, useBitDepth10);
        mEncoder.setCallback(new Av1EncoderCallback(), mHandler);
        finishSettingUpEncoder(useBitDepth10);
    }

    protected static String findAv1Fallback() {
        String av1 = null; // first AV1 encoder
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (!info.isEncoder()) {
                continue;
            }
            MediaCodecInfo.CodecCapabilities caps = null;
            try {
                caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AV1);
            } catch (IllegalArgumentException e) { // mime is not supported
                continue;
            }
            if (!caps.getVideoCapabilities().isSizeSupported(GRID_WIDTH, GRID_HEIGHT)) {
                continue;
            }
            if (caps.getEncoderCapabilities().isBitrateModeSupported(
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)) {
                // Encoder that supports CQ mode is preferred over others,
                // return the first encoder that supports CQ mode.
                // (No need to check if it's hw based, it's already listed in
                // order of preference.)
                return info.getName();
            }
            if (av1 == null) {
                av1 = info.getName();
            }
        }
        // If no encoders support CQ, return the first AV1 encoder.
        return av1;
    }

    /**
     * MediaCodec callback for AV1 encoding.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    protected class Av1EncoderCallback extends EncoderCallback {
        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            if (codec != mEncoder) return;

            if (DEBUG) Log.d(TAG, "onOutputFormatChanged: " + format);

            // TODO(b/252835975) replace "image/avif" with  MIMETYPE_IMAGE_AVIF.
            if (!format.getString(MediaFormat.KEY_MIME).equals("image/avif")) {
                format.setString(MediaFormat.KEY_MIME, "image/avif");
                format.setInteger(MediaFormat.KEY_WIDTH, mWidth);
                format.setInteger(MediaFormat.KEY_HEIGHT, mHeight);

                if (mUseGrid) {
                    format.setInteger(MediaFormat.KEY_TILE_WIDTH, mGridWidth);
                    format.setInteger(MediaFormat.KEY_TILE_HEIGHT, mGridHeight);
                    format.setInteger(MediaFormat.KEY_GRID_ROWS, mGridRows);
                    format.setInteger(MediaFormat.KEY_GRID_COLUMNS, mGridCols);
                }
            }

            mCallback.onOutputFormatChanged(AvifEncoder.this, format);
        }
    }
}
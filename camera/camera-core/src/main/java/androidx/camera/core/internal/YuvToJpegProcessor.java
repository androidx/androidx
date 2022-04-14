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

package androidx.camera.core.internal;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageWriter;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.core.impl.utils.ExifData;
import androidx.camera.core.impl.utils.ExifOutputStream;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.compat.ImageWriterCompat;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * A CaptureProcessor which produces JPEGs from input YUV images.
 */
@RequiresApi(26)
public class YuvToJpegProcessor implements CaptureProcessor {
    private static final String TAG = "YuvToJpegProcessor";

    private static final Rect UNINITIALIZED_RECT = new Rect(0, 0, 0, 0);

    private final int mMaxImages;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    @IntRange(from = 0, to = 100)
    private int mQuality;
    @GuardedBy("mLock")
    @ImageOutputConfig.RotationDegreesValue
    private int mRotationDegrees = 0;

    @GuardedBy("mLock")
    private boolean mClosed = false;
    @GuardedBy("mLock")
    private int mProcessingImages = 0;
    @GuardedBy("mLock")
    private ImageWriter mImageWriter;
    @GuardedBy("mLock")
    private Rect mImageRect = UNINITIALIZED_RECT;

    @GuardedBy("mLock")
    CallbackToFutureAdapter.Completer<Void> mCloseCompleter;
    @GuardedBy("mLock")
    private ListenableFuture<Void> mCloseFuture;

    public YuvToJpegProcessor(@IntRange(from = 0, to = 100) int quality, int maxImages) {
        mQuality = quality;
        mMaxImages = maxImages;
    }

    /**
     * Sets the compression quality for the output JPEG image.
     */
    public void setJpegQuality(@IntRange(from = 0, to = 100) int quality) {
        synchronized (mLock) {
            mQuality = quality;
        }
    }

    /**
     * Sets the rotation degrees value of the output images.
     *
     * @param rotationDegrees The rotation in degrees which will be a value in {0, 90, 180, 270}.
     */
    public void setRotationDegrees(@ImageOutputConfig.RotationDegreesValue int rotationDegrees) {
        synchronized (mLock) {
            mRotationDegrees = rotationDegrees;
        }
    }

    @Override
    public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
        Preconditions.checkState(imageFormat == ImageFormat.JPEG, "YuvToJpegProcessor only "
                + "supports JPEG output format.");
        synchronized (mLock) {
            if (!mClosed) {
                if (mImageWriter != null) {
                    throw new IllegalStateException("Output surface already set.");
                }
                mImageWriter = ImageWriterCompat.newInstance(surface, mMaxImages, imageFormat);
            } else {
                Logger.w(TAG, "Cannot set output surface. Processor is closed.");
            }
        }
    }

    @Override
    public void process(@NonNull ImageProxyBundle bundle) {
        List<Integer> ids = bundle.getCaptureIds();
        Preconditions.checkArgument(ids.size() == 1,
                "Processing image bundle have single capture id, but found " + ids.size());

        ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(ids.get(0));
        Preconditions.checkArgument(imageProxyListenableFuture.isDone());

        ImageWriter imageWriter;
        Rect imageRect;
        boolean processing;
        int quality;
        int rotationDegrees;
        synchronized (mLock) {
            imageWriter = mImageWriter;
            processing = !mClosed;
            imageRect = mImageRect;
            if (processing) {
                mProcessingImages++;
            }
            quality = mQuality;
            rotationDegrees = mRotationDegrees;
        }

        ImageProxy imageProxy = null;
        Image jpegImage = null;
        try {
            imageProxy = imageProxyListenableFuture.get();
            if (!processing) {
                Logger.w(TAG, "Image enqueued for processing on closed processor.");
                imageProxy.close();
                imageProxy = null;
                return;
            }

            jpegImage = imageWriter.dequeueInputImage();

            imageProxy = imageProxyListenableFuture.get();
            Preconditions.checkState(imageProxy.getFormat() == ImageFormat.YUV_420_888,
                    "Input image is not expected YUV_420_888 image format");
            byte[] yuvBytes = ImageUtil.yuv_420_888toNv21(imageProxy);

            YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, imageProxy.getWidth(),
                    imageProxy.getHeight(), null);

            ByteBuffer jpegBuf = jpegImage.getPlanes()[0].getBuffer();
            int initialPos = jpegBuf.position();
            OutputStream os = new ExifOutputStream(new ByteBufferOutputStream(jpegBuf),
                    getExifData(imageProxy, rotationDegrees));
            yuvImage.compressToJpeg(imageRect, quality, os);

            // Input can now be closed.
            imageProxy.close();
            imageProxy = null;

            // Set limits on jpeg buffer and rewind
            jpegBuf.limit(jpegBuf.position());
            jpegBuf.position(initialPos);

            // Enqueue the completed jpeg image
            imageWriter.queueInputImage(jpegImage);
            jpegImage = null;
        } catch (Exception e) {
            // InterruptedException, ExecutionException and EOFException might be caught here.
            //
            // InterruptedException should not be possible here since
            // imageProxyListenableFuture.isDone() returned true, but we have to handle the
            // exception case so bundle it with ExecutionException.
            //
            // EOFException might happen if the compressed JPEG data size exceeds the byte buffer
            // size of the output image reader.
            if (processing) {
                Logger.e(TAG, "Failed to process YUV -> JPEG", e);
                // Something went wrong attempting to retrieve ImageProxy. Enqueue an invalid buffer
                // to make sure the downstream isn't blocked.
                jpegImage = imageWriter.dequeueInputImage();
                ByteBuffer jpegBuf = jpegImage.getPlanes()[0].getBuffer();
                jpegBuf.rewind();
                jpegBuf.limit(0);
                imageWriter.queueInputImage(jpegImage);
            }
        } finally {
            boolean shouldCloseImageWriter;
            synchronized (mLock) {
                // Note: order of condition is important here due to short circuit of &&
                shouldCloseImageWriter = processing && (mProcessingImages-- == 0) && mClosed;
            }

            // Fallback in case something went wrong during processing.
            if (jpegImage != null) {
                jpegImage.close();
            }
            if (imageProxy != null) {
                imageProxy.close();
            }

            if (shouldCloseImageWriter) {
                imageWriter.close();
                Logger.d(TAG, "Closed after completion of last image processed.");

                synchronized (mLock) {
                    if (mCloseCompleter != null) {
                        // Notify listeners of close
                        mCloseCompleter.set(null);
                    }
                }
            }
        }
    }

    /**
     * Closes the YuvToJpegProcessor so that no more processing will occur.
     *
     * This should only be called once no more images will be produced for processing. Otherwise
     * the images may not be propagated to the output surface and the pipeline could stall.
     */
    public void close() {
        synchronized (mLock) {
            if (!mClosed) {
                mClosed = true;
                // Close the ImageWriter if no images are currently processing. Otherwise the
                // ImageWriter will be closed once the last image is closed.
                if (mProcessingImages == 0 && mImageWriter != null) {
                    Logger.d(TAG, "No processing in progress. Closing immediately.");
                    mImageWriter.close();

                    if (mCloseCompleter != null) {
                        mCloseCompleter.set(null);
                    }
                } else {
                    Logger.d(TAG, "close() called while processing. Will close after completion.");
                }
            }
        }
    }

    /**
     * Returns a future that will complete when the YuvToJpegProcessor is actually closed.
     *
     * @return A future that signals when the YuvToJpegProcessor is actually closed
     * (after all processing). Cancelling this future has no effect.
     */
    @NonNull
    public ListenableFuture<Void> getCloseFuture() {
        ListenableFuture<Void> closeFuture;
        synchronized (mLock) {
            if (mClosed && mProcessingImages == 0) {
                // Everything should be closed. Return immediate future.
                closeFuture = Futures.immediateFuture(null);
            } else {
                if (mCloseFuture == null) {
                    mCloseFuture = CallbackToFutureAdapter.getFuture(completer -> {
                        // Should already be locked, but lock again to satisfy linter.
                        synchronized (mLock) {
                            mCloseCompleter = completer;
                        }
                        return "YuvToJpegProcessor-close";
                    });
                }
                closeFuture = Futures.nonCancellationPropagating(mCloseFuture);
            }
        }
        return closeFuture;
    }

    @Override
    public void onResolutionUpdate(@NonNull Size size) {
        synchronized (mLock) {
            mImageRect = new Rect(0, 0, size.getWidth(), size.getHeight());
        }
    }

    @NonNull
    private static ExifData getExifData(@NonNull ImageProxy imageProxy,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees) {
        ExifData.Builder builder = ExifData.builderForDevice();
        imageProxy.getImageInfo().populateExifData(builder);

        // Overwrites the orientation degrees value of the output image because the capture
        // results might not have correct value when capturing image in YUV_420_888 format. See
        // b/204375890.
        builder.setOrientationDegrees(rotationDegrees);

        return builder.setImageWidth(imageProxy.getWidth())
                .setImageHeight(imageProxy.getHeight())
                .build();
    }

    private static final class ByteBufferOutputStream extends OutputStream {

        private final ByteBuffer mByteBuffer;

        ByteBufferOutputStream(@NonNull ByteBuffer buf) {
            mByteBuffer = buf;
        }

        @Override
        public void write(int b) throws IOException {
            if (!mByteBuffer.hasRemaining()) {
                throw new EOFException("Output ByteBuffer has no bytes remaining.");
            }

            mByteBuffer.put((byte) b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0)
                    || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            } else if (mByteBuffer.remaining() < len) {
                throw new EOFException("Output ByteBuffer has insufficient bytes remaining.");
            }

            mByteBuffer.put(b, off, len);
        }
    }
}

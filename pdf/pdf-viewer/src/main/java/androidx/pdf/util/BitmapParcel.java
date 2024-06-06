/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility to share {@link Bitmap}s across processes using a {@link android.os.Parcelable} reference
 * that can fit safely in an Intent.
 *
 * <p>A {@link BitmapParcel} wraps a {@link Bitmap} instance and exposes an output file descriptor
 * that can be used to fill in the bytes of the wrapped bitmap from any process.
 *
 * <p>Uses a piped file descriptor, and natively reads and copies bytes from the source end into the
 * {@link Bitmap}'s byte buffer. Runs on a new Thread.
 *
 * <p>Note: Only one-way transfers are implemented (write into a bitmap from any source).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BitmapParcel {

    private static final String TAG = BitmapParcel.class.getSimpleName();
    private final Bitmap mBitmap;
    private final Timer mTimer = Timer.start();
    private CountDownLatch mCountDownLatch;

    /**
     * Creates a BitmapParcel that allows writing bytes into the given {@link Bitmap}.
     *
     * @param bitmap the destination bitmap: its contents will be replaced by what is sent on the
     *               fd.
     */
    public BitmapParcel(@NonNull Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    /** Opens a file descriptor that will write into the wrapped bitmap. */
    @Nullable
    public ParcelFileDescriptor openOutputFd() {
        ParcelFileDescriptor[] pipe;
        try {
            // TODO: StrictMode - close() is not explicitly called.
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            return null;
        }
        ParcelFileDescriptor source = pipe[0];
        ParcelFileDescriptor sink = pipe[1];
        receiveAsync(source);
        return sink;
    }

    /** Terminates any running copy and close all resources. */
    public void close() {
        if (mCountDownLatch != null) {
            boolean timedOut = false;
            try {
                timedOut = !mCountDownLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /** Receives the bitmap's bytes from a file descriptor. Runs on a new thread. */
    private void receiveAsync(final ParcelFileDescriptor source) {
        mCountDownLatch = new CountDownLatch(1);
        new Thread(
                () -> {
                    Timer t = Timer.start();
                    receiveBitmap(mBitmap, source);
                    mCountDownLatch.countDown();
                },
                "Pico-AsyncPipedFdNative.receiveAsync")
                .start();
    }

    /**
     * Reads bytes from a file descriptor into a {@link Bitmap}, using a native memcpy.
     *
     * @param bitmap   A bitmap whose pixels to populate.
     * @param sourceFd The source file descriptor.
     */
    protected void receiveBitmap(@NonNull Bitmap bitmap, @NonNull ParcelFileDescriptor sourceFd) {
        readIntoBitmap(bitmap, sourceFd.detachFd());
    }

    /**
     * Reads bytes from the given file descriptor and fill in the Bitmap instance.
     *
     * @param bitmap   A bitmap whose pixels to populate.
     * @param sourceFd The source file descriptor. Will be closed after transfer.
     */
    private static native boolean readIntoBitmap(Bitmap bitmap, int sourceFd);

    /**
     *
     */
    public static void loadNdkLib() {
        System.loadLibrary("bitmapParcel");
    }
}

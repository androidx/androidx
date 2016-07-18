/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.print;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import android.net.Uri;

import java.io.FileNotFoundException;

/**
 * Helper for printing bitmaps.
 */
public final class PrintHelper {
    /**
     * image will be scaled but leave white space
     */
    public static final int SCALE_MODE_FIT = 1;

    /**
     * image will fill the paper and be cropped (default)
     */
    public static final int SCALE_MODE_FILL = 2;

    /**
     * this is a black and white image
     */
    public static final int COLOR_MODE_MONOCHROME = 1;

    /**
     * this is a color image (default)
     */
    public static final int COLOR_MODE_COLOR = 2;

    /**
     * Print the image in landscape orientation (default).
     */
    public static final int ORIENTATION_LANDSCAPE = 1;

    /**
     * Print the image in  portrait orientation.
     */
    public static final int ORIENTATION_PORTRAIT = 2;

    /**
     * Callback for observing when a print operation is completed.
     * When print is finished either the system acquired the
     * document to print or printing was cancelled.
     */
    public interface OnPrintFinishCallback {

        /**
         * Called when a print operation is finished.
         */
        public void onFinish();
    }

    PrintHelperVersionImpl mImpl;

    /**
     * Gets whether the system supports printing.
     *
     * @return True if printing is supported.
     */
    public static boolean systemSupportsPrint() {
        if (Build.VERSION.SDK_INT >= 19) {
            // Supported on Android 4.4 or later.
            return true;
        }
        return false;
    }

    /**
     * Interface implemented by classes that support printing
     */
    static interface PrintHelperVersionImpl {

        public void setScaleMode(int scaleMode);

        public int getScaleMode();

        public void setColorMode(int colorMode);

        public int getColorMode();

        public void setOrientation(int orientation);

        public int getOrientation();

        public void printBitmap(String jobName, Bitmap bitmap, OnPrintFinishCallback callback);

        public void printBitmap(String jobName, Uri imageFile, OnPrintFinishCallback callback)
                throws FileNotFoundException;
    }

    /**
     * Implementation used when we do not support printing
     */
    private static final class PrintHelperStubImpl implements PrintHelperVersionImpl {
        int mScaleMode = SCALE_MODE_FILL;
        int mColorMode = COLOR_MODE_COLOR;
        int mOrientation = ORIENTATION_LANDSCAPE;
        @Override
        public void setScaleMode(int scaleMode) {
            mScaleMode = scaleMode;
        }

        @Override
        public int getColorMode() {
            return mColorMode;
        }

        @Override
        public void setColorMode(int colorMode) {
            mColorMode = colorMode;
        }

        @Override
        public void setOrientation(int orientation) { mOrientation = orientation; }

        @Override
        public int getOrientation() { return mOrientation; }

        @Override
        public int getScaleMode() {
            return mScaleMode;
        }

        @Override
        public void printBitmap(String jobName, Bitmap bitmap, OnPrintFinishCallback callback) {
        }

        @Override
        public void printBitmap(String jobName, Uri imageFile, OnPrintFinishCallback callback) {
        }
    }

    /**
     * Generic implementation for KitKat to Api24
     */
    private static class PrintHelperImpl<RealHelper extends PrintHelperKitkat>
            implements PrintHelperVersionImpl {
        private final RealHelper mPrintHelper;

        protected PrintHelperImpl(RealHelper helper) {
            mPrintHelper = helper;
        }

        @Override
        public void setScaleMode(int scaleMode) {
            mPrintHelper.setScaleMode(scaleMode);
        }

        @Override
        public int getScaleMode() {
            return mPrintHelper.getScaleMode();
        }

        @Override
        public void setColorMode(int colorMode) {
            mPrintHelper.setColorMode(colorMode);
        }

        @Override
        public int getColorMode() {
            return mPrintHelper.getColorMode();
        }

        @Override
        public void setOrientation(int orientation) {
            mPrintHelper.setOrientation(orientation);
        }

        @Override
        public int getOrientation() {
            return mPrintHelper.getOrientation();
        }

        @Override
        public void printBitmap(String jobName, Bitmap bitmap,
                final OnPrintFinishCallback callback) {
            RealHelper.OnPrintFinishCallback delegateCallback = null;
            if (callback != null) {
                delegateCallback = new RealHelper.OnPrintFinishCallback() {
                    @Override
                    public void onFinish() {
                        callback.onFinish();
                    }
                };
            }
            mPrintHelper.printBitmap(jobName, bitmap, delegateCallback);
        }

        @Override
        public void printBitmap(String jobName, Uri imageFile,
                final OnPrintFinishCallback callback) throws FileNotFoundException {
            RealHelper.OnPrintFinishCallback delegateCallback = null;
            if (callback != null) {
                delegateCallback = new RealHelper.OnPrintFinishCallback() {
                    @Override
                    public void onFinish() {
                        callback.onFinish();
                    }
                };
            }
            mPrintHelper.printBitmap(jobName, imageFile, delegateCallback);
        }
    }

    /**
     * Implementation used on KitKat
     */
    private static final class PrintHelperKitkatImpl extends PrintHelperImpl<PrintHelperKitkat> {
        PrintHelperKitkatImpl(Context context) {
            super(new PrintHelperKitkat(context));
        }
    }

    /**
     * Implementation used on Api20 to Api22
     */
    private static final class PrintHelperApi20Impl extends PrintHelperImpl<PrintHelperApi20> {
        PrintHelperApi20Impl(Context context) {
            super(new PrintHelperApi20(context));
        }
    }

    /**
     * Implementation used on Api23
     */
    private static final class PrintHelperApi23Impl extends PrintHelperImpl<PrintHelperApi23> {
        PrintHelperApi23Impl(Context context) {
            super(new PrintHelperApi23(context));
        }
    }


    /**
     * Implementation used on Api24 and above
     */
    private static final class PrintHelperApi24Impl extends PrintHelperImpl<PrintHelperApi24> {
        PrintHelperApi24Impl(Context context) {
            super(new PrintHelperApi24(context));
        }
    }

    /**
     * Returns the PrintHelper that can be used to print images.
     *
     * @param context A context for accessing system resources.
     * @return the <code>PrintHelper</code> to support printing images.
     */
    public PrintHelper(Context context) {
        if (systemSupportsPrint()) {
            if (Build.VERSION.SDK_INT >= 24) {
                mImpl = new PrintHelperApi24Impl(context);
            } else if (Build.VERSION.SDK_INT >= 23) {
                mImpl = new PrintHelperApi23Impl(context);
            } else if (Build.VERSION.SDK_INT >= 20) {
                mImpl = new PrintHelperApi20Impl(context);
            } else {
                mImpl = new PrintHelperKitkatImpl(context);
            }
        } else {
            mImpl = new PrintHelperStubImpl();
        }
    }

    /**
     * Selects whether the image will fill the paper and be cropped
     * {@link #SCALE_MODE_FIT}
     * or whether the image will be scaled but leave white space
     * {@link #SCALE_MODE_FILL}.
     *
     * @param scaleMode {@link #SCALE_MODE_FIT} or
     *                  {@link #SCALE_MODE_FILL}
     */
    public void setScaleMode(int scaleMode) {
        mImpl.setScaleMode(scaleMode);
    }

    /**
     * Returns the scale mode with which the image will fill the paper.
     *
     * @return The scale Mode: {@link #SCALE_MODE_FIT} or
     * {@link #SCALE_MODE_FILL}
     */
    public int getScaleMode() {
        return mImpl.getScaleMode();
    }

    /**
     * Sets whether the image will be printed in color (default)
     * {@link #COLOR_MODE_COLOR} or in back and white
     * {@link #COLOR_MODE_MONOCHROME}.
     *
     * @param colorMode The color mode which is one of
     * {@link #COLOR_MODE_COLOR} and {@link #COLOR_MODE_MONOCHROME}.
     */
    public void setColorMode(int colorMode) {
        mImpl.setColorMode(colorMode);
    }

    /**
     * Gets the color mode with which the image will be printed.
     *
     * @return The color mode which is one of {@link #COLOR_MODE_COLOR}
     * and {@link #COLOR_MODE_MONOCHROME}.
     */
    public int getColorMode() {
        return mImpl.getColorMode();
    }

    /**
     * Sets whether the image will be printed in landscape {@link #ORIENTATION_LANDSCAPE} (default)
     * or portrait {@link #ORIENTATION_PORTRAIT}.
     *
     * @param orientation The page orientation which is one of
     *                    {@link #ORIENTATION_LANDSCAPE} or {@link #ORIENTATION_PORTRAIT}.
     */
    public void setOrientation(int orientation) {
        mImpl.setOrientation(orientation);
    }

    /**
     * Gets whether the image will be printed in landscape or portrait.
     *
     * @return The page orientation which is one of
     * {@link #ORIENTATION_LANDSCAPE} or {@link #ORIENTATION_PORTRAIT}.
     */
    public int getOrientation() {
        return mImpl.getOrientation();
    }


    /**
     * Prints a bitmap.
     *
     * @param jobName The print job name.
     * @param bitmap  The bitmap to print.
     */
    public void printBitmap(String jobName, Bitmap bitmap) {
        mImpl.printBitmap(jobName, bitmap, null);
    }

    /**
     * Prints a bitmap.
     *
     * @param jobName The print job name.
     * @param bitmap  The bitmap to print.
     * @param callback Optional callback to observe when printing is finished.
     */
    public void printBitmap(String jobName, Bitmap bitmap, OnPrintFinishCallback callback) {
        mImpl.printBitmap(jobName, bitmap, callback);
    }

    /**
     * Prints an image located at the Uri. Image types supported are those of
     * {@link android.graphics.BitmapFactory#decodeStream(java.io.InputStream)
     * android.graphics.BitmapFactory.decodeStream(java.io.InputStream)}
     *
     * @param jobName   The print job name.
     * @param imageFile The <code>Uri</code> pointing to an image to print.
     * @throws FileNotFoundException if <code>Uri</code> is not pointing to a valid image.
     */
    public void printBitmap(String jobName, Uri imageFile) throws FileNotFoundException {
        mImpl.printBitmap(jobName, imageFile, null);
    }

    /**
     * Prints an image located at the Uri. Image types supported are those of
     * {@link android.graphics.BitmapFactory#decodeStream(java.io.InputStream)
     * android.graphics.BitmapFactory.decodeStream(java.io.InputStream)}
     *
     * @param jobName   The print job name.
     * @param imageFile The <code>Uri</code> pointing to an image to print.
     * @throws FileNotFoundException if <code>Uri</code> is not pointing to a valid image.
     * @param callback Optional callback to observe when printing is finished.
     */
    public void printBitmap(String jobName, Uri imageFile, OnPrintFinishCallback callback)
            throws FileNotFoundException {
        mImpl.printBitmap(jobName, imageFile, callback);
    }
}

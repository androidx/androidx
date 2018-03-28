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

package androidx.print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for printing bitmaps.
 */
public final class PrintHelper {
    private static final String LOG_TAG = "PrintHelper";
    // will be <= 300 dpi on A4 (8.3×11.7) paper (worst case of 150 dpi)
    private static final int MAX_PRINT_SIZE = 3500;

    /**
     * Whether the PrintActivity respects the suggested orientation.
     *
     * There is a bug in the PrintActivity that causes it to ignore the orientation
     */
    private static final boolean PRINT_ACTIVITY_RESPECTS_ORIENTATION =
            Build.VERSION.SDK_INT < 20 || Build.VERSION.SDK_INT > 23;

    /**
     * Whether the print subsystem handles min margins correctly. If not the print helper needs
     * to fake this.
     */
    private static final boolean IS_MIN_MARGINS_HANDLING_CORRECT = Build.VERSION.SDK_INT != 23;

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
    public static final int COLOR_MODE_MONOCHROME = PrintAttributes.COLOR_MODE_MONOCHROME;

    /**
     * this is a color image (default)
     */
    public static final int COLOR_MODE_COLOR = PrintAttributes.COLOR_MODE_COLOR;

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
        void onFinish();
    }

    @IntDef({SCALE_MODE_FIT, SCALE_MODE_FILL})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ScaleMode {}

    @IntDef({COLOR_MODE_MONOCHROME, COLOR_MODE_COLOR})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ColorMode {}

    @IntDef({ORIENTATION_LANDSCAPE, ORIENTATION_PORTRAIT})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Orientation {}

    private final Context mContext;

    BitmapFactory.Options mDecodeOptions = null;
    private final Object mLock = new Object();

    @ScaleMode int mScaleMode = SCALE_MODE_FILL;
    @ColorMode int mColorMode = COLOR_MODE_COLOR;
    @Orientation int mOrientation = ORIENTATION_LANDSCAPE;

    /**
     * Gets whether the system supports printing.
     *
     * @return True if printing is supported.
     */
    public static boolean systemSupportsPrint() {
        // Supported on Android 4.4 or later.
        return Build.VERSION.SDK_INT >= 19;
    }

    /**
     * Constructs the PrintHelper that can be used to print images.
     *
     * @param context A context for accessing system resources.
     */
    public PrintHelper(@NonNull Context context) {
        mContext = context;
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
    public void setScaleMode(@ScaleMode int scaleMode) {
        mScaleMode = scaleMode;
    }

    /**
     * Returns the scale mode with which the image will fill the paper.
     *
     * @return The scale Mode: {@link #SCALE_MODE_FIT} or
     * {@link #SCALE_MODE_FILL}
     */
    @ScaleMode
    public int getScaleMode() {
        return mScaleMode;
    }

    /**
     * Sets whether the image will be printed in color (default)
     * {@link #COLOR_MODE_COLOR} or in back and white
     * {@link #COLOR_MODE_MONOCHROME}.
     *
     * @param colorMode The color mode which is one of
     * {@link #COLOR_MODE_COLOR} and {@link #COLOR_MODE_MONOCHROME}.
     */
    public void setColorMode(@ColorMode int colorMode) {
        mColorMode = colorMode;
    }

    /**
     * Gets the color mode with which the image will be printed.
     *
     * @return The color mode which is one of {@link #COLOR_MODE_COLOR}
     * and {@link #COLOR_MODE_MONOCHROME}.
     */
    @ColorMode
    public int getColorMode() {
        return mColorMode;
    }

    /**
     * Sets whether the image will be printed in landscape {@link #ORIENTATION_LANDSCAPE} (default)
     * or portrait {@link #ORIENTATION_PORTRAIT}.
     *
     * @param orientation The page orientation which is one of
     *                    {@link #ORIENTATION_LANDSCAPE} or {@link #ORIENTATION_PORTRAIT}.
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    /**
     * Gets whether the image will be printed in landscape or portrait.
     *
     * @return The page orientation which is one of
     * {@link #ORIENTATION_LANDSCAPE} or {@link #ORIENTATION_PORTRAIT}.
     */
    public int getOrientation() {
        // Unset defaults to landscape but might turn image
        if (Build.VERSION.SDK_INT >= 19 && mOrientation == 0) {
            return ORIENTATION_LANDSCAPE;
        }
        return mOrientation;
    }


    /**
     * Prints a bitmap.
     *
     * @param jobName The print job name.
     * @param bitmap  The bitmap to print.
     */
    public void printBitmap(@NonNull String jobName, @NonNull Bitmap bitmap) {
        printBitmap(jobName, bitmap, null);
    }

    /**
     * Prints a bitmap.
     *
     * @param jobName The print job name.
     * @param bitmap  The bitmap to print.
     * @param callback Optional callback to observe when printing is finished.
     */
    public void printBitmap(@NonNull final String jobName, @NonNull final Bitmap bitmap,
            @Nullable final OnPrintFinishCallback callback) {
        if (Build.VERSION.SDK_INT < 19 || bitmap == null) {
            return;
        }

        PrintManager printManager =
                (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes.MediaSize mediaSize;
        if (isPortrait(bitmap)) {
            mediaSize = PrintAttributes.MediaSize.UNKNOWN_PORTRAIT;
        } else {
            mediaSize = PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE;
        }
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(mColorMode)
                .build();

        printManager.print(jobName,
                new PrintBitmapAdapter(jobName, mScaleMode, bitmap, callback), attr);
    }

    @RequiresApi(19)
    private class PrintBitmapAdapter extends PrintDocumentAdapter {
        private final String mJobName;
        private final int mFittingMode;
        private final Bitmap mBitmap;
        private final OnPrintFinishCallback mCallback;
        private PrintAttributes mAttributes;

        PrintBitmapAdapter(String jobName, int fittingMode, Bitmap bitmap,
                OnPrintFinishCallback callback) {
            mJobName = jobName;
            mFittingMode = fittingMode;
            mBitmap = bitmap;
            mCallback = callback;
        }

        @Override
        public void onLayout(PrintAttributes oldPrintAttributes,
                PrintAttributes newPrintAttributes,
                CancellationSignal cancellationSignal,
                LayoutResultCallback layoutResultCallback,
                Bundle bundle) {

            mAttributes = newPrintAttributes;

            PrintDocumentInfo info = new PrintDocumentInfo.Builder(mJobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                    .setPageCount(1)
                    .build();
            boolean changed = !newPrintAttributes.equals(oldPrintAttributes);
            layoutResultCallback.onLayoutFinished(info, changed);
        }

        @Override
        public void onWrite(PageRange[] pageRanges,
                ParcelFileDescriptor fileDescriptor,
                CancellationSignal cancellationSignal,
                WriteResultCallback writeResultCallback) {
            writeBitmap(mAttributes, mFittingMode, mBitmap, fileDescriptor,
                    cancellationSignal, writeResultCallback);
        }

        @Override
        public void onFinish() {
            if (mCallback != null) {
                mCallback.onFinish();
            }
        }
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
    public void printBitmap(@NonNull String jobName, @NonNull Uri imageFile)
            throws FileNotFoundException {
        printBitmap(jobName, imageFile, null);
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
    public void printBitmap(@NonNull final String jobName, @NonNull final Uri imageFile,
            @Nullable final OnPrintFinishCallback callback)
            throws FileNotFoundException {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }

        PrintDocumentAdapter printDocumentAdapter = new PrintUriAdapter(jobName, imageFile,
                callback, mScaleMode);

        PrintManager printManager =
                (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setColorMode(mColorMode);

        if (mOrientation == ORIENTATION_LANDSCAPE || mOrientation == 0) {
            builder.setMediaSize(PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE);
        } else if (mOrientation == ORIENTATION_PORTRAIT) {
            builder.setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT);
        }
        PrintAttributes attr = builder.build();

        printManager.print(jobName, printDocumentAdapter, attr);
    }

    @RequiresApi(19)
    private class PrintUriAdapter extends PrintDocumentAdapter {
        private final String mJobName;
        private final Uri mImageFile;
        private final OnPrintFinishCallback mCallback;
        private final int mFittingMode;
        private PrintAttributes mAttributes;
        AsyncTask<Uri, Boolean, Bitmap> mLoadBitmap;
        Bitmap mBitmap;

        PrintUriAdapter(String jobName, Uri imageFile, OnPrintFinishCallback callback,
                int fittingMode) {
            mJobName = jobName;
            mImageFile = imageFile;
            mCallback = callback;
            mFittingMode = fittingMode;
            mBitmap = null;
        }

        @Override
        public void onLayout(final PrintAttributes oldPrintAttributes,
                final PrintAttributes newPrintAttributes,
                final CancellationSignal cancellationSignal,
                final LayoutResultCallback layoutResultCallback,
                Bundle bundle) {

            synchronized (this) {
                mAttributes = newPrintAttributes;
            }

            if (cancellationSignal.isCanceled()) {
                layoutResultCallback.onLayoutCancelled();
                return;
            }
            // we finished the load
            if (mBitmap != null) {
                PrintDocumentInfo info = new PrintDocumentInfo.Builder(mJobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                        .setPageCount(1)
                        .build();
                boolean changed = !newPrintAttributes.equals(oldPrintAttributes);
                layoutResultCallback.onLayoutFinished(info, changed);
                return;
            }

            mLoadBitmap = new AsyncTask<Uri, Boolean, Bitmap>() {
                @Override
                protected void onPreExecute() {
                    // First register for cancellation requests.
                    cancellationSignal.setOnCancelListener(
                            new CancellationSignal.OnCancelListener() {
                                @Override
                                public void onCancel() { // on different thread
                                    cancelLoad();
                                    cancel(false);
                                }
                            });
                }

                @Override
                protected Bitmap doInBackground(Uri... uris) {
                    try {
                        return loadConstrainedBitmap(mImageFile);
                    } catch (FileNotFoundException e) {
                        /* ignore */
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);

                    // If orientation was not set by the caller, try to fit the bitmap on
                    // the current paper by potentially rotating the bitmap by 90 degrees.
                    if (bitmap != null
                            && (!PRINT_ACTIVITY_RESPECTS_ORIENTATION || mOrientation == 0)) {
                        PrintAttributes.MediaSize mediaSize;

                        synchronized (this) {
                            mediaSize = mAttributes.getMediaSize();
                        }

                        if (mediaSize != null) {
                            if (mediaSize.isPortrait() != isPortrait(bitmap)) {
                                Matrix rotation = new Matrix();

                                rotation.postRotate(90);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                        bitmap.getWidth(), bitmap.getHeight(), rotation,
                                        true);
                            }
                        }
                    }

                    mBitmap = bitmap;
                    if (bitmap != null) {
                        PrintDocumentInfo info = new PrintDocumentInfo.Builder(mJobName)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                                .setPageCount(1)
                                .build();

                        boolean changed = !newPrintAttributes.equals(oldPrintAttributes);

                        layoutResultCallback.onLayoutFinished(info, changed);

                    } else {
                        layoutResultCallback.onLayoutFailed(null);
                    }
                    mLoadBitmap = null;
                }

                @Override
                protected void onCancelled(Bitmap result) {
                    // Task was cancelled, report that.
                    layoutResultCallback.onLayoutCancelled();
                    mLoadBitmap = null;
                }
            }.execute();
        }

        private void cancelLoad() {
            synchronized (mLock) { // prevent race with set null below
                if (mDecodeOptions != null) {
                    mDecodeOptions.requestCancelDecode();
                    mDecodeOptions = null;
                }
            }
        }

        @Override
        public void onFinish() {
            super.onFinish();
            cancelLoad();
            if (mLoadBitmap != null) {
                mLoadBitmap.cancel(true);
            }
            if (mCallback != null) {
                mCallback.onFinish();
            }
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
        }

        @Override
        public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor fileDescriptor,
                CancellationSignal cancellationSignal,
                WriteResultCallback writeResultCallback) {
            writeBitmap(mAttributes, mFittingMode, mBitmap, fileDescriptor,
                    cancellationSignal, writeResultCallback);
        }
    }

    /**
     * Check if the supplied bitmap should best be printed on a portrait orientation paper.
     *
     * @param bitmap The bitmap to be printed.
     * @return true iff the picture should best be printed on a portrait orientation paper.
     */
    private static boolean isPortrait(Bitmap bitmap) {
        return bitmap.getWidth() <= bitmap.getHeight();
    }

    /**
     * Create a build with a copy from the other print attributes.
     *
     * @param other The other print attributes
     *
     * @return A builder that will build print attributes that match the other attributes
     */
    @RequiresApi(19)
    private static PrintAttributes.Builder copyAttributes(PrintAttributes other) {
        PrintAttributes.Builder b = (new PrintAttributes.Builder())
                .setMediaSize(other.getMediaSize())
                .setResolution(other.getResolution())
                .setMinMargins(other.getMinMargins());

        if (other.getColorMode() != 0) {
            b.setColorMode(other.getColorMode());
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (other.getDuplexMode() != 0) {
                b.setDuplexMode(other.getDuplexMode());
            }
        }

        return b;
    }

    /**
     * Calculates the transform the print an Image to fill the page
     *
     * @param imageWidth  with of bitmap
     * @param imageHeight height of bitmap
     * @param content     The output page dimensions
     * @param fittingMode The mode of fitting {@link #SCALE_MODE_FILL} vs
     *                    {@link #SCALE_MODE_FIT}
     * @return Matrix to be used in canvas.drawBitmap(bitmap, matrix, null) call
     */
    private static Matrix getMatrix(int imageWidth, int imageHeight, RectF content,
            @ScaleMode int fittingMode) {
        Matrix matrix = new Matrix();

        // Compute and apply scale to fill the page.
        float scale = content.width() / imageWidth;
        if (fittingMode == SCALE_MODE_FILL) {
            scale = Math.max(scale, content.height() / imageHeight);
        } else {
            scale = Math.min(scale, content.height() / imageHeight);
        }
        matrix.postScale(scale, scale);

        // Center the content.
        final float translateX = (content.width()
                - imageWidth * scale) / 2;
        final float translateY = (content.height()
                - imageHeight * scale) / 2;
        matrix.postTranslate(translateX, translateY);
        return matrix;
    }

    /**
     * Write a bitmap for a PDF document.
     *
     * @param attributes          The print attributes
     * @param fittingMode         How to fit the bitmap
     * @param bitmap              The bitmap to write
     * @param fileDescriptor      The file to write to
     * @param cancellationSignal  Signal cancelling operation
     * @param writeResultCallback Callback to call once written
     */
    @RequiresApi(19)
    private void writeBitmap(final PrintAttributes attributes, final int fittingMode,
            final Bitmap bitmap, final ParcelFileDescriptor fileDescriptor,
            final CancellationSignal cancellationSignal,
            final PrintDocumentAdapter.WriteResultCallback writeResultCallback) {
        final PrintAttributes pdfAttributes;
        if (IS_MIN_MARGINS_HANDLING_CORRECT) {
            pdfAttributes = attributes;
        } else {
            // If the handling of any margin != 0 is broken, strip the margins and add them to
            // the bitmap later
            pdfAttributes = copyAttributes(attributes)
                    .setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0)).build();
        }

        (new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... params) {
                try {
                    if (cancellationSignal.isCanceled()) {
                        return null;
                    }

                    PrintedPdfDocument pdfDocument = new PrintedPdfDocument(mContext,
                            pdfAttributes);

                    Bitmap maybeGrayscale = convertBitmapForColorMode(bitmap,
                            pdfAttributes.getColorMode());

                    if (cancellationSignal.isCanceled()) {
                        return null;
                    }

                    try {
                        PdfDocument.Page page = pdfDocument.startPage(1);

                        RectF contentRect;
                        if (IS_MIN_MARGINS_HANDLING_CORRECT) {
                            contentRect = new RectF(page.getInfo().getContentRect());
                        } else {
                            // Create dummy doc that has the margins to compute correctly sized
                            // content rectangle
                            PrintedPdfDocument dummyDocument = new PrintedPdfDocument(mContext,
                                    attributes);
                            PdfDocument.Page dummyPage = dummyDocument.startPage(1);
                            contentRect = new RectF(dummyPage.getInfo().getContentRect());
                            dummyDocument.finishPage(dummyPage);
                            dummyDocument.close();
                        }

                        // Resize bitmap
                        Matrix matrix = getMatrix(
                                maybeGrayscale.getWidth(), maybeGrayscale.getHeight(),
                                contentRect, fittingMode);

                        if (IS_MIN_MARGINS_HANDLING_CORRECT) {
                            // The pdfDocument takes care of the positioning and margins
                        } else {
                            // Move it to the correct position.
                            matrix.postTranslate(contentRect.left, contentRect.top);

                            // Cut off margins
                            page.getCanvas().clipRect(contentRect);
                        }

                        // Draw the bitmap.
                        page.getCanvas().drawBitmap(maybeGrayscale, matrix, null);

                        // Finish the page.
                        pdfDocument.finishPage(page);

                        if (cancellationSignal.isCanceled()) {
                            return null;
                        }

                        // Write the document.
                        pdfDocument.writeTo(
                                new FileOutputStream(fileDescriptor.getFileDescriptor()));
                        return null;
                    } finally {
                        pdfDocument.close();

                        if (fileDescriptor != null) {
                            try {
                                fileDescriptor.close();
                            } catch (IOException ioe) {
                                // ignore
                            }
                        }
                        // If we created a new instance for grayscaling, then recycle it here.
                        if (maybeGrayscale != bitmap) {
                            maybeGrayscale.recycle();
                        }
                    }
                } catch (Throwable t) {
                    return t;
                }
            }

            @Override
            protected void onPostExecute(Throwable throwable) {
                if (cancellationSignal.isCanceled()) {
                    // Cancelled.
                    writeResultCallback.onWriteCancelled();
                } else if (throwable == null) {
                    // Done.
                    writeResultCallback.onWriteFinished(
                            new PageRange[] { PageRange.ALL_PAGES });
                } else {
                    // Failed.
                    Log.e(LOG_TAG, "Error writing printed content", throwable);
                    writeResultCallback.onWriteFailed(null);
                }
            }
        }).execute();
    }

    /**
     * Loads a bitmap while limiting its size
     *
     * @param uri           location of a valid image
     * @return the Bitmap
     * @throws FileNotFoundException if the Uri does not point to an image
     */
    private Bitmap loadConstrainedBitmap(Uri uri)
            throws FileNotFoundException {
        if (uri == null || mContext == null) {
            throw new IllegalArgumentException("bad argument to getScaledBitmap");
        }
        // Get width and height of stored bitmap
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        loadBitmap(uri, opt);

        int w = opt.outWidth;
        int h = opt.outHeight;

        // If bitmap cannot be decoded, return null
        if (w <= 0 || h <= 0) {
            return null;
        }

        // Find best downsampling size
        int imageSide = Math.max(w, h);

        int sampleSize = 1;
        while (imageSide > MAX_PRINT_SIZE) {
            imageSide >>>= 1;
            sampleSize <<= 1;
        }

        // Make sure sample size is reasonable
        if (sampleSize <= 0 || 0 >= (Math.min(w, h) / sampleSize)) {
            return null;
        }
        BitmapFactory.Options decodeOptions;
        synchronized (mLock) { // prevent race with set null below
            mDecodeOptions = new BitmapFactory.Options();
            mDecodeOptions.inMutable = true;
            mDecodeOptions.inSampleSize = sampleSize;
            decodeOptions = mDecodeOptions;
        }
        try {
            return loadBitmap(uri, decodeOptions);
        } finally {
            synchronized (mLock) {
                mDecodeOptions = null;
            }
        }
    }

    /**
     * Returns the bitmap from the given uri loaded using the given options.
     * Returns null on failure.
     */
    private Bitmap loadBitmap(Uri uri, BitmapFactory.Options o) throws FileNotFoundException {
        if (uri == null || mContext == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        InputStream is = null;
        try {
            is = mContext.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(is, null, o);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException t) {
                    Log.w(LOG_TAG, "close fail ", t);
                }
            }
        }
    }

    private static Bitmap convertBitmapForColorMode(Bitmap original, @ColorMode int colorMode) {
        if (colorMode != COLOR_MODE_MONOCHROME) {
            return original;
        }
        // Create a grayscale bitmap
        Bitmap grayscale = Bitmap.createBitmap(original.getWidth(), original.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(grayscale);
        Paint p = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        p.setColorFilter(f);
        c.drawBitmap(original, 0, 0, p);
        c.setBitmap(null);

        return grayscale;
    }
}

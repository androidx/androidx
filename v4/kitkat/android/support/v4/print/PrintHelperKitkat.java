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
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument.Page;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.PrintAttributes.MediaSize;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Kitkat specific PrintManager API implementation.
 */
class PrintHelperKitkat {
    private static final String LOG_TAG = "PrintHelperKitkat";
    // will be <= 300 dpi on A4 (8.3×11.7) paper (worst case of 150 dpi)
    private final static int MAX_PRINT_SIZE = 3500;
    final Context mContext;
    BitmapFactory.Options mDecodeOptions = null;
    private final Object mLock = new Object();
    /**
     * image will be scaled but leave white space
     */
    public static final int SCALE_MODE_FIT = 1;
    /**
     * image will fill the paper and be cropped (default)
     */
    public static final int SCALE_MODE_FILL = 2;

    /**
     * select landscape (default)
     */
    public static final int ORIENTATION_LANDSCAPE = 1;

    /**
     * select portrait
     */
    public static final int ORIENTATION_PORTRAIT = 2;

    /**
     * this is a black and white image
     */
    public static final int COLOR_MODE_MONOCHROME = 1;
    /**
     * this is a color image (default)
     */
    public static final int COLOR_MODE_COLOR = 2;

    public interface OnPrintFinishCallback {
        public void onFinish();
    }

    /**
     * Whether the PrintActivity respects the suggested orientation
     */
    protected boolean mPrintActivityRespectsOrientation;

    /**
     * Whether the print subsystem handles min margins correctly. If not the print helper needs to
     * fake this.
     */
    protected boolean mIsMinMarginsHandlingCorrect;

    int mScaleMode = SCALE_MODE_FILL;

    int mColorMode = COLOR_MODE_COLOR;

    int mOrientation;

    PrintHelperKitkat(Context context) {
        mPrintActivityRespectsOrientation = true;
        mIsMinMarginsHandlingCorrect = true;

        mContext = context;
    }

    /**
     * Selects whether the image will fill the paper and be cropped
     * <p/>
     * {@link #SCALE_MODE_FIT}
     * or whether the image will be scaled but leave white space
     * {@link #SCALE_MODE_FILL}.
     *
     * @param scaleMode {@link #SCALE_MODE_FIT} or
     *                  {@link #SCALE_MODE_FILL}
     */
    public void setScaleMode(int scaleMode) {
        mScaleMode = scaleMode;
    }

    /**
     * Returns the scale mode with which the image will fill the paper.
     *
     * @return The scale Mode: {@link #SCALE_MODE_FIT} or
     * {@link #SCALE_MODE_FILL}
     */
    public int getScaleMode() {
        return mScaleMode;
    }

    /**
     * Sets whether the image will be printed in color (default)
     * {@link #COLOR_MODE_COLOR} or in back and white
     * {@link #COLOR_MODE_MONOCHROME}.
     *
     * @param colorMode The color mode which is one of
     *                  {@link #COLOR_MODE_COLOR} and {@link #COLOR_MODE_MONOCHROME}.
     */
    public void setColorMode(int colorMode) {
        mColorMode = colorMode;
    }

    /**
     * Sets whether to select landscape (default), {@link #ORIENTATION_LANDSCAPE}
     * or portrait {@link #ORIENTATION_PORTRAIT}
     * @param orientation The page orientation which is one of
     *                    {@link #ORIENTATION_LANDSCAPE} or {@link #ORIENTATION_PORTRAIT}.
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    /**
     * Gets the page orientation with which the image will be printed.
     *
     * @return The preferred orientation which is one of
     * {@link #ORIENTATION_LANDSCAPE} or {@link #ORIENTATION_PORTRAIT}
     */
    public int getOrientation() {
        /// Unset defaults to landscape but might turn image
        if (mOrientation == 0) {
            return ORIENTATION_LANDSCAPE;
        }
        return mOrientation;
    }

    /**
     * Gets the color mode with which the image will be printed.
     *
     * @return The color mode which is one of {@link #COLOR_MODE_COLOR}
     * and {@link #COLOR_MODE_MONOCHROME}.
     */
    public int getColorMode() {
        return mColorMode;
    }

    /**
     * Check if the supplied bitmap should best be printed on a portrait orientation paper.
     *
     * @param bitmap The bitmap to be printed.
     * @return true iff the picture should best be printed on a portrait orientation paper.
     */
    private static boolean isPortrait(Bitmap bitmap) {
        if (bitmap.getWidth() <= bitmap.getHeight()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create a build with a copy from the other print attributes.
     *
     * @param other The other print attributes
     *
     * @return A builder that will build print attributes that match the other attributes
     */
    protected PrintAttributes.Builder copyAttributes(PrintAttributes other) {
        PrintAttributes.Builder b = (new PrintAttributes.Builder())
                .setMediaSize(other.getMediaSize())
                .setResolution(other.getResolution())
                .setMinMargins(other.getMinMargins());

        if (other.getColorMode() != 0) {
            b.setColorMode(other.getColorMode());
        }

        return b;
    }

    /**
     * Prints a bitmap.
     *
     * @param jobName The print job name.
     * @param bitmap  The bitmap to print.
     * @param callback Optional callback to observe when printing is finished.
     */
    public void printBitmap(final String jobName, final Bitmap bitmap,
            final OnPrintFinishCallback callback) {
        if (bitmap == null) {
            return;
        }
        final int fittingMode = mScaleMode; // grab the fitting mode at time of call
        PrintManager printManager = (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE);
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
                new PrintDocumentAdapter() {
                    private PrintAttributes mAttributes;

                    @Override
                    public void onLayout(PrintAttributes oldPrintAttributes,
                                         PrintAttributes newPrintAttributes,
                                         CancellationSignal cancellationSignal,
                                         LayoutResultCallback layoutResultCallback,
                                         Bundle bundle) {

                        mAttributes = newPrintAttributes;

                        PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                                .setPageCount(1)
                                .build();
                        boolean changed = !newPrintAttributes.equals(oldPrintAttributes);
                        layoutResultCallback.onLayoutFinished(info, changed);
                    }

                    @Override
                    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor fileDescriptor,
                                        CancellationSignal cancellationSignal,
                                        WriteResultCallback writeResultCallback) {
                        writeBitmap(mAttributes, fittingMode, bitmap, fileDescriptor,
                                writeResultCallback);
                    }

                    @Override
                    public void onFinish() {
                        if (callback != null) {
                            callback.onFinish();
                        }
                    }
                }, attr);
    }

    /**
     * Calculates the transform the print an Image to fill the page
     *
     * @param imageWidth  with of bitmap
     * @param imageHeight height of bitmap
     * @param content     The output page dimensions
     * @param fittingMode The mode of fitting {@link #SCALE_MODE_FILL} vs {@link #SCALE_MODE_FIT}
     * @return Matrix to be used in canvas.drawBitmap(bitmap, matrix, null) call
     */
    private Matrix getMatrix(int imageWidth, int imageHeight, RectF content, int fittingMode) {
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
     * @param writeResultCallback Callback to call once written
     */
    private void writeBitmap(PrintAttributes attributes, int fittingMode, Bitmap bitmap,
            ParcelFileDescriptor fileDescriptor,
            PrintDocumentAdapter.WriteResultCallback writeResultCallback) {
        PrintAttributes pdfAttributes;
        if (mIsMinMarginsHandlingCorrect) {
            pdfAttributes = attributes;
        } else {
            // If the handling of any margin != 0 is broken, strip the margins and add them to the
            // bitmap later
            pdfAttributes = copyAttributes(attributes)
                    .setMinMargins(new PrintAttributes.Margins(0,0,0,0)).build();
        }

        PrintedPdfDocument pdfDocument = new PrintedPdfDocument(mContext,
                pdfAttributes);

        Bitmap maybeGrayscale = convertBitmapForColorMode(bitmap,
                pdfAttributes.getColorMode());
        try {
            Page page = pdfDocument.startPage(1);

            RectF contentRect;
            if (mIsMinMarginsHandlingCorrect) {
                contentRect = new RectF(page.getInfo().getContentRect());
            } else {
                // Create dummy doc that has the margins to compute correctly sized content
                // rectangle
                PrintedPdfDocument dummyDocument = new PrintedPdfDocument(mContext,
                        attributes);
                Page dummyPage = dummyDocument.startPage(1);
                contentRect = new RectF(dummyPage.getInfo().getContentRect());
                dummyDocument.finishPage(dummyPage);
                dummyDocument.close();
            }

            // Resize bitmap
            Matrix matrix = getMatrix(
                    maybeGrayscale.getWidth(), maybeGrayscale.getHeight(),
                    contentRect, fittingMode);

            if (mIsMinMarginsHandlingCorrect) {
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

            try {
                // Write the document.
                pdfDocument.writeTo(new FileOutputStream(fileDescriptor.getFileDescriptor()));
                // Done.
                writeResultCallback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (IOException ioe) {
                // Failed.
                Log.e(LOG_TAG, "Error writing printed content", ioe);
                writeResultCallback.onWriteFailed(null);
            }
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
    }

    /**
     * Prints an image located at the Uri. Image types supported are those of
     * <code>BitmapFactory.decodeStream</code> (JPEG, GIF, PNG, BMP, WEBP)
     *
     * @param jobName   The print job name.
     * @param imageFile The <code>Uri</code> pointing to an image to print.
     * @param callback Optional callback to observe when printing is finished.
     * @throws FileNotFoundException if <code>Uri</code> is not pointing to a valid image.
     */
    public void printBitmap(final String jobName, final Uri imageFile,
            final OnPrintFinishCallback callback) throws FileNotFoundException {
        final int fittingMode = mScaleMode;

        PrintDocumentAdapter printDocumentAdapter = new PrintDocumentAdapter() {
            private PrintAttributes mAttributes;
            AsyncTask<Uri, Boolean, Bitmap> mLoadBitmap;
            Bitmap mBitmap = null;

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
                    PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
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
                            return loadConstrainedBitmap(imageFile, MAX_PRINT_SIZE);
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
                                && (!mPrintActivityRespectsOrientation || mOrientation == 0)) {
                            MediaSize mediaSize;

                            synchronized (this) {
                                mediaSize = mAttributes.getMediaSize();
                            }

                            if (mediaSize != null) {
                                if (mediaSize.isPortrait() != isPortrait(bitmap)) {
                                    Matrix rotation = new Matrix();

                                    rotation.postRotate(90);
                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                            bitmap.getHeight(), rotation, true);
                                }
                            }
                        }

                        mBitmap = bitmap;
                        if (bitmap != null) {
                            PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
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
                if (callback != null) {
                    callback.onFinish();
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
                writeBitmap(mAttributes, fittingMode, mBitmap, fileDescriptor, writeResultCallback);
            }
        };

        PrintManager printManager = (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE);
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

    /**
     * Loads a bitmap while limiting its size
     *
     * @param uri           location of a valid image
     * @param maxSideLength the maximum length of a size
     * @return the Bitmap
     * @throws FileNotFoundException if the Uri does not point to an image
     */
    private Bitmap loadConstrainedBitmap(Uri uri, int maxSideLength) throws FileNotFoundException {
        if (maxSideLength <= 0 || uri == null || mContext == null) {
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
        while (imageSide > maxSideLength) {
            imageSide >>>= 1;
            sampleSize <<= 1;
        }

        // Make sure sample size is reasonable
        if (sampleSize <= 0 || 0 >= (int) (Math.min(w, h) / sampleSize)) {
            return null;
        }
        BitmapFactory.Options decodeOptions = null;
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

    private Bitmap convertBitmapForColorMode(Bitmap original, int colorMode) {
        if (colorMode != COLOR_MODE_MONOCHROME) {
            return original;
        }
        // Create a grayscale bitmap
        Bitmap grayscale = Bitmap.createBitmap(original.getWidth(), original.getHeight(),
                Config.ARGB_8888);
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

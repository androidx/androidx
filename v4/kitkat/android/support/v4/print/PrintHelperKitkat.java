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
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument.Page;
import android.net.Uri;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Kitkat specific PrintManager API implementation.
 */
public class PrintHelperKitkat {
    private static final String LOG_TAG = "PrintHelperKitkat";
    // will be <= 300 dpi on A4 (8.3×11.7) paper (worst case of 150 dpi)
    private final static int MAX_PRINT_SIZE = 3500;
    final Context mContext;
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

    int mScaleMode = SCALE_MODE_FILL;

    int mColorMode = COLOR_MODE_COLOR;

    PrintHelperKitkat(Context context) {
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
     * {@link #COLOR_MODE_COLOR} and {@link #COLOR_MODE_MONOCHROME}.
     */
    public void setColorMode(int colorMode) {
        mColorMode = colorMode;
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
     * Prints a bitmap.
     *
     * @param jobName The print job name.
     * @param bitmap  The bitmap to print.
     */
    public void printBitmap(final String jobName, final Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        final int fittingMode = mScaleMode; // grab the fitting mode at time of call
        PrintManager printManager = (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes.MediaSize mediaSize = PrintAttributes.MediaSize.UNKNOWN_PORTRAIT;
        if (bitmap.getWidth() > bitmap.getHeight()) {
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
                        PrintedPdfDocument pdfDocument = new PrintedPdfDocument(mContext,
                                mAttributes);
                        try {
                            Page page = pdfDocument.startPage(1);

                            RectF content = new RectF(page.getInfo().getContentRect());
                            Matrix matrix = new Matrix();

                            // Compute and apply scale to fill the page.
                            float scale = content.width() / bitmap.getWidth();
                            if (fittingMode == SCALE_MODE_FILL) {
                                scale = Math.max(scale, content.height() / bitmap.getHeight());
                            } else {
                                scale = Math.min(scale, content.height() / bitmap.getHeight());
                            }
                            matrix.postScale(scale, scale);

                            // Center the content.
                            final float translateX = (content.width()
                                    - bitmap.getWidth() * scale) / 2;
                            final float translateY = (content.height()
                                    - bitmap.getHeight() * scale) / 2;
                            matrix.postTranslate(translateX, translateY);

                            // Draw the bitmap.
                            page.getCanvas().drawBitmap(bitmap, matrix, null);

                            // Finish the page.
                            pdfDocument.finishPage(page);

                            try {
                                // Write the document.
                                pdfDocument.writeTo(new FileOutputStream(
                                        fileDescriptor.getFileDescriptor()));
                                // Done.
                                writeResultCallback.onWriteFinished(
                                        new PageRange[]{PageRange.ALL_PAGES});
                            } catch (IOException ioe) {
                                // Failed.
                                Log.e(LOG_TAG, "Error writing printed content", ioe);
                                writeResultCallback.onWriteFailed(null);
                            }
                        } finally {
                            if (pdfDocument != null) {
                                pdfDocument.close();
                            }
                            if (fileDescriptor != null) {
                                try {
                                    fileDescriptor.close();
                                } catch (IOException ioe) {
                                    /* ignore */
                                }
                            }
                        }
                    }
                }, attr);
    }

    /**
     * Prints an image located at the Uri. Image types supported are those of
     * <code>BitmapFactory.decodeStream</code> (JPEG, GIF, PNG, BMP, WEBP)
     *
     * @param jobName   The print job name.
     * @param imageFile The <code>Uri</code> pointing to an image to print.
     * @throws FileNotFoundException if <code>Uri</code> is not pointing to a valid image.
     */
    public void printBitmap(String jobName, Uri imageFile) throws FileNotFoundException {
        Bitmap bitmap = loadConstrainedBitmap(imageFile, MAX_PRINT_SIZE);
        printBitmap(jobName, bitmap);
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = sampleSize;
        return loadBitmap(uri, options);
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
}
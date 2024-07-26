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

package androidx.pdf.service;

import android.graphics.pdf.LoadParams;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRendererPreV;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Supplier;

import java.io.IOException;

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PdfRendererAdapter implements AutoCloseable {
    private PdfRenderer mPdfRenderer;
    private PdfRendererPreV mPdfRendererPreV;

    PdfRendererAdapter(@NonNull ParcelFileDescriptor parcelFileDescriptor,
            @NonNull String password)
            throws IOException, SecurityException {
        if (Build.VERSION.SDK_INT >= 35) {
            LoadParams params = new LoadParams.Builder().setPassword(password).build();
            mPdfRenderer = new PdfRenderer(parcelFileDescriptor, params);
        } else {
            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
                LoadParams params = new LoadParams.Builder().setPassword(password).build();
                mPdfRendererPreV = new PdfRendererPreV(parcelFileDescriptor, params);
            }
        }
    }

    /**  */
    @NonNull
    PdfPageAdapter openPage(int pageNum) {
        if (mPdfRenderer != null) {
            return new PdfPageAdapter(mPdfRenderer, pageNum);
        }
        return new PdfPageAdapter(mPdfRendererPreV, pageNum);
    }

    public int getPageCount() {
        if (mPdfRenderer != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRenderer.getPageCount();
        }
        return checkAndExecute(() -> mPdfRendererPreV.getPageCount());
    }

    public int getDocumentLinearizationType() {
        if (mPdfRenderer != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRenderer.getDocumentLinearizationType();
        }
        return checkAndExecute(() -> mPdfRendererPreV.getDocumentLinearizationType());
    }

    public int getDocumentFormType() {
        if (mPdfRenderer != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRenderer.getPdfFormType();
        }
        return checkAndExecute(() -> mPdfRendererPreV.getPdfFormType());
    }

    private static void checkAndExecute(@NonNull Runnable block) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            block.run();
        }
        throw new UnsupportedOperationException("Operation support above S");
    }

    private static <T> T checkAndExecute(@NonNull Supplier<T> block) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            return block.get();
        }
        throw new UnsupportedOperationException("Operation support above S");
    }

    @Override
    public void close() throws IOException {
        if (mPdfRenderer != null) {
            mPdfRenderer.close();
            mPdfRenderer = null;
        } else {
            checkAndExecute(() -> {
                mPdfRendererPreV.close();
                mPdfRendererPreV = null;
            });
        }
    }
}

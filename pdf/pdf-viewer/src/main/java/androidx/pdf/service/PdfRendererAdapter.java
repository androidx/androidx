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

import android.annotation.SuppressLint;
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
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PdfRendererAdapter implements AutoCloseable {
    private PdfRenderer mPdfRenderer;
    private PdfRendererPreV mPdfRendererPreV;
    @SuppressLint({"UseSparseArrays", "BanConcurrentHashMap"})
    private final Map<Integer, PdfPageAdapter> mCachedPageMap =
            new java.util.concurrent.ConcurrentHashMap<>();

    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
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

    /**
     * Caller should use {@link #releasePage(PdfPageAdapter, int)} to close the page resource
     * reliably after usage.
     */
    @NonNull
    PdfPageAdapter openPage(int pageNum, boolean useCache) {
        if (mPdfRenderer != null) {
            if (useCache) {
                return openPageWithCache(pageNum);
            }
            return new PdfPageAdapter(mPdfRenderer, pageNum);
        }
        return new PdfPageAdapter(mPdfRendererPreV, pageNum);
    }

    @NonNull
    private PdfPageAdapter openPageWithCache(int pageNum) {
        if (mPdfRenderer != null) {
            // Fetched either from cache or native layer.
            PdfPageAdapter page = mCachedPageMap.get(pageNum);
            if (page != null) {
                return page;
            }
            page = new PdfPageAdapter(mPdfRenderer, pageNum);
            mCachedPageMap.put(pageNum, page);
            return page;
        } else {
            return new PdfPageAdapter(mPdfRendererPreV, pageNum);
        }
    }

    /** Closes the page. Also removes and clears the cached instance, if held. */
    public void releasePage(PdfPageAdapter pageAdapter, int pageNum) {
        if (mPdfRenderer != null) {
            if (pageAdapter != null) {
                pageAdapter.close();
            }
            PdfPageAdapter removedPage = mCachedPageMap.remove(pageNum);
            if (removedPage != null) {
                removedPage.close();
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public int getPageCount() {
        if (mPdfRenderer != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRenderer.getPageCount();
        }
        return checkAndExecute(() -> mPdfRendererPreV.getPageCount());
    }

    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public int getDocumentLinearizationType() {
        if (mPdfRenderer != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRenderer.getDocumentLinearizationType();
        }
        return checkAndExecute(() -> mPdfRendererPreV.getDocumentLinearizationType());
    }

    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
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

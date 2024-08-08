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
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRendererPreV;
import android.graphics.pdf.RenderParams;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.graphics.pdf.content.PdfPageImageContent;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.models.PageMatchBounds;
import android.graphics.pdf.models.selection.PageSelection;
import android.graphics.pdf.models.selection.SelectionBoundary;
import android.os.Build;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Supplier;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PdfPageAdapter implements AutoCloseable {
    private int mPageNum;
    private int mHeight;
    private int mWidth;

    private PdfRenderer.Page mPdfRendererPage;
    private PdfRendererPreV.Page mPdfRendererPreVPage;

    PdfPageAdapter(@NonNull PdfRenderer pdfRenderer, int pageNum) {
        mPageNum = pageNum;
        mPdfRendererPage = pdfRenderer.openPage(pageNum);
        mHeight = mPdfRendererPage.getHeight();
        mWidth = mPdfRendererPage.getWidth();
    }

    PdfPageAdapter(@NonNull PdfRendererPreV pdfRendererPreV, int pageNum) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            mPageNum = pageNum;
            mPdfRendererPreVPage = pdfRendererPreV.openPage(pageNum);
            mHeight = mPdfRendererPreVPage.getHeight();
            mWidth = mPdfRendererPreVPage.getWidth();
        }
    }

    public int getPageNum() {
        return mPageNum;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public void render(@NonNull Bitmap bitmap) {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            mPdfRendererPage.render(bitmap, null, null, getRenderParams());
        } else {
            checkAndExecute(
                    () -> mPdfRendererPreVPage.render(bitmap, null, null, getRenderParams()));
        }
    }

    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public void renderTile(@NonNull Bitmap bitmap,
            int left, int top, int scaledPageWidth, int scaledPageHeight) {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            int pageWidth = mPdfRendererPage.getWidth();
            int pageHeight = mPdfRendererPage.getHeight();
            Matrix transform = getTransformationMatrix(left, top, (float) scaledPageWidth,
                    (float) scaledPageHeight, pageWidth,
                    pageHeight);
            RenderParams renderParams = getRenderParams();
            mPdfRendererPage.render(bitmap, null, transform, renderParams);
        } else {
            checkAndExecute(() -> {
                {
                    int pageWidth = mPdfRendererPreVPage.getWidth();
                    int pageHeight = mPdfRendererPreVPage.getHeight();
                    Matrix transform = getTransformationMatrix(left, top, (float) scaledPageWidth,
                            (float) scaledPageHeight, pageWidth,
                            pageHeight);
                    RenderParams renderParams = getRenderParams();
                    mPdfRendererPreVPage.render(bitmap, null, transform, renderParams);
                }
            });
        }
    }

    @NonNull
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public List<PdfPageTextContent> getPageTextContents() {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRendererPage.getTextContents();
        }
        return checkAndExecute(() -> mPdfRendererPreVPage.getTextContents());
    }

    @NonNull
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public List<PdfPageImageContent> getPageImageContents() {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRendererPage.getImageContents();
        }
        return checkAndExecute(() -> mPdfRendererPreVPage.getImageContents());
    }

    @Nullable
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public PageSelection selectPageText(@NonNull SelectionBoundary start,
            @NonNull SelectionBoundary stop) {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRendererPage.selectContent(start, stop);
        }
        return checkAndExecute(() -> mPdfRendererPreVPage.selectContent(start, stop));
    }

    @NonNull
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public List<PageMatchBounds> searchPageText(@NonNull String query) {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRendererPage.searchText(query);
        }
        return checkAndExecute(() -> mPdfRendererPreVPage.searchText(query));
    }

    @NonNull
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public List<PdfPageLinkContent> getPageLinks() {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRendererPage.getLinkContents();
        }
        return checkAndExecute(() -> mPdfRendererPreVPage.getLinkContents());
    }

    @NonNull
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public List<PdfPageGotoLinkContent> getPageGotoLinks() {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            return mPdfRendererPage.getGotoLinks();
        }
        return checkAndExecute(() -> mPdfRendererPreVPage.getGotoLinks());

    }

    private Matrix getTransformationMatrix(int left, int top, float scaledPageWidth,
            float scaledPageHeight,
            int pageWidth, int pageHeight) {
        Matrix matrix = new Matrix();
        matrix.setScale(scaledPageWidth / pageWidth,
                scaledPageHeight / pageHeight);
        matrix.postTranslate(-left, -top);
        return matrix;
    }

    private RenderParams getRenderParams() {
        return checkAndExecute(() -> {
            RenderParams.Builder renderParamsBuilder = new RenderParams.Builder(
                    RenderParams.RENDER_MODE_FOR_DISPLAY);
            return renderParamsBuilder.setRenderFlags(
                    RenderParams.FLAG_RENDER_HIGHLIGHT_ANNOTATIONS
                    | RenderParams.FLAG_RENDER_TEXT_ANNOTATIONS).build();
        });
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
    @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
    public void close() {
        if (mPdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            mPdfRendererPage.close();
            mPdfRendererPage = null;
        } else {
            checkAndExecute(() -> {
                mPdfRendererPreVPage.close();
                mPdfRendererPreVPage = null;
            });
        }
    }
}

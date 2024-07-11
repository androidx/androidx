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

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.StrictModeUtils;

import java.util.List;

// TODO: Delete this class once framework API calls are added
@RestrictTo(RestrictTo.Scope.LIBRARY)
class PdfDocument {
    private static final String LIB_NAME = "pdfclient";

    private final long mPdfDocPtr;

    private final int mNumPages;

    protected PdfDocument(long pdfDocPtr, int numPages) {
        this.mPdfDocPtr = pdfDocPtr;
        this.mNumPages = numPages;
    }

    public static LoadPdfResult createFromFd(int fd, String password) {
        return null;
    }

    static void loadLibPdf() {
        StrictModeUtils.bypass(() -> System.loadLibrary(LIB_NAME));
    }

    public void destroy() {
    }

    public boolean saveAs(ParcelFileDescriptor destination) {
        return false;
    }

    public int numPages() {
        return mNumPages;
    }

    public Dimensions getPageDimensions(int pageNum) {
        return null;
    }

    public Bitmap renderPageFd(int pageNum, int pageWidth, int pageHeight, boolean hideTextAnnots) {
        return null;
    }

    public Bitmap renderTileFd(int pageNum, int tileWidth, int tileHeight, int scaledPageWidth,
            int scaledPageHeight, int left, int top, boolean hideTextAnnots) {
        return null;
    }

    public boolean cloneWithoutSecurity(ParcelFileDescriptor destination) {
        return false;
    }

    public String getPageText(int pageNum) {
        return null;
    }

    public List<String> getPageAltText(int pageNum) {
        return null;
    }

    public MatchRects searchPageText(int pageNum, String query) {
        return null;
    }

    public PageSelection selectPageText(int pageNum, SelectionBoundary start,
            SelectionBoundary stop) {
        return null;
    }

    public LinkRects getPageLinks(int pageNum) {
        return null;
    }

    public List<GotoLink> getPageGotoLinks(int pageNum) {
        return null;
    }

    public boolean isPdfLinearized() {
        return false;
    }

    public int getFormType() {
        return -1;
    }

    @Override
    public String toString() {
        return String.format("PdfDocument(%x, %d pages)", mPdfDocPtr, mNumPages);
    }
}

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

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRendererPreV;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.graphics.pdf.content.PdfPageImageContent;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.models.PageMatchBounds;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.PdfDocumentRemote;
import androidx.pdf.models.SelectionBoundary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Isolated Service wrapper around the PdfClient native lib, for security purposes. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfDocumentService extends Service {

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        return new PdfDocumentRemoteImpl();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private static class PdfDocumentRemoteImpl extends PdfDocumentRemote.Stub {
        private PdfRendererAdapter mAdapter;

        PdfDocumentRemoteImpl() {
        }

        @Override
        public int create(ParcelFileDescriptor pfd, String password) {
            try {
                mAdapter = new PdfRendererAdapter(pfd, password);
                return PdfStatus.LOADED.getNumber();
            } catch (SecurityException e) {
                return PdfStatus.REQUIRES_PASSWORD.getNumber();
            } catch (Exception e) {
                return PdfStatus.PDF_ERROR.getNumber();
            }
        }

        @Override
        public int numPages() {
            return mAdapter.getPageCount();
        }

        @Override
        public Dimensions getPageDimensions(int pageNum) {
            PdfPageAdapter pageAdapter = null;
            try {
                pageAdapter = mAdapter.openPage(pageNum, false);
                return new Dimensions(pageAdapter.getWidth(),
                        pageAdapter.getHeight());
            } finally {
                mAdapter.releasePage(pageAdapter, pageNum);
            }
        }

        @Override
        public Bitmap renderPage(int pageNum, int pageWidth, int pageHeight,
                boolean hideTextAnnots) {
            Bitmap output = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);
            output.eraseColor(Color.WHITE);
            mAdapter.openPage(pageNum, true).render(output);
            return output;
        }

        @Override
        public Bitmap renderTile(int pageNum, int tileWidth, int tileHeight, int scaledPageWidth,
                int scaledPageHeight, int left, int top, boolean hideTextAnnots) {
            Bitmap output = Bitmap.createBitmap(tileWidth, tileHeight, Bitmap.Config.ARGB_8888);
            output.eraseColor(Color.WHITE);
            mAdapter.openPage(pageNum, true)
                    .renderTile(output, left, top, scaledPageWidth, scaledPageHeight);
            return output;
        }


        @Override
        public String getPageText(int pageNum) {
            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
                PdfPageAdapter pageAdapter = null;
                try {
                    pageAdapter = mAdapter.openPage(pageNum, false);
                    List<PdfPageTextContent> textPdfContentList = pageAdapter.getPageTextContents();
                    // TODO: Add list handling instead of taking its first element
                    return textPdfContentList.get(0).getText();
                } finally {
                    mAdapter.releasePage(pageAdapter, pageNum);
                }
            }
            throw new UnsupportedOperationException("Operation support above S");
        }

        @Override
        public List<String> getPageAltText(int pageNum) {
            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
                PdfPageAdapter pageAdapter = null;
                try {
                    pageAdapter = mAdapter.openPage(pageNum, false);
                    List<PdfPageImageContent> text = pageAdapter.getPageImageContents();
                    return text.stream().map(PdfPageImageContent::getAltText).collect(
                            Collectors.toList());
                } finally {
                    mAdapter.releasePage(pageAdapter, pageNum);
                }
            }
            throw new UnsupportedOperationException("Operation support above S");
        }

        @Override
        public MatchRects searchPageText(int pageNum, String query) {
            PdfPageAdapter pageAdapter = null;
            try {
                pageAdapter = mAdapter.openPage(pageNum, false);
                List<PageMatchBounds> searchResultList = pageAdapter.searchPageText(query);
                return MatchRects.flattenList(searchResultList);
            } finally {
                mAdapter.releasePage(pageAdapter, pageNum);
            }
        }

        @Override
        public PageSelection selectPageText(int pageNum, SelectionBoundary start,
                SelectionBoundary stop) {
            PdfPageAdapter pageAdapter = null;
            try {
                pageAdapter = mAdapter.openPage(pageNum, false);
                android.graphics.pdf.models.selection.PageSelection pageSelection =
                        pageAdapter.selectPageText(SelectionBoundary.convert(start),
                                SelectionBoundary.convert(stop));
                if (pageSelection != null) {
                    return PageSelection.convert(pageSelection);
                }
                return null;
            } finally {
                mAdapter.releasePage(pageAdapter, pageNum);
            }
        }

        @Override
        public LinkRects getPageLinks(int pageNum) {
            PdfPageAdapter pageAdapter = null;
            try {
                pageAdapter = mAdapter.openPage(pageNum, false);
                List<PdfPageLinkContent> pageLinks = pageAdapter.getPageLinks();
                return LinkRects.flattenList(pageLinks);
            } finally {
                mAdapter.releasePage(pageAdapter, pageNum);
            }
        }

        @Override
        public List<GotoLink> getPageGotoLinks(int pageNum) {
            PdfPageAdapter pageAdapter = null;
            try {
                pageAdapter = mAdapter.openPage(pageNum, false);
                List<PdfPageGotoLinkContent> gotoLinks = pageAdapter.getPageGotoLinks();
                if (!gotoLinks.isEmpty()) {
                    List<GotoLink> list = new ArrayList<>();
                    for (PdfPageGotoLinkContent link : gotoLinks) {
                        GotoLink convertedLink = GotoLink.convert(link);
                        list.add(convertedLink);
                    }
                    return list;
                }
                return null;
            } finally {
                mAdapter.releasePage(pageAdapter, pageNum);
            }
        }

        @Override
        public void releasePage(int pageNum) {
            mAdapter.releasePage(null, pageNum);
        }

        @Override
        public boolean isPdfLinearized() {
            return mAdapter.getDocumentLinearizationType()
                    == PdfRendererPreV.DOCUMENT_LINEARIZED_TYPE_LINEARIZED;
        }

        @Override
        public int getFormType() {
            return mAdapter.getDocumentFormType();

        }

        @Override
        public boolean cloneWithoutSecurity(ParcelFileDescriptor destination) {
            // TODO: Implementation pending as use-case undiscovered.
            return true;
        }

        @Override
        public boolean saveAs(ParcelFileDescriptor destination) {
            // TODO: Implementation pending as use-case undiscovered.
            return true;
        }

        @Override
        protected void finalize() throws Throwable {
            mAdapter.close();
            mAdapter = null;
            super.finalize();
        }
    }
}

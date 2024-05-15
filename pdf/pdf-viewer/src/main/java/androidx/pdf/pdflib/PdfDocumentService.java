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

package androidx.pdf.pdflib;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.FutureValues;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.PdfDocumentRemote;
import androidx.pdf.models.SelectionBoundary;

import java.util.List;

/** Isolated Service wrapper around the PdfClient native lib, for security purposes. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfDocumentService extends Service {

    private static final String TAG = "PdfDocumentService";

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

        private final FutureValues.BlockingCallback<Boolean> mLoaderCallback =
                new FutureValues.BlockingCallback<>();

        private PdfDocument mPdfDocument;

        PdfDocumentRemoteImpl() {
        }

        @Override
        public int create(ParcelFileDescriptor pfd, String password) throws RemoteException {
            mLoaderCallback.getBlocking();
            ensurePdfDestroyed();
            int fd = pfd.detachFd();
            LoadPdfResult result = PdfDocument.createFromFd(fd, password);
            if (result.isLoaded()) {
                mPdfDocument = result.getPdfDocument();
            }
            return result.getStatus().getNumber();
        }

        @Override
        public int numPages() {
            mLoaderCallback.getBlocking();
            return mPdfDocument.numPages();
        }

        @Override
        public Dimensions getPageDimensions(int pageNum) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.getPageDimensions(pageNum);
        }

        @Override
        public String getPageText(int pageNum) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.getPageText(pageNum);
        }

        @Override
        public List<String> getPageAltText(int pageNum) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.getPageAltText(pageNum);
        }

        @Override
        public Bitmap renderPage(int pageNum, int pageWidth, int pageHeight,
                boolean hideTextAnnots) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.renderPageFd(pageNum, pageWidth, pageHeight, hideTextAnnots);
        }

        @Override
        public Bitmap renderTile(int pageNum, int tileWidth, int tileHeight, int scaledPageWidth,
                int scaledPageHeight, int left, int top, boolean hideTextAnnots) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.renderTileFd(pageNum, tileWidth, tileHeight, scaledPageWidth,
                    scaledPageHeight, left, top, hideTextAnnots);
        }

        @Override
        public MatchRects searchPageText(int pageNum, String query) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.searchPageText(pageNum, query);
        }

        @Override
        public PageSelection selectPageText(int pageNum, SelectionBoundary start,
                SelectionBoundary stop) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.selectPageText(pageNum, start, stop);
        }

        @Override
        public LinkRects getPageLinks(int pageNum) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.getPageLinks(pageNum);
        }

        @Override
        public List<GotoLink> getPageGotoLinks(int pageNum) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.getPageGotoLinks(pageNum);
        }

        @Override
        public boolean isPdfLinearized() {
            mLoaderCallback.getBlocking();
            return mPdfDocument.isPdfLinearized();
        }

        @Override
        public int getFormType() {
            mLoaderCallback.getBlocking();
            return mPdfDocument.getFormType();
        }

        @Override
        protected void finalize() throws Throwable {
            mLoaderCallback.getBlocking();
            ensurePdfDestroyed();
            super.finalize();
        }

        private void ensurePdfDestroyed() {
            if (mPdfDocument != null) {
                try {
                    mPdfDocument.destroy();
                } catch (Throwable ignored) {
                }
            }
            mPdfDocument = null;
        }

        @Override
        public boolean cloneWithoutSecurity(ParcelFileDescriptor destination) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.cloneWithoutSecurity(destination);
        }

        @Override
        public boolean saveAs(ParcelFileDescriptor destination) {
            mLoaderCallback.getBlocking();
            return mPdfDocument.saveAs(destination);
        }
    }
}

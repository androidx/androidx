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

package androidx.pdf.viewer.loader;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.Opener;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.PdfDocumentRemote;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.service.PdfDocumentRemoteProto;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.util.TileBoard.TileInfo;

import java.lang.ref.WeakReference;

/**
 * Allows the caller to make asynchronous requests for data from a PdfDocument.
 * The caller must provide a {@link PdfLoaderCallbacks}, which is only held as a
 * {@link WeakReference} and called when the requested data is ready.
 * <p>
 * PdfLoader automatically opens and maintains a connection to the PdfDocumentService.
 * This connection must be {@link #disconnect}ed when no longer needed, and {@link #reconnect}ed to
 * when this activity is restarted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("UnusedVariable")
public class PdfLoader {
    private static final String TAG = PdfLoader.class.getSimpleName();

    final Context mContext;
    private final Opener mOpener;
    final PdfTaskExecutor mExecutor;
    final BitmapRecycler mBitmapRecycler;
    private final PdfConnection mConnection;

    // Keep a reference to the PDF data until it's successfully opened.
    private final DisplayData mData;
    private final boolean mHideTextAnnotations;

    private WeakPdfLoaderCallbacks mCallbacks;

    private final SparseArray<PdfPageLoader> mPageLoaders;
    private String mLoadedPassword;
    private int mNumPages;

    /** Creates a new {@link PdfLoader} and loads the document from the given data. */
    @NonNull
    public static PdfLoader create(@NonNull Context context, @NonNull DisplayData data,
            @NonNull BitmapRecycler bitmaps,
            @NonNull PdfLoaderCallbacks callbacks) {
        return create(context, data, bitmaps, callbacks, false /* hideTextAnnotations */);
    }

    /**
     * Creates a new {@link PdfLoader} and loads the document from the given data.
     *
     * @param hideTextAnnotations whether to skip rendering text and highlight annotations in the
     *                            PDF
     */
    @NonNull
    public static PdfLoader create(
            @NonNull Context context,
            @NonNull DisplayData data,
            @NonNull BitmapRecycler bitmaps,
            @NonNull PdfLoaderCallbacks callbacks,
            boolean hideTextAnnotations) {
        final WeakPdfLoaderCallbacks weakCallbacks = WeakPdfLoaderCallbacks.wrap(callbacks);
        PdfConnection conn = new PdfConnection(context);
        final PdfLoader pdfLoader =
                new PdfLoader(
                        context,
                        conn,
                        data,
                        bitmaps,
                        weakCallbacks,
                        hideTextAnnotations);
        conn.setOnConnectInitializer(
                () -> pdfLoader.mExecutor.schedule(pdfLoader.new LoadDocumentTask()));
        conn.setConnectionFailureHandler(() -> weakCallbacks.documentNotLoaded(PdfStatus.NONE));

        conn.connect(data.getUri());
        return pdfLoader;
    }

    PdfLoader(
            Context context,
            PdfConnection mConnection,
            DisplayData data,
            BitmapRecycler mBitmapRecycler,
            WeakPdfLoaderCallbacks callbacks,
            boolean hideTextAnnotations) {
        this.mContext = context;
        this.mOpener = new Opener(context);
        this.mConnection = mConnection;
        this.mData = data;
        this.mHideTextAnnotations = hideTextAnnotations;
        this.mExecutor = new PdfTaskExecutor();
        this.mExecutor.start();
        this.mBitmapRecycler = mBitmapRecycler;
        this.mCallbacks = callbacks;
        this.mPageLoaders = new SparseArray<>();
    }

    public int getNumPages() {
        return mNumPages;
    }

    public void setNumPages(int numPages) {
        mNumPages = numPages;
    }

    public void setCallbacks(@NonNull WeakPdfLoaderCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /** Schedule task to load a PdfDocument. */
    public void reloadDocument() {
        mExecutor.schedule(new LoadDocumentTask(mLoadedPassword));
    }

    /**
     * Reconnect to the PdfDocumentService if needed, after it may have been killed by the
     * system.
     */
    public void reconnect() {
        mConnection.connect(mData.getUri());
    }

    /** Disconnect from the PdfDocumentService, which will close itself and free its resources. */
    public void disconnect() {
        mConnection.disconnect();
    }

    /** Tries to re-open the PDF with the given password. */
    public void applyPassword(@NonNull String password) {
        mExecutor.schedule(new LoadDocumentTask(password));
    }

    /** Cancels all requests related to one page (bitmaps, texts,...). */
    public void cancel(int pageNum) {
        getPageLoader(pageNum).cancel();
    }

    /** Cancel all tasks except search and form-filling. */
    public void cancelExceptSearchAndFormFilling(int pageNum) {
        getPageLoader(pageNum).cancelExceptSearchAndFormFilling();
    }

    /**
     * Loads page dimensions for the given page - once it is ready, will call the
     * {@link PdfLoaderCallbacks#setPageDimensions} callback.
     */
    public void loadPageDimensions(int pageNum) {
        getPageLoader(pageNum).loadPageDimensions();
    }

    /**
     * Renders a bitmap for the given page - once it is ready, will call the
     * {@link PdfLoaderCallbacks#setPageBitmap} callback.
     */
    public void loadPageBitmap(int pageNum, @NonNull Dimensions pageSize) {
        getPageLoader(pageNum).loadPageBitmap(pageSize);
    }

    /**
     * Renders bitmaps for the given tiles - once it is ready, will call the
     * {@link PdfLoaderCallbacks#setPageBitmap} callback.
     */
    public void loadTileBitmaps(int pageNum, @NonNull Dimensions pageSize,
            @NonNull Iterable<TileInfo> tiles) {
        getPageLoader(pageNum).loadTilesBitmaps(pageSize, tiles);
    }

    /** Cancels requests for all tile bitmaps */
    public void cancelAllTileBitmaps(int pageNum) {
        getPageLoader(pageNum).cancelAllTileBitmaps();
    }

    /** Cancels requests for some tile bitmaps */
    public void cancelTileBitmaps(int pageNum, @NonNull Iterable<Integer> tileIds) {
        getPageLoader(pageNum).cancelTileBitmaps(tileIds);
    }

    /**
     * Loads text for the given page - once it is ready, will call the
     * {@link PdfLoaderCallbacks#setPageText} callback.
     */
    public void loadPageText(int pageNum) {
        getPageLoader(pageNum).loadPageText();
    }

    /**
     * Searches for the given term on the given page - once it is ready, will call the
     * {@link PdfLoaderCallbacks#setSearchResults} callback.
     */
    public void searchPageText(int pageNum, @NonNull String query) {
        getPageLoader(pageNum).searchPageText(query);
    }

    /**
     * Selects the text between the given two boundaries - once it is ready, will call the
     * the {@link PdfLoaderCallbacks#setSelection} callback.
     */
    public void selectPageText(int pageNum, @NonNull SelectionBoundary start,
            @NonNull SelectionBoundary stop) {
        getPageLoader(pageNum).selectPageText(start, stop);
    }

    /**
     * Finds all the url links on the page - once it is ready, will call the {@link
     * PdfLoaderCallbacks#setPageUrlLinks} callback.
     */
    public void loadPageUrlLinks(int pageNum) {
        getPageLoader(pageNum).loadPageLinks();
    }

    /**
     * Finds all the go-to links on the page - once it is ready, will call the {@link
     * PdfLoaderCallbacks#setPageGotoLinks} callback.
     */
    public void loadPageGotoLinks(int pageNum) {
        getPageLoader(pageNum).loadPageGotoLinks();
    }

    /** Cancels all data requested for every page. */
    public void cancelAll() {
        for (int i = 0; i < mPageLoaders.size(); i++) {
            mPageLoaders.valueAt(i).cancel();
        }
    }

    /**
     * Returns a {@link PdfDocumentRemote} which maybe ready or not (i.e. still initializing).
     */
    @NonNull
    protected PdfDocumentRemote getPdfDocument(@NonNull String forTask) {
        return mConnection.getPdfDocument(forTask);
    }

    protected void releasePdfDocument() {
        mConnection.releasePdfDocument();
    }

    /**
     * Returns a valid {@link PdfDocumentRemote} or null if there isn't one (i.e. the service is not
     * currently bound, or it is but still initializing).
     */
    @Nullable
    protected PdfDocumentRemote getLoadedPdfDocument(@NonNull String forTask) {
        return mConnection.isLoaded() ? mConnection.getPdfDocument(forTask) : null;
    }

    // Always returns a non-null callbacks - however the callbacks hold only a weak reference to the
    // PdfViewer, so it can be garbage collected if no longer in use, in which case the callbacks
    // all become no-ops.
    @NonNull
    public WeakPdfLoaderCallbacks getCallbacks() {
        return mCallbacks;
    }

    PdfPageLoader getPageLoader(int pageNum) {
        PdfPageLoader pageLoader = mPageLoaders.get(pageNum);
        if (pageLoader == null) {
            pageLoader = new PdfPageLoader(this, pageNum, mHideTextAnnotations);
            mPageLoaders.put(pageNum, pageLoader);
        }
        return pageLoader;
    }

    /** AsyncTask for loading a PdfDocument. */
    class LoadDocumentTask extends AbstractPdfTask<PdfStatus> {
        private final String mPassword;
//        private boolean mIsLinearized;

        LoadDocumentTask() {
            this(null);
        }

        LoadDocumentTask(String password) {
            super(PdfLoader.this, Priority.INITIALIZE);
            this.mPassword = password;
        }

        @Override
        protected String getLogTag() {
            return "LoadDocumentTask";
        }

        @Override
        protected PdfDocumentRemote getPdfDocument() {
            // This Task needs to work with an uninitialized PdfDocument.
            return PdfLoader.this.getPdfDocument(getLogTag());
        }

        @Override
        protected PdfStatus doInBackground(PdfDocumentRemoteProto pdfDocument)
                throws RemoteException {
            PdfStatus result;
            if (mConnection.isLoaded()) {
                // Already loaded, skip the loading process (e.g., during screen rotation).
                result = PdfStatus.LOADED;
            } else {
                if (mData == null) {
                    return PdfStatus.FILE_ERROR;
                }

                // NOTE: This filedescriptor is not closed since it continues to be used by Pdfium.
                // TODO: StrictMode- Look into filedescriptors more and document
                // exactly when they should be opened and closed, making sure they are not leaked.
                ParcelFileDescriptor fd = mData.openFd(mOpener);

                if (fd == null || fd.getFd() == -1) {
                    return PdfStatus.FILE_ERROR;
                }
                int statusIndex = pdfDocument.getPdfDocumentRemote().create(fd, mPassword);
                result = PdfStatus.values()[statusIndex];
            }

            if (result == PdfStatus.LOADED) {
                mNumPages = pdfDocument.getPdfDocumentRemote().numPages();
//                mIsLinearized = pdfDocument.getPdfDocumentRemote().isPdfLinearized();
            }
            return result;
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, PdfStatus status) {
            // TODO: Track the state of the FileInfo object.
            switch (status) {
                case LOADED:
                    mLoadedPassword = mPassword;
                    mConnection.setDocumentLoaded();
                    // TODO: Track loaded PDF info.
                    callbacks.documentLoaded(mNumPages);
                    break;
                case REQUIRES_PASSWORD:
                    // TODO: Reflect this in the state of the FileInfo object.
                    boolean wrongPasswordSupplied = !TextUtils.isEmpty(mPassword);
                    callbacks.requestPassword(wrongPasswordSupplied);
                    break;
                case FILE_ERROR:
                case PDF_ERROR:
                case NONE:
                    callbacks.documentNotLoaded(status);
                    break;
                default:
                    // TODO: Add default case to non-exhaustive switches on Java enums
            }
        }

        @Override
        protected void cleanup() { /* nothing to do. */ }

        @NonNull
        @Override
        public String toString() {
            return "LoadDocumentTask(" + mData + ")";
        }
    }
}

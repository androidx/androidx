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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.R;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.service.PdfDocumentRemoteProto;
import androidx.pdf.util.TileBoard.TileInfo;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Loads data for an individual page of the PDF document. Makes sure that if
 * the same data is requested more than once, it doesn't lead to duplicate
 * tasks being scheduled.
 * <p>
 * For all bitmap loading tasks, it will cancel any task for a lower or different resolution when
 * a new request is accepted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfPageLoader {
    public static final String TAG = PdfPageLoader.class.getSimpleName();

    /** Arbitrary dimensions used for pages that are broken. */
    private static final Dimensions DEFAULT_PAGE = new Dimensions(400, 400);

    private final PdfLoader mParent;
    private final int mPageNum;
    private final boolean mHideTextAnnotations;
    /** Currently scheduled tasks - null if no task of this type is scheduled. */
    GetDimensionsTask mDimensionsTask;
    RenderBitmapTask mBitmapTask;
    GetPageTextTask mTextTask;
    SearchPageTextTask mSearchPageTextTask;
    SelectionTask mSelectionTask;
    GetPageLinksTask mLinksTask;
    GetPageGotoLinksTask mGotoLinksTask;
    ReleasePageTask mReleasePageTask;

    /**
     * All currently scheduled tile tasks.
     *
     * <p>Map is preferred to SparseArray because of frequent access by key, and leaner API (e.g.
     * combined remove-get).
     */
    @SuppressLint({"UseSparseArrays", "BanConcurrentHashMap"})
    Map<Integer, RenderTileTask> mTileTasks =
            new java.util.concurrent.ConcurrentHashMap<Integer, RenderTileTask>();

    /** The reference pageWidth for all tile related tasks. */
    int mTilePageWidth;
    /**
     * This flag is set when this page makes pdfClient crash, and we'd better avoid crashing it
     * again.
     */
    private boolean mIsBroken = false;

    PdfPageLoader(PdfLoader parent, int pageNum, boolean hideTextAnnotations) {
        this.mParent = parent;
        this.mPageNum = pageNum;
        this.mHideTextAnnotations = hideTextAnnotations;
    }

    /** Schedule task to get the page's dimensions. */
    public void loadPageDimensions() {
        if (mDimensionsTask == null) {
            mDimensionsTask = new GetDimensionsTask();
            if (mIsBroken) {
                mDimensionsTask.reportError(mParent.getCallbacks());
            } else {
                mParent.mExecutor.schedule(mDimensionsTask);
            }
        }
    }

    private void cancelPageDimensions() {
        if (mDimensionsTask != null) {
            mDimensionsTask.cancel();
            mDimensionsTask = null;
        }
    }

    /** Schedule task to render a page as a bitmap. */
    public void loadPageBitmap(@NonNull Dimensions pageSize) {
        if (mBitmapTask != null && mBitmapTask.mDimensions.getWidth() < pageSize.getWidth()) {
            cancelPageBitmap();
        }
        if (mBitmapTask == null) {
            mBitmapTask = new RenderBitmapTask(pageSize);
            if (mIsBroken) {
                mBitmapTask.reportError(mParent.getCallbacks());
            } else {
                mParent.mExecutor.schedule(mBitmapTask);
            }
        }
    }

    private void cancelPageBitmap() {
        if (mBitmapTask != null) {
            mBitmapTask.cancel();
            mBitmapTask = null;
        }
    }

    /**
     * Loads the given tiles. If there are any pending tasks targeting a different page size that
     * this one, they are all canceled. If any tile request is a duplicate, it will not be
     * scheduled.
     */
    public void loadTilesBitmaps(@NonNull Dimensions pageSize, @NonNull Iterable<TileInfo> tiles) {
        if (!mTileTasks.isEmpty() && mTilePageWidth != pageSize.getWidth()) {
            cancelAllTileBitmaps();
        }
        if (!mIsBroken) {
            for (TileInfo tile : tiles) {
                RenderTileTask tileTask = new RenderTileTask(pageSize, tile);
                if (!mTileTasks.containsKey(tile.getIndex())) {
                    mTileTasks.put(tile.getIndex(), tileTask);
                    mParent.mExecutor.schedule(tileTask);
                }
            }
            mTilePageWidth = pageSize.getWidth();
        }
    }

    void cancelTileBitmaps(Iterable<Integer> tileIds) {
        for (int tileId : tileIds) {
            RenderTileTask task = mTileTasks.remove(tileId);
            if (task != null) {
                task.cancel();
            }
        }
    }

    void cancelAllTileBitmaps() {
        // Unusual iteration since cancelling a task removes it from the tileTasks,
        // which can lead to ConcurrentModificationException if we are iterating over it.
        Iterator<Entry<Integer, RenderTileTask>> it = mTileTasks.entrySet().iterator();
        while (it.hasNext()) {
            RenderTileTask task = it.next().getValue();
            it.remove();
            task.cancel();
        }
        mTileTasks.clear();
        mTilePageWidth = 0;
    }

    /** Schedule task to get a page's text. */
    public void loadPageText() {
        if (!mIsBroken && mTextTask == null) {
            mTextTask = new GetPageTextTask();
            mParent.mExecutor.schedule(mTextTask);
        }
    }

    private void cancelPageText() {
        if (mTextTask != null) {
            mTextTask.cancel();
            mTextTask = null;
        }
    }

    /** Schedule task to search a page's text. */
    public void searchPageText(@NonNull String query) {
        if (!mIsBroken && mSearchPageTextTask != null && !mSearchPageTextTask.mQuery.equals(
                query)) {
            cancelSearch();
        }
        if (mSearchPageTextTask == null) {
            mSearchPageTextTask = new SearchPageTextTask(query);
            mParent.mExecutor.schedule(mSearchPageTextTask);
        }
    }

    private void cancelSearch() {
        if (mSearchPageTextTask != null) {
            mSearchPageTextTask.cancel();
            mSearchPageTextTask = null;
        }
    }

    /** Schedule task to select some of the page text. */
    public void selectPageText(@NonNull SelectionBoundary start, @NonNull SelectionBoundary stop) {
        // These tasks will be requested almost continuously as long as the user
        // is dragging a handle - only start a new one if we finished the last one.
        if (mSelectionTask != null) {
            if (Objects.equals(start, stop)) {
                cancelSelect();  // New selection at a single point - cancel existing task.
            } else {
                return;  // Dragging: just wait for the currently running task to finish.
            }
        }
        if (!mIsBroken && mSelectionTask == null) {
            mSelectionTask = new SelectionTask(start, stop);
            mParent.mExecutor.schedule(mSelectionTask);
        }
    }

    private void cancelSelect() {
        if (mSelectionTask != null) {
            mSelectionTask.cancel();
            mSelectionTask = null;
        }
    }

    /** Schedule task to get a page's url links. */
    public void loadPageLinks() {
        if (!mIsBroken && mLinksTask == null) {
            mLinksTask = new GetPageLinksTask();
            mParent.mExecutor.schedule(mLinksTask);
        }
    }

    private void cancelLinks() {
        if (mLinksTask != null) {
            mLinksTask.cancel();
            mLinksTask = null;
        }
    }

    /** Schedule task to get a page's goto links. */
    public void loadPageGotoLinks() {
        if (!mIsBroken && mGotoLinksTask == null) {
            mGotoLinksTask = new GetPageGotoLinksTask();
            mParent.mExecutor.schedule(mGotoLinksTask);
        }
    }

    /** Releases object in memory related to a page when that page is no longer visible. */
    public void releasePage() {
        if (mReleasePageTask == null) {
            mReleasePageTask = new ReleasePageTask();
            mParent.mExecutor.schedule(mReleasePageTask);
        }
    }

    /**
     *
     */
    public void cancel() {
        cancelExceptSearchAndFormFilling();
        cancelSearch();
    }

    /** Cancel all tasks except search and form-filling. */
    public void cancelExceptSearchAndFormFilling() {
        cancelPageDimensions();
        cancelPageBitmap();
        cancelAllTileBitmaps();
        cancelPageText();
        cancelSelect();
        cancelLinks();
    }

    private void setBroken() {
        // TODO: Track the broken state of the FileInfo object.
        if (!mIsBroken) {
            mIsBroken = true;
        }
    }

    /** AsyncTask for getting a page's dimensions. */
    class GetDimensionsTask extends AbstractPdfTask<Dimensions> {

        GetDimensionsTask() {
            super(mParent, Priority.DIMENSIONS);
        }

        @Override
        protected String getLogTag() {
            return "GetDimensionsTask";
        }

        @Override
        protected Dimensions doInBackground(PdfDocumentRemoteProto pdfDocument)
                throws RemoteException {
            return pdfDocument.getPdfDocumentRemote().getPageDimensions(mPageNum);
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, Dimensions result) {
            callbacks.setPageDimensions(mPageNum, result);
        }

        @Override
        protected void reportError(PdfLoaderCallbacks callbacks) {
            setBroken();
            callbacks.setPageDimensions(mPageNum, DEFAULT_PAGE);
            callbacks.pageBroken(mPageNum);
        }

        @Override
        protected void cleanup() {
            mDimensionsTask = null;
        }
    }

    /** AsyncTask for rendering a page as a bitmap. */
    class RenderBitmapTask extends AbstractPdfTask<Bitmap> {

        final Dimensions mDimensions;

        RenderBitmapTask(Dimensions dimensions) {
            super(mParent, Priority.BITMAP);
            this.mDimensions = dimensions;
        }

        @Override
        protected String getLogTag() {
            return "RenderBitmapTask";
        }

        @Override
        protected Bitmap doInBackground(PdfDocumentRemoteProto pdfDocument) throws RemoteException {
            return pdfDocument.getPdfDocumentRemote().renderPage(mPageNum, mDimensions.getWidth(),
                    mDimensions.getHeight(),
                    mHideTextAnnotations);
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, Bitmap result) {
            if (result != null) {
                callbacks.setPageBitmap(mPageNum, result);
            }
        }

        @Override
        protected void cleanup() {
            mBitmapTask = null;
        }

        @Override
        protected void reportError(PdfLoaderCallbacks callbacks) {
            setBroken();
            callbacks.pageBroken(mPageNum);
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("RenderBitmapTask(page=%d width=%d height=%d)",
                    mPageNum, mDimensions.getWidth(), mDimensions.getHeight());
        }
    }

    /** AsyncTask for releasing page objects from memory after it is no longer visible. */
    class ReleasePageTask extends AbstractPdfTask<Void> {
        ReleasePageTask() {
            super(mParent, Priority.RELEASE);
        }

        @Override
        protected String getLogTag() {
            return "ReleasePageTask";
        }

        @Override
        protected Void doInBackground(PdfDocumentRemoteProto pdfDocument) throws RemoteException {
            pdfDocument.getPdfDocumentRemote().releasePage(mPageNum);
            return null;
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, Void unused) {
            /* no-op */
        }

        @Override
        protected void cleanup() {
            mReleasePageTask = null;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("ReleasePageTask(page=%d)", mPageNum);
        }
    }

    /** AsyncTask for rendering a tile as a bitmap. */
    class RenderTileTask extends AbstractPdfTask<Bitmap> {
        final Dimensions mPageSize;
        final TileInfo mTileInfo;

        RenderTileTask(Dimensions pageDimensions, TileInfo tileInfo) {
            super(mParent, Priority.BITMAP_TILE);
            this.mPageSize = pageDimensions;
            this.mTileInfo = tileInfo;
        }

        @Override
        protected String getLogTag() {
            return "RenderTileTask";
        }

        @Override
        protected Bitmap doInBackground(PdfDocumentRemoteProto pdfDocument) throws RemoteException {
            Point offset = mTileInfo.getOffset();
            return pdfDocument.getPdfDocumentRemote().renderTile(
                    mPageNum,
                    mTileInfo.getSize().getWidth(),
                    mTileInfo.getSize().getHeight(),
                    mPageSize.getWidth(),
                    mPageSize.getHeight(),
                    offset.x,
                    offset.y,
                    mHideTextAnnotations);
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, Bitmap result) {
            if (result != null) {
                callbacks.setTileBitmap(mPageNum, mTileInfo, result);
            }
        }

        @Override
        protected void cleanup() {
            mTileTasks.remove(mTileInfo.getIndex());
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("RenderTileTask(page=%d width=%d height=%d tile=%s)",
                    mPageNum, mPageSize.getWidth(), mPageSize.getHeight(), mTileInfo);
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof RenderTileTask) {
                RenderTileTask that = (RenderTileTask) o;
                return this.mPageSize.equals(that.mPageSize) && this.mTileInfo.equals(
                        that.mTileInfo);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return mTileInfo.hashCode();
        }
    }

    /** AsyncTask for getting a page's text. */
    class GetPageTextTask extends AbstractPdfTask<String> {
        GetPageTextTask() {
            super(mParent, Priority.TEXT);
        }

        @Override
        protected String getLogTag() {
            return "GetPageTextTask";
        }

        @Override
        protected String doInBackground(PdfDocumentRemoteProto pdfDocument) throws RemoteException {
            if (TaskDenyList.sDisableAltText) {
                return pdfDocument.getPdfDocumentRemote().getPageText(mPageNum);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(pdfDocument.getPdfDocumentRemote().getPageText(mPageNum));
                sb.append("\r\n");
                for (String altText : pdfDocument.getPdfDocumentRemote().getPageAltText(mPageNum)) {
                    // Format the alt text appropriately, so that user knows it is alt text.
                    altText = mParent.mContext.getString(R.string.desc_image_alt_text, altText);
                    sb.append(altText).append("\r\n");
                }
                return sb.toString();
            }
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, String result) {
            callbacks.setPageText(mPageNum, result);
        }

        @Override
        protected void cleanup() {
            mTextTask = null;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("GetPageTextTask(page=%d)", mPageNum);
        }
    }

    /** AsyncTask for searching a page's text. */
    class SearchPageTextTask extends AbstractPdfTask<MatchRects> {
        private final String mQuery;

        SearchPageTextTask(String query) {
            super(mParent, Priority.SEARCH);
            this.mQuery = query;
        }

        @Override
        protected String getLogTag() {
            return "SearchPageTextTask";
        }

        @Override
        protected MatchRects doInBackground(PdfDocumentRemoteProto pdfDocument)
                throws RemoteException {
            return pdfDocument.getPdfDocumentRemote().searchPageText(mPageNum, mQuery);
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, MatchRects matches) {
            callbacks.setSearchResults(mQuery, mPageNum, matches);
        }

        @Override
        protected void cleanup() {
            mSearchPageTextTask = null;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("SearchPageTextTask(page=%d, query=\"%s\")", mPageNum, mQuery);
        }
    }

    /** AsyncTask for selecting some of a page's text. */
    class SelectionTask extends AbstractPdfTask<PageSelection> {
        private final SelectionBoundary mStart;
        private final SelectionBoundary mStop;

        SelectionTask(SelectionBoundary start, SelectionBoundary stop) {
            super(mParent, Priority.SELECT);
            this.mStart = start;
            this.mStop = stop;
        }

        @Override
        protected String getLogTag() {
            return "SelectionTask";
        }

        @Override
        protected PageSelection doInBackground(PdfDocumentRemoteProto pdfDocument)
                throws RemoteException {
            return pdfDocument.getPdfDocumentRemote().selectPageText(mPageNum, mStart, mStop);
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, PageSelection selection) {
            callbacks.setSelection(mPageNum, selection);
        }

        @Override
        protected void cleanup() {
            mSelectionTask = null;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("SelectionTask(page=%d, start=%s, stop=%s)", mPageNum, mStart,
                    mStop);
        }
    }

    /** AsyncTask for getting a page's url links. */
    class GetPageLinksTask extends AbstractPdfTask<LinkRects> {
        GetPageLinksTask() {
            super(mParent, Priority.LINKS);
        }

        @Override
        protected String getLogTag() {
            return "GetPageLinksTask";
        }

        @Override
        protected LinkRects doInBackground(PdfDocumentRemoteProto pdfDocument)
                throws RemoteException {
            if (TaskDenyList.sDisableLinks) {
                return LinkRects.NO_LINKS;
            } else {
                return pdfDocument.getPdfDocumentRemote().getPageLinks(mPageNum);
            }
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, LinkRects result) {
            callbacks.setPageUrlLinks(mPageNum, result);
        }

        @Override
        protected void cleanup() {
            mLinksTask = null;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("GetPageLinksTask(page=%d)", mPageNum);
        }
    }

    /** AsyncTask for getting a page's go-to links. */
    class GetPageGotoLinksTask extends AbstractPdfTask<List<GotoLink>> {
        GetPageGotoLinksTask() {
            // Go-to links are a subset of links so we will follow all link settings.
            super(mParent, Priority.LINKS);
        }

        @Override
        protected String getLogTag() {
            return "GetPageGotoLinksTask";
        }

        @Override
        protected List<GotoLink> doInBackground(PdfDocumentRemoteProto pdfDocument)
                throws RemoteException {
            if (TaskDenyList.sDisableLinks) {
                return Collections.emptyList();
            } else {
                return pdfDocument.getPdfDocumentRemote().getPageGotoLinks(mPageNum);
            }
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, List<GotoLink> links) {
            callbacks.setPageGotoLinks(mPageNum, links);
        }

        @Override
        protected void cleanup() {
            mGotoLinksTask = null;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("GetPageGotoLinksTask(page=%d)", mPageNum);
        }
    }
}

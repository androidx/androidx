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

package androidx.pdf.viewer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.R;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.FutureValue;
import androidx.pdf.data.FutureValues.SettableFutureValue;
import androidx.pdf.data.Openable;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.data.Range;
import androidx.pdf.fetcher.Fetcher;
import androidx.pdf.find.FindInFileListener;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.find.MatchCount;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.CycleRange;
import androidx.pdf.util.ErrorLog;
import androidx.pdf.util.ExternalLinks;
import androidx.pdf.util.GestureTracker;
import androidx.pdf.util.GestureTracker.GestureHandler;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.ObservableValue.ValueObserver;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.ProjectorContext;
import androidx.pdf.util.StrictModeUtils;
import androidx.pdf.util.ThreadUtils;
import androidx.pdf.util.TileBoard;
import androidx.pdf.util.TileBoard.TileInfo;
import androidx.pdf.util.Toaster;
import androidx.pdf.util.Uris;
import androidx.pdf.util.ZoomUtils;
import androidx.pdf.viewer.PageViewFactory.PageView;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.viewer.loader.PdfLoaderCallbacks;
import androidx.pdf.widget.FastScrollContentModel;
import androidx.pdf.widget.FastScrollView;
import androidx.pdf.widget.MosaicView.BitmapSource;
import androidx.pdf.widget.ZoomView;
import androidx.pdf.widget.ZoomView.ContentResizedMode;
import androidx.pdf.widget.ZoomView.FitMode;
import androidx.pdf.widget.ZoomView.InitialZoomMode;
import androidx.pdf.widget.ZoomView.RotateMode;
import androidx.pdf.widget.ZoomView.ZoomScroll;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link Viewer} that can display paginated PDFs. Each page is rendered in its own View.
 * Rendering is done in 2 passes:
 *
 * <ol>
 *   <li>Layout: Request the dimensions of the page and set them as measure for the image view,
 *   <li>Render: Create bitmap(s) at adequate dimensions and attach them to the page view.
 * </ol>
 *
 * <p>The layout pass is progressive: starts with a few first pages of the document, then reach
 * further as the user scrolls down (and ultimately spans the whole document). The rendering pass is
 * tightly limited to the currently visible pages. Pages that are scrolled past (become not visible)
 * have their bitmaps released to free up memory.
 *
 * <p>This is a {@link #SELF_MANAGED_CONTENTS} Viewer: its contents and internal models are kept
 * when the view is destroyed, and re-used when the view is re-created.
 *
 * <p>Major lifecycle events include:
 *
 * <ol>
 *   <li>{@link #onContentsAvailable} / {@link #onDestroy} : Content model is created.
 *   <li>{@link #onCreateView} / {@link #destroyView} : All views are created, the pdf service is
 *       connected.
 * </ol>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
public class PdfViewer extends LoadingViewer implements FastScrollContentModel {

    private static final String TAG = "PdfViewer";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /** {@link View#setElevation(float)} value for PDF Pages (API 21+). */
    private static final int PAGE_ELEVATION_DP = 2;

    /** Key for saving {@link #mPageLayoutReach} in bundles. */
    private static final String KEY_LAYOUT_REACH = "plr";

    private static final String KEY_SPACE_LEFT = "leftSpace";
    private static final String KEY_SPACE_TOP = "topSpace";
    private static final String KEY_SPACE_BOTTOM = "bottomSpace";
    private static final String KEY_SPACE_RIGHT = "rightSpace";
    private static final String KEY_QUIT_ON_ERROR = "quitOnError";
    private static final String KEY_EXIT_ON_CANCEL = "exitOnCancel";
    private static final String ACTION_ANNOTATE_PDF = "android.intent.action.ANNOTATE";
    private static final String PDF_MIME_TYPE = "application/pdf";

    /** Key to save/retrieve {@link #mEditingAuthorized} from Bundle. */
    private static final String KEY_EDITING_AUTHORIZED = "editingAuthorized";

    /** Single access to the PDF document: loads contents asynchronously (bitmaps, text,...) */
    private PdfLoader mPdfLoader;

    /** The file being displayed by this viewer. */
    private DisplayData mFileData;

    /** Callbacks of PDF loading asynchronous tasks. */
    @VisibleForTesting
    public final PdfLoaderCallbacks mPdfLoaderCallbacks;

    /** Observer of the page position that controls loading of relevant PDF assets. */
    private final ValueObserver<ZoomScroll> mZoomScrollObserver;

    /** Observer to be set when the view is created. */
    @Nullable
    private ValueObserver<ZoomScroll> mPendingScrollPositionObserver;

    private Object mScrollPositionObserverKey;

    /** The number of pages of this PDF, set to -1 when not available. */
    private int mNumPages = -1;

    /** The range of currently visible pages. */
    private Range mVisiblePages;

    /** The highest number page reached. */
    private int mMaxPage = -1;

    private int mInitialPageLayoutReach = 4;

    /** The number of pages that have been laid out in the document. */
    private int mPageLayoutReach;

    /** The last stable zoom: we only re-draw bitmaps at stable zoom (not during a gesture). */
    private float mStableZoom;

    private ZoomView mZoomView;

    private PaginatedView mPaginatedView;
    private PaginationModel mPaginationModel;

    private PageIndicator mPageIndicator;

    private SearchModel mSearchModel;
    private PdfSelectionModel mSelectionModel;
    private PdfSelectionHandles mSelectionHandles;
    private final ValueObserver<String> mSearchQueryObserver;
    private final ValueObserver<SelectedMatch> mSelectedMatchObserver;
    private final ValueObserver<PageSelection> mSelectionObserver;
    private final ValueObserver<Integer> mFastscrollerPositionObserver;
    private Object mFastscrollerPositionObserverKey;
    private FastScrollView mFastScrollView;

    private boolean mDocumentLoaded = false;
    /**
     * After the document content is saved over the original in InkActivity, we set this bit to true
     * so we know to callwhen the new document content is loaded.
     */
    private boolean mShouldRedrawOnDocumentLoaded = false;

    @Nullable
    private SettableFutureValue<Boolean> mPrintableVersionCallback;

    // Non-null when a save-as operation is in progress. Cleared when operation is complete and
    // value has been set with success/failure result.
    @Nullable
    private SettableFutureValue<Boolean> mSaveAsCallback;

    // Base padding for ZoomView in px as set in saveZoomViewBasePadding().
    private Rect mZoomViewBasePadding = new Rect();
    private boolean mZoomViewBasePaddingSaved;

    private boolean mWaitingOnSelectionToCreateInlineComment;
    private boolean mEditingAuthorized;

    /** Only interact with Queue on the main thread. */
    private final List<OnDimensCallback> mDimensCallbackQueue = new ArrayList<>();
    private Uri mLocalUri;
    private FrameLayout mPdfViewer;

    private FindInFileView mFindInFileView;

    private FloatingActionButton mAnnotationButton;

    /** Callback is called everytime dimensions for a page have loaded. */
    private interface OnDimensCallback {
        /** Return true to continue receiving callbacks, else false. */
        boolean onDimensLoaded(int pageNum);
    }

    public PdfViewer() {
        super(SELF_MANAGED_CONTENTS);
    }

    @Override
    public void configureShareScroll(boolean left, boolean right, boolean top, boolean bottom) {
        mZoomView.setShareScroll(left, right, top, bottom);
    }

    /**
     * If set, this Viewer will call {@link Activity#finish()} if it can't load the PDF. By default,
     * the value is false.
     */
    @CanIgnoreReturnValue
    public PdfViewer setQuitOnError(boolean quit) {
        getArguments().putBoolean(KEY_QUIT_ON_ERROR, quit);
        return this;
    }

    /**
     * If set, this viewer will finish the attached activity when the user presses cancel on the
     * prompt for the document password.
     */
    @CanIgnoreReturnValue
    public PdfViewer setExitOnPasswordCancel(boolean shouldExitOnPasswordCancel) {
        getArguments().putBoolean(KEY_EXIT_ON_CANCEL, shouldExitOnPasswordCancel);
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFetcher = Fetcher.build(getContext(), 1);
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        super.onCreateView(inflater, container, savedState);
        mPaginationModel = new PaginationModel();

        mPdfViewer = (FrameLayout) inflater.inflate(R.layout.pdf_viewer_container, container,
                false);
        mFindInFileView = mPdfViewer.findViewById(R.id.search);
        mFastScrollView = mPdfViewer.findViewById(R.id.fast_scroll_view);

        mZoomView = mFastScrollView.findViewById(R.id.zoom_view);
        mZoomView.setStraightenVerticalScroll(true);

        mZoomView
                .setFitMode(FitMode.FIT_TO_WIDTH)
                .setInitialZoomMode(InitialZoomMode.ZOOM_TO_FIT)
                .setRotateMode(RotateMode.KEEP_SAME_VIEWPORT_WIDTH)
                .setContentResizedModeX(ContentResizedMode.KEEP_SAME_RELATIVE);

        // Setting an id so that the View can restore itself. The Id has to be unique and
        // predictable. An alternative that doesn't require id is to rely on this Fragment's
        // onSaveInstanceState().
        mZoomView.setId(getId() * 100);
        mPaginatedView = mFastScrollView.findViewById(R.id.pdf_view);

        mVisiblePages = new Range();
        mPageLayoutReach = 0;

        mPageIndicator = new PageIndicator(getActivity(), mFastScrollView);
        applyReservedSpace();
        mFastscrollerPositionObserver.onChange(null, mFastScrollView.getScrollerPositionY().get());
        mFastscrollerPositionObserverKey =
                mFastScrollView.getScrollerPositionY().addObserver(mFastscrollerPositionObserver);

        // The view system requires the document loaded in order to be properly initialized, so
        // we delay anything view-related until ViewState.VIEW_READY.
        mZoomView.setVisibility(View.GONE);

        mFastScrollView.setScrollable(this);
        mFastScrollView.setId(getId() * 10);

        setUpEditFab();

        return mPdfViewer;
    }

    private void applyReservedSpace() {
        if (getArguments().containsKey(KEY_SPACE_TOP)) {
            saveZoomViewBasePadding();
            int left = getArguments().getInt(KEY_SPACE_LEFT, 0);
            int top = getArguments().getInt(KEY_SPACE_TOP, 0);
            int right = getArguments().getInt(KEY_SPACE_RIGHT, 0);
            int bottom = getArguments().getInt(KEY_SPACE_BOTTOM, 0);

            mPageIndicator.getView().setTranslationX(-right);

            mZoomView.setPadding(
                    mZoomViewBasePadding.left + left,
                    mZoomViewBasePadding.top + top,
                    mZoomViewBasePadding.right + right,
                    mZoomViewBasePadding.bottom + bottom);

            // Adjust the scroll bar to also include the same padding.
            mFastScrollView.setScrollbarMarginTop(mZoomView.getPaddingTop());
            mFastScrollView.setScrollbarMarginRight(right);
            mFastScrollView.setScrollbarMarginBottom(mZoomView.getPaddingBottom());
        }
    }

    /**
     * Saves the padding set on {@link ZoomView} following initial inflation from XML.
     *
     * <p>This does not have to be called immediately following inflation but <i>must</i> be called
     * before any methods change the padding on {@link ZoomView}.
     *
     * <p>This can be used by methods that need to set padding to (base padding + some other
     * dimension). If these values were obtained directly from {@link ZoomView} or this method was
     * allowed to execute multiple times it could result in padding expanding continually.
     */
    private void saveZoomViewBasePadding() {
        if (mZoomView == null || mZoomViewBasePaddingSaved) {
            return;
        }

        mZoomViewBasePadding =
                new Rect(
                        mZoomView.getPaddingLeft(),
                        mZoomView.getPaddingTop(),
                        mZoomView.getPaddingRight(),
                        mZoomView.getPaddingBottom());

        mZoomViewBasePadding.top +=
                getResources().getDimensionPixelSize(R.dimen.viewer_doc_additional_top_offset);

        mZoomViewBasePaddingSaved = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mZoomView.zoomScroll().addObserver(mZoomScrollObserver);
        if (mPendingScrollPositionObserver != null) {
            mScrollPositionObserverKey = mZoomView.zoomScroll().addObserver(
                    mPendingScrollPositionObserver);
            mPendingScrollPositionObserver = null;
        }
    }

    @Override
    protected void onContentsAvailable(DisplayData contents, @Nullable Bundle savedState) {
        mFileData = contents;

        // TODO: StrictMode- disk read 58ms.
        int lengthMb = StrictModeUtils.bypassAndReturn(() -> (int) (contents.length() >> 20));
        Log.v(TAG, "File length in MB: " + lengthMb);
        createContentModel(
                PdfLoader.create(
                        getActivity().getApplicationContext(),
                        contents,
                        TileBoard.DEFAULT_RECYCLER,
                        mPdfLoaderCallbacks,
                        false));

        if (savedState != null) {
            int layoutReach = savedState.getInt(KEY_LAYOUT_REACH);
            mEditingAuthorized = savedState.getBoolean(KEY_EDITING_AUTHORIZED);
            mInitialPageLayoutReach = Math.max(mInitialPageLayoutReach, layoutReach);
            Log.v(TAG, "Restore current reach " + mInitialPageLayoutReach);
        }
    }

    @Override
    protected void onEnter() {
        super.onEnter();
        // This is necessary for password protected PDF documents. If the user failed to produce the
        // correct password, we want to prompt for the correct password every time the film strip
        // comes back to this viewer.
        if (!mDocumentLoaded && mPdfLoader != null) {
            mPdfLoader.reconnect();
        }

        if (mPaginatedView != null && mPaginatedView.getChildCount() > 0) {
            loadPageAssets(mZoomView.zoomScroll().get());
        }
    }

    @Override
    public void onExit() {
        if (mVisiblePages != null && mVisiblePages.getLast() > mMaxPage) {
            mMaxPage = mVisiblePages.getLast();
        }

        super.onExit();
        if (!mDocumentLoaded && mPdfLoader != null) {
            // e.g. a password-protected pdf that wasn't loaded.
            mPdfLoader.disconnect();
        }

        if (mPaginatedView != null && mPaginatedView.getChildCount() > 0) {
            for (PageMosaicView page : mPaginatedView.getChildViews()) {
                page.clearTiles();
                if (mPdfLoader != null) {
                    mPdfLoader.cancelAllTileBitmaps(page.getPageNum());
                }
            }
        }
    }

    private void createContentModel(PdfLoader pdfLoader) {
        this.mPdfLoader = pdfLoader;

        mSearchModel = new SearchModel(pdfLoader);
        mSearchModel.query().addObserver(mSearchQueryObserver);
        mSearchModel.selectedMatch().addObserver(mSelectedMatchObserver);

        mSelectionModel = new PdfSelectionModel(pdfLoader);
        mSelectionModel.selection().addObserver(mSelectionObserver);

        mSelectionHandles = new PdfSelectionHandles(mSelectionModel, mZoomView, mPaginatedView);

    }

    private void destroyContentModel() {

        mPageIndicator = null;

        mSelectionHandles.destroy();
        mSelectionHandles = null;

        mSelectionModel.selection().removeObserver(mSelectionObserver);
        mSelectionModel = null;

        mSearchModel.selectedMatch().removeObserver(mSelectedMatchObserver);
        mSearchModel.query().removeObserver(mSearchQueryObserver);
        mSearchModel = null;

        mPdfLoader.disconnect();
        mPdfLoader = null;
        mDocumentLoaded = false;
    }

    /**
     *
     */
    public void setPassword(String password) {
        if (mPdfLoader != null) {
            mPdfLoader.applyPassword(password);
        }
    }

    @Override
    public void destroyView() {
        if (mZoomView != null) {
            mZoomView.zoomScroll().removeObserver(mZoomScrollObserver);
            if (mScrollPositionObserverKey != null) {
                mZoomView.zoomScroll().removeObserver(mScrollPositionObserverKey);
            }
            mZoomView = null;
        }

        if (mPaginatedView != null) {
            mPaginatedView.removeAllViews();
            mPaginationModel.removeObserver(mPaginatedView);
            mPaginatedView = null;
        }
        // Clears the model so we can start fresh if we rebuild views.
        mPaginationModel = new PaginationModel();
        mVisiblePages = null;

        if (mPdfLoader != null) {
            mPdfLoader.cancelAll();
            mPdfLoader.disconnect();
            mDocumentLoaded = false;
        }
        mZoomViewBasePadding = new Rect();
        mZoomViewBasePaddingSaved = false;
        super.destroyView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFastscrollerPositionObserverKey != null && mFastScrollView != null) {
            mFastScrollView.getScrollerPositionY().removeObserver(mFastscrollerPositionObserverKey);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPdfLoader != null) {
            destroyContentModel();
        }
        mPrintableVersionCallback = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LAYOUT_REACH, mPageLayoutReach);
        Log.v(TAG, "Saved current reach " + mPageLayoutReach);

        outState.putBoolean(KEY_EDITING_AUTHORIZED, mEditingAuthorized);
    }

    @Override
    public long getContentLength() {
        return getPageCount();
    }

    @Override
    public int getViewProgress() {
        if (mMaxPage > 0) {
            return (int) (((double) mMaxPage / getPageCount() * 100) * PROGRESS_SCALER);
        }
        return -1;
    }

    /**
     * Load the PDF document.
     *
     * @param fileUri URI of the document.
     */
    public void loadFile(@NonNull Uri fileUri) {
        Preconditions.checkNotNull(fileUri);
        try {
            validateFileUri(fileUri);
        } catch (SecurityException e) {
            // TODO Toaster.LONG.popToast(this, R.string.problem_with_file);
            Log.e(TAG, e.getMessage());
            finishActivity();
        }

        fetchFile(fileUri);
        mLocalUri = fileUri;
    }

    private void validateFileUri(Uri fileUri) {
        if (!Uris.isContentUri(fileUri) && !Uris.isFileUri(fileUri)) {
            throw new IllegalArgumentException("Only content and file uri is supported");
        }

        // TODO confirm this exception
        if (Uris.isFileUriInSamePackageDataDir(fileUri)) {
            throw new SecurityException(
                    "Disallow opening file:// URIs in the parent package's data directory for "
                            + "security reasons");
        }
    }

    private void fetchFile(@NonNull final Uri fileUri) {
        Preconditions.checkNotNull(fileUri);
        final String fileName = getFileName(fileUri);
        final FutureValue<Openable> openable;
        openable = mFetcher.loadLocal(fileUri);

        // Only make this visible when we know a file needs to be fetched.
        // TODO loadingScreen.setVisibility(View.VISIBLE);

        openable.get(
                new FutureValue.Callback<Openable>() {
                    @Override
                    public void available(Openable openable) {
                        viewerAvailable(fileUri, fileName, openable);
                    }

                    @Override
                    public void failed(Throwable thrown) {
                        ErrorLog.log(TAG, "fetchFile:" + fileUri.getScheme(), thrown);
                        finishActivity();
                    }

                    @Override
                    public void progress(float progress) {
                    }
                });
    }

    private void finishActivity() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Nullable
    private ContentResolver getResolver() {
        if (getActivity() != null) {
            return getActivity().getContentResolver();
        }
        return null;
    }

    private String getFileName(@NonNull Uri fileUri) {
        ContentResolver resolver = getResolver();
        return resolver != null ? Uris.extractName(fileUri, resolver) : Uris.extractFileName(
                fileUri);
    }

    private void viewerAvailable(Uri fileUri, String fileName, Openable openable) {
        DisplayData contents = new DisplayData(fileUri, fileName, openable);

        // TODO loadingScreen.setVisibility(View.GONE);

        startViewer(contents);
    }

    private void startViewer(@NonNull DisplayData contents) {
        Preconditions.checkNotNull(contents);

        setQuitOnError(true);
        setExitOnPasswordCancel(true);
        feed(contents);
        postEnter();
    }

    private int getPageCount() {
        return mNumPages;
    }

    /** Returns the page currently roughly centered in the view. */
    public int getViewingPage() {
        return (mVisiblePages != null) ? (mVisiblePages.getFirst() + mVisiblePages.getLast()) / 2
                : 0;
    }

    /**
     * Lay out some pages up to some distant page. Not guaranteed to lay out any pages: maybe all
     * pages, or at least enough pages, are already laid out.
     */
    private void maybeLayoutPages(int current) {
        int peekAhead = Math.min(current + 2, 100);
        int distantPage = Math.max(current + peekAhead, mInitialPageLayoutReach);
        layoutPages(distantPage);
    }

    /**
     * Lays out all the pages until {@code untilPage}, or equivalently so that {@code untilPage}s
     * are laid out. So calling with {@code untilPage = 10} will ensure pages 0-9 are laid out.
     *
     * @param untilPage The upper limit of the range of pages to be laid out. Cropped to the
     *                  number of pages of the document if this number was larger.
     */
    private void layoutPages(int untilPage) {
        if (mPdfLoader == null) {
            ErrorLog.log(TAG, "ERROR Can't layout pages as no pdfLoader " + mPageLayoutReach);
            return;
        }
        boolean pushed = false;
        int lastPage = Math.min(untilPage, getPageCount());
        int requestLayoutPage = mPageLayoutReach;
        while (requestLayoutPage < lastPage) {
            mPdfLoader.loadPageDimensions(requestLayoutPage);
            requestLayoutPage++;
            pushed = true;
        }

        if (pushed) {
            Log.v(TAG, "Pushed the boundaries of known pages to " + requestLayoutPage);
        }
    }

    private PageView createPage(final int pageNum) {
        BitmapSource bitmapSource =
                new BitmapSource() {

                    @Override
                    public void requestPageBitmap(Dimensions pageSize,
                            boolean alsoRequestingTiles) {
                        mPdfLoader.loadPageBitmap(pageNum, pageSize);
                    }

                    @Override
                    public void requestNewTiles(Dimensions pageSize, Iterable<TileInfo> tiles) {
                        mPdfLoader.loadTileBitmaps(pageNum, pageSize, tiles);
                    }

                    @Override
                    public void cancelTiles(Iterable<Integer> tileIds) {
                        mPdfLoader.cancelTileBitmaps(pageNum, tileIds);
                    }
                };
        Dimensions dimensions = mPaginationModel.getPageSize(pageNum);
        PageView pageView =
                PageViewFactory.createPageView(
                        getActivity(),
                        pageNum,
                        dimensions,
                        bitmapSource,
                        TileBoard.DEFAULT_RECYCLER,
                        mZoomView.zoomScroll());
        mPaginatedView.addView(pageView);

        GestureTracker gestureTracker = new GestureTracker("PageView", getActivity());
        pageView.asView().setOnTouchListener(gestureTracker);
        gestureTracker.setDelegateHandler(new PageTouchListener(pageView));

        PageMosaicView pageMosaicView = pageView.getPageView();
        // Setting Elevation only works if there is a background color.
        pageMosaicView.setBackgroundColor(Color.WHITE);
        pageMosaicView.setElevation(ProjectorContext.get().getScreen().pxFromDp(PAGE_ELEVATION_DP));
        return pageView;
    }

    /**
     * Convenience method that always returns a child view. However in the case it's just been
     * created, that View is not laid out.
     */
    private PageView getOrCreatePage(int pageNum) {
        PageView pageView = mPaginatedView.getViewAt(pageNum);
        if (pageView != null) {
            return pageView;
        }
        return createPage(pageNum);
    }

    private boolean isPageCreated(int pageNum) {
        return pageNum < mPaginationModel.getSize() && mPaginatedView.getViewAt(pageNum) != null;
    }

    private PageView getPage(int pageNum) {
        return mPaginatedView.getViewAt(pageNum);
    }

    private Range allPages() {
        return new Range(0, mPaginationModel.getSize() - 1);
    }

    private void lookAtSelection(SelectedMatch selection) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        if (selection.getPage() >= mPaginationModel.getSize()) {
            layoutPages(selection.getPage() + 1);
            return;
        }
        Rect rect = selection.getPageMatches().getFirstRect(selection.getSelected());
        int x = mPaginationModel.getLookAtX(selection.getPage(), rect.centerX());
        int y = mPaginationModel.getLookAtY(selection.getPage(), rect.centerY());
        mZoomView.centerAt(x, y);

        PageMosaicView pageView = getOrCreatePage(selection.getPage()).getPageView();
        pageView.setOverlay(selection.getOverlay());
    }

    private void loadPageAssets(ZoomScroll position) {

        // 1. Refresh visible pages and view area.
        mVisiblePages = computeVisibleRange(position);

        if (mVisiblePages.getLast() > mMaxPage) {
            mMaxPage = mVisiblePages.getLast();
        }

        // Change the resolution of the bitmaps only when a gesture is not in progress.
        if (position.stable || mStableZoom == 0) {
            mStableZoom = position.zoom;
        }

        mPaginationModel.setViewArea(mZoomView.getVisibleAreaInContentCoords());

        Range allPages = allPages();
        int prefetchRadius = 1; // Note: could make this variable depending on resolution.
        Range nearPages = expandRange(mVisiblePages, prefetchRadius, allPages);

        // 2. Release pages that we don't need anymore.
        Range[] gonePages = allPages.minus(nearPages);
        for (Range pages : gonePages) {
            // Keep Views around for now, we'll clear them in step (4) if applicable.
            clearPages(pages, false);
        }

        // 3. Bring minimal service to pages that we might need very soon
        Range[] invisibleNearPages = nearPages.minus(mVisiblePages);
        for (Range pages : invisibleNearPages) {
            loadPageOnly(pages);
        }

        // The step (4) below requires page Views to be created and laid out. So we create them here
        // and set this flag if that operation needs to wait for a layout pass.
        boolean requiresLayoutPass = false;
        for (int pageNum : mVisiblePages) {
            if (mPaginatedView.getViewAt(pageNum) == null) {
                createPage(pageNum);
                requiresLayoutPass = true;
            }
        }

        // 4. Refresh tiles and/or full pages.
        if (position.stable) {
            // Perform a full refresh on all visible pages
            if (requiresLayoutPass) {
                refreshPagesAfterLayout(mVisiblePages);
            } else {
                refreshPages(mVisiblePages);
            }
            for (Range pages : gonePages) {
                clearPages(pages, true);
            }
        } else if (mStableZoom == position.zoom) {
            // Just load a few more tiles in case of tile-scroll
            if (requiresLayoutPass) {
                refreshTilesAfterLayout(mVisiblePages);
            } else {
                refreshTiles(mVisiblePages);
            }
        }

        maybeLayoutPages(mVisiblePages.getLast());
    }

    private void refreshTiles(Range pages) {
        for (int page : pages) {
            PageMosaicView pageView = getOrCreatePage(page).getPageView();
            pageView.requestTiles();
        }
    }

    private void refreshTilesAfterLayout(final Range pages) {
        ThreadUtils.postOnUiThread(
                () -> {
                    if (viewState().get() != ViewState.NO_VIEW) {
                        refreshTiles(pages);
                    }
                });
    }

    private void loadPageOnly(Range pages) {
        for (int page : pages) {
            mPdfLoader.cancelAllTileBitmaps(page);
            PageMosaicView pageView = getOrCreatePage(page).getPageView();
            pageView.clearTiles();
            pageView.requestFastDrawAtZoom(mStableZoom);
            loadVisiblePageText(page);
            maybeLoadFormAccessibilityInfo(page);
        }
    }

    private void refreshPages(Range pages) {
        for (int page : pages) {
            PageMosaicView pageView = getOrCreatePage(page).getPageView();
            pageView.requestDrawAtZoom(mStableZoom);
            loadVisiblePageText(page);
            maybeLoadFormAccessibilityInfo(page);
        }
    }

    private void refreshPagesAfterLayout(final Range pages) {
        ThreadUtils.postOnUiThread(
                () -> {
                    if (viewState().get() != ViewState.NO_VIEW) {
                        refreshPages(pages);
                    }
                });
    }

    private void clearPages(Range pages, boolean clearViews) {
        for (int page : pages) {
            // Don't cancel search - search results for the current search are always useful,
            // even for pages we can't see right now. Form filling operations should always
            // be executed against the document, even if the user has scrolled away from the page.
            mPdfLoader.cancelExceptSearchAndFormFilling(page);
            if (clearViews) {
                mPaginatedView.removeViewAt(page);
            }
        }
    }

    private void loadVisiblePageText(int page) {
        PageView pageView = getOrCreatePage(page);
        PageMosaicView pageMosaicView = pageView.getPageView();
        if (pageMosaicView.needsPageText()) {
            mPdfLoader.loadPageText(page);
        }
        if (!pageMosaicView.hasPageUrlLinks()) {
            mPdfLoader.loadPageUrlLinks(page);
        }

        if (page == mSelectionModel.getPage()) {
            pageMosaicView.setOverlay(new PdfHighlightOverlay(mSelectionModel.selection().get()));
        } else if (mSearchModel.query().get() != null) {
            if (!pageMosaicView.hasOverlay()) {
                mPdfLoader.searchPageText(page, mSearchModel.query().get());
            }
        } else {
            pageMosaicView.setOverlay(null);
        }
    }

    /**
     * Load accessibility information for the form if document can be edited and accessibility is
     * required.
     */
    private void maybeLoadFormAccessibilityInfo(int pageNum) {
        getOrCreatePage(pageNum);
    }

    /** Computes the range of visible pages in the given position. */
    private Range computeVisibleRange(ZoomScroll position) {
        int top = Math.round(position.scrollY / position.zoom);
        int bottom = Math.round((position.scrollY + mZoomView.getHeight()) / position.zoom);
        Range window = new Range(top, bottom);
        return mPaginationModel.getPagesInWindow(window, true);
    }

    /** Expand the range to include more page(s) in the each direction. */
    private static Range expandRange(Range range, int margin, Range allPages) {
        return range.expand(margin, allPages);
    }

    /**
     * Computes the range of pages that are entirely visible, or if no page is entirely visible,
     * returns the most visible page.
     */
    private Range computeImportantRange(ZoomScroll position) {
        int top = Math.round(position.scrollY / position.zoom);
        int bottom = Math.round((position.scrollY + mZoomView.getHeight()) / position.zoom);
        Range window = new Range(top, bottom);
        return mPaginationModel.getPagesInWindow(window, false);
    }

    { // Listen to ZoomView.
        mZoomScrollObserver =
                new ValueObserver<ZoomScroll>() {
                    @Override
                    public void onChange(ZoomScroll oldPosition, ZoomScroll position) {
                        loadPageAssets(position);
                        if (mPageIndicator.setRangeAndZoom(
                                computeImportantRange(position), position.zoom, position.stable)) {
                            showFastScrollView();
                        }

                        if (showEditFab()) {
                            if (position.scrollY > 0) {
                                mAnnotationButton.setVisibility(View.GONE);
                            } else if (position.scrollY == 0
                                    && mAnnotationButton.getVisibility() == View.GONE
                                    && mFindInFileView.getVisibility() == View.GONE) {
                                mAnnotationButton.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return TAG + "#zoomScrollObserver";
                    }
                };
    }

    { // Listen to searchModel.
        mSearchQueryObserver =
                new ValueObserver<String>() {
                    @Override
                    public void onChange(String oldQuery, String newQuery) {
                        mPaginatedView.clearAllOverlays();
                    }

                    @Override
                    public String toString() {
                        return TAG + "#searchQueryObserver";
                    }
                };

        mSelectedMatchObserver =
                new ValueObserver<SelectedMatch>() {
                    @Override
                    public void onChange(
                            @Nullable SelectedMatch oldSelection,
                            @Nullable SelectedMatch newSelection) {
                        if (newSelection == null) {
                            mPaginatedView.clearAllOverlays();
                            return;
                        }
                        if (oldSelection != null && isPageCreated(oldSelection.getPage())) {
                            // Selected match has moved onto a new page - update the overlay on
                            // the old page.
                            getPage(oldSelection.getPage())
                                    .getPageView()
                                    .setOverlay(
                                            new PdfHighlightOverlay(oldSelection.getPageMatches()));
                        }
                        lookAtSelection(newSelection);
                    }

                    @Override
                    public String toString() {
                        return TAG + "#selectedMatchObserver";
                    }
                };
    }

    { // Listen to selectionModel.
        mSelectionObserver =
                new ValueObserver<PageSelection>() {
                    @Override
                    public void onChange(PageSelection oldSelection, PageSelection newSelection) {
                        if (oldSelection != null && isPageCreated(oldSelection.getPage())) {
                            getPage(oldSelection.getPage()).getPageView().setOverlay(null);
                        }
                        if (newSelection != null && mVisiblePages.contains(
                                newSelection.getPage())) {
                            getOrCreatePage(newSelection.getPage())
                                    .getPageView()
                                    .setOverlay(new PdfHighlightOverlay(newSelection));
                        }
                    }

                    @NonNull
                    @Override
                    public String toString() {
                        return TAG + "#selectionObserver";
                    }
                };
    }

    {
        mFastscrollerPositionObserver =
                new ValueObserver<Integer>() {
                    @Override
                    public void onChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                        if (mPageIndicator != null && newValue != null) {
                            mPageIndicator.getView().setY(
                                    newValue - (mPageIndicator.getView().getHeight() / 2));
                            mPageIndicator.show();
                            showFastScrollView();
                        }
                    }

                    @Override
                    public String toString() {
                        return TAG + "#fastscrollerPositionObserver";
                    }
                };
    }

    private FindInFileListener makeFindInFileListener() {
        return new FindInFileListener() {
            @Override
            public boolean onQueryTextChange(@Nullable String query) {
                if (mSearchModel != null) {
                    mSearchModel.setQuery(query, getViewingPage());
                    return true;
                }
                return false;
            }

            @Override
            public boolean onFindNextMatch(String query, boolean backwards) {
                if (mSearchModel != null) {
                    CycleRange.Direction direction;
                    if (backwards) {
                        direction = CycleRange.Direction.BACKWARDS;
                        // TODO: Track "find previous" action event.
                    } else {
                        direction = CycleRange.Direction.FORWARDS;
                        // TODO: Track "find next" action event.
                    }
                    mSearchModel.selectNextMatch(direction, getViewingPage());
                    return true;
                }
                return false;
            }

            @Nullable
            @Override
            public ObservableValue<MatchCount> matchCount() {
                return mSearchModel != null ? mSearchModel.matchCount() : null;
            }
        };
    }

    /** Gesture listener for PageView's handling of tap and long press. */
    private class PageTouchListener extends GestureHandler {

        private final PageView mPageView;

        PageTouchListener(PageView pageView) {
            this.mPageView = pageView;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return handleSingleTapNoFormFilling(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            SelectionBoundary boundary =
                    SelectionBoundary.atPoint(new Point((int) e.getX(), (int) e.getY()));
            mPdfLoader.selectPageText(mPageView.getPageNum(), boundary, boundary);
        }

        // TODO: Add method to track enter form-filling via widget.

        /**
         * Handles a tap event for non-formfilling actions.
         *
         * <p>This is includes comments, links and full screen toggle. Separate from form filling as
         * form filling involves asynchronous evaluations that must be completed outside normal
         * branch
         * statements.
         */
        private boolean handleSingleTapNoFormFilling(MotionEvent e) {
            if (showEditFab()) {
                if (mAnnotationButton.getVisibility() == View.GONE
                        && mFindInFileView.getVisibility() == GONE) {
                    mAnnotationButton.setVisibility(View.VISIBLE);
                } else {
                    mAnnotationButton.setVisibility(View.GONE);
                }
            }
            boolean hadSelection =
                    mSelectionModel != null && mSelectionModel.selection().get() != null;
            if (hadSelection) {
                mSelectionModel.setSelection(null);
            }

            Point point = new Point((int) e.getX(), (int) e.getY());
            String linkUrl = mPageView.getPageView().getLinkUrl(point);
            if (linkUrl != null) {
                ExternalLinks.open(linkUrl, getActivity());
            }

            return true;
        }
    }

    // TODO: Revisit this method for its usage. Currently redundant

    /** Goes to the {@code pageNum} and fits the page to the current viewport. */
    private void gotoPage(int pageNum) {
        if (pageNum >= mPaginationModel.getSize()) {
            // We have not yet loaded our destination.
            layoutPages(pageNum + 1);
            mDimensCallbackQueue.add(
                    loadedPageNum -> {
                        if (pageNum == loadedPageNum) {
                            gotoPage(pageNum);
                            return false;
                        }
                        return true;
                    });
            return;
        }

        Rect pageRect = mPaginationModel.getPageLocation(pageNum);

        int x = pageRect.left + (pageRect.width() / 2);
        int y = pageRect.top + (pageRect.height() / 2);
        float zoom =
                ZoomUtils.calculateZoomToFit(
                        mZoomView.getViewportWidth(),
                        mZoomView.getViewportHeight(),
                        pageRect.width(),
                        pageRect.height());

        mZoomView.setZoom(zoom);
        mZoomView.centerAt(x, y);
    }

    { // Init pdfLoaderCallbacks
        mPdfLoaderCallbacks =
                new PdfLoaderCallbacks() {
                    // Callbacks should exit early if viewState == NO_VIEW (typically a Destroy
                    // is in progress).
                    @Override
                    public void requestPassword(boolean incorrect) {
                        throw new UnsupportedOperationException("TODO: PDF Password");
                    }

                    @Override
                    public void documentLoaded(int numPages) {
                        if (numPages <= 0) {
                            documentNotLoaded(PdfStatus.PDF_ERROR);
                            return;
                        }

                        mDocumentLoaded = true;
                        PdfViewer.this.mNumPages = numPages;
                        // Assume we see at least the first page
                        mMaxPage = 1;
                        if (viewState().get() != ViewState.NO_VIEW) {
                            mPaginationModel.initialize(numPages);
                            mPaginatedView.setModel(mPaginationModel);
                            mPaginationModel.addObserver(mPaginatedView);

                            maybeLayoutPages(1);
                            mPageIndicator.setNumPages(numPages);
                            mSearchModel.setNumPages(numPages);
                        }

                        if (mShouldRedrawOnDocumentLoaded) {
                            mShouldRedrawOnDocumentLoaded = false;
                        }

                        if (showEditFab()) {
                            mAnnotationButton.setVisibility(VISIBLE);
                        }
                    }

                    @Override
                    public void documentNotLoaded(PdfStatus status) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            if (getArguments().getBoolean(KEY_QUIT_ON_ERROR)) {
                                getActivity().finish();
                            }
                            switch (status) {
                                case NONE:
                                case FILE_ERROR:
                                    handleError();
                                    break;
                                case PDF_ERROR:
                                    Toaster.LONG.popToast(
                                            getActivity(), R.string.error_file_format_pdf,
                                            mFileData.getName());
                                    break;
                                case LOADED:
                                case REQUIRES_PASSWORD:
                                    Preconditions.checkArgument(
                                            false,
                                            "Document not loaded but status " + status.getNumber());
                                    break;
                                case PAGE_BROKEN:
                                case NEED_MORE_DATA:
                                    // no op.
                            }
                            // TODO: Tracker render error.
                        }
                    }

                    @Override
                    public void pageBroken(int page) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            getOrCreatePage(page)
                                    .getPageView()
                                    .setFailure(getString(R.string.error_on_page, page + 1));
                            Toaster.LONG.popToast(getActivity(), R.string.error_on_page, page + 1);
                            // TODO: Track render error.
                        }
                    }

                    @Override
                    public void setPageDimensions(int pageNum, Dimensions dimensions) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            mPaginationModel.addPage(pageNum, dimensions);
                            mPageLayoutReach = mPaginationModel.getSize();

                            if (mSearchModel.query().get() != null
                                    && mSearchModel.selectedMatch().get() != null
                                    && mSearchModel.selectedMatch().get().getPage() == pageNum) {
                                // lookAtSelection is posted to run once layout has finished:
                                ThreadUtils.postOnUiThread(
                                        () -> {
                                            if (viewState().get() != ViewState.NO_VIEW) {
                                                lookAtSelection(mSearchModel.selectedMatch().get());
                                            }
                                        });
                            }

                            ThreadUtils.postOnUiThread(
                                    () -> {
                                        if (mDimensCallbackQueue.isEmpty()
                                                || viewState().get() == ViewState.NO_VIEW) {
                                            return;
                                        }

                                        Iterator<OnDimensCallback> iterator =
                                                mDimensCallbackQueue.iterator();
                                        while (iterator.hasNext()) {
                                            OnDimensCallback callback = iterator.next();
                                            boolean shouldKeep = callback.onDimensLoaded(pageNum);
                                            if (!shouldKeep) {
                                                iterator.remove();
                                            }
                                        }
                                    });

                            // The new page might actually be visible on the screen, so we need
                            // to fetch assets:
                            Range newRange = computeVisibleRange(mZoomView.zoomScroll().get());
                            if (newRange.isEmpty()) {
                                // During fast-scroll, we mostly don't need to fetch assets, but
                                // make sure we keep pushing layout bounds far enough, and update
                                // page numbers as we "scroll" down.
                                if (mPageIndicator.setRangeAndZoom(newRange, mStableZoom, false)) {
                                    showFastScrollView();
                                }
                                maybeLayoutPages(newRange.getLast());
                            } else if (newRange.contains(pageNum)) {
                                // The new page is visible, fetch its assets.
                                loadPageAssets(mZoomView.zoomScroll().get());
                            }
                        }
                    }

                    @Override
                    public void setTileBitmap(int pageNum, TileInfo tileInfo, Bitmap bitmap) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setTileBitmap(tileInfo, bitmap);
                        }
                    }

                    @Override
                    public void setPageBitmap(int pageNum, Bitmap bitmap) {
                        // We announce that the viewer is ready as soon as a bitmap is loaded
                        // (not before).
                        if (mViewState.get() == ViewState.VIEW_CREATED) {
                            mZoomView.setVisibility(View.VISIBLE);
                            mViewState.set(ViewState.VIEW_READY);
                        }
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setPageBitmap(bitmap);
                        }
                    }

                    @Override
                    public void setPageText(int pageNum, String text) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setPageText(text);
                        }
                    }

                    @Override
                    public void setSearchResults(String query, int pageNum, MatchRects matches) {
                        if (viewState().get() != ViewState.NO_VIEW && query.equals(
                                mSearchModel.query().get())) {
                            mSearchModel.updateMatches(query, pageNum, matches);
                            if (isPageCreated(pageNum)) {
                                getPage(pageNum)
                                        .getPageView()
                                        .setOverlay(
                                                mSearchModel.getOverlay(query, pageNum, matches));
                            }
                        }
                    }

                    @Override
                    public void setSelection(int pageNum, @Nullable PageSelection selection) {
                        if (viewState().get() == ViewState.NO_VIEW) {
                            return;
                        }
                        if (selection != null) {
                            if (mWaitingOnSelectionToCreateInlineComment) {
                                mSelectionModel.setSelection(selection);
                                return;
                            }
                            // Clear searchModel - we hide the search and show the selection
                            // instead.
                            mSearchModel.setQuery(null, -1);
                        }
                        mSelectionModel.setSelection(selection);
                    }

                    @Override
                    public void setPageUrlLinks(int pageNum, LinkRects links) {
                        if (viewState().get() != ViewState.NO_VIEW && links != null
                                && isPageCreated(pageNum)) {
                            getPage(pageNum).setPageUrlLinks(links);
                        }
                    }

                    @Override
                    public void documentCloned(boolean result) {
                        if (mPrintableVersionCallback != null) {
                            mPrintableVersionCallback.set(result);
                        }
                        mPrintableVersionCallback = null;
                    }

                    @Override
                    public void documentSavedAs(boolean result) {
                        if (mSaveAsCallback != null) {
                            mSaveAsCallback.set(result);
                        }
                        mSaveAsCallback = null;
                    }

                    /**
                     * Receives areas of a page that have been invalidated by an editing action
                     * and asks the
                     * appropriate page view to redraw them.
                     */
                    @Override
                    public void setInvalidRects(int pageNum, List<Rect> invalidRects) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            if (invalidRects == null || invalidRects.isEmpty()) {
                                return;
                            }
                            mPaginatedView.getViewAt(pageNum).getPageView().requestRedrawAreas(
                                    invalidRects);
                        }
                    }
                };
    }

    @Override
    public float estimateFullContentHeight() {
        return mPaginationModel.getEstimatedFullHeight();
    }

    @Override
    public float visibleHeight() {
        return mZoomView.getViewportHeight() / mZoomView.getZoom();
    }

    @Override
    public void fastScrollTo(float position, boolean stable) {
        mZoomView.scrollTo(mZoomView.getScrollX(), (int) (position * mZoomView.getZoom()), stable);
    }

    @Override
    public void setFastScrollListener(final FastScrollListener listener) {
        mZoomView
                .getViewTreeObserver()
                .addOnScrollChangedListener(
                        new OnScrollChangedListener() {
                            @Override
                            public void onScrollChanged() {
                                if (mZoomView != null) {
                                    listener.updateFastScrollbar(
                                            mZoomView.getScrollY() / mZoomView.getZoom());
                                }
                            }
                        });
    }

    protected void handleError() {
        mViewState.set(ViewState.ERROR);
    }


    /** Create callback to retry password input when user cancels password prompt. */
    public void setPasswordCancelError() {

    }

    private void showFastScrollView() {
        if (mFastScrollView != null) {
            mFastScrollView.setVisible();
        }
    }

    /**
     * Set up the find in file menu.
     * @param visibility
     */
    public void setFindInFileView(boolean visibility) {
        if (visibility) {
            mFindInFileView.setVisibility(VISIBLE);
            setupFindInFileBtn();
        } else {
            mFindInFileView.setVisibility(GONE);
        }
    }

    private void setupFindInFileBtn() {
        mFindInFileView.setFindInFileListener(this.makeFindInFileListener());
        mFindInFileView.queryBoxRequestFocus();

        TextView queryBox = mFindInFileView.findViewById(R.id.find_query_box);
        ImageView close_button = mFindInFileView.findViewById(R.id.close_btn);
        close_button.setOnClickListener(view -> {
            View parentLayout = (View) close_button.getParent();
            queryBox.clearFocus();
            queryBox.setText("");
            parentLayout.setVisibility(GONE);
            if (showEditFab()) {
                mAnnotationButton.setVisibility(VISIBLE);
            }
        });
    }

    private void setUpEditFab() {
        mAnnotationButton = mPdfViewer.findViewById(R.id.edit_fab);
        mAnnotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performEdit();
            }
        });

    }

    private void performEdit() {
        Intent intent = getAnnotationIntent();
        intent.setData(mLocalUri);
        startActivity(intent);
    }

    private boolean showEditFab() {
        Intent intent = getAnnotationIntent();
        return intent.resolveActivity(getContext().getPackageManager()) != null;
    }

    private Intent getAnnotationIntent() {
        Intent intent = new Intent(ACTION_ANNOTATE_PDF);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(mLocalUri, PDF_MIME_TYPE);
        return intent;
    }
}

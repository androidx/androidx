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

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.pdf.R;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.ErrorType;
import androidx.pdf.data.FutureValue;
import androidx.pdf.data.Openable;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.data.Range;
import androidx.pdf.fetcher.Fetcher;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.select.SelectionActionMode;
import androidx.pdf.util.AnnotationUtils;
import androidx.pdf.util.ObservableValue.ValueObserver;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.Screen;
import androidx.pdf.util.ThreadUtils;
import androidx.pdf.util.TileBoard;
import androidx.pdf.util.TileBoard.TileInfo;
import androidx.pdf.util.Toaster;
import androidx.pdf.util.Uris;
import androidx.pdf.viewer.PageViewFactory.PageView;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.viewer.loader.PdfLoaderCallbacks;
import androidx.pdf.widget.FastScrollView;
import androidx.pdf.widget.ZoomView;
import androidx.pdf.widget.ZoomView.ZoomScroll;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
public class PdfViewer extends LoadingViewer {

    private static final String TAG = "PdfViewer";

    /** {@link View#setElevation(float)} value for PDF Pages (API 21+). */
    private static final int PAGE_ELEVATION_DP = 2;

    /** Key for saving page layout reach in bundles. */
    private static final String KEY_LAYOUT_REACH = "plr";
    private static final String KEY_QUIT_ON_ERROR = "quitOnError";
    private static final String KEY_EXIT_ON_CANCEL = "exitOnCancel";

    private static Screen sScreen;

    /** Single access to the PDF document: loads contents asynchronously (bitmaps, text,...) */
    private PdfLoader mPdfLoader;

    /** The file being displayed by this viewer. */
    private DisplayData mFileData;

    /** Callbacks of PDF loading asynchronous tasks. */
    @VisibleForTesting
    public final PdfLoaderCallbacks mPdfLoaderCallbacks;

    /** Observer of the page position that controls loading of relevant PDF assets. */
    private ValueObserver<ZoomScroll> mZoomScrollObserver;

    /** Observer to be set when the view is created. */
    @Nullable
    private ValueObserver<ZoomScroll> mPendingScrollPositionObserver;

    private Object mScrollPositionObserverKey;

    private ZoomView mZoomView;

    private PaginatedView mPaginatedView;
    private PaginationModel mPaginationModel;

    private SearchModel mSearchModel;
    private PdfSelectionModel mSelectionModel;
    private PdfSelectionHandles mSelectionHandles;

    private ValueObserver<String> mSearchQueryObserver;
    private ValueObserver<SelectedMatch> mSelectedMatchObserver;
    private ValueObserver<PageSelection> mSelectionObserver;

    private FastScrollView mFastScrollView;
    private ProgressBar mLoadingSpinner;

    private boolean mDocumentLoaded = false;
    private boolean mIsAnnotationIntentResolvable = false;

    /**
     * After the document content is saved over the original in InkActivity, we set this bit to true
     * so we know to call when the new document content is loaded.
     */
    private boolean mShouldRedrawOnDocumentLoaded = false;
    private Snackbar mSnackbar;

    private LayoutHandler mLayoutHandler;

    private Uri mLocalUri;
    private FrameLayout mPdfViewer;

    private FindInFileView mFindInFileView;

    private FloatingActionButton mAnnotationButton;

    private PageViewFactory mPageViewFactory;

    private SingleTapHandler mSingleTapHandler;

    private SelectionActionMode mSelectionActionMode;

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
    @NonNull
    @CanIgnoreReturnValue
    public PdfViewer setQuitOnError(boolean quit) {
        getArguments().putBoolean(KEY_QUIT_ON_ERROR, quit);
        return this;
    }

    /**
     * If set, this viewer will finish the attached activity when the user presses cancel on the
     * prompt for the document password.
     */
    @NonNull
    @CanIgnoreReturnValue
    public PdfViewer setExitOnPasswordCancel(boolean shouldExitOnPasswordCancel) {
        getArguments().putBoolean(KEY_EXIT_ON_CANCEL, shouldExitOnPasswordCancel);
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFetcher = Fetcher.build(getContext(), 1);
        sScreen = new Screen(this.requireActivity().getApplicationContext());
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedState) {
        super.onCreateView(inflater, container, savedState);

        mPdfViewer = (FrameLayout) inflater.inflate(R.layout.pdf_viewer_container, container,
                false);
        mFindInFileView = mPdfViewer.findViewById(R.id.search);
        mFastScrollView = mPdfViewer.findViewById(R.id.fast_scroll_view);
        mPaginatedView = mPdfViewer.findViewById(R.id.pdf_view);
        mPaginationModel = mPaginatedView.getModel();
        mZoomView = mPdfViewer.findViewById(R.id.zoom_view);
        mLoadingSpinner = mPdfViewer.findViewById(R.id.progress_indicator);
        setUpEditFab();

        return mPdfViewer;
    }

    @Nullable
    public static Screen getScreen() {
        return sScreen;
    }

    @VisibleForTesting
    public static void setScreenForTest(@NonNull Context context) {
        sScreen = new Screen(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mPendingScrollPositionObserver != null) {
            mScrollPositionObserverKey = mZoomView.zoomScroll().addObserver(
                    mPendingScrollPositionObserver);
            mPendingScrollPositionObserver = null;
        }
    }

    @Override
    protected void onContentsAvailable(@NonNull DisplayData contents, @Nullable Bundle savedState) {
        mFileData = contents;
        mLocalUri = contents.getUri();

        createContentModel(
                PdfLoader.create(
                        getActivity().getApplicationContext(),
                        contents,
                        TileBoard.DEFAULT_RECYCLER,
                        mPdfLoaderCallbacks,
                        false));
        mLayoutHandler = new LayoutHandler(mPdfLoader);
        mZoomView.setPdfSelectionModel(mSelectionModel);
        mPaginatedView.setSelectionModel(mSelectionModel);
        mPaginatedView.setSearchModel(mSearchModel);
        mPaginatedView.setPdfLoader(mPdfLoader);

        mSearchQueryObserver =
                new SearchQueryObserver(mPaginatedView);
        mSearchModel.query().addObserver(mSearchQueryObserver);

        mSingleTapHandler = new SingleTapHandler(getContext(), mAnnotationButton, mPaginatedView,
                mFindInFileView, mZoomView, mSelectionModel, mPaginationModel, mLayoutHandler);
        mPageViewFactory = new PageViewFactory(requireContext(), mPdfLoader,
                mPaginatedView, mZoomView, mSingleTapHandler, mFindInFileView);
        mPaginatedView.setPageViewFactory(mPageViewFactory);

        mSelectionObserver =
                new PageSelectionValueObserver(mPaginatedView, mPaginationModel, mPageViewFactory,
                        requireContext());
        mSelectionModel.selection().addObserver(mSelectionObserver);

        mSelectedMatchObserver =
                new SelectedMatchValueObserver(mPaginatedView, mPaginationModel, mPageViewFactory,
                        mZoomView, mLayoutHandler, requireContext());
        mSearchModel.selectedMatch().addObserver(mSelectedMatchObserver);

        mFindInFileView.setPaginatedView(mPaginatedView);
        mFindInFileView.setAnnotationIntentResolvable(mIsAnnotationIntentResolvable);

        if (savedState != null) {
            int layoutReach = savedState.getInt(KEY_LAYOUT_REACH);
            mLayoutHandler.setInitialPageLayoutReachWithMax(layoutReach);
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
            mZoomView.loadPageAssets(mLayoutHandler, null);
        }
    }

    @Override
    public void onExit() {
        super.onExit();
        if (!mDocumentLoaded && mPdfLoader != null) {
            // e.g. a password-protected pdf that wasn't loaded.
            mPdfLoader.disconnect();
        }
    }

    private void createContentModel(PdfLoader pdfLoader) {
        this.mPdfLoader = pdfLoader;
        mFindInFileView.setPdfLoader(pdfLoader);

        mSearchModel = mFindInFileView.getSearchModel();

        mSelectionModel = new PdfSelectionModel(pdfLoader);

        mSelectionHandles = new PdfSelectionHandles(mSelectionModel, mZoomView, mPaginatedView,
                mSelectionActionMode);

    }

    private void destroyContentModel() {
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
    public void setPassword(@NonNull String password) {
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
            mPaginatedView = null;
        }

        if (mPdfLoader != null) {
            mPdfLoader.cancelAll();
            mPdfLoader.disconnect();
            mDocumentLoaded = false;
        }
        super.destroyView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPdfLoader != null) {
            destroyContentModel();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_LAYOUT_REACH, mLayoutHandler.getPageLayoutReach());
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
            finishActivity();
        }

        showSpinner();
        fetchFile(fileUri);
        mLocalUri = fileUri;
        mIsAnnotationIntentResolvable = AnnotationUtils.resolveAnnotationIntent(requireContext(),
                mLocalUri);
        mSingleTapHandler.setAnnotationIntentResolvable(mIsAnnotationIntentResolvable);
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
                    public void failed(@NonNull Throwable thrown) {
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
        setExitOnPasswordCancel(false);
        feed(contents);
        postEnter();
    }

    private boolean isPageCreated(int pageNum) {
        return pageNum < mPaginationModel.getSize() && mPaginatedView.getViewAt(pageNum) != null;
    }

    private PageView getPage(int pageNum) {
        return mPaginatedView.getViewAt(pageNum);
    }

    private void lookAtSelection(SelectedMatch selection) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        if (selection.getPage() >= mPaginationModel.getSize()) {
            mLayoutHandler.layoutPages(selection.getPage() + 1);
            return;
        }
        Rect rect = selection.getPageMatches().getFirstRect(selection.getSelected());
        int x = mPaginationModel.getLookAtX(selection.getPage(), rect.centerX());
        int y = mPaginationModel.getLookAtY(selection.getPage(), rect.centerY());
        mZoomView.centerAt(x, y);

        PageMosaicView pageView = (PageMosaicView) mPageViewFactory.getOrCreatePageView(
                selection.getPage(),
                sScreen.pxFromDp(PAGE_ELEVATION_DP),
                mPaginationModel.getPageSize(selection.getPage()));
        pageView.setOverlay(selection.getOverlay());
    }

    /** Show the loading spinner. */
    @UiThread
    public void showSpinner() {
        if (mLoadingSpinner != null) {
            mLoadingSpinner.post(() -> mLoadingSpinner.setVisibility(View.VISIBLE));
        }
    }

    /** Hide the loading spinner. */
    @UiThread
    public void hideSpinner() {
        if (mLoadingSpinner != null) {
            mLoadingSpinner.post(() -> mLoadingSpinner.setVisibility(View.GONE));
        }
    }

    // TODO: Revisit this method for its usage. Currently redundant

    { // Init pdfLoaderCallbacks
        mPdfLoaderCallbacks =
                new PdfLoaderCallbacks() {
                    static final String PASSWORD_DIALOG_TAG = "password-dialog";

                    @Nullable
                    private PdfPasswordDialog currentPasswordDialog(@Nullable FragmentManager fm) {
                        if (fm != null) {
                            Fragment passwordDialog = fm.findFragmentByTag(PASSWORD_DIALOG_TAG);
                            if (passwordDialog instanceof PdfPasswordDialog) {
                                return (PdfPasswordDialog) passwordDialog;
                            }
                        }
                        return null;
                    }

                    // Callbacks should exit early if viewState == NO_VIEW (typically a Destroy
                    // is in progress).
                    @Override
                    @SuppressWarnings("deprecation")
                    public void requestPassword(boolean incorrect) {
                        mIsPasswordProtected = true;

                        if (!isShowing()) {
                            // This would happen if the service decides to start while we're in
                            // the background.
                            // The dialog code below would then crash. We can't just bypass it
                            // because then we'd
                            // have
                            // a started service with no loaded PDF and no means to load it. The
                            // best way is to
                            // just
                            // kill the service which will restart on the next onStart.
                            if (mPdfLoader != null) {
                                mPdfLoader.disconnect();
                            }
                            return;
                        }

                        if (viewState().get() != ViewState.NO_VIEW) {
                            FragmentManager fm = requireActivity().getSupportFragmentManager();

                            PdfPasswordDialog passwordDialog = currentPasswordDialog(fm);
                            if (passwordDialog == null) {
                                passwordDialog = new PdfPasswordDialog();
                                passwordDialog.setTargetFragment(PdfViewer.this, 0);
                                passwordDialog.setFinishOnCancel(
                                        getArguments().getBoolean(KEY_EXIT_ON_CANCEL));
                                passwordDialog.show(fm, PASSWORD_DIALOG_TAG);
                            }

                            if (incorrect) {
                                passwordDialog.retry();
                            }
                        }
                    }

                    @Override
                    public void documentLoaded(int numPages, @NonNull DisplayData data) {
                        if (numPages <= 0) {
                            documentNotLoaded(PdfStatus.PDF_ERROR);
                            return;
                        }

                        mDocumentLoaded = true;
                        hideSpinner();

                        // Assume we see at least the first page
                        mPaginatedView.getPageRangeHandler().setMaxPage(1);
                        if (viewState().get() != ViewState.NO_VIEW) {
                            mPaginationModel.initialize(numPages);
                            mFastScrollView.setPaginationModel(mPaginationModel);

                            dismissPasswordDialog();
                            mLayoutHandler.maybeLayoutPages(1);
                            mSearchModel.setNumPages(numPages);
                        }

                        if (mShouldRedrawOnDocumentLoaded) {
                            mShouldRedrawOnDocumentLoaded = false;
                        }

                        if (mIsAnnotationIntentResolvable) {
                            mAnnotationButton.setVisibility(VISIBLE);
                        }
                    }

                    @Override
                    public void documentNotLoaded(@NonNull PdfStatus status) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            dismissPasswordDialog();
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
                            ((PageMosaicView) mPageViewFactory.getOrCreatePageView(
                                    page,
                                    sScreen.pxFromDp(PAGE_ELEVATION_DP),
                                    mPaginationModel.getPageSize(page)))
                                    .setFailure(getString(R.string.error_on_page, page + 1));
                            Toaster.LONG.popToast(getActivity(), R.string.error_on_page, page + 1);
                            // TODO: Track render error.
                        }
                    }

                    @SuppressWarnings("deprecation")
                    private void dismissPasswordDialog() {
                        DialogFragment passwordDialog = currentPasswordDialog(
                                requireActivity().getSupportFragmentManager());
                        if (passwordDialog != null
                                && PdfViewer.this.equals(passwordDialog.getTargetFragment())) {
                            passwordDialog.dismiss();
                        }
                    }

                    @Override
                    public void setPageDimensions(int pageNum, @NonNull Dimensions dimensions) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            mPaginationModel.addPage(pageNum, dimensions);
                            mLayoutHandler.setPageLayoutReach(mPaginationModel.getSize());

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

                            // The new page might actually be visible on the screen, so we need
                            // to fetch assets:
                            ZoomScroll position = mZoomView.zoomScroll().get();
                            Range newRange =
                                    mPaginatedView.getPageRangeHandler().computeVisibleRange(
                                            position.scrollY, position.zoom, mZoomView.getHeight(),
                                            true);
                            if (newRange.isEmpty()) {
                                mLayoutHandler.maybeLayoutPages(newRange.getLast());
                            } else if (newRange.contains(pageNum)) {
                                // The new page is visible, fetch its assets.
                                mZoomView.loadPageAssets(mLayoutHandler, null);
                            }
                        }
                    }

                    @Override
                    public void setTileBitmap(int pageNum, @NonNull TileInfo tileInfo,
                            @NonNull Bitmap bitmap) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setTileBitmap(tileInfo, bitmap);
                        }
                    }

                    @Override
                    public void setPageBitmap(int pageNum, @NonNull Bitmap bitmap) {
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
                    public void setPageText(int pageNum, @NonNull String text) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setPageText(text);
                        }
                    }

                    @Override
                    public void setSearchResults(@NonNull String query, int pageNum,
                            @NonNull MatchRects matches) {
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
                            // Clear searchModel - we hide the search and show the selection
                            // instead.
                            mSearchModel.setQuery(null, -1);
                        }
                        mSelectionModel.setSelection(selection);
                    }

                    @Override
                    public void setPageUrlLinks(int pageNum, @NonNull LinkRects links) {
                        if (viewState().get() != ViewState.NO_VIEW && links != null
                                && isPageCreated(pageNum)) {
                            getPage(pageNum).setPageUrlLinks(links);
                        }
                    }

                    @Override
                    public void setPageGotoLinks(int pageNum, @NonNull List<GotoLink> links) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).setPageGotoLinks(links);
                        }
                    }

                    /**
                     * Receives areas of a page that have been invalidated by an editing action
                     * and asks the
                     * appropriate page view to redraw them.
                     */
                    @Override
                    public void setInvalidRects(int pageNum, @NonNull List<Rect> invalidRects) {
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

    protected void handleError() {
        mViewState.set(ViewState.ERROR);
    }


    /** Create callback to retry password input when user cancels password prompt. */
    public void setPasswordCancelError() {
        Runnable retryCallback = () -> mPdfLoaderCallbacks.requestPassword(false);
        displayViewerError(ErrorType.FILE_PASSWORD_PROTECTED, this, retryCallback);
    }

    private void displayViewerError(ErrorType errorType, Viewer viewer, Runnable actionCallback) {
        switch (errorType) {
            case FILE_PASSWORD_PROTECTED:
                showSnackBar(R.string.password_not_entered, R.string.retry_button_text,
                        actionCallback);
                return;
            default:
                break;
        }

    }

    private void showSnackBar(int text, int actionText, Runnable actionCallback) {
        mSnackbar = Snackbar.make(mPdfViewer, text, Snackbar.LENGTH_INDEFINITE);
        View.OnClickListener mResolveClickListener =
                v -> {
                    actionCallback.run();
                };
        mSnackbar.setAction(actionText, mResolveClickListener);
        mSnackbar.show();
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
        Intent intent = AnnotationUtils.getAnnotationIntent(mLocalUri);
        intent.setData(mLocalUri);
        startActivity(intent);
    }

}

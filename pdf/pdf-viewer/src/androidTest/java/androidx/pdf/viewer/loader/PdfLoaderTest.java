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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.Opener;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.PdfDocumentRemote;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.RectUtils;
import androidx.pdf.util.TileBoard;
import androidx.pdf.util.TileBoard.CancelTilesCallback;
import androidx.pdf.util.TileBoard.TileInfo;
import androidx.pdf.util.TileBoard.ViewAreaUpdateCallback;
import androidx.pdf.viewer.loader.PdfPageLoader.GetDimensionsTask;
import androidx.pdf.viewer.loader.PdfPageLoader.GetPageTextTask;
import androidx.pdf.viewer.loader.PdfPageLoader.RenderBitmapTask;
import androidx.pdf.viewer.loader.PdfPageLoader.RenderTileTask;
import androidx.pdf.viewer.loader.PdfPageLoader.SelectionTask;
import androidx.pdf.widget.WidgetType;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Unit tests for {@link PdfLoader}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class PdfLoaderTest {

    private static final int PAGE = 5;

    private Context mContext;
    @Mock
    private PdfDocumentRemote mPdfDocument;
    @Mock
    private DisplayData mDisplayData;
    @Mock
    private PdfConnection mConnection;
    @Mock
    private ParcelFileDescriptor mParcelFileDescriptor;

    @Captor
    private ArgumentCaptor<List<WidgetType>> mListArgumentCaptor;

    private TestCallbacks mWeakPdfLoaderCallbacks;
    private FileOutputStream mFileOutputStream;
    private PdfLoader mPdfLoader;

    private static final int TEST_FD = 1234;
    private static final String TEST_PW = "TESTPW";

    /** {@link PdfTaskExecutor} waits 10 seconds if it doesn't have any tasks, so we use 12. */
    private static final int LATCH_TIMEOUT_MS = 12000;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        when(mConnection.isLoaded()).thenReturn(true);
        when(mConnection.getPdfDocument(any())).thenReturn(mPdfDocument);
        when(mDisplayData.openFd(any(Opener.class))).thenReturn(mParcelFileDescriptor);
        when(mParcelFileDescriptor.getFd()).thenReturn(TEST_FD);
        when(mPdfDocument.create(mParcelFileDescriptor, TEST_PW))
                .thenReturn(PdfStatus.LOADED.getNumber());

        mWeakPdfLoaderCallbacks = new TestCallbacks();
        mPdfLoader =
                new PdfLoader(
                        mContext,
                        mConnection,
                        mDisplayData,
                        TileBoard.DEFAULT_RECYCLER,
                        mWeakPdfLoaderCallbacks,
                        false /* hideTextAnnotations */);

        File file = new File(mContext.getCacheDir(), "test");
        mFileOutputStream = new FileOutputStream(file);
    }

    @Test
    @UiThreadTest
    public void testLoadDimensions() {
        Dimensions testDimensions = new Dimensions(100, 200);
        try {
            when(mPdfDocument.getPageDimensions(PAGE)).thenReturn(testDimensions);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }

        mPdfLoader.loadPageDimensions(PAGE);
        GetDimensionsTask task = mPdfLoader.getPageLoader(PAGE).mDimensionsTask;
        assertThat(task.isCancelled()).isFalse();

        mPdfLoader.loadPageDimensions(PAGE);
        assertThat(mPdfLoader.getPageLoader(PAGE).mDimensionsTask).isSameInstanceAs(task);
        assertThat(task.isCancelled()).isFalse();

        mPdfLoader.cancelExceptSearchAndFormFilling(PAGE);
        assertThat(task.isCancelled()).isTrue();
        assertThat(mPdfLoader.getPageLoader(PAGE).mDimensionsTask).isNull();
    }

    @Test
    @UiThreadTest
    public void testLoadBitmap() {
        Dimensions original = new Dimensions(300, 400);
        mPdfLoader.loadPageBitmap(PAGE, original);
        RenderBitmapTask task1 = mPdfLoader.getPageLoader(PAGE).mBitmapTask;
        assertThat(task1.mDimensions).isEqualTo(original);
        assertThat(task1.isCancelled()).isFalse();

        mPdfLoader.loadPageBitmap(PAGE, original);
        assertThat(mPdfLoader.getPageLoader(PAGE).mBitmapTask).isSameInstanceAs(task1);
        assertThat(task1.isCancelled()).isFalse();

        Dimensions smaller = new Dimensions(150, 200);
        mPdfLoader.loadPageBitmap(PAGE, smaller);
        assertThat(mPdfLoader.getPageLoader(PAGE).mBitmapTask).isSameInstanceAs(task1);
        assertThat(task1.isCancelled()).isFalse();

        Dimensions bigger = new Dimensions(600, 800);
        mPdfLoader.loadPageBitmap(PAGE, bigger);
        assertThat(mPdfLoader.getPageLoader(PAGE).mBitmapTask).isNotSameInstanceAs(task1);
        assertThat(task1.isCancelled()).isTrue();

        RenderBitmapTask task2 = mPdfLoader.getPageLoader(PAGE).mBitmapTask;
        assertThat(task2.mDimensions).isEqualTo(bigger);
        assertThat(task2.isCancelled()).isFalse();

        mPdfLoader.cancel(PAGE);
        assertThat(task2.isCancelled()).isTrue();
        assertThat(mPdfLoader.getPageLoader(PAGE).mBitmapTask).isNull();
    }

    @Test
    @UiThreadTest
    public void testLoadText() {
        mPdfLoader.loadPageText(PAGE);
        GetPageTextTask task = mPdfLoader.getPageLoader(PAGE).mTextTask;
        assertThat(task.isCancelled()).isFalse();

        mPdfLoader.loadPageText(PAGE);
        assertThat(mPdfLoader.getPageLoader(PAGE).mTextTask).isSameInstanceAs(task);
        assertThat(task.isCancelled()).isFalse();

        mPdfLoader.cancel(PAGE);
        assertThat(task.isCancelled()).isTrue();
        assertThat(mPdfLoader.getPageLoader(PAGE).mBitmapTask).isNull();
    }

    @Test
    @UiThreadTest
    public void testSelectTask() throws RemoteException {
        SelectionBoundary select1 = new SelectionBoundary(12, 24, 48, false);
        SelectionBoundary select2 = new SelectionBoundary(9, 18, 27, false);
        PageSelection selection = new PageSelection(PAGE, select1, select2, new ArrayList<Rect>(),
                "test");

        when(mPdfDocument.selectPageText(anyInt(), any(SelectionBoundary.class),
                any(SelectionBoundary.class))).thenReturn(selection);

        mPdfLoader.selectPageText(PAGE, select1, select2);
        SelectionTask task = mPdfLoader.getPageLoader(PAGE).mSelectionTask;
        assertThat(task.isCancelled()).isFalse();

        // We don't start a new task if there is already one ongoing.
        mPdfLoader.selectPageText(PAGE, select2, select1);
        assertThat(mPdfLoader.getPageLoader(PAGE).mSelectionTask).isSameInstanceAs(task);
        assertThat(task.isCancelled()).isFalse();

        // We cancel the selection task if the selection boundary arguments are the same.
        mPdfLoader.selectPageText(PAGE, select1, select1);
        assertThat(task.isCancelled()).isTrue();
        assertThat(mPdfLoader.getPageLoader(PAGE).mSelectionTask).isNotNull();
    }

    @Ignore // b/342212541
    @Test
    @UiThreadTest
    public void testLoadTiles() {
        Dimensions pageSize = new Dimensions(2000, 1200);

        List<TileInfo> requestedTiles = getSomeTiles(pageSize);

        Iterable<TileInfo> aFewTiles = Iterables.limit(requestedTiles, 3);
        Iterable<TileInfo> firstTile = Iterables.limit(requestedTiles, 1);

        mPdfLoader.loadTileBitmaps(PAGE, pageSize, aFewTiles);
        assertThat(mPdfLoader.getPageLoader(PAGE).mTileTasks).hasSize(3);
        RenderTileTask task1 = mPdfLoader.getPageLoader(PAGE).mTileTasks.get(
                Iterables.getOnlyElement(firstTile).getIndex());
        for (RenderTileTask task : mPdfLoader.getPageLoader(PAGE).mTileTasks.values()) {
            assertThat(task.isCancelled()).isFalse();
        }

        // re-submit one tile
        mPdfLoader.loadTileBitmaps(0, pageSize, firstTile);
        assertThat(mPdfLoader.getPageLoader(PAGE).mTileTasks).hasSize(3);

        mPdfLoader.cancelTileBitmaps(PAGE, Arrays.asList(0));
        for (RenderTileTask task : mPdfLoader.getPageLoader(PAGE).mTileTasks.values()) {
            if (Objects.equal(task, task1)) {
                assertThat(task.isCancelled()).isTrue();
            } else {
                assertThat(task.isCancelled()).isFalse();
            }
        }
        mPdfLoader.cancelAllTileBitmaps(PAGE);
        for (RenderTileTask task : mPdfLoader.getPageLoader(PAGE).mTileTasks.values()) {
            assertThat(task.isCancelled()).isTrue();
        }

        assertThat(mPdfLoader.getPageLoader(PAGE).mTileTasks).isEmpty();
    }

    @Test
    @UiThreadTest
    public void testGotoLinksTask() throws RemoteException, InterruptedException {
        getGotoLinks(mPdfLoader);
        verify(mPdfDocument).getPageGotoLinks(PAGE);
    }

    @Test
    @UiThreadTest
    public void testLoadDocumentTask() throws InterruptedException, RemoteException {
        CountDownLatch latch = new CountDownLatch(1);
        mWeakPdfLoaderCallbacks.setDocumentLoadedLatch(latch);
        // ensure document is not already loaded
        when(mConnection.isLoaded()).thenReturn(false);
        mPdfLoader.applyPassword(TEST_PW);
        /** Wait for {@link TestCallbacks#documentLoaded(int)} ()} to be called. */
        latch.await(LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        verify(mPdfDocument).create(mParcelFileDescriptor, TEST_PW);
    }

    @Test
    @UiThreadTest
    public void testSearchTask() throws InterruptedException, RemoteException {
        CountDownLatch latch = new CountDownLatch(1);
        mWeakPdfLoaderCallbacks.setSearchResultsLatch(latch);
        mPdfLoader.searchPageText(PAGE, "testQuery");
        /** Wait for {@link TestCallbacks#setSearchResults(String, int, MatchRects)} to be called
         *  . */
        latch.await(LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        verify(mPdfDocument).searchPageText(PAGE, "testQuery");
    }

    @Test
    @UiThreadTest
    public void testPageUrlLinksTask() throws InterruptedException, RemoteException {
        when(mPdfDocument.getPageLinks(anyInt())).thenReturn(new LinkRects(
                new ArrayList<Rect>(), new ArrayList<Integer>(), new ArrayList<String>()));
        CountDownLatch latch = new CountDownLatch(1);
        mWeakPdfLoaderCallbacks.setUrlLinksLatch(latch);
        mPdfLoader.loadPageUrlLinks(PAGE);
        /** Wait for {@link TestCallbacks#setPageUrlLinks(int, LinkRects)} to be called. */
        latch.await(LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        verify(mPdfDocument).getPageLinks(PAGE);
    }
    private void getGotoLinks(PdfLoader pdfLoader) throws InterruptedException, RemoteException {
        when(mPdfDocument.getPageGotoLinks(anyInt())).thenReturn(new ArrayList<GotoLink>());
        CountDownLatch latch = new CountDownLatch(1);
        mWeakPdfLoaderCallbacks.setGotoLinksLatch(latch);
        pdfLoader.loadPageGotoLinks(PAGE);
        /** Wait for {@link TestCallbacks#setPageGotoLinks(int, List)} to be called. */
        latch.await(LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private List<TileInfo> getSomeTiles(Dimensions pageSize) {
        TileBoard board = new TileBoard(PAGE, pageSize, TileBoard.DEFAULT_RECYCLER,
                new CancelTilesCallback() {
                    @Override
                    public void cancelTiles(Iterable<Integer> tileIds) {
                        // No action.
                    }
                });
        final List<TileInfo> requestedTiles = new ArrayList<TileInfo>();
        board.updateViewArea(RectUtils.fromDimensions(pageSize), new ViewAreaUpdateCallback() {
            @Override
            public void requestNewTiles(Iterable<TileInfo> tiles) {
                Iterables.addAll(requestedTiles, tiles);
            }

            @Override
            public void discardTiles(Iterable<Integer> tileIds) {
                fail("No tile to discard.");
            }
        });
        return requestedTiles;
    }

    private static class TestCallbacks extends WeakPdfLoaderCallbacks {

        private CountDownLatch mClonedLatch;
        private CountDownLatch mSearchLatch;
        private CountDownLatch mDocumentLoadedLatch;
        private CountDownLatch mLinksUrlLatch;
        private CountDownLatch mGotoLinksLatch;

        private TestCallbacks() {
            super(null);
        }

        @Override
        public void documentLoaded(int numPages, @NonNull DisplayData data) {
            super.documentLoaded(numPages, data);
            if (mDocumentLoadedLatch != null) {
                mDocumentLoadedLatch.countDown();
            }
        }

        @Override
        public void setSearchResults(String query, int pageNum, MatchRects matches) {
            super.setSearchResults(query, pageNum, matches);
            if (mSearchLatch != null) {
                mSearchLatch.countDown();
            }
        }

        @Override
        public void setPageUrlLinks(int pageNum, LinkRects links) {
            super.setPageUrlLinks(pageNum, links);
            if (mLinksUrlLatch != null) {
                mLinksUrlLatch.countDown();
            }
        }
        @Override
        public void setPageGotoLinks(int pageNum, List<GotoLink> links) {
            super.setPageGotoLinks(pageNum, links);
            if (mLinksUrlLatch != null) {
                mLinksUrlLatch.countDown();
            }
        }

        public void setClonedLatch(CountDownLatch latch) {
            mClonedLatch = latch;
        }

        public void setDocumentLoadedLatch(CountDownLatch documentLoadedLatch) {
            this.mDocumentLoadedLatch = documentLoadedLatch;
        }

        public void setSearchResultsLatch(CountDownLatch searchResultsLatch) {
            this.mSearchLatch = searchResultsLatch;
        }

        public void setUrlLinksLatch(CountDownLatch linksLatch) {
            this.mLinksUrlLatch = linksLatch;
        }

        public void setGotoLinksLatch(CountDownLatch linksLatch) {
            this.mGotoLinksLatch = linksLatch;
        }
    }
}

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

package androidx.pdf.widget;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.pdf.models.Dimensions;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.util.TileBoard;
import androidx.pdf.util.TileBoard.TileInfo;
import androidx.pdf.widget.MosaicView.BitmapSource;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Unit tests for {@link MosaicView}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("deprecation")
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class MosaicViewTest {

    @Mock
    private BitmapSource mBitmapSource;
    @Mock
    private BitmapRecycler mMockRecycler;

    private Bitmap mBitmap;

    @Captor
    ArgumentCaptor<Iterable<TileInfo>> mRequestedTiles;
    @Captor
    ArgumentCaptor<Iterable<Integer>> mCancelledTiles;

    private MosaicView mView;
    private int mMaxTileSize;

    private AutoCloseable mOpenMocks;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);

        mView = new MosaicView(ApplicationProvider.getApplicationContext());
        mMaxTileSize = MosaicView.getMaxTileSize(ApplicationProvider.getApplicationContext());
        assertThat(mMaxTileSize).isGreaterThan(0);
        assertThat(mMaxTileSize).isAtMost(1024);

        mBitmap =
                BitmapFactory.decodeFile("src/test/assets/images/launcher_pdfviewer.png");
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    public void testPageBitmapNoTiles() {
        Dimensions dimensions = new Dimensions(300, 400);
        Dimensions cappedDimensions = new Dimensions(600, 800);

        mView.init(dimensions, TileBoard.DEFAULT_RECYCLER, mBitmapSource);
        mView.setViewArea(0, 0, 67, 89);  // Arbitrary value.
        mView.requestDrawAtZoom(2.0f);

        ArgumentCaptor<Dimensions> requestedSize = ArgumentCaptor.forClass(Dimensions.class);
        verify(mBitmapSource, only()).requestPageBitmap(requestedSize.capture(), eq(false));
        assertThat(requestedSize.getValue()).isEqualTo(cappedDimensions);
    }

    @Test
    public void testPageBitmapAndTiles() {
        Dimensions dimensions = new Dimensions(300, 400);
        Rect viewArea = new Rect(0, 0, 150, 200);

        mView.init(dimensions, TileBoard.DEFAULT_RECYCLER, mBitmapSource);
        mView.setViewArea(viewArea);
        mView.requestDrawAtZoom(20f);

        ArgumentCaptor<Dimensions> requestedSize = ArgumentCaptor.forClass(Dimensions.class);
        verify(mBitmapSource).requestPageBitmap(requestedSize.capture(), eq(true));
        verify(mBitmapSource, times(1)).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());
        assertThat(requestedSize.getValue().getWidth()).isAtMost(mMaxTileSize / 2);
        assertThat(requestedSize.getValue().getHeight()).isAtMost(mMaxTileSize / 2);

        checkCoverage(viewArea, mRequestedTiles.getValue());
    }

    @Test
    public void testMoreTiles() {
        float zoom = 20f;
        Dimensions dimensions = new Dimensions(300, 400);
        mView.init(dimensions, mMockRecycler, mBitmapSource);

        // 1. Request 4 tiles.
        // At a zoom of 20, to cover an area of 50x50 units we will need to cover a 1000x1000 px
        // square.
        // To do this we will need four 800x800 px tiles covering in total 1600x1600 px.
        Rect viewArea = new Rect(0, 0, 50, 50);
        mView.setViewArea(viewArea);
        mView.requestDrawAtZoom(zoom);

        verify(mBitmapSource, times(1)).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());

        Set<Integer> tiles = new HashSet<Integer>();
        Iterables.addAll(tiles, Iterables.transform(mRequestedTiles.getValue(), GET_INDEX));
        assertThat(tiles.isEmpty()).isFalse();
        checkCoverage(viewArea, mRequestedTiles.getValue());
        fillTiles(mView, mRequestedTiles.getValue());
        reset(mBitmapSource);

        // 2. Move, request no change.
        // Moving 5 units across means we need an extra 100 pixels across - but this is still
        // inside the
        // 1600x1600 px square we covered last time, no new tiles are needed.
        viewArea.offset(0, 5);
        mView.setViewArea(viewArea);
        mView.requestTiles();
        verifyNoMoreInteractions(mBitmapSource);
        verifyNoMoreInteractions(mMockRecycler);
        reset(mBitmapSource);

        // 3. Move again, request 2 new tiles
        // Moving 40 units across means we need an extra 800 pixels - this is exactly one tile wide.
        // Since our viewport is still two tiles high, we need two more tiles.
        viewArea.offset(0, 40);
        mView.setViewArea(viewArea);
        mView.requestTiles();

        verify(mBitmapSource, only()).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());
        assertThat(Iterables.size(mRequestedTiles.getValue())).isEqualTo(2);
        Iterables.addAll(tiles, Iterables.transform(mRequestedTiles.getValue(), GET_INDEX));
        assertThat(tiles.size()).isEqualTo(6);
        verifyNoMoreInteractions(mMockRecycler);
        reset(mBitmapSource);
        fillTiles(mView, mRequestedTiles.getValue());

        // 4. Move more, request & discard tiles.
        // Moving 40 units across means we need 800 more pixels to the right and 800 less to the
        // left.
        // We need two more tiles on the right but we can discard two on the left.
        viewArea.offset(0, 40);
        mView.setViewArea(viewArea);
        mView.requestTiles();
        verify(mBitmapSource, times(1)).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());
        assertThat(Iterables.size(mRequestedTiles.getValue())).isEqualTo(2);
        // Verify 2 tiles were discarded.
        verify(mMockRecycler, times(2)).discardBitmap(isA(Bitmap.class));
    }

    @Test
    public void testSetTileOutOfViewAreaAndCancellation() {
        float zoom = 20f;
        Dimensions dimensions = new Dimensions(300, 400);
        mView.init(dimensions, mMockRecycler, mBitmapSource);

        Rect viewArea = new Rect(0, 0, 50, 50);
        mView.setViewArea(viewArea);
        mView.requestDrawAtZoom(zoom);

        verify(mBitmapSource, times(1)).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());

        Set<Integer> tiles = new HashSet<Integer>();
        Iterables.addAll(tiles, Iterables.transform(mRequestedTiles.getValue(), GET_INDEX));
        assertThat(tiles.isEmpty()).isFalse();
        checkCoverage(viewArea, mRequestedTiles.getValue());

        // Set the viewArea which will not share any tiles with previous viewArea.
        Rect newViewArea = new Rect(100, 100, 150, 150);
        mView.setViewArea(newViewArea);
        mView.requestTiles();
        verify(mBitmapSource, times(1)).cancelTiles(mCancelledTiles.capture());
        assertThat(Iterables.size(mCancelledTiles.getValue())).isEqualTo(tiles.size());
        verifyNoMoreInteractions(mMockRecycler);

        // set tile bitmaps for the old view viewArea after the new tileboard
        fillTiles(mView, mRequestedTiles.getValue());
        verify(mMockRecycler, times(tiles.size())).discardBitmap(isA(Bitmap.class));
    }

    @Test
    public void testCancellationOnNewTileBoard() {
        float zoom = 20f;
        Dimensions dimensions = new Dimensions(300, 400);
        mView.init(dimensions, mMockRecycler, mBitmapSource);

        Rect viewArea = new Rect(0, 0, 50, 50);
        mView.setViewArea(viewArea);
        mView.requestDrawAtZoom(zoom);

        verify(mBitmapSource, times(1)).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());

        Set<Integer> tiles = new HashSet<Integer>();
        Iterables.addAll(tiles, Iterables.transform(mRequestedTiles.getValue(), GET_INDEX));
        assertThat(tiles.isEmpty()).isFalse();
        checkCoverage(viewArea, mRequestedTiles.getValue());
        reset(mBitmapSource);

        // Request draw at a different zoom, which will make the existing tile board stale. Tile
        // requests for current tile board are still pending and will be stale.
        mView.requestDrawAtZoom(10f);
        verify(mBitmapSource, times(1)).cancelTiles(mCancelledTiles.capture());
        assertThat(Iterables.size(mCancelledTiles.getValue())).isEqualTo(tiles.size());
    }

    @Test
    public void testOverlays() {
        FakeDrawable fakeDrawable = new FakeDrawable();
        FakeDrawable fakeDrawable2 = new FakeDrawable();
        mView.addOverlay("test", fakeDrawable);
        mView.addOverlay("test2", fakeDrawable2);
        mView.removeOverlay("test");
        Canvas canvas = new Canvas();

        mView.dispatchDraw(canvas);

        assertThat(fakeDrawable.mDrawCount).isEqualTo(0);
        assertThat(fakeDrawable2.mDrawCount).isEqualTo(1);
    }

    @Test
    public void requestRedrawAreas_pageBitmapOnly() {
        Dimensions dimensions = new Dimensions(300, 400);

        mView.init(dimensions, TileBoard.DEFAULT_RECYCLER, mBitmapSource);
        mView.setViewArea(0, 0, 50, 50);
        mView.requestDrawAtZoom(2.0f); // First drawing here.

        // requestDrawAtZoom calls the BitmapSource which for us is a mock. The bitmap source
        // asynchronously fetches bitmaps then calls the below method to set them on the view.
        // requestRedrawAreas only redraws bitmaps that exist so we must call this manually for the
        // test to proceed.
        mView.setPageBitmap(mBitmap);

        Rect invalidRect = new Rect(0, 0, 100, 100);
        mView.requestRedrawAreas(ImmutableList.of(invalidRect)); // Second drawing here.

        ArgumentCaptor<Dimensions> bitmapSizeArgCaptor = ArgumentCaptor.forClass(Dimensions.class);
        verify(mBitmapSource, times(2)).requestPageBitmap(bitmapSizeArgCaptor.capture(), eq(false));

        // Our scaled bitmap size is below the maximum size so will not be changed.
        Dimensions scaledDimensions = new Dimensions(600, 800);
        assertThat(bitmapSizeArgCaptor.getAllValues()).hasSize(2);
        assertThat(bitmapSizeArgCaptor.getAllValues().get(0)).isEqualTo(scaledDimensions);
        assertThat(bitmapSizeArgCaptor.getAllValues().get(1)).isEqualTo(scaledDimensions);

        // Confirm we never requested tiles.
        verify(mBitmapSource, never()).requestNewTiles(any(), any());
    }

    @Test
    public void requestRedrawAreas_pageBitmapAndTiles() {
        Dimensions dimensions = new Dimensions(2000, 4000);
        Rect viewArea = new Rect(0, 0, 750, 750);

        mView.init(dimensions, TileBoard.DEFAULT_RECYCLER, mBitmapSource);
        mView.setViewArea(viewArea);
        mView.requestDrawAtZoom(2f); // First drawing here.

        // requestDrawAtZoom calls the BitmapSource which for us is a mock. The bitmap source
        // asynchronously fetches bitmaps then calls the below method to set them on the view.
        // requestRedrawAreas only redraws bitmaps that exist so we must call this manually for the
        // test to proceed. We do not need to set the tile bitmaps because requestDrawAtZoom
        // configured
        // the TileBoard which is used to check if tile bitmaps exist.
        mView.setPageBitmap(mBitmap);

        Rect invalidRect = new Rect(0, 0, 100, 100);
        mView.requestRedrawAreas(ImmutableList.of(invalidRect)); // Second drawing here.

        // Should have requested the background page bitmap twice, once on full draw, once on
        // redraw.
        ArgumentCaptor<Dimensions> pageBitmapDimensCaptor = ArgumentCaptor.forClass(
                Dimensions.class);
        verify(mBitmapSource, times(2))
                .requestPageBitmap(pageBitmapDimensCaptor.capture(), eq(true));

        // Determine the max sizes used by the view. 1024/512 or determined by context,
        int maxBitmapSize = MosaicView.getMaxTileSize(ApplicationProvider.getApplicationContext());
        int maxBackgroundBitmapSize = maxBitmapSize / 2;
        // Page is 2x as long as wide so height == maxBackgroundBitmapSize, width == 1/2 that.
        assertThat(pageBitmapDimensCaptor.getAllValues().get(0))
                .isEqualTo(new Dimensions(maxBackgroundBitmapSize / 2, maxBackgroundBitmapSize));
        assertThat(pageBitmapDimensCaptor.getAllValues().get(1))
                .isEqualTo(new Dimensions(maxBackgroundBitmapSize / 2, maxBackgroundBitmapSize));

        // Check that we tiled twice, the first time should have covered viewArea.
        verify(mBitmapSource, times(2)).requestNewTiles(isA(Dimensions.class),
                mRequestedTiles.capture());
        List<Iterable<TileInfo>> requestedTileGroups = mRequestedTiles.getAllValues();
        assertThat(requestedTileGroups).hasSize(2);
        checkCoverage(viewArea, requestedTileGroups.get(0));

        // Second tiling should only be for tiles affected by area we requested. invalidRect is
        // fully
        // within tile 0 so that should be the only tile we requested.
        Iterable<TileInfo> redrawRequestTiles = requestedTileGroups.get(1);
        assertThat(redrawRequestTiles).hasSize(1);
        TileInfo redrawnTile = redrawRequestTiles.iterator().next();
        assertThat(redrawnTile.getBounds()).isEqualTo(new Rect(0, 0, 800, 800));
    }

    @Test
    public void requestRedrawAreas_doesNotRequestClearedBitmaps() {
        Dimensions dimensions = new Dimensions(2000, 4000);
        Rect viewArea = new Rect(0, 0, 750, 750);

        mView.init(dimensions, TileBoard.DEFAULT_RECYCLER, mBitmapSource);
        mView.setViewArea(viewArea);
        mView.requestDrawAtZoom(2f); // First drawing here.

        // User clears the bitmaps, view still exists but no longer contains any.
        mView.clearAllBitmaps();

        Rect invalidRect = new Rect(0, 0, 100, 100);
        mView.requestRedrawAreas(ImmutableList.of(invalidRect)); // Second drawing here.

        // Should have made bitmap requests only on the first draw request because there weren't any
        // to replace when requestRedrawAreas was called.
        verify(mBitmapSource, times(1)).requestPageBitmap(any(Dimensions.class), eq(true));
        verify(mBitmapSource, times(1)).requestNewTiles(any(Dimensions.class), any());
    }

    private void checkCoverage(Rect area, Iterable<TileInfo> tileInfos) {
        assertThat(Iterables.isEmpty(tileInfos)).isFalse();
        Rect coverage = new Rect();
        for (TileInfo tileInfo : tileInfos) {
            coverage.union(tileInfo.getBounds());
        }
        assertWithMessage(String.format("Coverage: %s, area: %s)", coverage, area))
                .that(coverage.contains(area))
                .isTrue();
    }

    private void fillTiles(MosaicView mosaicView, Iterable<TileInfo> tileInfos) {
        for (TileInfo tileInfo : tileInfos) {
            mosaicView.setTileBitmap(tileInfo,
                    TileBoard.DEFAULT_RECYCLER.obtainBitmap(new Dimensions(1, 1)));
        }
    }

    private static final Function<TileInfo, Integer> GET_INDEX = TileInfo::getIndex;

    /** Bare-bones fake that counts {@link Drawable#draw(Canvas)} invocations. */
    private static class FakeDrawable extends Drawable {

        int mDrawCount = 0;

        FakeDrawable() {
            super();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            mDrawCount++;
        }

        @Override
        public void setAlpha(int alpha) {
            /* no-op */
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            /* no-op */
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return 0;
        }
    }
}

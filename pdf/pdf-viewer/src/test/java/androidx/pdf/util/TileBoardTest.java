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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;

import androidx.pdf.models.Dimensions;
import androidx.pdf.util.TileBoard.CancelTilesCallback;
import androidx.pdf.util.TileBoard.TileInfo;
import androidx.pdf.util.TileBoard.ViewAreaUpdateCallback;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Unit tests for {@link TileBoard}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class TileBoardTest {

    private static final Dimensions TILE_SIZE = new Dimensions(800, 800);

    /** {@link Bitmap} cannot be mocked, so we use any random (small!) bitmap we can make. */
    private final Bitmap mBitmap = Bitmap.createBitmap(new int[]{0xFFFFFF00}, 1, 1,
            Bitmap.Config.ARGB_8888);

    @Mock
    private BitmapRecycler mBitmapRecycler;

    private final Dimensions mLandscape3By2 = new Dimensions(2 * TILE_SIZE.getWidth() + 200,
            TILE_SIZE.getHeight() + 300);

    private final Dimensions mPortrait1By6 = new Dimensions(5, 5 * TILE_SIZE.getHeight() + 200);

    private final Dimensions mLandscape12By8 = new Dimensions(12 * TILE_SIZE.getWidth() - 50,
            8 * TILE_SIZE.getHeight() - 100);

    private final Dimensions mLandscape4By2Exact = new Dimensions(4 * TILE_SIZE.getWidth(),
            2 * TILE_SIZE.getHeight());

    private TileBoard mBoard;

    private Rect mViewArea;

    private AutoCloseable mOpenMocks;

    private final Set<TileInfo> mRequestedTiles = new HashSet<>();
    private final Set<Integer> mDiscardedTiles = new HashSet<>();
    private final Set<Integer> mCancelledTiles = new HashSet<>();

    private final ViewAreaUpdateCallback mSavingCallback = new ViewAreaUpdateCallback() {
        @Override
        public void requestNewTiles(Iterable<TileInfo> tiles) {
            for (TileInfo tile : tiles) {
                mRequestedTiles.add(tile);
            }
        }

        @Override
        public void discardTiles(Iterable<Integer> tileIds) {
            for (int tile : tileIds) {
                mDiscardedTiles.add(tile);
            }
        }
    };

    private final CancelTilesCallback mCancelcallback = tileIds -> {
        for (int tile : tileIds) {
            mCancelledTiles.add(tile);
        }
    };

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);
        mViewArea = new Rect(0, 0, mLandscape3By2.getWidth(), mLandscape3By2.getHeight());
        clearLists();
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    private void clearLists() {
        mRequestedTiles.clear();
        mDiscardedTiles.clear();
    }

    @Test
    public void testFullBoard() {
        mBoard = new TileBoard(0, mLandscape3By2, mBitmapRecycler, mCancelcallback);
        mBoard.updateViewArea(mViewArea, mSavingCallback);
    }

    @Test
    public void testTileInfos() {
        mBoard = new TileBoard(1, mLandscape3By2, mBitmapRecycler, mCancelcallback);
        final List<TileInfo> tiles1 = new ArrayList<TileInfo>();

        mBoard.updateViewArea(mViewArea, new ViewAreaUpdateCallback() {
            @Override
            public void requestNewTiles(Iterable<TileInfo> tiles) {
                Iterables.addAll(tiles1, tiles);
            }

            @Override
            public void discardTiles(Iterable<Integer> tileIds) {
                fail("No tile to discard.");
            }
        });

        final List<Integer> board2CancelledTiles = new ArrayList<Integer>();
        TileBoard board2 = new TileBoard(2, mPortrait1By6, mBitmapRecycler,
                new CancelTilesCallback() {
                    @Override
                    public void cancelTiles(Iterable<Integer> tileIds) {
                        Iterables.addAll(board2CancelledTiles, tileIds);
                    }
                });
        final List<TileInfo> tiles2 = new ArrayList<TileInfo>();
        board2.updateViewArea(RectUtils.fromDimensions(mPortrait1By6),
                new ViewAreaUpdateCallback() {
                    @Override
                    public void requestNewTiles(Iterable<TileInfo> tiles) {
                        Iterables.addAll(tiles2, tiles);
                    }

                    @Override
                    public void discardTiles(Iterable<Integer> tileIds) {
                        fail("No tile to discard.");
                    }
                });

        TileInfo tile0 = tiles1.get(0);
        assertThat(tile0.getIndex()).isEqualTo(0);
        assertThat(tile0.belongsTo(mBoard)).isTrue();
        assertThat(tile0.belongsTo(board2)).isFalse();
        checkDimensions(tile0, new Point());

        mBoard.clearTiles();
        assertThat(mCancelledTiles.size()).isEqualTo(tiles1.size());

        TileInfo tile4 = tiles1.get(4);
        assertThat(tile4.getIndex()).isEqualTo(4);
        checkDimensions(tile4,
                new Point(TileBoard.TILE_SIZE.getWidth(), TileBoard.TILE_SIZE.getHeight()));
        board2.clearTiles();
        assertThat(board2CancelledTiles.size()).isEqualTo(tiles2.size());
    }

    private void checkDimensions(TileInfo tile, Point expectedOffset) {
        Point offset = tile.getOffset();
        assertThat(offset).isEqualTo(expectedOffset);
        Rect bounds = tile.getBounds();
        assertThat(bounds.left).isEqualTo(offset.x);
        assertThat(bounds.top).isEqualTo(offset.y);
        assertThat(bounds.width()).isEqualTo(TileBoard.TILE_SIZE.getWidth());
        assertThat(bounds.height()).isEqualTo(TileBoard.TILE_SIZE.getHeight());

        // Check that you can shamelessly mess up your own copy of the bounds
        bounds.inset(20, 20);
        assertThat(bounds.equals(tile.getBounds())).isFalse();
    }

    @Test
    public void testUpdateArea() {
        mBoard = new TileBoard(0, mLandscape3By2, mBitmapRecycler, mCancelcallback);
        mBoard.updateViewArea(mViewArea, mSavingCallback);
        assertThat(mRequestedTiles.size()).isEqualTo(6);
        assertThat(mDiscardedTiles.size()).isEqualTo(0);

        for (TileInfo tile : mRequestedTiles) {
            mBoard.setTile(tile, mBitmap);
        }

        mViewArea = new Rect(100, 10, 150, 20);
        mViewArea.offset(2 * TILE_SIZE.getWidth(), 0); // that's tile (0, 2), + expand to (0, 1).
        clearLists();

        mBoard.updateViewArea(mViewArea, mSavingCallback);

        assertThat(mRequestedTiles.size()).isEqualTo(0);
        assertThat(mDiscardedTiles.size()).isEqualTo(4);
        assertThat(mDiscardedTiles.contains(0)).isTrue();
        assertThat(mDiscardedTiles.contains(3)).isTrue();
        assertThat(mDiscardedTiles.contains(4)).isTrue();
        assertThat(mDiscardedTiles.contains(5)).isTrue();
        verify(mBitmapRecycler, times(4)).discardBitmap(eq(mBitmap));

        mBoard.clearTiles();
        verify(mBitmapRecycler, times(6)).discardBitmap(eq(mBitmap));
    }

    @Test
    public void testExpand() {
        mBoard = new TileBoard(0, mLandscape12By8, mBitmapRecycler, mCancelcallback);
        mViewArea = new Rect(1, 1, 2, 2);
        mBoard.updateViewArea(mViewArea, mSavingCallback);
        assertThat(mRequestedTiles.size()).isEqualTo(1);

        mViewArea.set(0, 0, round(TILE_SIZE.getWidth() * 0.9), round(TILE_SIZE.getHeight() * 0.9));
        mBoard.updateViewArea(mViewArea, mSavingCallback);
        assertThat(mRequestedTiles.size()).isEqualTo(4);

        mRequestedTiles.clear();
        mCancelledTiles.clear();
        mViewArea.set(5, 5, 7, 7);
        mViewArea.offset(3 * TILE_SIZE.getWidth(), 4 * TILE_SIZE.getHeight());
        mBoard.updateViewArea(mViewArea, mSavingCallback);
        assertThat(mRequestedTiles.size()).isEqualTo(4);
        // verify that previously added tile requests are cancelled. Note that these are still
        // pending,
        // as setTile is not called for them.
        assertThat(mCancelledTiles.size()).isEqualTo(4);

        final List<TileInfo> alreadyRequested = new ArrayList<TileInfo>();
        Iterables.addAll(alreadyRequested, mRequestedTiles);
        mRequestedTiles.clear();

        mViewArea.set(20, 50, round(TILE_SIZE.getWidth() * 0.7),
                round(TILE_SIZE.getHeight() * 0.6));
        mViewArea.offset(3 * TILE_SIZE.getWidth(), 4 * TILE_SIZE.getHeight());
        mBoard.updateViewArea(mViewArea, mSavingCallback);
        assertThat(mRequestedTiles.size() + alreadyRequested.size())
                .isEqualTo(Iterables.size(mBoard.getVisibleTileIndexes()));
        Iterables.addAll(mRequestedTiles, alreadyRequested);
        checkOverflows(mViewArea, mRequestedTiles, TILE_SIZE.getWidth() / 2);
    }

    @Test
    public void testExactFit() {
        mBoard = new TileBoard(0, mLandscape4By2Exact, mBitmapRecycler, mCancelcallback);
        mBoard.updateViewArea(RectUtils.fromDimensions(mLandscape4By2Exact), mSavingCallback);
        assertThat(mRequestedTiles.size()).isEqualTo(8);
        assertThat(mDiscardedTiles.size()).isEqualTo(0);
    }

    @Test
    public void testSetTileInvisible() {
        mBoard = new TileBoard(0, mLandscape12By8, mBitmapRecycler, mCancelcallback);
        mViewArea = new Rect(1, 1, 2, 2);
        mBoard.updateViewArea(mViewArea, mSavingCallback);
        assertThat(mRequestedTiles.size()).isEqualTo(1);
        assertThat(mDiscardedTiles.size()).isEqualTo(0);
        Set<TileInfo> oldRequestedTiles = new HashSet<TileInfo>(mRequestedTiles);
        clearLists();

        // Update view area such that all new tiles are requested.
        mViewArea.set(5, 5, 7, 7);
        mViewArea.offset(3 * TILE_SIZE.getWidth(), 4 * TILE_SIZE.getHeight());
        mBoard.updateViewArea(mViewArea, mSavingCallback);

        for (TileInfo tile : oldRequestedTiles) {
            assertThat(!mBoard.setTile(tile, mBitmap)).isTrue();
        }
    }

    @Test
    public void findTileInfosForRects_singleRectSingleTile() {
        TileBoard tileBoard = new TileBoard(0, new Dimensions(2000, 4000), null, null);
        Rect insideTileZero = new Rect(0, 0, 100, 100);
        Rect insideTileOne = new Rect(801, 0, 900, 100);
        Rect insideTileThree = new Rect(0, 801, 100, 900);

        List<TileInfo> tileInfos = tileBoard.findTileInfosForRects(
                ImmutableList.of(insideTileZero));
        assertThat(tileInfos).hasSize(1);
        assertThat(tileInfos.get(0).getBounds()).isEqualTo(new Rect(0, 0, 800, 800));

        tileInfos = tileBoard.findTileInfosForRects(ImmutableList.of(insideTileOne));
        assertThat(tileInfos).hasSize(1);
        assertThat(tileInfos.get(0).getBounds()).isEqualTo(new Rect(800, 0, 1600, 800));

        tileInfos = tileBoard.findTileInfosForRects(ImmutableList.of(insideTileThree));
        assertThat(tileInfos).hasSize(1);
        assertThat(tileInfos.get(0).getBounds()).isEqualTo(new Rect(0, 800, 800, 1600));
    }

    @Test
    public void findTileInfosForRects_singleRectSpansMultipleTiles() {
        TileBoard tileBoard = new TileBoard(0, new Dimensions(2000, 4000), null, null);
        Rect spansTwoTiles = new Rect(0, 0, 1000, 100);
        Rect spansFourTiles = new Rect(0, 0, 1000, 1000);

        List<TileInfo> tileInfos = tileBoard.findTileInfosForRects(ImmutableList.of(spansTwoTiles));
        assertThat(tileInfos).hasSize(2);
        Set<Rect> rects = ImmutableSet.of(tileInfos.get(0).getBounds(),
                tileInfos.get(1).getBounds());
        assertThat(rects).containsExactly(new Rect(0, 0, 800, 800), new Rect(800, 0, 1600, 800));

        tileInfos = tileBoard.findTileInfosForRects(ImmutableList.of(spansFourTiles));
        assertThat(tileInfos).hasSize(4);
        rects =
                ImmutableSet.of(
                        tileInfos.get(0).getBounds(),
                        tileInfos.get(1).getBounds(),
                        tileInfos.get(2).getBounds(),
                        tileInfos.get(3).getBounds());
        assertThat(rects)
                .containsExactly(
                        new Rect(0, 0, 800, 800),
                        new Rect(800, 0, 1600, 800),
                        new Rect(0, 800, 800, 1600),
                        new Rect(800, 800, 1600, 1600));
    }

    /**
     * Tests case of a huge area where rect may covers tiles entirely without placing a corner in
     * them.
     */
    @Test
    public void findTileInfosForRects_singlRectCoversMoreThanFourTiles() {
        // 10x10 tile board.
        TileBoard tileBoard = new TileBoard(0, new Dimensions(8000, 8000), null, null);
        Rect coversNineTiles = new Rect(801, 801, 2500, 2500);
        List<TileInfo> tileInfos = tileBoard.findTileInfosForRects(
                ImmutableList.of(coversNineTiles));

        Set<Rect> rects = new HashSet<>();
        for (TileInfo tileInfo : tileInfos) {
            rects.add(tileInfo.getBounds());
        }
        assertThat(rects)
                .containsExactly(
                        new Rect(800, 800, 1600, 1600),
                        new Rect(1600, 800, 2400, 1600),
                        new Rect(2400, 800, 3200, 1600),
                        new Rect(800, 1600, 1600, 2400),
                        new Rect(1600, 1600, 2400, 2400),
                        new Rect(2400, 1600, 3200, 2400),
                        new Rect(800, 2400, 1600, 3200),
                        new Rect(1600, 2400, 2400, 3200),
                        new Rect(2400, 2400, 3200, 3200));
    }

    @Test
    public void findTileInfosForRects_multipleRectsSameTileReturnsNoDups() {
        TileBoard tileBoard = new TileBoard(0, new Dimensions(2000, 4000), null, null);
        Rect first = new Rect(0, 0, 100, 100);
        Rect second = new Rect(100, 100, 200, 200);

        List<TileInfo> tileInfos = tileBoard.findTileInfosForRects(ImmutableList.of(first, second));
        assertThat(tileInfos).hasSize(1);
        assertThat(tileInfos.get(0).getBounds()).isEqualTo(new Rect(0, 0, 800, 800));
    }

    private void checkOverflows(Rect area, Iterable<TileInfo> tiles, int margin) {
        assertThat(Iterables.isEmpty(tiles)).isFalse();
        Rect coverage = new Rect();
        for (TileInfo tileInfo : tiles) {
            coverage.union(tileInfo.getBounds());
        }
        assertThat(coverage.left).isAtMost(area.left - margin);
        assertThat(coverage.top).isAtMost(area.top - margin);
        assertThat(coverage.right).isAtLeast(area.right + margin);
        assertThat(coverage.bottom).isAtLeast(area.bottom + margin);
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }
}

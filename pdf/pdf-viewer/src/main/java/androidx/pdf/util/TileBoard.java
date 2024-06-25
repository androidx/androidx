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

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.models.Dimensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A mosaic of tiles matching an image. It takes the image dimensions and arranges a tiling plan
 * that covers it. It then computes what tiles are required for each update of the viewing area, and
 * will collect and discard the corresponding bitmaps.
 *
 * <p>A TileBoard is an {@link Iterable} over the {@link TileInfo}s that make the whole board.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TileBoard {

    private static final String TAG = TileBoard.class.getSimpleName();
    private static final String TAG_PREFIX = String.format("%s #", TAG);
    private final String mTag;

    public static final Dimensions TILE_SIZE = new Dimensions(800, 800);

    public static final BitmapRecycler DEFAULT_RECYCLER = new BitmapRecycler();

    /** Recycling of dead bitmaps. */
    protected final BitmapRecycler mBitmapRecycler;

    /** The dimensions of the area to be covered by this tiling. */
    public final Dimensions mBounds;

    /** The number of columns of the tiling. */
    protected final int mNumCols;

    /** The bitmaps of the active tiles. Sized for all tiles, but many are null. */
    protected final Bitmap[] mTiles;

    /** The list of all tiles required to cover the whole board. */
    protected final TileInfo[] mTileInfos;

    /** The area currently visible on the screen. Only tiles that intersect this area matter. */
    private Area mVisibleArea;

    /**
     * Array which maintains the ids of currently requested tiles. Note that the access to this
     * array
     * need not be synchronized as methods like {@code setTile} and {@code updateViewArea} which
     * read/write to it are all called on UI thread.
     */
    private final HashSet<Integer> mPendingTileRequests;

    /** Callback which is called when tile requests are to be cancelled */
    private final CancelTilesCallback mCancelTilesCallback;

    public TileBoard(
            int id,
            @NonNull Dimensions imageBounds,
            @NonNull BitmapRecycler bitmapRecycler,
            @NonNull CancelTilesCallback cancelTilesCallback) {
        this(
                /* numRows = */ 1 + (imageBounds.getHeight() - 1) / TILE_SIZE.getHeight(),
                /* numCols = */ 1 + (imageBounds.getWidth() - 1) / TILE_SIZE.getWidth(),
                TAG_PREFIX + id,
                imageBounds,
                bitmapRecycler,
                cancelTilesCallback);
    }

    protected TileBoard(
            int numRows,
            int numCols,
            @NonNull String tag,
            @NonNull Dimensions bounds,
            @NonNull BitmapRecycler bitmapRecycler,
            @NonNull CancelTilesCallback cancelTilesCallback) {
        this.mTag = tag;
        this.mBitmapRecycler = bitmapRecycler;
        this.mBounds = bounds;
        this.mNumCols = numCols;
        mTiles = new Bitmap[numRows * numCols];
        mTileInfos = new TileInfo[numRows * numCols];
        mPendingTileRequests = new HashSet<>(numRows * numCols);
        this.mCancelTilesCallback = cancelTilesCallback;
    }

    /**
     * At high zoom level, there may be an awful lot of tile infos, so it's best to avoid creating
     * them all up-front. This is a lazy accessor for them.
     */
    @VisibleForTesting
    @NonNull
    public TileInfo getTileInfo(int k) {
        TileInfo tileInfo = mTileInfos[k];
        if (tileInfo == null) {
            tileInfo = new TileInfo(k);
            mTileInfos[k] = tileInfo;
        }
        return tileInfo;
    }

    /**
     *
     */
    public int numTiles() {
        return mTiles.length;
    }

    /**
     *
     */
    protected int numRows() {
        return numTiles() / mNumCols;
    }

    /** Return true if we need a new set of tiles at that width. */
    public boolean needsTiles(int requestedWidth) {
        return mBounds.getWidth() != requestedWidth;
    }

    /** Returns true if the tile is still relevant and was saved. */
    public boolean setTile(@NonNull TileInfo tileInfo, @NonNull Bitmap tile) {
        if (!isTileVisible(tileInfo)) {
            return false;
        }
        if (!tileInfo.belongsTo(this)) {
            return false;
        }
        mTiles[tileInfo.getIndex()] = tile;
        mPendingTileRequests.remove(tileInfo.getIndex());
        logMem();
        return true;
    }

    /**
     *
     */
    public void clearTiles() {
        if (!mPendingTileRequests.isEmpty()) {
            mCancelTilesCallback.cancelTiles(new HashSet<>(mPendingTileRequests));
            mPendingTileRequests.clear();
        }
        for (Bitmap obsoleteTile : mTiles) {
            mBitmapRecycler.discardBitmap(obsoleteTile);
        }
        Arrays.fill(mTiles, null);
        logMem();
    }

    /**
     * To be called when the viewing area moves on the image. The given viewArea is assumed to be
     * clipped to the actual image dimensions.
     */
    public boolean updateViewArea(@NonNull Rect viewArea,
            @NonNull ViewAreaUpdateCallback callback) {
        Preconditions.checkArgument(
                viewArea.top >= 0
                        && viewArea.left >= 0
                        && viewArea.width() <= mBounds.getWidth()
                        && viewArea.height() <= mBounds.getHeight(),
                "ViewArea extends beyond our bounds, should be clipped." + viewArea);
        Area newVisibleArea = getExpandedArea(viewArea);

        if (newVisibleArea.equals(mVisibleArea)) {
            return false;
        } else {
            mVisibleArea = newVisibleArea;
        }

        // Accumulate tiles that we still need here, then replace 'tiles' with it.
        Bitmap[] retainedTiles = new Bitmap[mTiles.length];
        List<TileInfo> newTiles = new ArrayList<>(mVisibleArea.size());
        List<Integer> retainRequests = new ArrayList<>(mPendingTileRequests.size());
        for (int k : areaIndexes(mVisibleArea)) {
            Bitmap tile = mTiles[k];
            if (tile == null) {
                // Check if this tile was already requested, if so do not request again.
                if (!mPendingTileRequests.contains(k)) {
                    newTiles.add(getTileInfo(k));
                } else {
                    retainRequests.add(k);
                }
            } else {
                retainedTiles[k] = tile;
                mTiles[k] = null;
            }
        }

        // Discard obsolete tiles
        int k = 0;
        List<Integer> disposed = new ArrayList<>();
        for (Bitmap obsoleteTile : mTiles) {
            if (obsoleteTile != null) {
                mBitmapRecycler.discardBitmap(obsoleteTile);
                disposed.add(k);
            }
            k++;
        }
        if (!disposed.isEmpty()) {
            callback.discardTiles(disposed);
        }

        // Cancel pending requests for tiles which are stale.
        List<Integer> staleRequests = new ArrayList<>();
        for (Integer tileId : mPendingTileRequests) {
            if (!retainRequests.contains(tileId)) {
                staleRequests.add(tileId);
            }
        }
        if (!staleRequests.isEmpty()) {
            mCancelTilesCallback.cancelTiles(staleRequests);
            mPendingTileRequests.removeAll(staleRequests);
        }

        System.arraycopy(retainedTiles, 0, mTiles, 0, mTiles.length);
        if (!newTiles.isEmpty()) {
            callback.requestNewTiles(newTiles);
            for (TileInfo requestedTile : newTiles) {
                mPendingTileRequests.add(requestedTile.getIndex());
            }
        }
        return true;
    }

    @NonNull
    protected Area getExpandedArea(@NonNull Rect viewArea) {
        return Area.expandFromArea(viewArea, mNumCols, numRows());
    }

    /** Callback for cancelling on-going tile requests */
    public interface CancelTilesCallback {
        /** Notifies of cancelling the tile requests for given tile indices */
        void cancelTiles(@NonNull Iterable<Integer> tileIds);
    }

    /** Callback for {@link #updateViewArea}. */
    public interface ViewAreaUpdateCallback {

        /** Notifies of new tiles required after the latest change in view area. */
        void requestNewTiles(@NonNull Iterable<TileInfo> tiles);

        /** Notifies of tiles are no longer needed after the latest change in view area. */
        void discardTiles(@NonNull Iterable<Integer> tileIds);
    }

    /** Lists the indexes of the tiles that are currently visible. */
    @NonNull
    public Iterable<Integer> getVisibleTileIndexes() {
        return areaIndexes(mVisibleArea);
    }

    /**
     *
     */
    public boolean isTileVisible(@NonNull TileInfo tileInfo) {
        return mVisibleArea != null
                && tileInfo.mRow >= mVisibleArea.mTop
                && tileInfo.mRow <= mVisibleArea.mBottom
                && tileInfo.mCol >= mVisibleArea.mLeft
                && tileInfo.mCol <= mVisibleArea.mRight;
    }

    /** Lists the tiles of an Area, yielding the indices of the corresponding tiles. */
    private Iterable<Integer> areaIndexes(final Area area) {
        return new Iterable<Integer>() {

            @NonNull
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    private int mCurrentRow = area.mTop;
                    private int mCurrentCol = area.mLeft;

                    @Override
                    public boolean hasNext() {
                        return mCurrentRow <= area.mBottom && mCurrentCol <= area.mRight;
                    }

                    @Override
                    public Integer next() {
                        int index = mNumCols * mCurrentRow + mCurrentCol;
                        advance();
                        return index;
                    }

                    private void advance() {
                        if (mCurrentCol < area.mRight) {
                            mCurrentCol++;
                        } else {
                            mCurrentRow++;
                            mCurrentCol = area.mLeft;
                        }
                    }

                    @Override
                    public void remove() {
                    }
                };
            }
        };
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(mTag + " (%s x %s), vis: %s", numRows(), mNumCols, mVisibleArea);
    }

    private void logMem() {
        int i = 0;
        StringBuilder out = new StringBuilder();
        for (Bitmap bitmap : mTiles) {
            if (bitmap != null) {
                out.append(i).append(",");
            }
            i++;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * Returns the {@link TileInfo}s for all tiles whose coordinates overlap with the areas
     * covered by
     * {@code rects}.
     */
    @NonNull
    public List<TileInfo> findTileInfosForRects(@NonNull List<Rect> rects) {
        Set<Integer> tileNums = new HashSet<>();
        for (Rect rect : rects) {
            tileNums.addAll(findTilesForRect(rect));
        }

        List<TileInfo> tileInfos = new ArrayList<>();
        for (Integer tileNum : tileNums) {
            tileInfos.add(getTileInfo(tileNum));
        }

        return tileInfos;
    }

    /** Returns the indices of every tile whose coordinates overlap with {@code rect}. */
    private Set<Integer> findTilesForRect(Rect rect) {
        int rowTop = rect.top / TILE_SIZE.getHeight();
        int rowBottom = rect.bottom / TILE_SIZE.getHeight();
        int colLeft = rect.left / TILE_SIZE.getWidth();
        int colRight = rect.right / TILE_SIZE.getWidth();

        Set<Integer> tileIndices = new HashSet<>();
        for (int i = rowTop; i <= rowBottom; i++) {
            int firstTileInRow = i * mNumCols;
            for (int j = colLeft; j <= colRight; j++) {
                tileIndices.add(firstTileInRow + j);
            }
        }

        return tileIndices;
    }

    /**
     * Specifies one tile of the board, by coordinates (row, column) or index in a tiles matrix.
     * Also
     * offers information about dimensions and position of this tile.
     */
    public class TileInfo {
        protected final int mRow;
        protected final int mCol;

        protected TileInfo(int index) {
            Preconditions.checkArgument(
                    index >= 0 && index < numTiles(),
                    String.format("Index %d incompatible with this board %s", index,
                            TileBoard.this));
            this.mRow = index / mNumCols;
            this.mCol = index % mNumCols;
        }

        /**
         *
         */
        public boolean belongsTo(@NonNull TileBoard board) {
            return TileBoard.this == board;
        }

        public int getIndex() {
            return mNumCols * mRow + mCol;
        }

        /** Returns the standard size of a tile. */
        @NonNull
        public Dimensions getSize() {
            return TILE_SIZE;
        }

        /** Returns the exact size of this tile, cropped to the page's bounds. */
        @NonNull
        public Dimensions getExactSize() {
            if (mRow < numRows() - 1 && mCol < mNumCols - 1) {
                return TILE_SIZE;
            }
            Point offset = getOffset();
            return new Dimensions(
                    Math.min(TILE_SIZE.getWidth(), mBounds.getWidth() - offset.x),
                    Math.min(TILE_SIZE.getHeight(), mBounds.getHeight() - offset.y));
        }

        @NonNull
        public Point getOffset() {
            return new Point(mCol * TILE_SIZE.getWidth(), mRow * TILE_SIZE.getHeight());
        }

        /**
         * Returns a new @link{Rect} matching the bounds of this tile. The Rect is not linked to
         * this
         * tile anymore.
         */
        @NonNull
        public Rect getBounds() {
            Point offset = getOffset();
            return new Rect(offset.x, offset.y, offset.x + TILE_SIZE.getWidth(),
                    offset.y + TILE_SIZE.getHeight());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof TileInfo) {
                TileInfo that = (TileInfo) o;
                return that.belongsTo(TileBoard.this) && this.mRow == that.mRow
                        && this.mCol == that.mCol;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 + TileBoard.this.hashCode() + getIndex();
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("Tile %d @(%d, %d)", getIndex(), mRow, mCol);
        }
    }

    /**
     * Describes an area (sub-rectangle) of a tiling. All 4 bounds are included: the area is
     * equal to
     * [{@link #mLeft}, {@link #mRight}] x [{@link #mTop}, {@link #mBottom}]. Areas are immutable.
     */
    protected static class Area {
        private final int mLeft;
        private final int mTop;
        private final int mRight;
        private final int mBottom;

        /**
         * Create a tiling {@link Area} covering the given area (in pixels) and overflowing at least
         * half a tile in each direction if possible.
         */
        public static Area expandFromArea(Rect areaPx, int numCols, int numRows) {
            int left = (areaPx.left - (TILE_SIZE.getWidth() / 2)) / TILE_SIZE.getWidth();
            int top = (areaPx.top - (TILE_SIZE.getHeight() / 2)) / TILE_SIZE.getHeight();
            int right = (areaPx.right + (TILE_SIZE.getWidth() / 2)) / TILE_SIZE.getWidth();
            int bottom = (areaPx.bottom + (TILE_SIZE.getHeight() / 2)) / TILE_SIZE.getHeight();

            left = Math.max(0, left);
            top = Math.max(0, top);
            right = Math.min(numCols - 1, right);
            bottom = Math.min(numRows - 1, bottom);

            return new Area(left, top, right, bottom);
        }

        protected Area(int left, int top, int right, int bottom) {
            this.mLeft = left;
            this.mTop = top;
            this.mRight = right;
            this.mBottom = bottom;
        }

        public int size() {
            return (mRight - mLeft + 1) * (mBottom - mTop + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Area)) {
                return false;
            }
            Area that = (Area) o;
            return this.mLeft == that.mLeft
                    && this.mTop == that.mTop
                    && this.mRight == that.mRight
                    && this.mBottom == that.mBottom;
        }

        @Override
        public int hashCode() {
            int result = 31 + mBottom;
            result = 31 * result + mLeft;
            result = 31 * result + mRight;
            result = 31 * result + mTop;
            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("Area [%d tiles] (%d %d - %d %d)", size(), mTop, mLeft, mBottom,
                    mRight);
        }
    }
}

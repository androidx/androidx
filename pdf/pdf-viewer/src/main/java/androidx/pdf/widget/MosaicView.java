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

import static java.lang.Math.max;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.RectUtils;
import androidx.pdf.util.TileBoard;
import androidx.pdf.util.overlays.ViewWithOverlays;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A View that can display a very large image (bitmap). Depending on the size at which it has to
 * display, it will either load the whole bitmap and scale it up or down, or operate on a tile map
 * of the bitmap and load the tiles that are visible, or both (use the whole-image bitmap as a
 * backup for missing tiles).
 * <p>
 * When using tiling, the tiles ({@link TileView}) are laid out in pre-scaled pixels but drawn at
 * post-scale pixels for full resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class MosaicView extends ViewGroup implements ViewWithOverlays {

    static final String TAG = MosaicView.class.getSimpleName();

    private static final Matrix IDENTITY = new Matrix();

    /** The maximum size we want for any bitmap (limited by texture size and memory). */
    private final int mMaxBitmapSize;

    /** Cap on the whole-page bitmap size, when tiling is active. */
    private final int mMaxBgPageSize;

    /**
     * Overlays to be drawn in {@link #dispatchDraw(Canvas)}, keyed by Strings to
     * allow removing.
     */
    private final Map<String, Drawable> mOverlays = new HashMap<>();

    private static final Paint WHITE_PAINT = new Paint() {{
            setColor(Color.WHITE);
            setStyle(Style.FILL);
        }};

    private static final Paint MESSAGE_PAINT = new Paint() {{
            setColor(Color.WHITE);
            setStyle(Style.FILL);
            setTextAlign(Align.CENTER);
            setTextSize(20);
            setStrokeWidth(1f);
        }};

    static final Paint DEBUG_PAINT = new Paint() {{
            setColor(Color.BLUE);
            setStyle(Style.STROKE);
            setTextAlign(Align.CENTER);
            setTextSize(20);
            setStrokeWidth(1f);
        }};

    private static final Paint DITHER_BITMAP_PAINT = new Paint(Paint.FILTER_BITMAP_FLAG);

    /**
     * Gets a conservative bound on the size of Bitmaps in order not to blow up something.
     * The returned number is a constant for this device (does not depend on rotations etc.).
     * Used to use @code{GLES20.GL_MAX_TEXTURE_SIZE}, which is now deprecated.
     */
    public static int getMaxTileSize(@NonNull Context context) {
        WindowManager wm = context.getSystemService(WindowManager.class);
        Display display = wm.getDefaultDisplay();
        Point dimPoint = new Point();
        display.getSize(dimPoint);
        int maxScreenDimension = max(dimPoint.x, dimPoint.y);
        return max(maxScreenDimension, 1024);
    }

    /** The bounds of the image, which will define the measures of this view. */
    private final Rect mBounds = new Rect();

    @NonNull
    protected BitmapRecycler mBitmapRecycler;

    @NonNull
    protected BitmapSource mBitmapSource;

    @Nullable
    protected TileBoard mTileBoard;

    @Nullable
    private Bitmap mBitmap;

    @Nullable
    private String mFailure;

    /**
     * The tiles of this view, if applicable. This is a type-safe and indexed replica of the
     * children of this View.
     */
    protected final SparseArray<TileView> mTiles = new SparseArray<>();

    /** The page width that was requested in the last call to {@link #requestDrawAtZoom}. */
    private int mRequestedWidth;

    /** The page zoom that was in effect during the last call to {@link #requestDrawAtZoom}. */
    protected float mBaseZoom;

    /**
     * The portion of this view that is currently (or last we knew) exposed on the screen.
     * In the co-ordinates of this view - so if this entire view is visible, then viewArea will
     * contain the rect Rect(0, 0, getWidth, getHeight).
     */
    private final Rect mViewArea = new Rect();

    /**
     * The viewArea, scaled up by the baseZoom - the zoom we were last requested to draw at.
     * So relative to (0, 0)-(getWidth() * baseZoom, getHeight() * baseZoom).
     */
    private final Rect mScaledViewArea = new Rect();

    public MosaicView(@NonNull Context context) {
        super(context);
    }

    public MosaicView(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    public MosaicView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    {
        setWillNotDraw(false);
        mMaxBitmapSize = getMaxTileSize(getContext());
        mMaxBgPageSize = mMaxBitmapSize / 2;
    }

    /**
     * Initializes this view by giving it more arguments that would fit in the standard
     * constructors.
     *
     * @param dimensions     the dimensions (in pixels of the raw drawing area (at zoom == 1)
     * @param bitmapRecycler a {@link BitmapRecycler} instance for recycling bitmaps.
     * @param bitmapSource   the object used to request all bitmaps.
     */
    public void init(@NonNull Dimensions dimensions, @NonNull BitmapRecycler bitmapRecycler,
            @NonNull BitmapSource bitmapSource) {
        mBounds.set(0, 0, dimensions.getWidth(), dimensions.getHeight());
        this.mBitmapRecycler = bitmapRecycler;
        this.mBitmapSource = bitmapSource;
        requestLayout();
    }

    /**
     * Broker of {@link Bitmap}s for this view. The requested bitmaps should be given back to this
     * view with {@link MosaicView#setPageBitmap} and {@link MosaicView#setTileBitmap}.
     */
    public interface BitmapSource {

        /** Request to load the whole page bitmap at given {@link Dimensions}. */
        void requestPageBitmap(@NonNull Dimensions pageSize, boolean alsoRequestingTiles);

        /** Request to load new tiles at given {@link Dimensions}. */
        void requestNewTiles(@NonNull Dimensions pageSize,
                @NonNull Iterable<TileBoard.TileInfo> newTiles);

        /**
         * Cancel the request for a tile bitmap for given tile indices. This method will be called
         * by MosaicView when requested tiles are not needed anymore (have become stale). This may
         * mean that the entire tile board is now stale or just the given tiles in the tile board
         * are stale. This function is not expected to do a lot of processing.
         */
        void cancelTiles(@NonNull Iterable<Integer> tileIds);
    }

    @NonNull
    public Rect getBounds() {
        return mBounds;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @NonNull
    protected Dimensions getPageDimensionsAtWidth(int width) {
        int height = width * mBounds.height() / mBounds.width();
        return new Dimensions(width, height);
    }

    @Override
    public void addOverlay(@NonNull String key, @NonNull Drawable overlay) {
        mOverlays.put(key, overlay);
        invalidate();
    }

    /** Check if the given key has an overlay. */
    public boolean hasOverlay(@NonNull String key) {
        return mOverlays.get(key) != null;
    }

    /** Remove overlay corresponding to the given key. */
    public void removeOverlay(@NonNull String key) {
        mOverlays.remove(key);
        invalidate();
    }

    /** Set page bitmap. */
    public void setPageBitmap(@NonNull Bitmap pageBitmap) {
        Preconditions.checkNotNull(pageBitmap, "Use removePageBitmap() instead.");
        mFailure = null;
        mBitmap = pageBitmap;
        invalidate();
    }

    /** Check if bitmap is null. */
    public boolean hasBitmap() {
        return mBitmap != null;
    }

    /** Removes all the bitmaps (whole and tiles). */
    public void clearAllBitmaps() {
        if (hasBitmap()) {
            mBitmapRecycler.discardBitmap(mBitmap);
        }
        mBitmap = null;
        mFailure = null;
        if (mTileBoard != null) {
            clearTiles();
        } else {
            Preconditions.checkState(getChildCount() == 0,
                    "Has Children with no TileBoard, e.g. " + getChildAt(0));
            Preconditions.checkState(mTiles.size() == 0, "Has TileViews with "
                    + "no TileBoard.");
        }
    }

    /** Clear the tiling for this view, reclaims the corresponding memory. */
    public void clearTiles() {
        removeAllViews();
        mTiles.clear();
        if (mTileBoard != null) {
            mTileBoard.clearTiles();
            mTileBoard = null;
            mBaseZoom = 0;
        }
    }

    /** Creates a new tiling for this view. */
    private void prepareTiles(Dimensions viewSize, float zoom) {
        TileBoard newBoard = createTileBoard(viewSize);
        if (mTileBoard != null) {
            float tileArea = (float) RectUtils.area(mBounds) / newBoard.numTiles();
            if (zoom > mBaseZoom && tileArea < 1) {
                return;
            }
        }
        clearTiles();
        mTileBoard = newBoard;
        setBaseZoom(zoom);
    }

    protected void setBaseZoom(float zoom) {
        mBaseZoom = zoom;
    }

    @NonNull
    protected TileBoard createTileBoard(@NonNull Dimensions viewSize) {
        return new TileBoard(getId(), viewSize, mBitmapRecycler,
                new TileBoard.CancelTilesCallback() {
                    @Override
                    public void cancelTiles(Iterable<Integer> tileIds) {
                        StringBuilder log = new StringBuilder("[");
                        int count = 0;
                        for (Integer tileId : tileIds) {
                            log.append(tileId);
                            log.append(", ");
                            count++;
                        }
                        log.append("]");
                        mBitmapSource.cancelTiles(tileIds);
                    }
                });
    }

    /**
     * Adds a tile for the given tileId and assigns it an id related to its index.
     * Since index == 0 is an acceptable value, we have to assign ids > 0.
     */
    private void addTile(TileBoard.TileInfo tileId) {
        TileView tile = new TileView(getContext(), tileId);
        mTiles.append(tileId.getIndex(), tile);
        addView(tile);
    }

    private TileView getTileByIndex(int index) {
        return mTiles.get(index);
    }

    /** Set failure message. */
    public void setFailure(@NonNull String message) {
        mFailure = message;
        invalidate();
    }

    /**
     * Updates this view to draw itself at the given width. May include re-tiling and requesting new
     * bitmaps for the whole page and / or tiles.
     *
     * @param zoom the zoom factor to be used for bitmaps.
     */
    public void requestDrawAtZoom(float zoom) {
        mRequestedWidth = (int) (zoom * mBounds.width());
        boolean needTiling = needTiling(mRequestedWidth);
        int cappedWidth = getCappedWidth(needTiling);
        if (mBitmap == null || mBitmap.getWidth() != cappedWidth) {
            Dimensions pageSize = getPageDimensionsAtWidth(cappedWidth);
            mBitmapSource.requestPageBitmap(pageSize, /* alsoRequestingTiles= */ needTiling);
        }

        if (needTiling) {
            if (mTileBoard == null || mTileBoard.needsTiles(mRequestedWidth)) {
                Dimensions pageSize = getPageDimensionsAtWidth(mRequestedWidth);
                prepareTiles(pageSize, zoom);
            }
            requestTiles();
        } else {
            clearTiles();
        }
    }

    /**
     * Asks the view to redraw the requested areas.
     *
     * <p>This is intended for areas that have already been rendered but whose <i>content</i> has
     * changed since that time. It should not be used for normal changes handled by MosaicView such
     * as changes to viewArea or zoom.
     *
     * <p>Areas will be redrawn to match the current state of the view, i.e.
     * {@link #mRequestedWidth} and {@link #mBounds} and will use the same scaling logic as
     * {@link #requestDrawAtZoom(float)}.
     *
     * <p>Only bitmaps that are currently held by this view will be replaced. If this view is not
     * holding any bitmaps, for example, following {@link #clearAllBitmaps()} or similar, no bitmaps
     * will be requested. If the view is currently only using a page bitmap, only a page bitmap will
     * be requested. If the view is using tiling, both the background page bitmap and any affected
     * tiles will be requested.
     */
    public void requestRedrawAreas(final @NonNull List<Rect> invalidRects) {
        if (invalidRects == null || invalidRects.isEmpty()) {
            return;
        }

        boolean needTiling = needTiling(mRequestedWidth);

        if (mBitmap != null) {
            int cappedWidth = getCappedWidth(needTiling);
            if (cappedWidth > 0) {
                Dimensions pageSize = getPageDimensionsAtWidth(cappedWidth);
                mBitmapSource.requestPageBitmap(pageSize, /* alsoRequestingTiles= */ needTiling);
            }
        }

        if (needTiling && mTileBoard != null) {
            final List<TileBoard.TileInfo> affectedTiles =
                    mTileBoard.findTileInfosForRects(scaleRects(invalidRects));
            final Dimensions scaledBounds = getPageDimensionsAtWidth(mRequestedWidth);
            mBitmapSource.requestNewTiles(scaledBounds, affectedTiles);
        }
    }

    /**
     * Get the capped width of the page bitmap based on whether or not tiling is being used.
     *
     * <p>If invalid (non-positive), logs the error. Also, throws a {@link RuntimeException} if
     * running a dev build.
     */
    private int getCappedWidth(boolean needTiling) {
        int cappedWidth =
                needTiling
                        ? limitBitmapWidth(mRequestedWidth, mMaxBgPageSize)
                        : limitBitmapWidth(mRequestedWidth, mMaxBitmapSize);
        if (cappedWidth <= 0) {
            throw new RuntimeException(String.format("Invalid width %s", cappedWidth));
        }
        return cappedWidth;
    }

    /** Determines the current zoom of this view and scales {@code unscaled} accordingly. */
    private List<Rect> scaleRects(List<Rect> unscaled) {
        float zoom = ((float) mRequestedWidth) / ((float) mBounds.width());

        List<Rect> scaled = new ArrayList<>(unscaled.size());
        for (Rect rect : unscaled) {
            int scaledLeft = (int) (rect.left * zoom);
            int scaledTop = (int) (rect.top * zoom);
            int scaledRight = (int) (rect.right * zoom);
            int scaledBottom = (int) (rect.bottom * zoom);
            scaled.add(new Rect(scaledLeft, scaledTop, scaledRight, scaledBottom));
        }
        return scaled;
    }

    /**
     * Updates this view to draw itself quickly at the given width. This is suitable for
     * intermediate, transient drawing states as it limits the amount of assets required
     * (i.e. no tiling).
     *
     * @param zoom the adequate zoom level for the required display.
     */
    public void requestFastDrawAtZoom(float zoom) {
        if (mBitmap == null) {
            mRequestedWidth = (int) (zoom * mBounds.width());
            int cappedWidth = limitBitmapWidth(mRequestedWidth, mMaxBgPageSize);
            if (cappedWidth <= 0) {
                throw new RuntimeException(
                        String.format("Invalid width cap:%s z:%s", cappedWidth, zoom));
            } else {
                Dimensions pageSize = getPageDimensionsAtWidth(cappedWidth);
                mBitmapSource.requestPageBitmap(pageSize, /* alsoRequestingTiles= */ false);
            }
        }
    }

    protected boolean needTiling(int width) {
        return width > limitBitmapWidth(width, mMaxBitmapSize);
    }

    /**
     * Updates the portion of this View that is visible on the screen, in this View's coordinates -
     * so relative to (0, 0)-(getWidth(), getHeight()).
     */
    public void setViewArea(int left, int top, int right, int bottom) {
        mViewArea.set(left, top, right, bottom);
        if (!mViewArea.intersect(0, 0, mBounds.width(), mBounds.height())) {
            // Modifies viewArea.
            mViewArea.setEmpty();
        }
    }

    /**
     * Updates the portion of this View that is visible on the screen, in this View's coordinates -
     * so relative to (0, 0)-(getWidth(), getHeight()).
     */
    public void setViewArea(@NonNull Rect viewArea) {
        setViewArea(viewArea.left, viewArea.top, viewArea.right, viewArea.bottom);
    }

    /**
     * Returns the portion of this View that is visible on the screen, in this View's coordinates -
     * so relative to (0, 0) - (getWidth(), getHeight()).
     */
    @NonNull
    public Rect getViewArea() {
        return mViewArea;
    }

    /**
     * Request new tiles for an existing tiling. Requires this view to be laid out. This will
     * request tiles of the right detail according to the baseZoom (updated by
     * {@link #requestDrawAtZoom}) and that cover the viewArea (updated by {@link #setViewArea}).
     */
    public void requestTiles() {
        if (mTileBoard == null) {
            return;
        }

        final Dimensions scaledBounds = getPageDimensionsAtWidth(mRequestedWidth);

        mScaledViewArea.set(mViewArea);
        RectUtils.scale(mScaledViewArea, mBaseZoom);
        // viewArea is already clipped to bounds, but we also clip scaledViewArea to scaledBounds.
        // Otherwise it might be slightly outside scaledBounds due to rounding errors.
        if (!clipAreaToPageSize(mScaledViewArea, scaledBounds)) {
            mScaledViewArea.setEmpty();
        }

        if (!mScaledViewArea.isEmpty()) {
            updateViewArea(mScaledViewArea, new TileBoard.ViewAreaUpdateCallback() {

                @Override
                public void requestNewTiles(Iterable<TileBoard.TileInfo> newTiles) {
                    mBitmapSource.requestNewTiles(scaledBounds, newTiles);
                }

                @Override
                public void discardTiles(Iterable<Integer> tileIds) {
                    // These tiles have already been created there is no point in telling the
                    // bitmap source to cancel its request. Thus this is no-op.
                }
            });
        }
    }

    protected boolean clipAreaToPageSize(@NonNull Rect scaledViewArea,
            @NonNull Dimensions pageSize) {
        return scaledViewArea.intersect(0, 0, pageSize.getWidth(), pageSize.getHeight());
    }

    @CanIgnoreReturnValue
    private boolean updateViewArea(Rect scaledViewArea,
            final TileBoard.ViewAreaUpdateCallback callback) {
        return mTileBoard.updateViewArea(scaledViewArea, new TileBoard.ViewAreaUpdateCallback() {

            @Override
            public void requestNewTiles(Iterable<TileBoard.TileInfo> newTiles) {
                StringBuilder sb = new StringBuilder("[");
                int count = 0;
                for (TileBoard.TileInfo tileId : newTiles) {
                    if (getTileByIndex(tileId.getIndex()) == null) {
                        addTile(tileId);
                        sb.append(tileId.getIndex()).append(", ");
                        count++;
                    }
                }
                sb.append("]");
                callback.requestNewTiles(newTiles);
            }

            @Override
            public void discardTiles(Iterable<Integer> tileIds) {
                callback.discardTiles(tileIds);
                StringBuilder sb = new StringBuilder("[");
                int count = 0;
                for (int k : tileIds) {
                    TileView tv = getTileByIndex(k);
                    sb.append(k).append(", ");
                    count++;
                    if (tv != null) {
                        tv.reset();
                        removeView(tv);
                        mTiles.remove(tv.mTileInfo.getIndex());
                    }
                }
                sb.append("]");
            }
        });
    }

    /** Set tile bitmap. */
    public void setTileBitmap(@NonNull TileBoard.TileInfo tileInfo, @NonNull Bitmap tileBitmap) {
        Preconditions.checkNotNull(tileBitmap, "Use removePageBitmap() instead.");
        if (mTileBoard != null && mTileBoard.setTile(tileInfo, tileBitmap)) {
            TileView tile = getTileByIndex(tileInfo.getIndex());
            if (tile != null) {
                tile.setBitmap(tileInfo, tileBitmap);
            }
        } else {
            mBitmapRecycler.discardBitmap(tileBitmap);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBounds.width(), mBounds.height());
        int count = mTiles.size();
        if (count != 0) {
            float scale = (float) mBounds.width() / mRequestedWidth;
            for (int i = 0; i < count; i++) {
                TileView tv = mTiles.valueAt(i);
                Dimensions size = tv.mTileInfo.getSize();
                tv.measure((int) Math.ceil(size.getWidth() * scale),
                        (int) Math.ceil(size.getHeight() * scale));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = mTiles.size();
        if (count != 0) {
            float scale = (float) getWidth() / mRequestedWidth;
            for (int i = 0; i < count; i++) {
                TileView tv = mTiles.valueAt(i);
                Rect bounds = RectUtils.scale(tv.mTileInfo.getBounds(), scale);
                tv.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // No tiling: draw the page bitmap or a white page.
        if (mBitmap != null) {
            canvas.save();
            float scale = (float) getWidth() / mBitmap.getWidth();
            canvas.scale(scale, scale);
            canvas.drawBitmap(mBitmap, IDENTITY, DITHER_BITMAP_PAINT);
            canvas.restore();
        } else if (mFailure != null) {
            canvas.drawText(mFailure, getWidth() / 2, getHeight() / 2 - 10,
                    MosaicView.MESSAGE_PAINT);
        } else {
            canvas.drawRect(mBounds, WHITE_PAINT);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        for (Drawable overlay : mOverlays.values()) {
            if (overlay != null) {
                overlay.draw(canvas);
            }
        }
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
        TileView tile = (TileView) child;
        canvas.save();
        float scale = (float) getWidth() / mTileBoard.mBounds.getWidth();
        canvas.scale(scale, scale);
        Point offset = tile.getOffset();
        canvas.translate(offset.x, offset.y);
        child.draw(canvas);
        canvas.restore();
        return true;
    }

    private int limitBitmapWidth(int requestedWidth, int maxSize) {
        return min(requestedWidth, maxSize, mBounds.width() * maxSize / mBounds.height());
    }

    private String getLogTag() {
        return TAG + getId();
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    @NonNull
    @Override
    public String toString() {
        return getLogTag() + String.format(" bg: %s /t: %s",
                (mBitmap != null ? mBitmap.getWidth() : "x"),
                (mTileBoard != null ? mTileBoard.toString() : "no tiles"));
    }
}

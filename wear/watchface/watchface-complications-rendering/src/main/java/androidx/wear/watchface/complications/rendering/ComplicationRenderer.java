/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.Icon.OnDrawableLoadedListener;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.text.Layout;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.wear.watchface.complications.rendering.utils.IconLayoutHelper;
import androidx.wear.watchface.complications.rendering.utils.LargeImageLayoutHelper;
import androidx.wear.watchface.complications.rendering.utils.LayoutHelper;
import androidx.wear.watchface.complications.rendering.utils.LayoutUtils;
import androidx.wear.watchface.complications.rendering.utils.LongTextLayoutHelper;
import androidx.wear.watchface.complications.rendering.utils.RangedValueLayoutHelper;
import androidx.wear.watchface.complications.rendering.utils.ShortTextLayoutHelper;
import androidx.wear.watchface.complications.rendering.utils.SmallImageLayoutHelper;

import java.time.Instant;
import java.util.Objects;

/**
 * Renders complication data on a canvas.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ComplicationRenderer {

    public interface OnInvalidateListener {
        void onInvalidate();
    }

    private static final String TAG = "ComplicationRenderer";

    /**
     * When set to true, this class will draw rectangles around every component. It is to see
     * padding and gravity. Testing this class with DEBUG_MODE set to true causes a test to fail so
     * it's only meant to be true on local builds.
     */
    @VisibleForTesting
    static final boolean DEBUG_MODE = false;

    /** The gap between the in progress stroke and the remain stroke. */
    @VisibleForTesting
    static final int STROKE_GAP_IN_DEGREES = 4;

    /**
     * Starting angle for ranged value, i.e. in progress part will start from this angle. As it's
     * drawn clockwise, -90 corresponds to 12 o'clock on a watch.
     */
    @VisibleForTesting
    static final int RANGED_VALUE_START_ANGLE = -90;

    /** Size fraction used for drawing icons. 1.0 here means no padding is applied. */
    private static final float ICON_SIZE_FRACTION = 1.0f;

    /**
     * Size fraction used for drawing small image. 0.95 here means image will be 0.95 the size of
     * its container.
     */
    private static final float SMALL_IMAGE_SIZE_FRACTION = 0.95f;

    /** Size fraction used for drawing large image. */
    private static final float LARGE_IMAGE_SIZE_FRACTION = 1.0f;

    /** Used to apply padding to the beginning of the text when it's left aligned. */
    private static final float TEXT_PADDING_HEIGHT_FRACTION = 0.1f;

    /** Context is required for localization. */
    private final Context mContext;

    private ComplicationData mComplicationData;
    private final Rect mBounds = new Rect();

    /** Used to render {@link ComplicationData#TYPE_NO_DATA}. */
    private CharSequence mNoDataText = "";

    private boolean mRangedValueProgressHidden;

    private boolean mHasNoData;

    // Below drawables will be null until they are fully loaded.
    @Nullable
    Drawable mIcon;
    @Nullable
    Drawable mBurnInProtectionIcon;
    @Nullable
    Drawable mSmallImage;
    @Nullable
    Drawable mBurnInProtectionSmallImage;
    @Nullable
    Drawable mLargeImage;

    // Drawables for rendering rounded images
    private RoundedDrawable mRoundedBackgroundDrawable = null;
    private RoundedDrawable mRoundedLargeImage = null;
    private RoundedDrawable mRoundedSmallImage = null;

    // Text renderers
    private final TextRenderer mMainTextRenderer = new TextRenderer();
    private final TextRenderer mSubTextRenderer = new TextRenderer();

    // Bounds for components. NB we want to avoid allocations in watch face rendering code to
    // reduce GC pressure.
    private final Rect mBackgroundBounds = new Rect();
    private final RectF mBackgroundBoundsF = new RectF();
    private final Rect mIconBounds = new Rect();
    private final Rect mSmallImageBounds = new Rect();
    private final Rect mLargeImageBounds = new Rect();
    private final Rect mMainTextBounds = new Rect();
    private final Rect mSubTextBounds = new Rect();
    private final Rect mRangedValueBounds = new Rect();
    private final RectF mRangedValueBoundsF = new RectF();

    // Paint sets for active and ambient modes.
    @VisibleForTesting
    PaintSet mActivePaintSet = null;
    @VisibleForTesting
    PaintSet mAmbientPaintSet = null;

    // Paints for texts
    @Nullable
    private TextPaint mMainTextPaint = null;
    @Nullable
    private TextPaint mSubTextPaint = null;

    // Styles for active and ambient modes.
    private ComplicationStyle mActiveStyle;
    private ComplicationStyle mAmbientStyle;

    @Nullable
    private Paint mDebugPaint;

    @Nullable
    private OnInvalidateListener mInvalidateListener;

    /**
     * Initializes complication renderer.
     *
     * @param context      Current [Context].
     * @param activeStyle  ComplicationSlot style to be used when in active mode.
     * @param ambientStyle ComplicationSlot style to be used when in ambient mode.
     */
    ComplicationRenderer(
            Context context, ComplicationStyle activeStyle, ComplicationStyle ambientStyle) {
        mContext = context;
        updateStyle(activeStyle, ambientStyle);
        if (DEBUG_MODE) {
            mDebugPaint = new Paint();
            mDebugPaint.setColor(Color.argb(128, 255, 255, 0));
            mDebugPaint.setStyle(Style.STROKE);
        }
    }

    /**
     * Updates the complication styles in active and ambient modes
     *
     * @param activeStyle  complication style in active mode
     * @param ambientStyle complication style in ambient mode
     */
    public void updateStyle(
            @NonNull ComplicationStyle activeStyle, @NonNull ComplicationStyle ambientStyle) {
        mActiveStyle = activeStyle;
        mAmbientStyle = ambientStyle;
        // Reset paint sets
        mActivePaintSet = new PaintSet(activeStyle, false, false, false);
        mAmbientPaintSet = new PaintSet(ambientStyle, true, false, false);
        calculateBounds();
    }

    /**
     * Sets the complication data to be rendered.
     *
     * @param data ComplicationSlot data to be rendered. If this is null, nothing is drawn.
     * @param loadDrawablesAsync If true any drawables will be loaded asynchronously, otherwise
     *     they will be loaded synchronously.
     */
    public void setComplicationData(@Nullable ComplicationData data, boolean loadDrawablesAsync) {
        if (Objects.equals(mComplicationData, data)) {
            return;
        }
        if (data == null) {
            mComplicationData = null;
            // Free unnecessary RoundedDrawables.
            mRoundedBackgroundDrawable = null;
            mRoundedLargeImage = null;
            mRoundedSmallImage = null;
            return;
        }
        if (data.getType() == ComplicationData.TYPE_NO_DATA) {
            if (!mHasNoData) {
                // Render TYPE_NO_DATA as a short text complication with a predefined string
                mHasNoData = true;
                mComplicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setShortText(ComplicationText.plainText(mNoDataText))
                                .build();
            } else {
                // This return prevents recalculating bounds if renderer already has TYPE_NO_DATA
                return;
            }
        } else {
            mComplicationData = data;
            mHasNoData = false;
        }
        if (loadDrawablesAsync) {
            if (!loadDrawableIconAndImagesAsync()) {
                invalidate();
            }
        } else {
            loadDrawableIconAndImages();
        }
        calculateBounds();

        // Based on the results of calculateBounds we know if mRoundedLargeImage or
        // mSmallImageBounds are needed for rendering and can null the references if not required.
        // NOTE mRoundedBackgroundDrawable has a different lifecycle which is based on the current
        // paint mode so it doesn't make sense to clear it's reference here.
        mRoundedLargeImage = null;
        mRoundedSmallImage = null;
    }

    /**
     * Sets bounds for the complication data to be drawn within. Returns true if the boundaries are
     * recalculated for components.
     *
     * @param bounds Bounds for the complication data to be drawn within.
     */
    public boolean setBounds(@NonNull Rect bounds) {
        // Calculations can be avoided if size didn't change
        boolean shouldCalculateBounds = true;
        if (mBounds.width() == bounds.width() && mBounds.height() == bounds.height()) {
            shouldCalculateBounds = false;
        }
        mBounds.set(bounds);
        if (shouldCalculateBounds) {
            calculateBounds();
        }
        return shouldCalculateBounds;
    }

    /**
     * Sets the text to be rendered when {@link ComplicationData} is of type {@link
     * ComplicationData#TYPE_NO_DATA}. If no data text is null, an empty string will be rendered.
     */
    public void setNoDataText(@Nullable CharSequence noDataText) {
        if (noDataText == null) {
            noDataText = "";
        }
        // Making a copy of the CharSequence because mutable CharSequences may cause undefined
        // behavior
        mNoDataText = noDataText.subSequence(0, noDataText.length());
        // Create complication data with new text.
        if (mHasNoData) {
            mHasNoData = false;
            setComplicationData(
                    new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build(), true);
        }
    }

    /** Sets if the ranged value progress should be hidden. */
    public void setRangedValueProgressHidden(boolean hidden) {
        if (mRangedValueProgressHidden != hidden) {
            mRangedValueProgressHidden = hidden;
            calculateBounds();
        }
    }

    /** Returns {@code true} if the ranged value progress should be hidden. */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public boolean isRangedValueProgressHidden() {
        return mRangedValueProgressHidden;
    }

    /**
     * Renders complication data on a canvas. Does nothing if the current data is null, has type
     * 'empty' or 'not configured', or is not active.
     *
     * @param canvas            canvas to be drawn on.
     * @param currentTime       current time as an {@link Instant}
     * @param inAmbientMode     true if the device is in ambient mode.
     * @param lowBitAmbient     true if the screen supports fewer bits for each color in ambient
     *                          mode.
     * @param burnInProtection  true if burn-in protection is required.
     * @param showTapHighlight  true if the complication should be drawn with a highlighted effect,
     *                          to provide visual feedback after a tap.
     */
    public void draw(
            @NonNull Canvas canvas,
            Instant currentTime,
            boolean inAmbientMode,
            boolean lowBitAmbient,
            boolean burnInProtection,
            boolean showTapHighlight) {
        // If complication data is not available or empty, or is not active, don't draw
        if (mComplicationData == null
                || mComplicationData.getType() == ComplicationData.TYPE_EMPTY
                || mComplicationData.getType() == ComplicationData.TYPE_NOT_CONFIGURED
                || !mComplicationData.isActiveAt(currentTime.toEpochMilli())
                || mBounds.isEmpty()) {
            return;
        }
        // If in ambient mode but paint set is not usable with current ambient properties,
        // reinitialize.
        if (inAmbientMode
                && (mAmbientPaintSet.mLowBitAmbient != lowBitAmbient
                || mAmbientPaintSet.mBurnInProtection != burnInProtection)) {
            mAmbientPaintSet = new PaintSet(mAmbientStyle, true, lowBitAmbient, burnInProtection);
        }
        // Choose the correct paint set to use
        PaintSet currentPaintSet = inAmbientMode ? mAmbientPaintSet : mActivePaintSet;
        // Update complication texts
        updateComplicationTexts(currentTime.toEpochMilli());
        canvas.save();
        canvas.translate(mBounds.left, mBounds.top);
        // Draw background first
        drawBackground(canvas, currentPaintSet);
        // Draw content
        drawIcon(canvas, currentPaintSet);
        drawSmallImage(canvas, currentPaintSet);
        drawLargeImage(canvas, currentPaintSet);
        drawRangedValue(canvas, currentPaintSet);
        drawMainText(canvas, currentPaintSet);
        drawSubText(canvas, currentPaintSet);
        // Draw highlight if highlighted
        if (showTapHighlight) {
            drawHighlight(canvas, currentPaintSet);
        }
        // Draw borders last (to ensure that they are always visible)
        drawBorders(canvas, currentPaintSet);
        canvas.restore();
    }

    public void setOnInvalidateListener(@Nullable OnInvalidateListener listener) {
        mInvalidateListener = listener;
    }

    private void invalidate() {
        if (mInvalidateListener != null) {
            mInvalidateListener.onInvalidate();
        }
    }

    private void updateComplicationTexts(long currentTimeMillis) {
        if (mComplicationData.hasShortText()) {
            mMainTextRenderer.setMaxLines(1);
            mMainTextRenderer.setText(
                    mComplicationData.getShortText().getTextAt(
                            mContext.getResources(), currentTimeMillis));
            if (mComplicationData.getShortTitle() != null) {
                mSubTextRenderer.setText(
                        mComplicationData.getShortTitle().getTextAt(
                                mContext.getResources(), currentTimeMillis));
            } else {
                mSubTextRenderer.setText("");
            }
        }
        if (mComplicationData.hasLongText()) {
            mMainTextRenderer.setText(
                    mComplicationData.getLongText().getTextAt(
                            mContext.getResources(), currentTimeMillis));
            if (mComplicationData.getLongTitle() != null) {
                mSubTextRenderer.setText(
                        mComplicationData.getLongTitle().getTextAt(
                                mContext.getResources(), currentTimeMillis));
                // If long text has title, only show one line from each
                mMainTextRenderer.setMaxLines(1);
            } else {
                mSubTextRenderer.setText("");
                // If long text doesn't have a title, show two lines from text
                mMainTextRenderer.setMaxLines(2);
            }
        }
    }

    private void drawBackground(Canvas canvas, PaintSet paintSet) {
        int radius = getBorderRadius(paintSet.mStyle);
        canvas.drawRoundRect(mBackgroundBoundsF, radius, radius, paintSet.mBackgroundPaint);
        if (paintSet.mStyle.getBackgroundDrawable() != null
                && !paintSet.isInBurnInProtectionMode()) {
            if (mRoundedBackgroundDrawable == null) {
                mRoundedBackgroundDrawable = new RoundedDrawable();
            }
            mRoundedBackgroundDrawable.setDrawable(paintSet.mStyle.getBackgroundDrawable());
            mRoundedBackgroundDrawable.setRadius(radius);
            mRoundedBackgroundDrawable.setBounds(mBackgroundBounds);
            mRoundedBackgroundDrawable.draw(canvas);
        } else {
            mRoundedBackgroundDrawable = null;
        }
    }

    private void drawBorders(Canvas canvas, PaintSet paintSet) {
        if (paintSet.mStyle.getBorderStyle() != ComplicationStyle.BORDER_STYLE_NONE) {
            int radius = getBorderRadius(paintSet.mStyle);
            canvas.drawRoundRect(mBackgroundBoundsF, radius, radius, paintSet.mBorderPaint);
        }
    }

    private void drawHighlight(Canvas canvas, PaintSet paintSet) {
        if (!paintSet.mIsAmbientStyle) {
            // Don't draw the highlight in ambient mode
            int radius = getBorderRadius(paintSet.mStyle);
            canvas.drawRoundRect(mBackgroundBoundsF, radius, radius, paintSet.mHighlightPaint);
        }
    }

    private void drawMainText(Canvas canvas, PaintSet paintSet) {
        if (mMainTextBounds.isEmpty()) {
            return;
        }
        if (DEBUG_MODE) {
            canvas.drawRect(mMainTextBounds, mDebugPaint);
        }
        if (mMainTextPaint != paintSet.mPrimaryTextPaint) {
            mMainTextPaint = paintSet.mPrimaryTextPaint;
            mMainTextRenderer.setPaint(mMainTextPaint);
            mMainTextRenderer.setInAmbientMode(paintSet.mIsAmbientStyle);
        }
        mMainTextRenderer.draw(canvas, mMainTextBounds);
    }

    private void drawSubText(Canvas canvas, PaintSet paintSet) {
        if (mSubTextBounds.isEmpty()) {
            return;
        }
        if (DEBUG_MODE) {
            canvas.drawRect(mSubTextBounds, mDebugPaint);
        }
        if (mSubTextPaint != paintSet.mSecondaryTextPaint) {
            mSubTextPaint = paintSet.mSecondaryTextPaint;
            mSubTextRenderer.setPaint(mSubTextPaint);
            mSubTextRenderer.setInAmbientMode(paintSet.mIsAmbientStyle);
        }
        mSubTextRenderer.draw(canvas, mSubTextBounds);
    }

    private void drawRangedValue(Canvas canvas, PaintSet paintSet) {
        if (mRangedValueBoundsF.isEmpty()) {
            return;
        }
        if (DEBUG_MODE) {
            canvas.drawRect(mRangedValueBoundsF, mDebugPaint);
        }
        float interval =
                (mComplicationData.getRangedMaxValue() - mComplicationData.getRangedMinValue());
        float progress = interval > 0 ? mComplicationData.getRangedValue() / interval : 0;

        float total = 360 - 2 * STROKE_GAP_IN_DEGREES;
        float inProgressAngle = total * progress;
        float remainderAngle = total - inProgressAngle;

        int insetAmount = (int) Math.ceil(paintSet.mInProgressPaint.getStrokeWidth());
        mRangedValueBoundsF.inset(insetAmount, insetAmount);
        // Draw the progress arc.
        canvas.drawArc(
                mRangedValueBoundsF,
                RANGED_VALUE_START_ANGLE + STROKE_GAP_IN_DEGREES / 2,
                inProgressAngle,
                false,
                paintSet.mInProgressPaint);

        // Draw the remain arc.
        canvas.drawArc(
                mRangedValueBoundsF,
                RANGED_VALUE_START_ANGLE
                        + STROKE_GAP_IN_DEGREES / 2.0f
                        + inProgressAngle
                        + STROKE_GAP_IN_DEGREES,
                remainderAngle,
                false,
                paintSet.mRemainingPaint);
        mRangedValueBoundsF.inset(-insetAmount, -insetAmount);
    }

    private void drawIcon(Canvas canvas, PaintSet paintSet) {
        if (mIconBounds.isEmpty()) {
            return;
        }
        if (DEBUG_MODE) {
            canvas.drawRect(mIconBounds, mDebugPaint);
        }
        Drawable icon = mIcon;
        if (icon != null) {
            if (paintSet.isInBurnInProtectionMode() && mBurnInProtectionIcon != null) {
                icon = mBurnInProtectionIcon;
            }
            icon.setColorFilter(paintSet.mIconColorFilter);
            drawIconOnCanvas(canvas, mIconBounds, icon);
        }
    }

    private void drawSmallImage(Canvas canvas, PaintSet paintSet) {
        if (mSmallImageBounds.isEmpty()) {
            return;
        }
        if (DEBUG_MODE) {
            canvas.drawRect(mSmallImageBounds, mDebugPaint);
        }
        if (mRoundedSmallImage == null) {
            mRoundedSmallImage = new RoundedDrawable();
        }
        if (!paintSet.isInBurnInProtectionMode()) {
            mRoundedSmallImage.setDrawable(mSmallImage);
            if (mSmallImage == null) {
                return;
            }
        } else {
            mRoundedSmallImage.setDrawable(mBurnInProtectionSmallImage);
            if (mBurnInProtectionSmallImage == null) {
                return;
            }
        }
        if (mComplicationData.getSmallImageStyle() == ComplicationData.IMAGE_STYLE_ICON) {
            // Don't apply radius or color filter on icon style images
            mRoundedSmallImage.setColorFilter(null);
            mRoundedSmallImage.setRadius(0);
        } else {
            mRoundedSmallImage.setColorFilter(paintSet.mStyle.getImageColorFilter());
            mRoundedSmallImage.setRadius(getImageBorderRadius(paintSet.mStyle, mSmallImageBounds));
        }
        mRoundedSmallImage.setBounds(mSmallImageBounds);
        mRoundedSmallImage.draw(canvas);
    }

    private void drawLargeImage(Canvas canvas, PaintSet paintSet) {
        if (mLargeImageBounds.isEmpty()) {
            return;
        }
        if (DEBUG_MODE) {
            canvas.drawRect(mLargeImageBounds, mDebugPaint);
        }
        // Draw the image if not in burn in protection mode (in active mode or burn in not enabled)
        if (!paintSet.isInBurnInProtectionMode()) {
            if (mRoundedLargeImage == null) {
                mRoundedLargeImage = new RoundedDrawable();
            }
            mRoundedLargeImage.setDrawable(mLargeImage);
            // Large image is always treated as photo style
            mRoundedLargeImage.setRadius(getImageBorderRadius(paintSet.mStyle, mLargeImageBounds));
            mRoundedLargeImage.setBounds(mLargeImageBounds);
            mRoundedLargeImage.setColorFilter(paintSet.mStyle.getImageColorFilter());
            mRoundedLargeImage.draw(canvas);
        }
    }

    private static void drawIconOnCanvas(Canvas canvas, Rect bounds, Drawable icon) {
        icon.setBounds(0, 0, bounds.width(), bounds.height());
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        icon.draw(canvas);
        canvas.restore();
    }

    private int getBorderRadius(ComplicationStyle currentStyle) {
        if (mBounds.isEmpty()) {
            return 0;
        } else {
            return Math.min(
                    Math.min(mBounds.height(), mBounds.width()) / 2,
                    currentStyle.getBorderRadius());
        }
    }

    @VisibleForTesting
    int getImageBorderRadius(ComplicationStyle currentStyle, Rect imageBounds) {
        if (mBounds.isEmpty()) {
            return 0;
        } else {
            return Math.max(
                    getBorderRadius(currentStyle)
                            - Math.min(
                            Math.min(imageBounds.left, mBounds.width() - imageBounds.right),
                            Math.min(
                                    imageBounds.top,
                                    mBounds.height() - imageBounds.bottom)),
                    0);
        }
    }

    private void calculateBounds() {
        if (mComplicationData == null || mBounds.isEmpty()) {
            return;
        }
        mBackgroundBounds.set(0, 0, mBounds.width(), mBounds.height());
        mBackgroundBoundsF.set(0, 0, mBounds.width(), mBounds.height());
        LayoutHelper currentLayoutHelper;
        switch (mComplicationData.getType()) {
            case ComplicationData.TYPE_ICON:
                currentLayoutHelper = new IconLayoutHelper();
                break;
            case ComplicationData.TYPE_SMALL_IMAGE:
                currentLayoutHelper = new SmallImageLayoutHelper();
                break;
            case ComplicationData.TYPE_LARGE_IMAGE:
                currentLayoutHelper = new LargeImageLayoutHelper();
                break;
            case ComplicationData.TYPE_SHORT_TEXT:
            case ComplicationData.TYPE_NO_PERMISSION:
                currentLayoutHelper = new ShortTextLayoutHelper();
                break;
            case ComplicationData.TYPE_LONG_TEXT:
                currentLayoutHelper = new LongTextLayoutHelper();
                break;
            case ComplicationData.TYPE_RANGED_VALUE:
                if (mRangedValueProgressHidden) {
                    if (mComplicationData.getShortText() == null) {
                        currentLayoutHelper = new IconLayoutHelper();
                    } else {
                        currentLayoutHelper = new ShortTextLayoutHelper();
                    }
                } else {
                    currentLayoutHelper = new RangedValueLayoutHelper();
                }
                break;
            case ComplicationData.TYPE_EMPTY:
            case ComplicationData.TYPE_NOT_CONFIGURED:
            case ComplicationData.TYPE_NO_DATA:
            default:
                currentLayoutHelper = new LayoutHelper();
                break;
        }
        currentLayoutHelper.update(mBounds.width(), mBounds.height(), mComplicationData);
        currentLayoutHelper.getRangedValueBounds(mRangedValueBounds);
        mRangedValueBoundsF.set(mRangedValueBounds);
        currentLayoutHelper.getIconBounds(mIconBounds);
        currentLayoutHelper.getSmallImageBounds(mSmallImageBounds);
        currentLayoutHelper.getLargeImageBounds(mLargeImageBounds);
        Layout.Alignment alignment;
        if (mComplicationData.getType() == ComplicationData.TYPE_LONG_TEXT) {
            alignment = currentLayoutHelper.getLongTextAlignment();
            currentLayoutHelper.getLongTextBounds(mMainTextBounds);
            mMainTextRenderer.setAlignment(alignment);
            mMainTextRenderer.setGravity(currentLayoutHelper.getLongTextGravity());
            currentLayoutHelper.getLongTitleBounds(mSubTextBounds);
            mSubTextRenderer.setAlignment(currentLayoutHelper.getLongTitleAlignment());
            mSubTextRenderer.setGravity(currentLayoutHelper.getLongTitleGravity());
        } else {
            alignment = currentLayoutHelper.getShortTextAlignment();
            currentLayoutHelper.getShortTextBounds(mMainTextBounds);
            mMainTextRenderer.setAlignment(alignment);
            mMainTextRenderer.setGravity(currentLayoutHelper.getShortTextGravity());
            currentLayoutHelper.getShortTitleBounds(mSubTextBounds);
            mSubTextRenderer.setAlignment(currentLayoutHelper.getShortTitleAlignment());
            mSubTextRenderer.setGravity(currentLayoutHelper.getShortTitleGravity());
        }
        if (alignment != Layout.Alignment.ALIGN_CENTER) {
            float paddingAmount = TEXT_PADDING_HEIGHT_FRACTION * mBounds.height();
            mMainTextRenderer.setRelativePadding(paddingAmount / mMainTextBounds.width(), 0, 0, 0);
            mSubTextRenderer.setRelativePadding(paddingAmount / mMainTextBounds.width(), 0, 0, 0);
        } else {
            mMainTextRenderer.setRelativePadding(0, 0, 0, 0);
            mSubTextRenderer.setRelativePadding(0, 0, 0, 0);
        }
        Rect innerBounds = new Rect();
        LayoutUtils.getInnerBounds(
                innerBounds,
                mBackgroundBounds,
                Math.max(getBorderRadius(mActiveStyle), getBorderRadius(mAmbientStyle)));
        // Intersect text bounds with inner bounds to avoid overflow
        if (!mMainTextBounds.intersect(innerBounds)) {
            mMainTextBounds.setEmpty();
        }
        if (!mSubTextBounds.intersect(innerBounds)) {
            mSubTextBounds.setEmpty();
        }
        // Intersect icon bounds with inner bounds and try to keep its center the same
        if (!mIconBounds.isEmpty()) {
            // Apply padding to icons
            LayoutUtils.scaledAroundCenter(mIconBounds, mIconBounds, ICON_SIZE_FRACTION);
            LayoutUtils.fitSquareToBounds(mIconBounds, innerBounds);
        }
        // Intersect small image with inner bounds and make it a square if image style is icon
        if (!mSmallImageBounds.isEmpty()) {
            // Apply padding to small images
            LayoutUtils.scaledAroundCenter(
                    mSmallImageBounds, mSmallImageBounds, SMALL_IMAGE_SIZE_FRACTION);
            if (mComplicationData.getSmallImageStyle() == ComplicationData.IMAGE_STYLE_ICON) {
                LayoutUtils.fitSquareToBounds(mSmallImageBounds, innerBounds);
            }
        }
        // Apply padding to large images
        if (!mLargeImageBounds.isEmpty()) {
            LayoutUtils.scaledAroundCenter(
                    mLargeImageBounds, mLargeImageBounds, LARGE_IMAGE_SIZE_FRACTION);
        }
    }

    /**
     * Returns true if the data contains images. If there are, the images will be loaded
     * asynchronously and the drawable will be invalidated when loading is complete.
     */
    private boolean loadDrawableIconAndImagesAsync() {
        Handler handler = new Handler(Looper.getMainLooper());
        Icon icon = null;
        Icon smallImage = null;
        Icon burnInProtectionSmallImage = null;
        Icon largeImage = null;
        Icon burnInProtectionIcon = null;
        mIcon = null;
        mSmallImage = null;
        mBurnInProtectionSmallImage = null;
        mLargeImage = null;
        mBurnInProtectionIcon = null;
        if (mComplicationData != null) {
            icon = mComplicationData.hasIcon() ? mComplicationData.getIcon() : null;
            burnInProtectionIcon = mComplicationData.hasBurnInProtectionIcon()
                    ? mComplicationData.getBurnInProtectionIcon() : null;
            burnInProtectionSmallImage =
                    mComplicationData.hasBurnInProtectionSmallImage()
                            ? mComplicationData.getBurnInProtectionSmallImage() : null;
            smallImage =
                    mComplicationData.hasSmallImage() ? mComplicationData.getSmallImage() : null;
            largeImage =
                    mComplicationData.hasLargeImage() ? mComplicationData.getLargeImage() : null;
        }

        boolean hasImage = false;
        if (icon != null) {
            hasImage = true;
            icon.loadDrawableAsync(
                    mContext,
                    new OnDrawableLoadedListener() {
                        @Override
                        @SuppressLint("SyntheticAccessor")
                        public void onDrawableLoaded(Drawable d) {
                            if (d == null) {
                                return;
                            }
                            mIcon = d;
                            mIcon.mutate();
                            invalidate();
                        }
                    },
                    handler);
        }

        if (burnInProtectionIcon != null) {
            hasImage = true;
            burnInProtectionIcon.loadDrawableAsync(
                    mContext,
                    new OnDrawableLoadedListener() {
                        @Override
                        @SuppressLint("SyntheticAccessor")
                        public void onDrawableLoaded(Drawable d) {
                            if (d == null) {
                                return;
                            }
                            mBurnInProtectionIcon = d;
                            mBurnInProtectionIcon.mutate();
                            invalidate();
                        }
                    },
                    handler);
        }

        if (smallImage != null) {
            hasImage = true;
            smallImage.loadDrawableAsync(
                    mContext,
                    new OnDrawableLoadedListener() {
                        @Override
                        @SuppressLint("SyntheticAccessor")
                        public void onDrawableLoaded(Drawable d) {
                            if (d == null) {
                                return;
                            }
                            mSmallImage = d;
                            invalidate();
                        }
                    },
                    handler);
        }

        if (burnInProtectionSmallImage != null) {
            hasImage = true;
            burnInProtectionSmallImage.loadDrawableAsync(
                    mContext,
                    new OnDrawableLoadedListener() {
                        @Override
                        @SuppressLint("SyntheticAccessor")
                        public void onDrawableLoaded(Drawable d) {
                            if (d == null) {
                                return;
                            }
                            mBurnInProtectionSmallImage = d;
                            invalidate();
                        }
                    },
                    handler);
        }

        if (largeImage != null) {
            hasImage = true;
            largeImage.loadDrawableAsync(
                    mContext,
                    new OnDrawableLoadedListener() {
                        @Override
                        @SuppressLint("SyntheticAccessor")
                        public void onDrawableLoaded(Drawable d) {
                            if (d == null) {
                                return;
                            }
                            mLargeImage = d;
                            invalidate();
                        }
                    },
                    handler);
        }
        return hasImage;
    }

    /** Synchronously loads any images. */
    private void loadDrawableIconAndImages() {
        Icon icon = null;
        Icon smallImage = null;
        Icon burnInProtectionSmallImage = null;
        Icon largeImage = null;
        Icon burnInProtectionIcon = null;
        mIcon = null;
        mSmallImage = null;
        mBurnInProtectionSmallImage = null;
        mLargeImage = null;
        mBurnInProtectionIcon = null;
        if (mComplicationData != null) {
            icon = mComplicationData.hasIcon() ? mComplicationData.getIcon() : null;
            burnInProtectionIcon = mComplicationData.hasBurnInProtectionIcon()
                    ? mComplicationData.getBurnInProtectionIcon() : null;
            burnInProtectionSmallImage =
                    mComplicationData.hasBurnInProtectionSmallImage()
                            ? mComplicationData.getBurnInProtectionSmallImage() : null;
            smallImage =
                    mComplicationData.hasSmallImage() ? mComplicationData.getSmallImage() : null;
            largeImage =
                    mComplicationData.hasLargeImage() ? mComplicationData.getLargeImage() : null;
        }

        if (icon != null) {
            mIcon = icon.loadDrawable(mContext);
        }

        if (burnInProtectionIcon != null) {
            mBurnInProtectionIcon = burnInProtectionIcon.loadDrawable(mContext);
        }

        if (smallImage != null) {
            mSmallImage = smallImage.loadDrawable(mContext);
        }

        if (burnInProtectionSmallImage != null) {
            mBurnInProtectionSmallImage = burnInProtectionSmallImage.loadDrawable(mContext);
        }

        if (largeImage != null) {
            mLargeImage = largeImage.loadDrawable(mContext);
        }
    }

    @VisibleForTesting
    static class PaintSet {

        private static final int SINGLE_COLOR_FILTER_ALPHA_CUTOFF = 127;

        /** TextPaint for drawing primary text */
        final TextPaint mPrimaryTextPaint;

        /** TextPaint for drawing secondary text */
        final TextPaint mSecondaryTextPaint;

        /** Paint for drawing first part of ranged value. */
        final Paint mInProgressPaint;

        /** Paint for drawing second part of ranged value. */
        final Paint mRemainingPaint;

        /** Paint for drawing borders. */
        final Paint mBorderPaint;

        /** Paint for drawing background. */
        final Paint mBackgroundPaint;

        /** Paint for drawing highlight. */
        final Paint mHighlightPaint;

        /** Style used to construct this paint set. */
        final ComplicationStyle mStyle;

        /** True if this paint set is for an ambient style. */
        final boolean mIsAmbientStyle;

        /** True if this paint set is for low bit ambient mode. */
        final boolean mLowBitAmbient;

        /** True if this paint set is for burn in protection mode. */
        final boolean mBurnInProtection;

        /** Icon tint color filter */
        final ColorFilter mIconColorFilter;

        @SuppressLint("SyntheticAccessor")
        PaintSet(
                ComplicationStyle style,
                boolean isAmbientStyle,
                boolean lowBitAmbient,
                boolean burnInProtection) {
            this.mStyle = style;
            this.mIsAmbientStyle = isAmbientStyle;
            this.mLowBitAmbient = lowBitAmbient;
            this.mBurnInProtection = burnInProtection;

            boolean antiAlias = !(isAmbientStyle && lowBitAmbient);
            if (lowBitAmbient) {
                style = lowBitAmbientStyleFrom(style);
            }

            mPrimaryTextPaint = new TextPaint();
            mPrimaryTextPaint.setColor(style.getTextColor());
            mPrimaryTextPaint.setAntiAlias(antiAlias);
            mPrimaryTextPaint.setTypeface(style.getTextTypeface());
            mPrimaryTextPaint.setTextSize(style.getTextSize());
            mPrimaryTextPaint.setAntiAlias(antiAlias);

            mIconColorFilter =
                    antiAlias
                            ? new PorterDuffColorFilter(
                            style.getIconColor(), PorterDuff.Mode.SRC_IN)
                            : new ColorMatrixColorFilter(
                                    createSingleColorMatrix(style.getIconColor()));

            mSecondaryTextPaint = new TextPaint();
            mSecondaryTextPaint.setColor(style.getTitleColor());
            mSecondaryTextPaint.setAntiAlias(antiAlias);
            mSecondaryTextPaint.setTypeface(style.getTitleTypeface());
            mSecondaryTextPaint.setTextSize(style.getTitleSize());
            mSecondaryTextPaint.setAntiAlias(antiAlias);

            mInProgressPaint = new Paint();
            mInProgressPaint.setColor(style.getRangedValuePrimaryColor());
            mInProgressPaint.setStyle(Paint.Style.STROKE);
            mInProgressPaint.setAntiAlias(antiAlias);
            mInProgressPaint.setStrokeWidth(style.getRangedValueRingWidth());

            mRemainingPaint = new Paint();
            mRemainingPaint.setColor(style.getRangedValueSecondaryColor());
            mRemainingPaint.setStyle(Paint.Style.STROKE);
            mRemainingPaint.setAntiAlias(antiAlias);
            mRemainingPaint.setStrokeWidth(style.getRangedValueRingWidth());

            mBorderPaint = new Paint();
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setColor(style.getBorderColor());
            if (style.getBorderStyle() == ComplicationStyle.BORDER_STYLE_DASHED) {
                mBorderPaint.setPathEffect(
                        new DashPathEffect(
                                new float[]{style.getBorderDashWidth(), style.getBorderDashGap()},
                                0));
            }
            if (style.getBorderStyle() == ComplicationStyle.BORDER_STYLE_NONE) {
                mBorderPaint.setAlpha(0);
            }
            mBorderPaint.setStrokeWidth(style.getBorderWidth());
            mBorderPaint.setAntiAlias(antiAlias);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(style.getBackgroundColor());
            mBackgroundPaint.setAntiAlias(antiAlias);

            mHighlightPaint = new Paint();
            mHighlightPaint.setColor(style.getHighlightColor());
            mHighlightPaint.setAntiAlias(antiAlias);
        }

        boolean isInBurnInProtectionMode() {
            return mIsAmbientStyle && mBurnInProtection;
        }

        /**
         * Returns a color matrix that maps every color to the specified {@code color}, with an
         * alpha value equal to zero if the input alpha is less than or equal to {@link
         * #SINGLE_COLOR_FILTER_ALPHA_CUTOFF} or alpha of 1 otherwise.
         */
        @VisibleForTesting
        static ColorMatrix createSingleColorMatrix(int color) {
            return new ColorMatrix(
                    new float[]{
                            0, 0, 0, 0, Color.red(color),
                            0, 0, 0, 0, Color.green(color),
                            0, 0, 0, 0, Color.blue(color),
                            0, 0, 0, 255, SINGLE_COLOR_FILTER_ALPHA_CUTOFF * -255
                    });
        }
    }

    @NonNull
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Rect getBounds() {
        return mBounds;
    }

    @NonNull
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Rect getIconBounds() {
        return mIconBounds;
    }

    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Drawable getIcon() {
        return mIcon;
    }

    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Drawable getSmallImage() {
        return mSmallImage;
    }

    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Drawable getBurnInProtectionIcon() {
        return mBurnInProtectionIcon;
    }

    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Drawable getBurnInProtectionSmallImage() {
        return mBurnInProtectionSmallImage;
    }

    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public RoundedDrawable getRoundedSmallImage() {
        return mRoundedSmallImage;
    }

    @NonNull
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Rect getMainTextBounds() {
        return mMainTextBounds;
    }

    @NonNull
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Rect getSubTextBounds() {
        return mSubTextBounds;
    }

    /** @param outRect Object that receives the computation of the complication's inner bounds */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void getComplicationInnerBounds(@NonNull Rect outRect) {
        LayoutUtils.getInnerBounds(
                outRect,
                mBounds,
                Math.max(getBorderRadius(mActiveStyle), getBorderRadius(mAmbientStyle)));
    }

    /**
     * @param drawable The {@link ComplicationRenderer} to check against this one
     * @return True if this {@link ComplicationRenderer} has the same layout as the provided one
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public boolean hasSameLayout(@NonNull ComplicationRenderer drawable) {
        return mBounds.equals(drawable.mBounds)
                && mBackgroundBounds.equals(drawable.mBackgroundBounds)
                && mIconBounds.equals(drawable.mIconBounds)
                && mLargeImageBounds.equals(drawable.mLargeImageBounds)
                && mSmallImageBounds.equals(drawable.mSmallImageBounds)
                && mMainTextBounds.equals(drawable.mMainTextBounds)
                && mSubTextBounds.equals(drawable.mSubTextBounds)
                && mRangedValueBounds.equals(drawable.mRangedValueBounds);
    }

    ComplicationData getComplicationData() {
        return mComplicationData;
    }

    /**
     * Returns a {@link ComplicationStyle} based on the provided {@code style} but with colors
     * restricted to black, white or transparent. All text and icon colors in the returned style
     * will be set to white.
     */
    @NonNull
    private static ComplicationStyle lowBitAmbientStyleFrom(@NonNull ComplicationStyle style) {
        ComplicationStyle newStyle = new ComplicationStyle(style);
        if (style.getBackgroundColor() != Color.BLACK) {
            newStyle.setBackgroundColor(Color.TRANSPARENT);
        }
        newStyle.setTextColor(Color.WHITE);
        newStyle.setTitleColor(Color.WHITE);
        newStyle.setIconColor(Color.WHITE);
        if (style.getBorderColor() != Color.BLACK && style.getBorderColor() != Color.TRANSPARENT) {
            newStyle.setBorderColor(Color.WHITE);
        }
        newStyle.setRangedValuePrimaryColor(Color.WHITE);
        if (style.getRangedValueSecondaryColor() != Color.BLACK) {
            newStyle.setRangedValueSecondaryColor(Color.TRANSPARENT);
        }
        return newStyle;
    }
}

/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.inflater.ArcWidgetHelper.getSignForClockwise;
import static androidx.wear.protolayout.renderer.inflater.ArcWidgetHelper.isPointInsideArcArea;
import static androidx.wear.protolayout.renderer.inflater.SweepGradientHelper.makeOpaque;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection;
import androidx.wear.protolayout.renderer.R;
import androidx.wear.widget.ArcLayout;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A line, drawn inside an arc.
 *
 * <p>This widget takes four parameters, the thickness of the line to draw, optionally the sweep
 * angle of the "container", the sweep angle of the line, and the color to draw with. This widget
 * will then draw an arc, with the specified thickness, around its parent arc. All sweep angles are
 * specified in degrees, clockwise.
 *
 * <p>The "container" length is used when calculating how much of the parent arc to occupy, such
 * that the line length can grow/shrink within that container length without affecting the elements
 * around it. If the line length is greater than the container length, then the line will be
 * truncated to fit inside the container.
 */
public class WearCurvedLineView extends View implements ArcLayout.Widget {
    public static final float SWEEP_ANGLE_WRAP_LENGTH = -1;

    private static final String TAG = "WearCurvedLineView";
    private static final int DEFAULT_THICKNESS_PX = 0;
    private static final float DEFAULT_MAX_SWEEP_ANGLE_DEGREES = SWEEP_ANGLE_WRAP_LENGTH;
    private static final float DEFAULT_LINE_SWEEP_ANGLE_DEGREES = 0;

    @SuppressWarnings("EnumOrdinal")
    private static final int DEFAULT_LINE_STROKE_CAP = Cap.ROUND.ordinal();

    @ColorInt private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private ArcDirection mLineDirection = ArcDirection.ARC_DIRECTION_CLOCKWISE;

    /**
     * The base angle for drawings. The zero angle in Android corresponds to the "3 o clock"
     * position, while ProtoLayout and ArcLayout use the "12 o clock" position as zero.
     */
    private static final float BASE_DRAW_ANGLE_SHIFT = -90f;

    private float mMaxSweepAngleDegrees;
    private float mLineSweepAngleDegrees;

    @Nullable SweepGradientHelper mSweepGradientHelper;

    private @Nullable ArcDrawable mArcDrawable;
    private @Nullable StrokeCapShadow mCapShadow;

    /** Base paint used for drawing. This paint doesn't include any gradient definition. */
    private final @NonNull Paint mBasePaint;

    private boolean updatesEnabled = true;

    public WearCurvedLineView(@NonNull Context context) {
        this(context, null);
    }

    public WearCurvedLineView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearCurvedLineView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearCurvedLineView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, R.styleable.WearCurvedLineView, defStyleAttr, defStyleRes);

        int thicknessPx =
                (int)
                        a.getDimension(
                                R.styleable.WearCurvedLineView_thickness, DEFAULT_THICKNESS_PX);
        @ColorInt int color = a.getColor(R.styleable.WearCurvedLineView_color, DEFAULT_COLOR);
        mMaxSweepAngleDegrees =
                a.getFloat(
                        R.styleable.WearCurvedLineView_maxSweepAngleDegrees,
                        DEFAULT_MAX_SWEEP_ANGLE_DEGREES);
        mLineSweepAngleDegrees =
                a.getFloat(
                        R.styleable.WearCurvedLineView_sweepAngleDegrees,
                        DEFAULT_LINE_SWEEP_ANGLE_DEGREES);
        Cap capStyle =
                Cap.values()[
                        a.getInt(
                                R.styleable.WearCurvedLineView_strokeCap, DEFAULT_LINE_STROKE_CAP)];
        a.recycle();

        mBasePaint = new Paint();
        mBasePaint.setStyle(Style.STROKE);
        mBasePaint.setStrokeCap(capStyle);
        mBasePaint.setColor(color);
        mBasePaint.setStrokeWidth(thicknessPx);
        mBasePaint.setAntiAlias(true);
    }

    /**
     * Sets whether updates are enabled for this view. That impacts the contents of the drawing of
     * this view.
     */
    void setUpdatesEnabled(boolean enabled) {
        boolean shouldTriggerUpdate = enabled && !updatesEnabled;
        updatesEnabled = enabled;
        if (shouldTriggerUpdate) {
            updateArcDrawable();
        }
    }

    private void updateArcDrawable() {
        if (!updatesEnabled) {
            return;
        }

        float insetPx = mBasePaint.getStrokeWidth() / 2f;
        RectF bounds =
                new RectF(
                        insetPx,
                        insetPx,
                        getMeasuredWidth() - insetPx,
                        getMeasuredHeight() - insetPx);
        float clampedSweepAngle = resolveSweepAngleDegrees();

        if (mSweepGradientHelper != null || mCapShadow != null) {
            mArcDrawable =
                    new ArcDrawableImpl(
                            bounds,
                            clampedSweepAngle,
                            mBasePaint.getStrokeWidth(),
                            getSignForClockwise(this, mLineDirection, /* defaultValue= */ 1),
                            mBasePaint,
                            mSweepGradientHelper,
                            mCapShadow);
        } else {
            mArcDrawable = new ArcDrawableLegacy(bounds, clampedSweepAngle, mBasePaint);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updateArcDrawable();
    }

    /** Sets the thickness of this arc in pixels. */
    public void setThickness(int thickness) {
        if (thickness < 0) {
            thickness = 0;
        }
        mBasePaint.setStrokeWidth(thickness);
        triggerRefresh();
    }

    /** Sets the direction which the line is drawn. */
    public void setLineDirection(@NonNull ArcDirection direction) {
        mLineDirection = direction;
    }

    private float resolveSweepAngleDegrees() {
        return mMaxSweepAngleDegrees == SWEEP_ANGLE_WRAP_LENGTH
                ? mLineSweepAngleDegrees
                : min(mLineSweepAngleDegrees, mMaxSweepAngleDegrees);
    }

    @Override
    public float getSweepAngleDegrees() {
        return resolveSweepAngleDegrees();
    }

    @Override
    public void setSweepAngleDegrees(float sweepAngleDegrees) {
        this.mLineSweepAngleDegrees = sweepAngleDegrees;
    }

    /** Gets the sweep angle of the actual line contained within this CurvedLineView. */
    public float getLineSweepAngleDegrees() {
        return mLineSweepAngleDegrees;
    }

    @Override
    public int getThickness() {
        return (int) mBasePaint.getStrokeWidth();
    }

    /**
     * Sets the maximum sweep angle of the line, in degrees. If a max size is not required, pass
     * {@link WearCurvedLineView#SWEEP_ANGLE_WRAP_LENGTH} instead.
     */
    public void setMaxSweepAngleDegrees(float maxSweepAngleDegrees) {
        this.mMaxSweepAngleDegrees = maxSweepAngleDegrees;
        triggerRefresh();
    }

    /**
     * Gets the maximum sweep angle of the line, in degrees. If a max size is not set, this will
     * return {@link WearCurvedLineView#SWEEP_ANGLE_WRAP_LENGTH}.
     */
    public float getMaxSweepAngleDegrees() {
        return mMaxSweepAngleDegrees;
    }

    /**
     * Sets the length of the line contained within this CurvedLineView. If this is greater than the
     * max sweep angle set using {@link WearCurvedLineView#setMaxSweepAngleDegrees(float)}, then the
     * sweep angle will be clamped to that value.
     */
    public void setLineSweepAngleDegrees(float lineLengthDegrees) {
        this.mLineSweepAngleDegrees = lineLengthDegrees;
        triggerRefresh();
    }

    /** Returns the color of this arc, in ARGB format. */
    @ColorInt
    public int getColor() {
        return mBasePaint.getColor();
    }

    /** Sets the color of this arc, in ARGB format. */
    public void setColor(@ColorInt int color) {
        // Force color to be fully opaque if stroke cap shadow is used.
        if (mCapShadow != null) {
            color = makeOpaque(color);
        }
        mBasePaint.setColor(color);
        triggerRefresh();
    }

    /** Sets a sweep gradient to be used to draw this arc. */
    public void setSweepGradient(@NonNull SweepGradientHelper sweepGradientHelper) {
        this.mSweepGradientHelper = sweepGradientHelper;
        triggerRefresh();
    }

    /** Returns the strokeCap of this arc. */
    public @NonNull Cap getStrokeCap() {
        return mBasePaint.getStrokeCap();
    }

    /** Sets the strokeCap of this arc. */
    public void setStrokeCap(@NonNull Cap cap) {
        mBasePaint.setStrokeCap(cap);
    }

    /** Sets the parameters for the stroke cap shadow. */
    public void setStrokeCapShadow(float blurRadius, int color) {
        this.mCapShadow = new StrokeCapShadow(blurRadius, color);
        // Re-set color.
        this.setColor(mBasePaint.getColor());
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mArcDrawable == null) {
            return;
        }
        mArcDrawable.onDraw(canvas);
    }

    @Override
    public void checkInvalidAttributeAsChild() {
        // Nothing required...
    }

    @Override
    public boolean isPointInsideClickArea(float x, float y) {
        return isPointInsideArcArea(
                this, x, y, mBasePaint.getStrokeWidth(), resolveSweepAngleDegrees());
    }

    /** Invalidates the view's current drawn state and triggers a redraw. */
    public void triggerRefresh() {
        updateArcDrawable();
        requestLayout();
        postInvalidate();
    }

    /**
     * A segment of a line, used as a building block for complex lines.
     *
     * <p>Each segment can be ArcLine or straight line.
     */
    interface Segment {
        void onDraw(@NonNull Canvas canvas);
    }

    /**
     * A segment of an arc line that represents a straight line, perpendicular to the arc. It can be
     * used to ensure a region in the arc has a desired color.
     */
    static class LineSegment implements Segment {

        private final @NonNull Path mPath;
        private final @NonNull Paint mPaint;

        LineSegment(
                @NonNull RectF bounds, float drawAngle, float thicknessPx, @NonNull Paint paint) {
            float innerRadius = (min(bounds.width(), bounds.height()) - thicknessPx) / 2f;
            double drawMidAngleRad = Math.toRadians(drawAngle);
            float centerX = (bounds.left + bounds.right) / 2f;
            float centerY = (bounds.top + bounds.bottom) / 2f;
            PointF midAngleVector =
                    new PointF(
                            (float) Math.cos(drawMidAngleRad), (float) Math.sin(drawMidAngleRad));

            Path line = new Path();
            line.moveTo(centerX, centerY);
            // Line start
            line.rMoveTo(innerRadius * midAngleVector.x, innerRadius * midAngleVector.y);
            // Line end
            line.rLineTo(thicknessPx * midAngleVector.x, thicknessPx * midAngleVector.y);

            this.mPath = line;
            this.mPaint = paint;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    /**
     * A segment of a line, used as a building block for complex lines. Each segment has its own
     * Paint for drawing.
     */
    static class ArcSegment implements Segment {
        /** The angle span of the sector that is clipped out. */
        private static final float CLIP_OUT_PATH_SPAN_DEGREES = 90f;

        private final @NonNull Paint mPaint;
        private final @NonNull List<ArcLinePath> mPathList;

        /** A region to be clipped out when drawing, in order to exclude one of the stroke caps. */
        private @Nullable Path mExcludedCapRegion = null;

        /** A region to be clipped in when drawing, in order to only include this region. */
        private @Nullable Path mMaskRegion = null;

        /** A segment that draws the shadow layer that matches the path of the given segment. */
        static ArcSegment strokeCapShadowLayer(
                @NonNull RectF bounds,
                float lineThicknessPx,
                @NonNull ArcSegment segment,
                @NonNull Paint paint) {
            // Use a mask to only include the region between inner and outer bounds of the arc line.
            // The Paint's shadow layer will draw the shadow in all directions around the stroke but
            // we only want the part in the direction of the Cap to be visible.
            Path maskRegion = new Path();
            RectF innerBounds = shrinkRectF(bounds, lineThicknessPx / 2f);
            RectF outerBounds = expandRectF(bounds, lineThicknessPx / 2f);
            maskRegion.addOval(innerBounds, Direction.CW);
            maskRegion.addOval(outerBounds, Direction.CCW);
            return new ArcSegment(segment.mPathList, paint, maskRegion, segment.mExcludedCapRegion);
        }

        ArcSegment(
                @NonNull RectF bounds,
                float startAngle,
                float sweepAngle,
                float thicknessPx,
                @NonNull CapPosition capPosition,
                @NonNull Paint paint) {
            if (Math.abs(sweepAngle) > 180f) {
                throw new IllegalArgumentException(
                        "ArcSegment's absolute sweepAngle must be less or equal than 180 degrees."
                                + " Got "
                                + sweepAngle);
            }

            mPaint = paint;
            if (capPosition == CapPosition.NONE) {
                mPaint.setStrokeCap(Cap.BUTT);
            }

            if (mPaint.getStrokeCap() != Cap.ROUND && Math.abs(sweepAngle) == 180f) {
                sweepAngle += Math.signum(sweepAngle) * 0.001f;
            }

            mPathList = new ArrayList<>();
            mPathList.add(new ArcLinePath(bounds, startAngle, sweepAngle));

            // If a single cap is present, we clip out the Cap that should not be included.
            if (capPosition != CapPosition.NONE) {

                RectF clipRectBounds = expandRectF(bounds, thicknessPx);

                mExcludedCapRegion = new Path();
                mExcludedCapRegion.moveTo(clipRectBounds.centerX(), clipRectBounds.centerY());
                float sweepDirection = Math.signum(sweepAngle);

                if (capPosition == CapPosition.START) {
                    // Clip out END of segment.
                    mExcludedCapRegion.arcTo(
                            clipRectBounds,
                            startAngle + sweepAngle,
                            sweepDirection * CLIP_OUT_PATH_SPAN_DEGREES);
                } else if (capPosition == CapPosition.END) {
                    // Clip out START of segment.
                    mExcludedCapRegion.arcTo(
                            clipRectBounds,
                            startAngle,
                            -sweepDirection * CLIP_OUT_PATH_SPAN_DEGREES);
                }
                mExcludedCapRegion.close();
            }
        }

        ArcSegment(
                @NonNull List<ArcLinePath> mainPath,
                @NonNull Paint paint,
                @Nullable Path maskRegion,
                @Nullable Path excludedCapRegion) {
            this.mPathList = mainPath;
            this.mPaint = paint;
            this.mMaskRegion = maskRegion;
            this.mExcludedCapRegion = excludedCapRegion;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            canvas.save();
            // TODO: b/395863595 - Handle aliasing here as these don't support it.
            if (mExcludedCapRegion != null) {
                canvas.clipOutPath(mExcludedCapRegion);
            }
            if (mMaskRegion != null) {
                canvas.clipPath(mMaskRegion);
            }
            mPathList.forEach(
                    arc ->
                            canvas.drawArc(
                                    arc.getOval(),
                                    arc.getStartAngle(),
                                    arc.getSweepAngle(),
                                    /* useCenter= */ false,
                                    mPaint));
            canvas.restore();
        }

        /**
         * Returns a new rectangle, expanding the given bounds by {@code offset} in all directions.
         * Use a negative offset value to shrink the original bounds.
         */
        static RectF expandRectF(@NonNull RectF bounds, float offset) {
            return new RectF(
                    bounds.left - offset,
                    bounds.top - offset,
                    bounds.right + offset,
                    bounds.bottom + offset);
        }

        static RectF shrinkRectF(@NonNull RectF bounds, float offset) {
            return expandRectF(bounds, -offset);
        }
    }

    /**
     * ArcDrawable that breaks down the arc line into two main segments for drawing.
     *
     * <p>The line wraps on top of itself when the length is over 360 degrees, creating multiple
     * layers. At any time, we only draw the top most layer (last 360 of the line's length)
     *
     * <p>The order or drawing follows the order they appear visually on screen (Head segment on top
     * the tail segment.
     *
     * <p>All other lower layers of the line are not visible so they are not drawn.
     */
    static class ArcDrawableImpl implements ArcDrawable {
        // The list of segments that compose the ArcDrawable, in the order that they should be
        // drawn.
        private final @NonNull List<Segment> mSegments = new ArrayList<>();

        ArcDrawableImpl(
                @NonNull RectF bounds,
                float sweepAngle,
                float thicknessPx,
                int arcDirectionSign,
                @NonNull Paint basePaint,
                @Nullable SweepGradientHelper sweepGradHelper,
                @Nullable StrokeCapShadow strokeCapShadow) {
            if (sweepAngle == 0f) {
                return;
            }

            float drawStartAngle = BASE_DRAW_ANGLE_SHIFT - arcDirectionSign * sweepAngle / 2f;
            sweepAngle *= arcDirectionSign;

            float sweepDirection = Math.signum(sweepAngle);
            float absSweepAngle = Math.abs(sweepAngle);

            CapPosition tailCapPosition = CapPosition.START;
            // The start of the top layer, relative to the Arc Line's full length.
            float topLayerStartCursor = 0f;
            float topLayerLength = sweepAngle;

            // If absolute length >= 360, we only draw the last 360 degrees of the line's length.
            if (absSweepAngle >= 360f) {
                // When drawing the last 360 degrees of the line, the start and end of the drawing
                // are at the same angle.
                drawStartAngle = (drawStartAngle + sweepAngle) % 360f;
                tailCapPosition = CapPosition.NONE;
                topLayerStartCursor = sweepAngle - sweepDirection * 360f;
                topLayerLength = sweepDirection * 360f;
            }

            float segmentSweep = topLayerLength / 2f;

            @Nullable Paint shadowPaint = null;
            if (strokeCapShadow != null) {
                shadowPaint = new Paint(basePaint);
                shadowPaint.setColor(Color.TRANSPARENT);
                shadowPaint.setShadowLayer(
                        strokeCapShadow.mBlurRadius,
                        /* dx= */ 0f,
                        /* dy= */ 0f,
                        strokeCapShadow.mColor);
            }

            // Tail Segment.
            Paint tailPaint = new Paint(basePaint);
            if (sweepGradHelper != null) {
                Shader shader =
                        sweepGradHelper.getShader(
                                bounds,
                                topLayerStartCursor,
                                topLayerStartCursor + segmentSweep,
                                drawStartAngle,
                                tailCapPosition);
                tailPaint.setShader(shader);
            }
            ArcSegment tailSegment =
                    new ArcSegment(
                            bounds,
                            drawStartAngle,
                            segmentSweep,
                            thicknessPx,
                            tailCapPosition,
                            tailPaint);

            // Add a shadow layer to the tail Cap if needed.
            if (tailCapPosition != CapPosition.NONE && shadowPaint != null) {
                mSegments.add(
                        ArcSegment.strokeCapShadowLayer(
                                bounds, thicknessPx, tailSegment, shadowPaint));
            }
            mSegments.add(tailSegment);

            // Head Segment.
            float midCursor = topLayerStartCursor + segmentSweep;
            float drawMidAngle = drawStartAngle + segmentSweep;
            Paint headPaint = new Paint(basePaint);
            if (sweepGradHelper != null) {
                Shader shader =
                        sweepGradHelper.getShader(
                                bounds,
                                midCursor,
                                midCursor + segmentSweep,
                                drawMidAngle,
                                CapPosition.END);
                headPaint.setShader(shader);
            }
            ArcSegment headSegment =
                    new ArcSegment(
                            bounds,
                            drawMidAngle,
                            segmentSweep,
                            thicknessPx,
                            CapPosition.END,
                            headPaint);

            // Add a shadow layer to the head Cap if needed.
            if (shadowPaint != null) {
                mSegments.add(
                        ArcSegment.strokeCapShadowLayer(
                                bounds, thicknessPx, headSegment, shadowPaint));
            }
            mSegments.add(headSegment);

            // Fix discontinuity caused by anti-alias layer between Tail and Head. This is an arc
            // with length equivalent to 1px.
            Paint midPaint = new Paint(basePaint);
            midPaint.setAntiAlias(false);
            midPaint.setStrokeWidth(1f);
            midPaint.setStrokeCap(Cap.BUTT);
            if (sweepGradHelper != null) {
                midPaint.setColor(sweepGradHelper.getColor(Math.abs(midCursor)));
            }
            mSegments.add(new LineSegment(bounds, drawMidAngle, thicknessPx, midPaint));
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            mSegments.forEach(seg -> seg.onDraw(canvas));
        }
    }

    /**
     * Legacy LinePath, which supports drawing the line as a single arc.
     *
     * <p>Note that this needs to use {@code canvas.drawArc} instead of previous {@code
     * canvas.drawPath}, because {@code canvas.drawPath} implementation has a bug when the path is
     * an Arc which causes the aliasing issue (b/393971851)
     */
    static class ArcDrawableLegacy implements ArcDrawable {

        private final @NonNull Paint mPaint;
        private final RectF mBounds;
        private final float mStartAngle;
        private final float mClampedLineLength;

        ArcDrawableLegacy(@NonNull RectF bounds, float clampedLineLength, @NonNull Paint paint) {
            this.mPaint = paint;
            this.mBounds = bounds;
            this.mStartAngle = BASE_DRAW_ANGLE_SHIFT - (clampedLineLength / 2f);
            this.mClampedLineLength = clampedLineLength;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            if (mClampedLineLength == 0) {
                return;
            }
            // drawArc will draw oval if abs(clampedLineLength) is >= 360
            canvas.drawArc(mBounds, mStartAngle, mClampedLineLength, false, mPaint);
        }
    }

    /** Definition of an arc that can be drawn on the canvas. */
    private interface ArcDrawable {
        /** Called when the arc should be drawn on the canvas. */
        void onDraw(@NonNull Canvas canvas);
    }

    /** Data holder for the stroke cap shadow. */
    private static class StrokeCapShadow {
        final float mBlurRadius;
        final int mColor;

        StrokeCapShadow(float blurRadius, int color) {
            this.mBlurRadius = blurRadius;
            this.mColor = color;
        }
    }

    /** Position of the Cap for an ArcSegment. */
    public enum CapPosition {
        NONE,
        START,
        END
    }
}

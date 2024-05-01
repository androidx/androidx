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

import static androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.isRtlLayoutDirectionFromLocale;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.proto.ColorProto;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection;
import androidx.wear.protolayout.renderer.R;
import androidx.wear.protolayout.renderer.inflater.WearCurvedLineView.ArcSegment.CapPosition;
import androidx.wear.widget.ArcLayout;

import com.google.common.primitives.Floats;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final int DEFAULT_LINE_STROKE_CAP = Cap.ROUND.ordinal();
    @ColorInt private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final int FULLY_OPAQUE_COLOR_MASK = 0xFF000000;

    private ArcDirection mLineDirection = ArcDirection.ARC_DIRECTION_CLOCKWISE;

    /**
     * The base angle for drawings. The zero angle in Android corresponds to the "3 o clock"
     * position, while ProtoLayout and ArcLayout use the "12 o clock" position as zero.
     */
    private static final float BASE_DRAW_ANGLE_SHIFT = -90f;

    private float mMaxSweepAngleDegrees;
    private float mLineSweepAngleDegrees;

    @Nullable @VisibleForTesting SweepGradientHelper mSweepGradientHelper;

    @Nullable private ArcDrawable mArcDrawable;
    @Nullable private StrokeCapShadow mCapShadow;

    /** Base paint used for drawing. This paint doesn't include any gradient definition. */
    @NonNull private final Paint mBasePaint;

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
                            getSignForClockwise(mLineDirection, /* defaultValue= */ 1),
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

        updateArcDrawable();
        requestLayout();
        postInvalidate();
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
        updateArcDrawable();
        requestLayout();
        postInvalidate();
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

        updateArcDrawable();
        requestLayout();
        postInvalidate();
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

        updateArcDrawable();
        invalidate();
    }

    /** Sets a brush to be used to draw this arc. */
    public void setBrush(@NonNull ColorProto.Brush brushProto) {
        if (!brushProto.hasSweepGradient()) {
            Log.e(TAG, "Only SweepGradient is currently supported in ArcLine.");
            return;
        }
        try {
            this.mSweepGradientHelper = new SweepGradientHelper(brushProto.getSweepGradient());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid SweepGradient definition: " + e.getMessage());
        }
        updateArcDrawable();
    }

    /** Returns the strokeCap of this arc. */
    @NonNull
    public Cap getStrokeCap() {
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
        // Stolen from WearCurvedTextView...
        float radius2 = min(getWidth(), getHeight()) / 2f - getPaddingTop();
        float radius1 = radius2 - mBasePaint.getStrokeWidth();

        float dx = x - getWidth() / 2f;
        float dy = y - getHeight() / 2f;

        float r2 = dx * dx + dy * dy;
        if (r2 < radius1 * radius1 || r2 > radius2 * radius2) {
            return false;
        }

        // Since we are symmetrical on the Y-axis, we can constrain the angle to the x>=0 quadrants.
        float angle = (float) Math.toDegrees(Math.atan2(Math.abs(dx), -dy));
        return angle < resolveSweepAngleDegrees() / 2;
    }

    static int getSignForClockwise(@NonNull ArcDirection arcDirection, int defaultValue) {
        switch (arcDirection) {
            case ARC_DIRECTION_CLOCKWISE:
                return 1;
            case ARC_DIRECTION_COUNTER_CLOCKWISE:
                return -1;
            case ARC_DIRECTION_NORMAL:
                return isRtlLayoutDirectionFromLocale() ? -1 : 1;
            case UNRECOGNIZED:
                return defaultValue;
        }
        return defaultValue;
    }

    static class SweepGradientHelper {

        private static class AngularColorStop {
            final float angle;
            @ColorInt final int color;

            AngularColorStop(float angle, int color) {
                this.angle = angle;
                this.color = color;
            }
        }

        private static final int MIN_COLOR_STOPS = 2;
        private static final int MAX_COLOR_STOPS = 10;

        /**
         * The size of the sector with a constant color, equivalent to 90 degrees, where the cap is
         * drawn. It's used to ensure that the cap is drawn with the adjacent color in the line.
         */
        private static final float CAP_COLOR_SHADER_OFFSET_SIZE = 0.25f;

        @NonNull private final List<AngularColorStop> colorStops;

        /** Constructor. All colors will have their alpha channel set to 0xFF (opaque). */
        SweepGradientHelper(@NonNull ColorProto.SweepGradient sweepGradProto) {
            int numColors = sweepGradProto.getColorStopsCount();
            if (numColors < MIN_COLOR_STOPS || numColors > MAX_COLOR_STOPS) {
                throw new IllegalArgumentException(
                        "SweepGradient color count must be >= "
                                + MIN_COLOR_STOPS
                                + "and <= "
                                + MAX_COLOR_STOPS);
            }

            final float gradStartAngle = sweepGradProto.getStartAngle().getValue();
            final float gradEndAngle =
                    sweepGradProto.getEndAngle().hasValue()
                            ? sweepGradProto.getEndAngle().getValue()
                            : 360f;

            // Use the first color stop to check for offsets to be present or absent.
            boolean offsetsRequired = sweepGradProto.getColorStops(0).hasOffset();

            colorStops = new ArrayList<>(numColors);
            for (int i = 0; i < numColors; i++) {
                ColorProto.ColorStop stop = sweepGradProto.getColorStops(i);
                if (offsetsRequired ^ stop.hasOffset()) {
                    throw new IllegalArgumentException(
                            "Either all or none of the color stops should contain an offset.");
                }
                float offset =
                        stop.hasOffset()
                                ? stop.getOffset().getValue()
                                : (float) i / (numColors - 1);
                float gradAngle = gradStartAngle + offset * (gradEndAngle - gradStartAngle);
                colorStops.add(
                        new AngularColorStop(gradAngle, makeOpaque(stop.getColor().getArgb())));
            }

            if (offsetsRequired) {
                colorStops.sort((a, b) -> Float.compare(a.angle, b.angle));
            }
        }

        /**
         * Interpolates colors linearly. Color interpolation needs to be done accordingly to the
         * underlying SweepGradient shader implementation so that all color transitions are smooth
         * and static.
         *
         * <p>The ArgbEvaluator class applies gamma correction to colors which results in a
         * different behavior compared to the shader's native implementation.
         */
        @ColorInt
        @VisibleForTesting
        int interpolateColors(
                int startColor, float startAngle, int endColor, float endAngle, float targetAngle) {
            if (startAngle == endAngle) {
                return startColor;
            }
            float fraction = (targetAngle - startAngle) / (endAngle - startAngle);
            if (Float.isInfinite(fraction)) {
                return startColor;
            }

            float startA = Color.alpha(startColor);
            float startR = Color.red(startColor);
            float startG = Color.green(startColor);
            float startB = Color.blue(startColor);

            float endA = Color.alpha(endColor);
            float endR = Color.red(endColor);
            float endG = Color.green(endColor);
            float endB = Color.blue(endColor);

            int a = (int) (startA + fraction * (endA - startA));
            int r = (int) (startR + fraction * (endR - startR));
            int g = (int) (startG + fraction * (endG - startG));
            int b = (int) (startB + fraction * (endB - startB));
            return Color.argb(a, r, g, b);
        }

        /**
         * Gets the color for a specific angle in the gradient. The color is an interpolation
         * between the color stops adjacent to the given {@code angle}.
         */
        @ColorInt
        int getColor(float angle) {
            for (int i = 0; i < colorStops.size(); i++) {
                float stopAngle = colorStops.get(i).angle;
                if (stopAngle >= angle) {
                    int stopColor = colorStops.get(i).color;
                    if (i == 0) {
                        return stopColor;
                    }
                    float prevAngle = colorStops.get(i - 1).angle;
                    int prevColor = colorStops.get(i - 1).color;
                    return interpolateColors(prevColor, prevAngle, stopColor, stopAngle, angle);
                }
            }

            // If no color was returned till here, return the last color in the gradient.
            return colorStops.get(colorStops.size() - 1).color;
        }

        /**
         * Gets a SweepGradient Shader object using colors present between gradStartAngle and
         * gradEndAngle, which are angles corresponding to the Brush proto definition. The
         * rotationAngle is applied to the generated Shader object.
         *
         * @param bounds the bounds of the drawing area for the arc
         * @param gradStartAngle the start angle position in the gradient, defining to the start
         *     color
         * @param gradEndAngle the end angle position in the gradient, defining to the end color
         * @param rotationAngle the angle to rotate the shader, defining the position of the start
         *     color
         * @param capPosition the position of the stroke cap.
         */
        @NonNull
        Shader getShader(
                @NonNull RectF bounds,
                float gradStartAngle,
                float gradEndAngle,
                float rotationAngle,
                CapPosition capPosition) {
            if (Math.abs(gradEndAngle - gradStartAngle) > 360f) {
                throw new IllegalArgumentException(
                        "Start and End angles must span at most 360 degrees");
            }

            boolean isClockwise = gradEndAngle >= gradStartAngle;
            if (!isClockwise) {
                gradStartAngle = Math.abs(gradStartAngle);
                gradEndAngle = Math.abs(gradEndAngle);
            }

            List<Integer> colors = new ArrayList<>();
            List<Float> offsets = new ArrayList<>();

            // Start Color
            int startColor = getColor(gradStartAngle);
            colors.add(startColor);
            offsets.add(0f);

            // Colors within range.
            for (int i = 0; i < colorStops.size(); i++) {
                float stopAngle = colorStops.get(i).angle;
                if (stopAngle <= gradStartAngle) {
                    continue;
                }
                if (stopAngle >= gradEndAngle) {
                    break;
                }

                colors.add(colorStops.get(i).color);
                offsets.add((stopAngle - gradStartAngle) / 360f);
            }

            // End Color
            int endColor = getColor(gradEndAngle);
            float endOffset = (gradEndAngle - gradStartAngle) / 360f;
            colors.add(endColor);
            offsets.add(endOffset);

            // Draw the Cap with a solid color. The Cap must have the same color as its adjacent
            // position in the ArcLine. So a new color stop is added to the Shader to make sure the
            // Cap region has a single color.
            if (capPosition == CapPosition.START) {
                colors.add(startColor);
                offsets.add(1f - CAP_COLOR_SHADER_OFFSET_SIZE);
            } else if (capPosition == CapPosition.END) {
                colors.add(endColor);
                offsets.add(endOffset + CAP_COLOR_SHADER_OFFSET_SIZE);
            }

            // Invert gradient if angle span is counter-clockwise.
            if (!isClockwise) {
                offsets.replaceAll(o -> 1f - o);
                Collections.reverse(offsets);
                Collections.reverse(colors);
            }

            float centerX = (bounds.left + bounds.right) / 2f;
            float centerY = (bounds.top + bounds.bottom) / 2f;
            SweepGradient shader =
                    new SweepGradient(
                            centerX,
                            centerY,
                            colors.stream().mapToInt(Integer::intValue).toArray(),
                            Floats.toArray(offsets));
            Matrix matrix = new Matrix();
            matrix.setRotate(rotationAngle, centerX, centerY);
            shader.setLocalMatrix(matrix);
            return shader;
        }
    }

    /**
     * A segment of a line, used as a building block for complex lines. Each segment has its own
     * Paint for drawing.
     */
    static class ArcSegment {
        enum CapPosition {
            NONE,
            START,
            END
        }

        /** The angle span of the sector that is clipped out. */
        private static final float CLIP_OUT_PATH_SPAN_DEGREES = 90f;

        @NonNull private final Paint mPaint;
        @NonNull private final Path mPath;

        /** A region to be clipped out when drawing, in order to exclude one of the stroke caps. */
        @Nullable private Path mExcludedCapRegion = null;

        /** A region to be clipped in when drawing, in order to only include this region. */
        @Nullable private Path mMaskRegion = null;

        /**
         * Creates a segment that draws perpendicular to the arc, covering a length equivalent to
         * the arc thickness. It can be used to ensure a region in the arc has a desired color.
         */
        static ArcSegment midJunction(
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

            return new ArcSegment(line, paint);
        }

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
            return new ArcSegment(segment.mPath, paint, maskRegion, segment.mExcludedCapRegion);
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
            mPath = new Path();
            if (capPosition == CapPosition.NONE) {
                mPaint.setStrokeCap(Cap.BUTT);
            }

            if (mPaint.getStrokeCap() != Cap.ROUND && Math.abs(sweepAngle) == 180f) {
                sweepAngle += Math.signum(sweepAngle) * 0.001f;
            }

            mPath.arcTo(bounds, startAngle, sweepAngle);

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
                @NonNull Path mainPath,
                @NonNull Paint paint,
                @Nullable Path maskRegion,
                @Nullable Path excludedCapRegion) {
            this.mPath = mainPath;
            this.mPaint = paint;
            this.mMaskRegion = maskRegion;
            this.mExcludedCapRegion = excludedCapRegion;
        }

        ArcSegment(@NonNull Path mainPath, @NonNull Paint paint) {
            this(mainPath, paint, /* maskRegion= */ null, /* excludedCapRegion= */ null);
        }

        public void onDraw(@NonNull Canvas canvas) {
            canvas.save();
            if (mExcludedCapRegion != null) {
                canvas.clipOutPath(mExcludedCapRegion);
            }
            if (mMaskRegion != null) {
                canvas.clipPath(mMaskRegion);
            }
            canvas.drawPath(mPath, mPaint);
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
        @NonNull private final List<ArcSegment> mSegments = new ArrayList<>();

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

            ArcSegment.CapPosition tailCapPosition = ArcSegment.CapPosition.START;
            // The start of the top layer, relative to the Arc Line's full length.
            float topLayerStartCursor = 0f;
            float topLayerLength = sweepAngle;

            // If absolute length >= 360, we only draw the last 360 degrees of the line's length.
            if (absSweepAngle >= 360f) {
                // When drawing the last 360 degrees of the line, the start and end of the drawing
                // are at the same angle.
                drawStartAngle = (drawStartAngle + sweepAngle) % 360f;
                tailCapPosition = ArcSegment.CapPosition.NONE;
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
                                ArcSegment.CapPosition.END);
                headPaint.setShader(shader);
            }
            ArcSegment headSegment =
                    new ArcSegment(
                            bounds,
                            drawMidAngle,
                            segmentSweep,
                            thicknessPx,
                            ArcSegment.CapPosition.END,
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
            mSegments.add(ArcSegment.midJunction(bounds, drawMidAngle, thicknessPx, midPaint));
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            mSegments.forEach(seg -> seg.onDraw(canvas));
        }
    }

    /** Legacy LinePath, which supports drawing the line as a single Path. */
    private static class ArcDrawableLegacy implements ArcDrawable {

        @NonNull private final Paint mPaint;
        @NonNull private final Path mPath = new Path();

        ArcDrawableLegacy(@NonNull RectF bounds, float clampedLineLength, @NonNull Paint paint) {
            this.mPaint = paint;

            if (clampedLineLength >= 360f) {
                // Android internally will take the modulus of the angle with 360, so drawing a full
                // ring can't be done using path.arcTo. In that case, just draw a circle.
                mPath.addOval(bounds, Direction.CW);
            } else if (clampedLineLength != 0) {
                mPath.moveTo(0, 0); // Work-around for b/177676885
                mPath.arcTo(
                        bounds,
                        BASE_DRAW_ANGLE_SHIFT - (clampedLineLength / 2f),
                        clampedLineLength,
                        /* forceMoveTo= */ true);
            }
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    /** Definition of an arc that can be drawn on the canvas. */
    private interface ArcDrawable {
        /** Called when the arc should be drawn on the canvas. */
        void onDraw(@NonNull Canvas canvas);
    }

    /** Data holder for the stroke cap shadow. */
    private static final class StrokeCapShadow {
        final float mBlurRadius;
        final int mColor;

        StrokeCapShadow(float blurRadius, int color) {
            this.mBlurRadius = blurRadius;
            this.mColor = color;
        }
    }

    /** Changes the alpha channel of the color to 0xFF (fully opaque). */
    private static int makeOpaque(int color) {
        return color | FULLY_OPAQUE_COLOR_MASK;
    }
}

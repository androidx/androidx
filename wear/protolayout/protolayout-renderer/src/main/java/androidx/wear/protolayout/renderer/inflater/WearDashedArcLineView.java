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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.inflater.ArcWidgetHelper.getSignForClockwise;
import static androidx.wear.protolayout.renderer.inflater.ArcWidgetHelper.isPointInsideArcArea;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection;
import androidx.wear.widget.ArcLayout.Widget;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A line, drawn inside an arc. It can be divided into segments by specifying gap size and gap
 * locations.
 */
public class WearDashedArcLineView extends View implements Widget {
    private static final String TAG = "WearDashedArcLineView";
    private static final int DEFAULT_THICKNESS_PX = 0;
    @ColorInt private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    /**
     * The base angle for drawings. The zero angle in Android corresponds to the "3 o clock"
     * position, while ProtoLayout and ArcLayout use the "12 o clock" position as zero.
     */
    private static final float BASE_DRAW_ANGLE_SHIFT = -90f;

    @NonNull private final Paint mPaint;
    @NonNull private final Paint mScaledPaint;
    private final ArrayList<Float> mGapLocationsInDegrees = new ArrayList<>();
    private final ArrayList<LineSegment> mSegments = new ArrayList<>();
    private ArcDirection mLineDirection = ArcDirection.ARC_DIRECTION_NORMAL;
    private float mMaxSweepAngleDegrees = 360F;
    private float mLineSweepAngleDegrees = 0F;
    private int mGapSizePx = 0;
    private float mStrokeWidthPx;
    private final List<ArcLinePath> mPathList = new ArrayList<>();
    private final List<ArcLinePath> mScaledPathList = new ArrayList<>();

    public WearDashedArcLineView(@NonNull Context context) {
        super(context, null, 0, 0);

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStrokeWidth(DEFAULT_THICKNESS_PX);
        mPaint.setAntiAlias(true);

        mScaledPaint = new Paint(mPaint);
    }

    private void updatePath() {
        if (this.getMeasuredWidth() <= 0) {
            return;
        }

        mPathList.clear();
        mScaledPathList.clear();

        float lineLength = resolveSweepAngleDegrees();
        float insetPx = mStrokeWidthPx / 2f;
        float radius = this.getMeasuredWidth() / 2f - insetPx;
        // Calculate arc length from angle and radius: ArcLength = Radius * ArcAngleInRadian
        float dotInDegree = (float) Math.toDegrees(mStrokeWidthPx / radius);

        float startAngleDegrees = mSegments.get(0).startAngleDegrees;
        if (lineLength <= startAngleDegrees) {
            return;
        }

        int arcDirectionSign = getSignForClockwise(this, mLineDirection, /* defaultValue= */ 1);

        RectF rect =
                new RectF(
                        insetPx,
                        insetPx,
                        this.getMeasuredWidth() - insetPx,
                        this.getMeasuredHeight() - insetPx);

        // The arc needs to be offset by -90 degrees. The ArcContainer will rotate this widget such
        // that the "12 o clock" position on the canvas is aligned to the center of our requested
        // angle, but 0 degrees in Android corresponds to the "3 o clock" position.
        float angleShift = BASE_DRAW_ANGLE_SHIFT - (lineLength / 2f) * arcDirectionSign;
        for (int i = 0; i < mSegments.size(); i++) {
            if (mSegments.get(i).startAngleDegrees > lineLength) {
                return;
            }

            float segmentArcLength =
                    min(
                            mSegments.get(i).lengthInDegrees,
                            lineLength - mSegments.get(i).startAngleDegrees);
            float scale = min(segmentArcLength / dotInDegree, 1F);
            // The android arc line does not count the caps as part of the length.
            segmentArcLength -= dotInDegree;
            // For each segment, during the enter transition, we set the stroke width to be equal to
            // the required arc length, and in the same time, to get the dot displayed stably, we
            // set the
            // segment to be 0.1 degree to make the dot get displayed.
            segmentArcLength = max(segmentArcLength, 0.1F);

            float segmentStartAngle = mSegments.get(i).startAngleDegrees + dotInDegree * scale / 2f;
            if (scale == 1F) {
                mPathList.add(
                        new ArcLinePath(
                                rect,
                                segmentStartAngle * arcDirectionSign + angleShift,
                                segmentArcLength * arcDirectionSign));
            } else {
                // Apply scale to the stroke width for dot transition.
                if (mStrokeWidthPx * scale < 1f) { // stroke width is smaller than 1 pixel
                    return;
                }
                mScaledPaint.setStrokeWidth(mStrokeWidthPx * scale);
                mScaledPaint.setAlpha((int) (scale * Color.alpha(mPaint.getColor())));
                mScaledPathList.add(
                        new ArcLinePath(
                                rect,
                                segmentStartAngle * arcDirectionSign + angleShift,
                                segmentArcLength * arcDirectionSign));
            }
        }
    }

    private void updateSegments() {
        if (this.getMeasuredWidth() <= 0) {
            return;
        }

        mSegments.clear();

        if (mGapLocationsInDegrees.isEmpty()) {
            mSegments.add(new LineSegment(0F, 360F));
            return;
        }

        float segmentStart = 0;
        int gapLocIndex = 0;
        float radius = this.getMeasuredWidth() / 2f - mStrokeWidthPx / 2f;
        float minSegmentLength = (float) Math.toDegrees(mStrokeWidthPx / radius);
        float gapInDegrees = (float) Math.toDegrees(mGapSizePx / radius);
        float halfGapInDegrees = gapInDegrees / 2;
        if (mGapLocationsInDegrees.get(0) <= halfGapInDegrees) {
            // start with gap
            segmentStart = mGapLocationsInDegrees.get(0) + halfGapInDegrees;
            gapLocIndex = 1;
        }

        while (gapLocIndex < mGapLocationsInDegrees.size()
                && mGapLocationsInDegrees.get(gapLocIndex) < 360F + halfGapInDegrees) {
            float gapLoc = mGapLocationsInDegrees.get(gapLocIndex);
            float segmentLength = gapLoc - halfGapInDegrees - segmentStart;
            if (segmentLength > minSegmentLength) {
                mSegments.add(new LineSegment(segmentStart, segmentLength));
                segmentStart = gapLoc + halfGapInDegrees;
            } else {
                // We could not fit a segment before the gap, ignoring this gap and log an error.
                Log.e(
                        TAG,
                        "Ignoring the gap at the location "
                                + mGapLocationsInDegrees.get(gapLocIndex)
                                + ", as the arc segment before this gap is shorter than its"
                                + " thickness.");
            }
            gapLocIndex++;
        }

        if (segmentStart < 360F && 360F - segmentStart > minSegmentLength) {
            mSegments.add(new LineSegment(segmentStart, 360F - segmentStart));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updateSegments();
        updatePath();
    }

    /** Gets the list of each gap's center location in degrees. */
    @NonNull
    public List<Float> getGapLocations() {
        return mGapLocationsInDegrees;
    }

    /**
     * Sets the list of each gap's center location in degrees.
     *
     * <p>The interval between any two locations could not be shorter than thickness plus gap size.
     * Any gap for which the distance from its previous gap is shorter than thickness plus gap will
     * be ignored.
     */
    public void setGapLocations(@NonNull List<Float> gapLocations) {
        mGapLocationsInDegrees.clear();
        mGapLocationsInDegrees.addAll(gapLocations);
        Collections.sort(mGapLocationsInDegrees);

        refresh(/* updateSegments= */ true);
    }

    /** Gets the size of the gap between the segments. */
    public int getGapSize() {
        return mGapSizePx;
    }

    /** Sets the size of the gap between the segments. If not defined, defaults to 0. */
    public void setGapSize(int lengthInDp) {
        mGapSizePx = max(0, lengthInDp);

        refresh(/* updateSegments= */ true);
    }

    /** Sets the length of the line contained within this CurvedLineView. */
    public void setLineSweepAngleDegrees(float lineLengthDegrees) {
        this.mLineSweepAngleDegrees = lineLengthDegrees;

        refresh(/* updateSegments= */ false);
    }

    private float resolveSweepAngleDegrees() {
        return min(mLineSweepAngleDegrees, mMaxSweepAngleDegrees);
    }

    private void refresh(boolean updateSegments) {
        if (updateSegments) {
            updateSegments();
        }
        updatePath();
        requestLayout();
        postInvalidate();
    }

    /** Sets the direction which the line is drawn. */
    public void setLineDirection(@NonNull ArcDirection direction) {
        mLineDirection = direction;
        refresh(/* updateSegments= */ false);
    }

    /** Gets the direction which the line is drawn. */
    @NonNull
    public ArcDirection getLineDirection() {
        return mLineDirection;
    }

    /** Returns the sweep angle that this widget is drawn with. */
    @Override
    public float getSweepAngleDegrees() {
        return resolveSweepAngleDegrees();
    }

    @Override
    public void setSweepAngleDegrees(float sweepAngleDegrees) {
        this.mLineSweepAngleDegrees = sweepAngleDegrees;
    }

    /** Returns the thickness of this widget inside the arc. */
    @Override
    public int getThickness() {
        return (int) mStrokeWidthPx;
    }

    /** Sets the thickness of this arc in pixels. */
    public void setThickness(int thickness) {
        mStrokeWidthPx = max(thickness, 0);
        mPaint.setStrokeWidth(mStrokeWidthPx);
        refresh(/* updateSegments= */ true);
    }

    /**
     * Check whether the widget contains invalid attributes as a child of ArcLayout, throwing a
     * Exception if something is wrong. This is important for widgets that can be both standalone or
     * used inside an ArcLayout, some parameters used when the widget is standalone doesn't make
     * sense when the widget is inside an ArcLayout.
     */
    @Override
    public void checkInvalidAttributeAsChild() {
        // Nothing required...
    }

    /** Gets the maximum sweep angle of the line, in degrees. */
    public float getMaxSweepAngleDegrees() {
        return mMaxSweepAngleDegrees;
    }

    /** Sets the maximum sweep angle of the line, in degrees. */
    public void setMaxSweepAngleDegrees(float maxSweepAngleDegrees) {
        this.mMaxSweepAngleDegrees = maxSweepAngleDegrees;

        refresh(/* updateSegments= */ false);
    }

    /** Returns the color of this arc, in ARGB format. */
    @ColorInt
    public int getColor() {
        return mPaint.getColor();
    }

    /** Sets the color of this arc, in ARGB format. */
    public void setColor(@ColorInt int color) {
        mPaint.setColor(color);
        mScaledPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        mPathList.forEach(
                arc ->
                        canvas.drawArc(
                                arc.getOval(),
                                arc.getStartAngle(),
                                arc.getSweepAngle(),
                                /* useCenter= */ false,
                                mPaint));
        mScaledPathList.forEach(
                arc ->
                        canvas.drawArc(
                                arc.getOval(),
                                arc.getStartAngle(),
                                arc.getSweepAngle(),
                                /* useCenter= */ false,
                                mScaledPaint));
    }

    /** Return true when the given point is in the clickable area of the child widget. */
    @Override
    public boolean isPointInsideClickArea(float x, float y) {
        return isPointInsideArcArea(this, x, y, mStrokeWidthPx, resolveSweepAngleDegrees());
    }

    private static class LineSegment {
        final float startAngleDegrees;
        final float lengthInDegrees;

        LineSegment(float startAngleDegrees, float lengthInDegrees) {
            this.startAngleDegrees = startAngleDegrees;
            this.lengthInDegrees = lengthInDegrees;
        }
    }
}

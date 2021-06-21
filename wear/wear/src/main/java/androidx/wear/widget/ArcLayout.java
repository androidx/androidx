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

package androidx.wear.widget;

import static java.lang.Math.asin;
import static java.lang.Math.max;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.wear.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Container which will lay its elements out on an arc. Elements will be relative to a given
 * anchor angle (where 0 degrees = 12 o clock), where the layout relative to the anchor angle is
 * controlled using {@code anchorAngleDegrees} and {@code anchorType}. The thickness of the arc is
 * calculated based on the child element with the greatest height (in the case of Android
 * widgets), or greatest thickness (for curved widgets). By default, the container lays its
 * children one by one in clockwise direction. The attribute 'clockwise' can be set to false to
 * make the layout direction as  anti-clockwise. These two types of widgets will be drawn as
 * follows.
 *
 * <p>Standard Android Widgets:
 *
 * <p>These widgets will be drawn as usual, but placed at the correct position on the arc, with
 * the correct amount of rotation applied. As an example, for an Android Text widget, the text
 * baseline would be drawn at a tangent to the arc. The arc length of a widget is obtained by
 * measuring the width of the widget, and transforming that to the length of an arc on a circle.
 *
 * <p>A standard Android widget will be measured as usual, but the maximum height constraint will be
 * capped at the minimum radius of the arc (i.e. width / 2).
 *
 * <p>"Curved" widgets:
 *
 * <p>Widgets which implement {@link ArcLayout.Widget} are expected to draw themselves within an arc
 * automatically. These widgets will be measured with the full dimensions of the arc container.
 * They are also expected to provide their thickness (used when calculating the thickness of the
 * arc) and the current sweep angle (used for laying out when drawing). Note that the
 * ArcLayout will apply a rotation transform to the canvas before drawing this child; the
 * inner child need not perform any rotations itself.
 *
 * <p>An example of a widget which implements this interface is {@link CurvedTextView}, which
 * will lay itself out along the arc.
 */
@UiThread
public class ArcLayout extends ViewGroup {

    /**
     * Interface for a widget which knows it is being rendered inside an arc, and will draw
     * itself accordingly. Any widget implementing this interface will receive the full-sized
     * canvas, pre-rotated, in its draw call.
     */
    public interface Widget {

        /** Returns the sweep angle that this widget is drawn with. */
        @FloatRange(from = 0.0f, to = 360.0f, toInclusive = true)
        float getSweepAngleDegrees();

        /** Returns the thickness of this widget inside the arc. */
        @Px
        int getThickness();

        /**
         * Check whether the widget contains invalid attributes as a child of ArcLayout, throwing
         * a Exception if something is wrong.
         * This is important for widgets that can be both standalone or used inside an ArcLayout,
         * some parameters used when the widget is standalone doesn't make sense when the widget
         * is inside an ArcLayout.
         */
        void checkInvalidAttributeAsChild();

        /**
         * Return true when the given point is in the clickable area of the child widget.
         * In particular, the coordinates should be considered as if the child was drawn
         * centered at the default angle (12 o clock).
         */
        boolean isPointInsideClickArea(float x, float y);
    }

    /**
     * Layout parameters for a widget added to an arc. This allows each element to specify
     * whether or not it should be rotated(around the center of the child) when drawn inside the
     * arc. For example, when the child is put at the center-bottom of the arc, whether the
     * parent layout is responsible to rotate it 180 degree to draw it upside down.
     *
     * <p>Note that the {@code rotate} parameter is ignored when drawing "Fullscreen" elements.
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /** Vertical alignment of elements within the arc. */
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @IntDef({VERTICAL_ALIGN_OUTER, VERTICAL_ALIGN_CENTER, VERTICAL_ALIGN_INNER})
        public @interface VerticalAlignment {
        }

        /** Align to the outer edge of the parent ArcLayout. */
        public static final int VERTICAL_ALIGN_OUTER = 0;

        /** Align to the center of the parent ArcLayout. */
        public static final int VERTICAL_ALIGN_CENTER = 1;

        /** Align to the inner edge of the parent ArcLayout. */
        public static final int VERTICAL_ALIGN_INNER = 2;

        private boolean mRotated = true;
        @VerticalAlignment
        private int mVerticalAlignment = VERTICAL_ALIGN_CENTER;

        // Internally used during layout/draw
        // Stores the angle of the child, used to handle touch events.
        float mMiddleAngle;

        // Position of the center of the child, in the parent's coordinate space.
        // Currently only used for normal (not ArcLayout.Widget) children.
        float mCenterX;
        float mCenterY;

        /**
         * Creates a new set of layout parameters. The values are extracted from the supplied
         * attributes set and context.
         *
         * @param context  The Context the ArcLayout is running in, through which it can access the
         *                 current theme, resources, etc.
         * @param attrs    The set of attributes from which to extract the layout parameters' values
         */
        public LayoutParams(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArcLayout_Layout);

            mRotated = a.getBoolean(R.styleable.ArcLayout_Layout_layout_rotate, true);
            mVerticalAlignment =
                    a.getInt(R.styleable.ArcLayout_Layout_layout_valign, VERTICAL_ALIGN_CENTER);

            a.recycle();
        }

        /**
         * Creates a new set of layout parameters with specified width and height
         *
         * @param width   The width, either WRAP_CONTENT, MATCH_PARENT or a fixed size in pixels
         * @param height  The height, either WRAP_CONTENT, MATCH_PARENT or a fixed size in pixels
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /** Copy constructor */
        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Gets whether the widget shall be rotated by the ArcLayout container corresponding
         * to its layout position angle
         */
        public boolean isRotated() {
            return mRotated;
        }

        /**
         * Sets whether the widget shall be rotated by the ArcLayout container corresponding
         * to its layout position angle
         */
        public void setRotated(boolean rotated) {
            mRotated = rotated;
        }

        /**
         * Gets how the widget is positioned vertically in the ArcLayout.
         */
        @VerticalAlignment
        public int getVerticalAlignment() {
            return mVerticalAlignment;
        }

        /**
         * Sets how the widget is positioned vertically in the ArcLayout.
         * @param verticalAlignment align the widget to outer, inner edges or center.
         */
        public void setVerticalAlignment(@VerticalAlignment int verticalAlignment) {
            mVerticalAlignment = verticalAlignment;
        }
    }

    /** Annotation for anchor types. */
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({ANCHOR_START, ANCHOR_CENTER, ANCHOR_END})
    public @interface AnchorType {
    }

    /**
     * Anchor at the start of the set of elements drawn within this container. This causes the first
     * child to be drawn from {@code anchorAngle} degrees, to the right.
     *
     * <p>As an example, if this container contains two arcs, one having 10 degrees of sweep and the
     * other having 20 degrees of sweep, the first will be drawn between 0-10 degrees, and the
     * second between 10-30 degrees.
     */
    public static final int ANCHOR_START = 0;

    /**
     * Anchor at the center of the set of elements drawn within this container.
     *
     * <p>As an example, if this container contains two arcs, one having 10 degrees of sweep and the
     * other having 20 degrees of sweep, the first will be drawn between -15 and -5 degrees, and the
     * second between -5 and 15 degrees.
     */
    public static final int ANCHOR_CENTER = 1;

    /**
     * Anchor at the end of the set of elements drawn within this container. This causes the last
     * element to end at {@code anchorAngle} degrees, with the other elements swept to the left.
     *
     * <p>As an example, if this container contains two arcs, one having 10 degrees of sweep and the
     * other having 20 degrees of sweep, the first will be drawn between -30 and -20 degrees, and
     * the second between -20 and 0 degrees.
     */
    public static final int ANCHOR_END = 2;

    private static final float DEFAULT_START_ANGLE_DEGREES = 0f;
    private static final boolean DEFAULT_LAYOUT_DIRECTION_IS_CLOCKWISE = true; // clockwise
    @AnchorType
    private static final int DEFAULT_ANCHOR_TYPE = ANCHOR_START;

    private int mThicknessPx = 0;

    @AnchorType
    private int mAnchorType;
    private float mAnchorAngleDegrees;
    private boolean mClockwise;

    @SuppressWarnings("SyntheticAccessor")
    private final ChildArcAngles mChildArcAngles = new ChildArcAngles();

    public ArcLayout(@NonNull Context context) {
        this(context, null);
    }

    public ArcLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ArcLayout(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, R.styleable.ArcLayout, defStyleAttr, defStyleRes
                );

        mAnchorType = a.getInt(R.styleable.ArcLayout_anchorPosition, DEFAULT_ANCHOR_TYPE);
        mAnchorAngleDegrees =
                a.getFloat(
                        R.styleable.ArcLayout_anchorAngleDegrees, DEFAULT_START_ANGLE_DEGREES
                );
        mClockwise = a.getBoolean(
                R.styleable.ArcLayout_clockwise, DEFAULT_LAYOUT_DIRECTION_IS_CLOCKWISE
        );

        a.recycle();
    }

    @Override
    public void requestLayout() {
        super.requestLayout();

        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).forceLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Need to derive the thickness of the curve from the children. We're a curve, so the
        // children can only be sized up to (width or height)/2 units. This currently only
        // supports fitting to a circle.
        //
        // No matter what, fit to the given size, be it a maximum or a fixed size. It doesn't make
        // sense for this container to wrap its children.
        int actualWidthPx = MeasureSpec.getSize(widthMeasureSpec);
        int actualHeightPx = MeasureSpec.getSize(heightMeasureSpec);

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            // We can't actually resolve this.
            // Let's fit to the screen dimensions, for need of anything better...
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            actualWidthPx = displayMetrics.widthPixels;
            actualHeightPx = displayMetrics.heightPixels;
        }

        // Fit to a square.
        if (actualWidthPx < actualHeightPx) {
            actualHeightPx = actualWidthPx;
        } else if (actualHeightPx < actualWidthPx) {
            actualWidthPx = actualHeightPx;
        }

        int maxChildDimension = actualHeightPx / 2;

        // Measure all children in the new measurespec, and cache the largest.
        int childMeasureSpec = MeasureSpec.makeMeasureSpec(maxChildDimension, MeasureSpec.AT_MOST);

        // We need to do two measure passes. First, we need to measure all "normal" children, and
        // get the thickness of all "CurvedContainer" children. Once we have that, we know the
        // maximum thickness, and we can lay out the "CurvedContainer" children, taking into
        // account their vertical alignment.
        int maxChildHeightPx = 0;
        int childState = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            // ArcLayoutWidget is a special case. Because of how it draws, fit it to the size
            // of the whole widget.
            int childMeasuredHeight;
            if (child instanceof Widget) {
                childMeasuredHeight = ((Widget) child).getThickness();
            } else {
                measureChild(
                        child,
                        getChildMeasureSpec(childMeasureSpec, 0, child.getLayoutParams().width),
                        getChildMeasureSpec(childMeasureSpec, 0, child.getLayoutParams().height)
                );
                childMeasuredHeight = child.getMeasuredHeight();
                childState = combineMeasuredStates(childState, child.getMeasuredState());

            }
            LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
            maxChildHeightPx = max(maxChildHeightPx, childMeasuredHeight
                    + childLayoutParams.topMargin +  childLayoutParams.bottomMargin);
        }

        mThicknessPx = maxChildHeightPx;

        // And now do the pass for the ArcLayoutWidgets
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            if (child instanceof Widget) {
                LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();

                float insetPx = getChildTopInset(child);

                int innerChildMeasureSpec =
                        MeasureSpec.makeMeasureSpec(
                                maxChildDimension * 2 - round(insetPx * 2), MeasureSpec.EXACTLY);

                measureChild(
                        child,
                        getChildMeasureSpec(innerChildMeasureSpec, 0, childLayoutParams.width),
                        getChildMeasureSpec(innerChildMeasureSpec, 0, childLayoutParams.height)
                );

                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }

        setMeasuredDimension(
                resolveSizeAndState(actualWidthPx, widthMeasureSpec, childState),
                resolveSizeAndState(actualHeightPx, heightMeasureSpec, childState));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
         // Layout the children in the arc, computing the center angle where they should be drawn.
        float currentCumulativeAngle = calculateInitialRotation();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            calculateArcAngle(child, mChildArcAngles);
            float preRotation = mChildArcAngles.leftMarginAsAngle
                    + mChildArcAngles.actualChildAngle / 2f;
            float multiplier = mClockwise ? 1f : -1f;

            float middleAngle = multiplier * (currentCumulativeAngle + preRotation);
            LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
            childLayoutParams.mMiddleAngle = middleAngle;

            // Distance from the center of the ArcLayout to the center of the child widget
            float centerToCenterDistance = (getMeasuredHeight() - child.getMeasuredHeight()) / 2
                    - getChildTopInset(child);
            // Move the center of the widget in the circle centered on this ArcLayout, and with
            // radius centerToCenterDistance
            childLayoutParams.mCenterX =
                    (float) (getMeasuredWidth() / 2f
                            + centerToCenterDistance * Math.sin(middleAngle * Math.PI / 180));
            childLayoutParams.mCenterY =
                    (float) (getMeasuredHeight() / 2f
                            - centerToCenterDistance * Math.cos(middleAngle * Math.PI / 180));

            currentCumulativeAngle += mChildArcAngles.getTotalAngle();

            // Curved container widgets have been measured so that the "arc" inside their widget
            // will touch the outside of the box they have been measured in, taking into account
            // the vertical alignment. Just grow them from the center.
            if (child instanceof Widget) {
                int leftPx =
                        round((getMeasuredWidth() / 2f) - (child.getMeasuredWidth() / 2f));
                int topPx =
                        round((getMeasuredHeight() / 2f) - (child.getMeasuredHeight() / 2f));

                child.layout(
                        leftPx,
                        topPx,
                        leftPx + child.getMeasuredWidth(),
                        topPx + child.getMeasuredHeight()
                );
            } else {
                // Normal widget's centers need to be placed on their final position,
                // the only thing left for drawing is to maybe rotate them.
                int leftPx = round(childLayoutParams.mCenterX - child.getMeasuredWidth() / 2f);
                int topPx = round(childLayoutParams.mCenterY - child.getMeasuredHeight() / 2f);

                child.layout(leftPx, topPx, leftPx + child.getMeasuredWidth(),
                        topPx + child.getMeasuredHeight());
            }
        }
    }

    // When a view (that can handle it) receives a TOUCH_DOWN event, it will get all subsequent
    // events until the touch is released, even if the pointer goes outside of it's bounds.
    private View mTouchedView = null;

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        if (mTouchedView == null && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                // Ensure that the view is visible
                if (child.getVisibility() != VISIBLE) {
                    continue;
                }

                // Map the event to the child's coordinate system
                LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
                float angle = childLayoutParams.mMiddleAngle;

                float[] point = new float[]{event.getX(), event.getY()};
                mapPoint(child, angle, point);

                // Check if the click is actually in the child area
                float x = point[0];
                float y = point[1];

                if (insideChildClickArea(child, x, y)) {
                    mTouchedView = child;
                    break;
                }
            }
        }
        // We can't do normal dispatching because it will capture touch in the original position
        // of children.
        return true;
    }

    private static boolean insideChildClickArea(View child, float x, float y) {
        if (child instanceof Widget) {
            return ((Widget) child).isPointInsideClickArea(x, y);
        }
        return x >= 0 && x < child.getMeasuredWidth() && y >= 0 && y < child.getMeasuredHeight();
    }

    // Map a point to local child coordinates.
    private void mapPoint(View child, float angle, float[] point) {
        Matrix m = new Matrix();

        LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
        if (child instanceof Widget) {
            m.postRotate(-angle, getMeasuredWidth() / 2, getMeasuredHeight() / 2);
            m.postTranslate(-child.getX(), -child.getY());
        } else {
            m.postTranslate(-childLayoutParams.mCenterX, -childLayoutParams.mCenterY);
            if (childLayoutParams.isRotated()) {
                m.postRotate(-angle);
            }
            m.postTranslate(child.getWidth() / 2, child.getHeight() / 2);
        }
        m.mapPoints(point);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mTouchedView != null) {
            // Map the event's coordinates to the child's coordinate space
            float[] point = new float[]{event.getX(), event.getY()};
            LayoutParams touchedViewLayoutParams = (LayoutParams) mTouchedView.getLayoutParams();
            mapPoint(mTouchedView, touchedViewLayoutParams.mMiddleAngle, point);

            float dx = point[0] - event.getX();
            float dy = point[1] - event.getY();
            event.offsetLocation(dx, dy);

            mTouchedView.dispatchTouchEvent(event);

            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                // We have finished handling these series of events.
                mTouchedView = null;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
        // Rotate the canvas to make the children render in the right place.
        canvas.save();

        LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
        float middleAngle = childLayoutParams.mMiddleAngle;

        if (child instanceof Widget) {
            // Rotate the child widget. This rotation places child widget in its correct place in
            // the circle. Rotation is done around the center of the circle that components make.
            canvas.rotate(
                    middleAngle,
                    getMeasuredWidth() / 2f,
                    getMeasuredHeight() / 2f);

            ((Widget) child).checkInvalidAttributeAsChild();
        } else {
            // Normal components already have their center in the right position during layout,
            // the only thing remaining is any needed rotation.
            // This rotation is done in place around the center of the
            // child to adjust it based on rotation and clockwise attributes.
            float angleToRotate = childLayoutParams.isRotated()
                    ? middleAngle + (mClockwise ? 0f : 180f)
                    : 0f;

            canvas.rotate(angleToRotate, childLayoutParams.mCenterX, childLayoutParams.mCenterY);
        }
        boolean wasInvalidateIssued = super.drawChild(canvas, child, drawingTime);

        canvas.restore();

        return wasInvalidateIssued;
    }

    private float calculateInitialRotation() {
        float multiplier = mClockwise ? 1f : -1f;
        if (mAnchorType == ANCHOR_START) {
            return multiplier * mAnchorAngleDegrees;
        }

        float totalArcAngle = 0;

        for (int i = 0; i < getChildCount(); i++) {
            calculateArcAngle(getChildAt(i), mChildArcAngles);
            totalArcAngle += mChildArcAngles.getTotalAngle();
        }

        if (mAnchorType == ANCHOR_CENTER) {
            return multiplier * mAnchorAngleDegrees - (totalArcAngle / 2f);
        } else if (mAnchorType == ANCHOR_END) {
            return multiplier * mAnchorAngleDegrees - totalArcAngle;
        }

        return 0;
    }

    private static float widthToAngleDegrees(float widthPx, float radiusPx) {
        return (float) Math.toDegrees(2 * asin(widthPx / radiusPx / 2f));
    }

    private void calculateArcAngle(@NonNull View view, @NonNull ChildArcAngles childAngles) {
        if (view.getVisibility() == GONE) {
            childAngles.leftMarginAsAngle = 0;
            childAngles.rightMarginAsAngle = 0;
            childAngles.actualChildAngle = 0;
            return;
        }

        float radiusPx = (getMeasuredWidth() / 2f) - mThicknessPx;

        LayoutParams childLayoutParams = (LayoutParams) view.getLayoutParams();

        childAngles.leftMarginAsAngle =
                widthToAngleDegrees(childLayoutParams.leftMargin, radiusPx);
        childAngles.rightMarginAsAngle =
                widthToAngleDegrees(childLayoutParams.rightMargin, radiusPx);

        if (view instanceof Widget) {
            childAngles.actualChildAngle = ((Widget) view).getSweepAngleDegrees();
        } else {
            childAngles.actualChildAngle =
                    widthToAngleDegrees(view.getMeasuredWidth(), radiusPx);
        }
    }

    private float getChildTopInset(@NonNull View child) {
        LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();

        int childHeight = child instanceof Widget
                ? ((Widget) child).getThickness()
                : child.getMeasuredHeight();

        int thicknessDiffPx =
                mThicknessPx - childLayoutParams.topMargin - childLayoutParams.bottomMargin
                        - childHeight;

        int margin = mClockwise ? childLayoutParams.topMargin : childLayoutParams.bottomMargin;
        float topInset = margin + getChildTopOffset(child);

        switch (childLayoutParams.getVerticalAlignment()) {
            case LayoutParams.VERTICAL_ALIGN_OUTER:
                return topInset;
            case LayoutParams.VERTICAL_ALIGN_CENTER:
                return topInset + thicknessDiffPx / 2f;
            case LayoutParams.VERTICAL_ALIGN_INNER:
                return topInset + thicknessDiffPx;
            default:
                // Normally unreachable...
                return 0;
        }
    }

    /**
     * For vertical rectangular screens, additional offset needs to be taken into the account for
     * y position of normal widget in order to be in the correct place in the circle.
     */
    private float getChildTopOffset(View child) {
        if (child instanceof Widget || getMeasuredWidth() >= getMeasuredHeight()) {
            return 0;
        }
        return round((getMeasuredHeight() - getMeasuredWidth()) / 2f);
    }

    @Override
    protected boolean checkLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    @NonNull
    protected ViewGroup.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    @NonNull
    public ViewGroup.LayoutParams generateLayoutParams(@NonNull AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    @NonNull
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /** Returns the anchor type used for this container. */
    @AnchorType
    public int getAnchorType() {
        return mAnchorType;
    }

    /** Sets the anchor type used for this container. */
    public void setAnchorType(@AnchorType int anchorType) {
        if (anchorType < ANCHOR_START || anchorType > ANCHOR_END) {
            throw new IllegalArgumentException("Unknown anchor type");
        }

        mAnchorType = anchorType;
        invalidate();
    }

    /** Returns the anchor angle used for this container, in degrees. */
    @FloatRange(from = 0.0f, to = 360.0f, toInclusive = true)
    public float getAnchorAngleDegrees() {
        return mAnchorAngleDegrees;
    }

    /** Sets the anchor angle used for this container, in degrees. */
    public void setAnchorAngleDegrees(
            @FloatRange(from = 0.0f, to = 360.0f, toInclusive = true) float anchorAngleDegrees) {
        mAnchorAngleDegrees = anchorAngleDegrees;
        invalidate();
    }

    /** returns the layout direction */
    public boolean isClockwise() {
        return mClockwise;
    }

    /** Sets the layout direction */
    public void setClockwise(boolean clockwise) {
        mClockwise = clockwise;
        invalidate();
    }

    private static class ChildArcAngles {
        public float leftMarginAsAngle;
        public float rightMarginAsAngle;
        public float actualChildAngle;

        public float getTotalAngle() {
            return leftMarginAsAngle + rightMarginAsAngle + actualChildAngle;
        }
    }
}

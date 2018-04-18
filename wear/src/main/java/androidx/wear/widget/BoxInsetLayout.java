/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.annotation.UiThread;
import androidx.wear.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * BoxInsetLayout is a screen shape-aware ViewGroup that can box its children in the center
 * square of a round screen by using the {@code boxedEdges} attribute. The values for this attribute
 * specify the child's edges to be boxed in: {@code left|top|right|bottom} or {@code all}. The
 * {@code boxedEdges} attribute is ignored on a device with a rectangular screen.
 */
@UiThread
public class BoxInsetLayout extends ViewGroup {

    private static final float FACTOR = 0.146447f; //(1 - sqrt(2)/2)/2
    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

    private final int mScreenHeight;
    private final int mScreenWidth;

    private boolean mIsRound;
    private Rect mForegroundPadding;
    private Rect mInsets;
    private Drawable mForegroundDrawable;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The {@link Context} the view is running in, through which it can access
     *                the current theme, resources, etc.
     */
    public BoxInsetLayout(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML
     * file. This version uses a default style of 0, so the only attribute values applied are those
     * in the Context's Theme and the given AttributeSet.
     * <p>
     * <p>
     * The method onFinishInflate() will be called after all children have been added.
     *
     * @param context The {@link Context} the view is running in, through which it can access
     *                the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public BoxInsetLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor allows subclasses to use their own base style when they are inflating.
     *
     * @param context  The {@link Context} the view is running in, through which it can
     *                 access the current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     *                 resource that supplies default values for the view. Can be 0 to not look for
     *                 defaults.
     */
    public BoxInsetLayout(@NonNull Context context, @Nullable AttributeSet attrs, @StyleRes int
            defStyle) {
        super(context, attrs, defStyle);
        // make sure we have a foreground padding object
        if (mForegroundPadding == null) {
            mForegroundPadding = new Rect();
        }
        if (mInsets == null) {
            mInsets = new Rect();
        }
        mScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        mScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    @Override
    public void setForeground(Drawable drawable) {
        super.setForeground(drawable);
        mForegroundDrawable = drawable;
        if (mForegroundPadding == null) {
            mForegroundPadding = new Rect();
        }
        if (mForegroundDrawable != null) {
            drawable.getPadding(mForegroundPadding);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new BoxInsetLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsRound = getResources().getConfiguration().isScreenRound();
        WindowInsets insets = getRootWindowInsets();
        mInsets.set(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        // find max size
        int maxWidth = 0;
        int maxHeight = 0;
        int childState = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (BoxInsetLayout.LayoutParams) child.getLayoutParams();
                int marginLeft = 0;
                int marginRight = 0;
                int marginTop = 0;
                int marginBottom = 0;
                if (mIsRound) {
                    // round screen, check boxed, don't use margins on boxed
                    if ((lp.boxedEdges & LayoutParams.BOX_LEFT) == 0) {
                        marginLeft = lp.leftMargin;
                    }
                    if ((lp.boxedEdges & LayoutParams.BOX_RIGHT) == 0) {
                        marginRight = lp.rightMargin;
                    }
                    if ((lp.boxedEdges & LayoutParams.BOX_TOP) == 0) {
                        marginTop = lp.topMargin;
                    }
                    if ((lp.boxedEdges & LayoutParams.BOX_BOTTOM) == 0) {
                        marginBottom = lp.bottomMargin;
                    }
                } else {
                    // rectangular, ignore boxed, use margins
                    marginLeft = lp.leftMargin;
                    marginTop = lp.topMargin;
                    marginRight = lp.rightMargin;
                    marginBottom = lp.bottomMargin;
                }
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + marginLeft + marginRight);
                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + marginTop + marginBottom);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }
        // Account for padding too
        maxWidth += getPaddingLeft() + mForegroundPadding.left + getPaddingRight()
                + mForegroundPadding.right;
        maxHeight += getPaddingTop() + mForegroundPadding.top + getPaddingBottom()
                + mForegroundPadding.bottom;

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Check against our foreground's minimum height and width
        if (mForegroundDrawable != null) {
            maxHeight = Math.max(maxHeight, mForegroundDrawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, mForegroundDrawable.getMinimumWidth());
        }

        int measuredWidth = resolveSizeAndState(maxWidth, widthMeasureSpec, childState);
        int measuredHeight = resolveSizeAndState(maxHeight, heightMeasureSpec,
                childState << MEASURED_HEIGHT_STATE_SHIFT);
        setMeasuredDimension(measuredWidth, measuredHeight);

        // determine boxed inset
        int boxInset = calculateInset(measuredWidth, measuredHeight);
        // adjust the the children measures, if necessary
        for (int i = 0; i < count; i++) {
            measureChild(widthMeasureSpec, heightMeasureSpec, boxInset, i);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();

        final int parentLeft = getPaddingLeft() + mForegroundPadding.left;
        final int parentRight = right - left - getPaddingRight() - mForegroundPadding.right;

        final int parentTop = getPaddingTop() + mForegroundPadding.top;
        final int parentBottom = bottom - top - getPaddingBottom() - mForegroundPadding.bottom;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }

                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
                final int horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                int desiredInset = calculateInset(getMeasuredWidth(), getMeasuredHeight());

                // If the child's width is match_parent then we can ignore gravity.
                int leftChildMargin = calculateChildLeftMargin(lp, horizontalGravity, desiredInset);
                int rightChildMargin = calculateChildRightMargin(lp, horizontalGravity,
                        desiredInset);
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    childLeft = parentLeft + leftChildMargin;
                } else {
                    switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = parentLeft + (parentRight - parentLeft - width) / 2
                                    + leftChildMargin - rightChildMargin;
                            break;
                        case Gravity.RIGHT:
                            childLeft = parentRight - width - rightChildMargin;
                            break;
                        case Gravity.LEFT:
                        default:
                            childLeft = parentLeft + leftChildMargin;
                    }
                }

                // If the child's height is match_parent then we can ignore gravity.
                int topChildMargin = calculateChildTopMargin(lp, verticalGravity, desiredInset);
                int bottomChildMargin = calculateChildBottomMargin(lp, verticalGravity,
                        desiredInset);
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    childTop = parentTop + topChildMargin;
                } else {
                    switch (verticalGravity) {
                        case Gravity.CENTER_VERTICAL:
                            childTop = parentTop + (parentBottom - parentTop - height) / 2
                                    + topChildMargin - bottomChildMargin;
                            break;
                        case Gravity.BOTTOM:
                            childTop = parentBottom - height - bottomChildMargin;
                            break;
                        case Gravity.TOP:
                        default:
                            childTop = parentTop + topChildMargin;
                    }
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    private void measureChild(int widthMeasureSpec, int heightMeasureSpec, int desiredMinInset,
            int i) {
        final View child = getChildAt(i);
        final LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();

        int gravity = childLayoutParams.gravity;
        if (gravity == -1) {
            gravity = DEFAULT_CHILD_GRAVITY;
        }
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        int leftParentPadding = getPaddingLeft() + mForegroundPadding.left;
        int rightParentPadding = getPaddingRight() + mForegroundPadding.right;
        int topParentPadding = getPaddingTop() + mForegroundPadding.top;
        int bottomParentPadding = getPaddingBottom() + mForegroundPadding.bottom;

        // adjust width
        int totalWidthMargin = leftParentPadding + rightParentPadding + calculateChildLeftMargin(
                childLayoutParams, horizontalGravity, desiredMinInset) + calculateChildRightMargin(
                childLayoutParams, horizontalGravity, desiredMinInset);

        // adjust height
        int totalHeightMargin = topParentPadding + bottomParentPadding + calculateChildTopMargin(
                childLayoutParams, verticalGravity, desiredMinInset) + calculateChildBottomMargin(
                childLayoutParams, verticalGravity, desiredMinInset);

        childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, totalWidthMargin,
                childLayoutParams.width);
        childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, totalHeightMargin,
                childLayoutParams.height);

        int maxAllowedWidth = getMeasuredWidth() - totalWidthMargin;
        int maxAllowedHeight = getMeasuredHeight() - totalHeightMargin;
        if (child.getMeasuredWidth() > maxAllowedWidth
                || child.getMeasuredHeight() > maxAllowedHeight) {
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    private int calculateChildLeftMargin(LayoutParams lp, int horizontalGravity, int
            desiredMinInset) {
        if (mIsRound && ((lp.boxedEdges & LayoutParams.BOX_LEFT) != 0)) {
            if (lp.width == LayoutParams.MATCH_PARENT || horizontalGravity == Gravity.LEFT) {
                return lp.leftMargin + desiredMinInset;
            }
        }
        return lp.leftMargin;
    }

    private int calculateChildRightMargin(LayoutParams lp, int horizontalGravity, int
            desiredMinInset) {
        if (mIsRound && ((lp.boxedEdges & LayoutParams.BOX_RIGHT) != 0)) {
            if (lp.width == LayoutParams.MATCH_PARENT || horizontalGravity == Gravity.RIGHT) {
                return lp.rightMargin + desiredMinInset;
            }
        }
        return lp.rightMargin;
    }

    private int calculateChildTopMargin(LayoutParams lp, int verticalGravity, int desiredMinInset) {
        if (mIsRound && ((lp.boxedEdges & LayoutParams.BOX_TOP) != 0)) {
            if (lp.height == LayoutParams.MATCH_PARENT || verticalGravity == Gravity.TOP) {
                return lp.topMargin + desiredMinInset;
            }
        }
        return lp.topMargin;
    }

    private int calculateChildBottomMargin(LayoutParams lp, int verticalGravity, int
            desiredMinInset) {
        if (mIsRound && ((lp.boxedEdges & LayoutParams.BOX_BOTTOM) != 0)) {
            if (lp.height == LayoutParams.MATCH_PARENT || verticalGravity == Gravity.BOTTOM) {
                return lp.bottomMargin + desiredMinInset;
            }
        }
        return lp.bottomMargin;
    }

    private int calculateInset(int measuredWidth, int measuredHeight) {
        int rightEdge = Math.min(measuredWidth, mScreenWidth);
        int bottomEdge = Math.min(measuredHeight, mScreenHeight);
        return (int) (FACTOR * Math.max(rightEdge, bottomEdge));
    }

    /**
     * Per-child layout information for layouts that support margins, gravity and boxedEdges.
     * See {@link R.styleable#BoxInsetLayout_Layout BoxInsetLayout Layout Attributes} for a list
     * of all child view attributes that this class supports.
     *
     * @attr ref R.styleable#BoxInsetLayout_Layout_boxedEdges
     */
    public static class LayoutParams extends FrameLayout.LayoutParams {

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef({BOX_NONE, BOX_LEFT, BOX_TOP, BOX_RIGHT, BOX_BOTTOM, BOX_ALL})
        @Retention(RetentionPolicy.SOURCE)
        public @interface BoxedEdges {}

        /** Default boxing setting. There are no insets forced on the child views. */
        public static final int BOX_NONE = 0x0;
        /** The view will force an inset on the left edge of the children. */
        public static final int BOX_LEFT = 0x01;
        /** The view will force an inset on the top edge of the children. */
        public static final int BOX_TOP = 0x02;
        /** The view will force an inset on the right edge of the children. */
        public static final int BOX_RIGHT = 0x04;
        /** The view will force an inset on the bottom edge of the children. */
        public static final int BOX_BOTTOM = 0x08;
        /** The view will force an inset on all of the edges of the children. */
        public static final int BOX_ALL = 0x0F;

        /** Specifies the screen-specific insets for each of the child edges. */
        @BoxedEdges
        public int boxedEdges = BOX_NONE;

        /**
         * Creates a new set of layout parameters. The values are extracted from the supplied
         * attributes set and context.
         *
         * @param context the application environment
         * @param attrs the set of attributes from which to extract the layout parameters' values
         */
        @SuppressWarnings("ResourceType")
        public LayoutParams(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BoxInsetLayout_Layout,
                    0, 0);
            boxedEdges = a.getInt(R.styleable.BoxInsetLayout_Layout_boxedEdges, BOX_NONE);
            a.recycle();
        }

        /**
         * Creates a new set of layout parameters with the specified width and height.
         *
         * @param width the width, either {@link #MATCH_PARENT},
         *              {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height the height, either {@link #MATCH_PARENT},
         *               {@link #WRAP_CONTENT} or a fixed size in pixelsy
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and gravity.
         *
         * @param width the width, either {@link #MATCH_PARENT},
         *              {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height the height, either {@link #MATCH_PARENT},
         *               {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param gravity the gravity
         *
         * @see android.view.Gravity
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }


        public LayoutParams(int width, int height, int gravity, @BoxedEdges int boxed) {
            super(width, height, gravity);
            boxedEdges = boxed;
        }

        /**
         * Copy constructor. Clones the width and height of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height and margin values.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(@NonNull ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, and
         * gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(@NonNull FrameLayout.LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, boxedEdges and
         * gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.boxedEdges = source.boxedEdges;
            this.gravity = source.gravity;
        }
    }
}

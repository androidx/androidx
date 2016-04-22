/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.percent;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import android.support.percent.R;

/**
 * Helper for layouts that want to support percentage based dimensions.
 *
 * <p>This class collects utility methods that are involved in extracting percentage based dimension
 * attributes and applying them to ViewGroup's children. If you would like to implement a layout
 * that supports percentage based dimensions, you need to take several steps:
 *
 * <ol>
 * <li> You need a {@link ViewGroup.LayoutParams} subclass in your ViewGroup that implements
 * {@link android.support.percent.PercentLayoutHelper.PercentLayoutParams}.
 * <li> In your {@code LayoutParams(Context c, AttributeSet attrs)} constructor create an instance
 * of {@link PercentLayoutHelper.PercentLayoutInfo} by calling
 * {@link PercentLayoutHelper#getPercentLayoutInfo(Context, AttributeSet)}. Return this
 * object from {@code public PercentLayoutHelper.PercentLayoutInfo getPercentLayoutInfo()}
 * method that you implemented for {@link android.support.percent.PercentLayoutHelper.PercentLayoutParams} interface.
 * <li> Override
 * {@link ViewGroup.LayoutParams#setBaseAttributes(TypedArray, int, int)}
 * with a single line implementation {@code PercentLayoutHelper.fetchWidthAndHeight(this, a,
 * widthAttr, heightAttr);}
 * <li> In your ViewGroup override {@link ViewGroup#generateLayoutParams(AttributeSet)} to return
 * your LayoutParams.
 * <li> In your {@link ViewGroup#onMeasure(int, int)} override, you need to implement following
 * pattern:
 * <pre class="prettyprint">
 * protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
 *     mHelper.adjustChildren(widthMeasureSpec, heightMeasureSpec);
 *     super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 *     if (mHelper.handleMeasuredStateTooSmall()) {
 *         super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 *     }
 * }
 * </pre>
 * <li>In your {@link ViewGroup#onLayout(boolean, int, int, int, int)} override, you need to
 * implement following pattern:
 * <pre class="prettyprint">
 * protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
 *     super.onLayout(changed, left, top, right, bottom);
 *     mHelper.restoreOriginalParams();
 * }
 * </pre>
 * </ol>
 */
public class PercentLayoutHelper {
    private static final String TAG = "PercentLayout";

    private static final boolean DEBUG = false;
    private static final boolean VERBOSE = false;

    private final ViewGroup mHost;

    public PercentLayoutHelper(@NonNull ViewGroup host) {
        if (host == null) {
            throw new IllegalArgumentException("host must be non-null");
        }
        mHost = host;
    }

    /**
     * Helper method to be called from {@link ViewGroup.LayoutParams#setBaseAttributes} override
     * that reads layout_width and layout_height attribute values without throwing an exception if
     * they aren't present.
     */
    public static void fetchWidthAndHeight(ViewGroup.LayoutParams params, TypedArray array,
            int widthAttr, int heightAttr) {
        params.width = array.getLayoutDimension(widthAttr, 0);
        params.height = array.getLayoutDimension(heightAttr, 0);
    }

    /**
     * Iterates over children and changes their width and height to one calculated from percentage
     * values.
     * @param widthMeasureSpec Width MeasureSpec of the parent ViewGroup.
     * @param heightMeasureSpec Height MeasureSpec of the parent ViewGroup.
     */
    public void adjustChildren(int widthMeasureSpec, int heightMeasureSpec) {
        if (DEBUG) {
            Log.d(TAG, "adjustChildren: " + mHost + " widthMeasureSpec: "
                    + View.MeasureSpec.toString(widthMeasureSpec) + " heightMeasureSpec: "
                    + View.MeasureSpec.toString(heightMeasureSpec));
        }

        // Calculate available space, accounting for host's paddings
        int widthHint = View.MeasureSpec.getSize(widthMeasureSpec) - mHost.getPaddingLeft()
                - mHost.getPaddingRight();
        int heightHint = View.MeasureSpec.getSize(heightMeasureSpec) - mHost.getPaddingTop()
                - mHost.getPaddingBottom();
        for (int i = 0, N = mHost.getChildCount(); i < N; i++) {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (DEBUG) {
                Log.d(TAG, "should adjust " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams) {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (DEBUG) {
                    Log.d(TAG, "using " + info);
                }
                if (info != null) {
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        info.fillMarginLayoutParams(view, (ViewGroup.MarginLayoutParams) params,
                                widthHint, heightHint);
                    } else {
                        info.fillLayoutParams(params, widthHint, heightHint);
                    }
                }
            }
        }
    }

    /**
     * Constructs a PercentLayoutInfo from attributes associated with a View. Call this method from
     * {@code LayoutParams(Context c, AttributeSet attrs)} constructor.
     */
    public static PercentLayoutInfo getPercentLayoutInfo(Context context,
            AttributeSet attrs) {
        PercentLayoutInfo info = null;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PercentLayout_Layout);
        float value = array.getFraction(R.styleable.PercentLayout_Layout_layout_widthPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent width: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.widthPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_heightPercent, 1, 1, -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent height: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.heightPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginPercent, 1, 1, -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.leftMarginPercent = value;
            info.topMarginPercent = value;
            info.rightMarginPercent = value;
            info.bottomMarginPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginLeftPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent left margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.leftMarginPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginTopPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent top margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.topMarginPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginRightPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent right margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.rightMarginPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginBottomPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent bottom margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.bottomMarginPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginStartPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent start margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.startMarginPercent = value;
        }
        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginEndPercent, 1, 1,
                -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "percent end margin: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.endMarginPercent = value;
        }

        value = array.getFraction(R.styleable.PercentLayout_Layout_layout_aspectRatio, 1, 1, -1f);
        if (value != -1f) {
            if (VERBOSE) {
                Log.v(TAG, "aspect ratio: " + value);
            }
            info = info != null ? info : new PercentLayoutInfo();
            info.aspectRatio = value;
        }

        array.recycle();
        if (DEBUG) {
            Log.d(TAG, "constructed: " + info);
        }
        return info;
    }

    /**
     * Iterates over children and restores their original dimensions that were changed for
     * percentage values. Calling this method only makes sense if you previously called
     * {@link PercentLayoutHelper#adjustChildren(int, int)}.
     */
    public void restoreOriginalParams() {
        for (int i = 0, N = mHost.getChildCount(); i < N; i++) {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (DEBUG) {
                Log.d(TAG, "should restore " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams) {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (DEBUG) {
                    Log.d(TAG, "using " + info);
                }
                if (info != null) {
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        info.restoreMarginLayoutParams((ViewGroup.MarginLayoutParams) params);
                    } else {
                        info.restoreLayoutParams(params);
                    }
                }
            }
        }
    }

    /**
     * Iterates over children and checks if any of them would like to get more space than it
     * received through the percentage dimension.
     *
     * If you are building a layout that supports percentage dimensions you are encouraged to take
     * advantage of this method. The developer should be able to specify that a child should be
     * remeasured by adding normal dimension attribute with {@code wrap_content} value. For example
     * he might specify child's attributes as {@code app:layout_widthPercent="60%p"} and
     * {@code android:layout_width="wrap_content"}. In this case if the child receives too little
     * space, it will be remeasured with width set to {@code WRAP_CONTENT}.
     *
     * @return True if the measure phase needs to be rerun because one of the children would like
     * to receive more space.
     */
    public boolean handleMeasuredStateTooSmall() {
        boolean needsSecondMeasure = false;
        for (int i = 0, N = mHost.getChildCount(); i < N; i++) {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (DEBUG) {
                Log.d(TAG, "should handle measured state too small " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams) {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (info != null) {
                    if (shouldHandleMeasuredWidthTooSmall(view, info)) {
                        needsSecondMeasure = true;
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    if (shouldHandleMeasuredHeightTooSmall(view, info)) {
                        needsSecondMeasure = true;
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "should trigger second measure pass: " + needsSecondMeasure);
        }
        return needsSecondMeasure;
    }

    private static boolean shouldHandleMeasuredWidthTooSmall(View view, PercentLayoutInfo info) {
        int state = ViewCompat.getMeasuredWidthAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.widthPercent >= 0 &&
                info.mPreservedParams.width == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private static boolean shouldHandleMeasuredHeightTooSmall(View view, PercentLayoutInfo info) {
        int state = ViewCompat.getMeasuredHeightAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.heightPercent >= 0 &&
                info.mPreservedParams.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    /* package */ static class PercentMarginLayoutParams extends ViewGroup.MarginLayoutParams {
        // These two flags keep track of whether we're computing the LayoutParams width and height
        // in the fill pass based on the aspect ratio. This allows the fill pass to be re-entrant
        // as the framework code can call onMeasure() multiple times before the onLayout() is
        // called. Those multiple invocations of onMeasure() are not guaranteed to be called with
        // the same set of width / height.
        private boolean mIsHeightComputedFromAspectRatio;
        private boolean mIsWidthComputedFromAspectRatio;

        public PercentMarginLayoutParams(int width, int height) {
            super(width, height);
        }
    }

    /**
     * Container for information about percentage dimensions and margins. It acts as an extension
     * for {@code LayoutParams}.
     */
    public static class PercentLayoutInfo {
        /** The decimal value of the percentage-based width. */
        public float widthPercent;

        /** The decimal value of the percentage-based height. */
        public float heightPercent;

        /** The decimal value of the percentage-based left margin. */
        public float leftMarginPercent;

        /** The decimal value of the percentage-based top margin. */
        public float topMarginPercent;

        /** The decimal value of the percentage-based right margin. */
        public float rightMarginPercent;

        /** The decimal value of the percentage-based bottom margin. */
        public float bottomMarginPercent;

        /** The decimal value of the percentage-based start margin. */
        public float startMarginPercent;

        /** The decimal value of the percentage-based end margin. */
        public float endMarginPercent;

        /** The decimal value of the percentage-based aspect ratio. */
        public float aspectRatio;

        /* package */ final PercentMarginLayoutParams mPreservedParams;

        public PercentLayoutInfo() {
            widthPercent = -1f;
            heightPercent = -1f;
            leftMarginPercent = -1f;
            topMarginPercent = -1f;
            rightMarginPercent = -1f;
            bottomMarginPercent = -1f;
            startMarginPercent = -1f;
            endMarginPercent = -1f;
            mPreservedParams = new PercentMarginLayoutParams(0, 0);
        }

        /**
         * Fills the {@link ViewGroup.LayoutParams#width} and {@link ViewGroup.LayoutParams#height}
         * fields of the passed {@link ViewGroup.LayoutParams} object based on currently set
         * percentage values.
         */
        public void fillLayoutParams(ViewGroup.LayoutParams params, int widthHint,
                int heightHint) {
            // Preserve the original layout params, so we can restore them after the measure step.
            mPreservedParams.width = params.width;
            mPreservedParams.height = params.height;

            // We assume that width/height set to 0 means that value was unset. This might not
            // necessarily be true, as the user might explicitly set it to 0. However, we use this
            // information only for the aspect ratio. If the user set the aspect ratio attribute,
            // it means they accept or soon discover that it will be disregarded.
            final boolean widthNotSet =
                    (mPreservedParams.mIsWidthComputedFromAspectRatio
                            || mPreservedParams.width == 0) && (widthPercent < 0);
            final boolean heightNotSet =
                    (mPreservedParams.mIsHeightComputedFromAspectRatio
                            || mPreservedParams.height == 0) && (heightPercent < 0);

            if (widthPercent >= 0) {
                params.width = (int) (widthHint * widthPercent);
            }

            if (heightPercent >= 0) {
                params.height = (int) (heightHint * heightPercent);
            }

            if (aspectRatio >= 0) {
                if (widthNotSet) {
                    params.width = (int) (params.height * aspectRatio);
                    // Keep track that we've filled the width based on the height and aspect ratio.
                    mPreservedParams.mIsWidthComputedFromAspectRatio = true;
                }
                if (heightNotSet) {
                    params.height = (int) (params.width / aspectRatio);
                    // Keep track that we've filled the height based on the width and aspect ratio.
                    mPreservedParams.mIsHeightComputedFromAspectRatio = true;
                }
            }

            if (DEBUG) {
                Log.d(TAG, "after fillLayoutParams: (" + params.width + ", " + params.height + ")");
            }
        }

        /**
         * @deprecated Use
         * {@link #fillMarginLayoutParams(View, ViewGroup.MarginLayoutParams, int, int)}
         * for proper RTL support.
         */
        @Deprecated
        public void fillMarginLayoutParams(ViewGroup.MarginLayoutParams params,
                int widthHint, int heightHint) {
            fillMarginLayoutParams(null, params, widthHint, heightHint);
        }

        /**
         * Fills the margin fields of the passed {@link ViewGroup.MarginLayoutParams} object based
         * on currently set percentage values and the current layout direction of the passed
         * {@link View}.
         */
        public void fillMarginLayoutParams(View view, ViewGroup.MarginLayoutParams params,
                int widthHint, int heightHint) {
            fillLayoutParams(params, widthHint, heightHint);

            // Preserve the original margins, so we can restore them after the measure step.
            mPreservedParams.leftMargin = params.leftMargin;
            mPreservedParams.topMargin = params.topMargin;
            mPreservedParams.rightMargin = params.rightMargin;
            mPreservedParams.bottomMargin = params.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(mPreservedParams,
                    MarginLayoutParamsCompat.getMarginStart(params));
            MarginLayoutParamsCompat.setMarginEnd(mPreservedParams,
                    MarginLayoutParamsCompat.getMarginEnd(params));

            if (leftMarginPercent >= 0) {
                params.leftMargin = (int) (widthHint * leftMarginPercent);
            }
            if (topMarginPercent >= 0) {
                params.topMargin = (int) (heightHint * topMarginPercent);
            }
            if (rightMarginPercent >= 0) {
                params.rightMargin = (int) (widthHint * rightMarginPercent);
            }
            if (bottomMarginPercent >= 0) {
                params.bottomMargin = (int) (heightHint * bottomMarginPercent);
            }
            boolean shouldResolveLayoutDirection = false;
            if (startMarginPercent >= 0) {
                MarginLayoutParamsCompat.setMarginStart(params,
                        (int) (widthHint * startMarginPercent));
                shouldResolveLayoutDirection = true;
            }
            if (endMarginPercent >= 0) {
                MarginLayoutParamsCompat.setMarginEnd(params,
                        (int) (widthHint * endMarginPercent));
                shouldResolveLayoutDirection = true;
            }
            if (shouldResolveLayoutDirection && (view != null)) {
                // Force the resolve pass so that start / end margins are propagated to the
                // matching left / right fields
                MarginLayoutParamsCompat.resolveLayoutDirection(params,
                        ViewCompat.getLayoutDirection(view));
            }
            if (DEBUG) {
                Log.d(TAG, "after fillMarginLayoutParams: (" + params.width + ", " + params.height
                        + ")");
            }
        }

        @Override
        public String toString() {
            return String.format("PercentLayoutInformation width: %f height %f, margins (%f, %f, "
                            + " %f, %f, %f, %f)", widthPercent, heightPercent, leftMarginPercent,
                    topMarginPercent, rightMarginPercent, bottomMarginPercent, startMarginPercent,
                    endMarginPercent);

        }

        /**
         * Restores the original dimensions and margins after they were changed for percentage based
         * values. You should call this method only if you previously called
         * {@link PercentLayoutHelper.PercentLayoutInfo#fillMarginLayoutParams(View, ViewGroup.MarginLayoutParams, int, int)}.
         */
        public void restoreMarginLayoutParams(ViewGroup.MarginLayoutParams params) {
            restoreLayoutParams(params);
            params.leftMargin = mPreservedParams.leftMargin;
            params.topMargin = mPreservedParams.topMargin;
            params.rightMargin = mPreservedParams.rightMargin;
            params.bottomMargin = mPreservedParams.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(params,
                    MarginLayoutParamsCompat.getMarginStart(mPreservedParams));
            MarginLayoutParamsCompat.setMarginEnd(params,
                    MarginLayoutParamsCompat.getMarginEnd(mPreservedParams));
        }

        /**
         * Restores original dimensions after they were changed for percentage based values.
         * You should call this method only if you previously called
         * {@link PercentLayoutHelper.PercentLayoutInfo#fillLayoutParams(ViewGroup.LayoutParams, int, int)}.
         */
        public void restoreLayoutParams(ViewGroup.LayoutParams params) {
            if (!mPreservedParams.mIsWidthComputedFromAspectRatio) {
                // Only restore the width if we didn't compute it based on the height and
                // aspect ratio in the fill pass.
                params.width = mPreservedParams.width;
            }
            if (!mPreservedParams.mIsHeightComputedFromAspectRatio) {
                // Only restore the height if we didn't compute it based on the width and
                // aspect ratio in the fill pass.
                params.height = mPreservedParams.height;
            }

            // Reset the tracking flags.
            mPreservedParams.mIsWidthComputedFromAspectRatio = false;
            mPreservedParams.mIsHeightComputedFromAspectRatio = false;
        }
    }

    /**
     * If a layout wants to support percentage based dimensions and use this helper class, its
     * {@code LayoutParams} subclass must implement this interface.
     *
     * Your {@code LayoutParams} subclass should contain an instance of {@code PercentLayoutInfo}
     * and the implementation of this interface should be a simple accessor.
     */
    public interface PercentLayoutParams {
        PercentLayoutInfo getPercentLayoutInfo();
    }
}

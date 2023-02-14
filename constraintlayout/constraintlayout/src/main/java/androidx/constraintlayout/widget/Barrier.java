/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.HelperWidget;

/**
 * <b>Added in 1.1</b>
 * <p>
 * A Barrier references multiple widgets as input,
 * and creates a virtual guideline based on the most
 * extreme widget on the specified side. For example,
 * a left barrier will align to the left of all the referenced views.
 * </p>
 * <p>
 * <h2>Example</h2>
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/barrier-buttons.png">
 *     </div>
 *     Let's have two buttons, @id/button1 and @id/button2.
 *     The constraint_referenced_ids field will reference
 *     them by simply having them as comma-separated list:
 *     <pre>
 *     {@code
 *         <androidx.constraintlayout.widget.Barrier
 *              android:id="@+id/barrier"
 *              android:layout_width="wrap_content"
 *              android:layout_height="wrap_content"
 *              app:barrierDirection="start"
 *              app:constraint_referenced_ids="button1,button2" />
 *     }
 *     </pre>
 *     <p>
 *         With the barrier direction set to start, we will have the following result:
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/barrier-start.png">
 *     </div>
 *     <p>
 *         Reversely, with the direction set to end, we will have:
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/barrier-end.png">
 *     </div>
 *     <p>
 *         If the widgets dimensions change,
 *         the barrier will automatically move according to its direction to get
 *         the most extreme widget:
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/barrier-adapt.png">
 *     </div>
 *
 *     <p>
 *         Other widgets can then be constrained to the barrier itself,
 *         instead of the individual widget. This allows a layout
 *         to automatically adapt on widget dimension changes
 *         (e.g. different languages will end up with different length for similar worlds).
 *     </p>
 * <h2>GONE widgets handling</h2>
 * <p>If the barrier references GONE widgets,
 * the default behavior is to create a barrier on the resolved position of the GONE widget.
 * If you do not want to have the barrier take GONE widgets into account,
 * you can change this by setting the attribute <i>barrierAllowsGoneWidgets</i>
 * to false (default being true).</p>
 *     </p>
 * </p>
 *
 */
public class Barrier extends ConstraintHelper {

    /**
     * Left direction constant
     */
    public static final int LEFT = androidx.constraintlayout.core.widgets.Barrier.LEFT;

    /**
     * Top direction constant
     */
    public static final int TOP = androidx.constraintlayout.core.widgets.Barrier.TOP;

    /**
     * Right direction constant
     */
    public static final int RIGHT = androidx.constraintlayout.core.widgets.Barrier.RIGHT;

    /**
     * Bottom direction constant
     */
    public static final int BOTTOM = androidx.constraintlayout.core.widgets.Barrier.BOTTOM;

    /**
     * Start direction constant
     */
    public static final int START = BOTTOM + 2;

    /**
     * End Barrier constant
     */
    public static final int END = START + 1;

    private int mIndicatedType;
    private int mResolvedType;
    private androidx.constraintlayout.core.widgets.Barrier mBarrier;

    public Barrier(Context context) {
        super(context);
        super.setVisibility(View.GONE);
    }

    public Barrier(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(View.GONE);
    }

    public Barrier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
    }

    /**
     * Get the barrier type ({@code Barrier.LEFT}, {@code Barrier.TOP},
     * {@code Barrier.RIGHT}, {@code Barrier.BOTTOM}, {@code Barrier.END},
     * {@code Barrier.START})
     */
    public int getType() {
        return mIndicatedType;
    }

    /**
     * Set the barrier type ({@code Barrier.LEFT}, {@code Barrier.TOP},
     * {@code Barrier.RIGHT}, {@code Barrier.BOTTOM}, {@code Barrier.END},
     * {@code Barrier.START})
     */
    public void setType(int type) {
        mIndicatedType = type;
    }

    private void updateType(ConstraintWidget widget, int type, boolean isRtl) {
        mResolvedType = type;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Pre JB MR1, left/right should take precedence, unless they are
            // not defined and somehow a corresponding start/end constraint exists
            if (mIndicatedType == START) {
                mResolvedType = LEFT;
            } else if (mIndicatedType == END) {
                mResolvedType = RIGHT;
            }
        } else {
            // Post JB MR1, if start/end are defined, they take precedence over left/right
            if (isRtl) {
                if (mIndicatedType == START) {
                    mResolvedType = RIGHT;
                } else if (mIndicatedType == END) {
                    mResolvedType = LEFT;
                }
            } else {
                if (mIndicatedType == START) {
                    mResolvedType = LEFT;
                } else if (mIndicatedType == END) {
                    mResolvedType = RIGHT;
                }
            }
        }
        if (widget instanceof androidx.constraintlayout.core.widgets.Barrier) {
            androidx.constraintlayout.core.widgets.Barrier barrier =
                    (androidx.constraintlayout.core.widgets.Barrier) widget;
            barrier.setBarrierType(mResolvedType);
        }
    }

    @Override
    public void resolveRtl(ConstraintWidget widget, boolean isRtl) {
        updateType(widget, mIndicatedType, isRtl);
    }

    /**
     * @param attrs
     *
     */
    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mBarrier = new androidx.constraintlayout.core.widgets.Barrier();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_Layout);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_barrierDirection) {
                    setType(a.getInt(attr, LEFT));
                } else if (attr == R.styleable.ConstraintLayout_Layout_barrierAllowsGoneWidgets) {
                    mBarrier.setAllowsGoneWidget(a.getBoolean(attr, true));
                } else if (attr == R.styleable.ConstraintLayout_Layout_barrierMargin) {
                    int margin = a.getDimensionPixelSize(attr, 0);
                    mBarrier.setMargin(margin);
                }
            }
            a.recycle();
        }
        mHelperWidget = mBarrier;
        validateParams();
    }

    /**
     * allows gone widgets to be included in the barrier
     * @param supportGone
     */
    public void setAllowsGoneWidget(boolean supportGone) {
        mBarrier.setAllowsGoneWidget(supportGone);
    }

    /**
     * Find if this barrier supports gone widgets.
     *
     * @return true if this barrier supports gone widgets, otherwise false
     *
     * @deprecated This method should be called
     * {@code getAllowsGoneWidget} such that {@code allowsGoneWidget}
     * can be accessed as a property from Kotlin;
     * {@see https://android.github.io/kotlin-guides/interop.html#property-prefixes}.
     * Use {@link #getAllowsGoneWidget()} instead.
     */
    @Deprecated
    public boolean allowsGoneWidget() {
        return mBarrier.getAllowsGoneWidget();
    }

    /**
     * Find if this barrier supports gone widgets.
     *
     * @return true if this barrier supports gone widgets, otherwise false
     */
    public boolean getAllowsGoneWidget() {
        return mBarrier.getAllowsGoneWidget();
    }

    /**
     * Set a margin on the barrier
     *
     * @param margin in dp
     */
    public void setDpMargin(int margin) {
        float density = getResources().getDisplayMetrics().density;
        int px = (int) (0.5f + margin * density);
        mBarrier.setMargin(px);
    }

    /**
     * Returns the barrier margin
     *
     * @return the barrier margin (in pixels)
     */
    public int getMargin() {
        return mBarrier.getMargin();
    }

    /**
     * Set the barrier margin
     *
     * @param margin in pixels
     */
    public void setMargin(int margin) {
        mBarrier.setMargin(margin);
    }

    @Override
    public void loadParameters(ConstraintSet.Constraint constraint,
                               HelperWidget child,
                               ConstraintLayout.LayoutParams layoutParams,
                               SparseArray<ConstraintWidget> mapIdToWidget) {
        super.loadParameters(constraint, child, layoutParams, mapIdToWidget);
        if (child instanceof androidx.constraintlayout.core.widgets.Barrier) {
            androidx.constraintlayout.core.widgets.Barrier barrier =
                    (androidx.constraintlayout.core.widgets.Barrier) child;
            ConstraintWidgetContainer container = (ConstraintWidgetContainer) child.getParent();
            boolean isRtl = container.isRtl();
            updateType(barrier, constraint.layout.mBarrierDirection, isRtl);
            barrier.setAllowsGoneWidget(constraint.layout.mBarrierAllowsGoneWidgets);
            barrier.setMargin(constraint.layout.mBarrierMargin);
        }
    }
}

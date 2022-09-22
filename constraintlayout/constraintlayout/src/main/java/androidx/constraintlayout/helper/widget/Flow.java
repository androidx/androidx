/*
 * Copyright (C) 2019 The Android Open Source Project
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
package androidx.constraintlayout.helper.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.HelperWidget;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.R;
import androidx.constraintlayout.widget.VirtualLayout;

/**
 *
 * Flow VirtualLayout. <b>Added in 2.0</b>
 *
 * Allows positioning of referenced widgets horizontally or vertically, similar to a Chain.
 *
 * The elements referenced are indicated via constraint_referenced_ids, as with other
 * ConstraintHelper implementations.
 *
 * Those referenced widgets are then laid out by the Flow virtual layout in three possible ways:
 * <ul>
 *     <li><a href="#wrap_none">wrap none</a> : simply create a chain out of the
 *     referenced elements</li>
 *     <li><a href="#wrap_chain">wrap chain</a> : create multiple chains (one after the other)
 *     if the referenced elements do not fit</li>
 *     <li><a href="#wrap_aligned">wrap aligned</a> : similar to wrap chain, but will align the
 *     elements by creating rows and columns</li>
 * </ul>
 *
 * As VirtualLayouts are ConstraintHelpers, they are normal views; you can thus treat them as such,
 * and setting up constraints on them (position, dimension) or some view attributes
 * (background, padding) will work. The main difference between VirtualLayouts and ViewGroups is
 * that:
 * <ul>
 *     <li>VirtualLayout keep the hierarchy flat</li>
 *     <li>Other views can thus reference / constrain to not only the VirtualLayout, but also
 *     the views laid out by the VirtualLayout</li>
 *     <li>VirtualLayout allow on the fly behavior modifications
 *     (e.g. for Flow, changing the orientation)</li>
 * </ul>
 *
 * <h4 id="wrap_none">flow_wrapMode = "none"</h4>
 *
 * This will simply create an horizontal or vertical chain out of the referenced widgets.
 * This is the default behavior of Flow.
 *
 * XML attributes that are allowed in this mode:
 *
 * <ul>
 *     <li>flow_horizontalStyle = "spread|spread_inside|packed"</li>
 *     <li>flow_verticalStyle = "spread|spread_inside|packed"</li>
 *     <li>flow_horizontalBias = "<i>float</i>"</li>
 *     <li>flow_verticalBias = "<i>float</i>"</li>
 *     <li>flow_horizontalGap = "<i>dimension</i>"</li>
 *     <li>flow_verticalGap = "<i>dimension</i>"</li>
 *     <li>flow_horizontalAlign = "start|end"</li>
 *     <li>flow_verticalAlign = "top|bottom|center|baseline</li>
 * </ul>
 *
 * While the elements are laid out as a chain in the orientation defined, the way they are laid
 * out in the other dimension is controlled
 * by <i>flow_horizontalAlign</i> and <i>flow_verticalAlign</i> attributes.
 *
 * <h4 id="wrap_chain">flow_wrapMode = "chain"</h4>
 *
 * Similar to wrap none in terms of creating chains, but if the referenced widgets do not fit the
 * horizontal or vertical dimension (depending
 * on the orientation picked), they will wrap around to the next line / column.
 *
 * XML attributes are the same same as in wrap_none, with the addition of attributes specifying
 * chain style and chain bias applied to the first chain. This way, it is possible to specify
 * different chain behavior between the first chain and the rest of the chains eventually created.
 *
 * <ul>
 *     <li>flow_firstHorizontalStyle = "spread|spread_inside|packed"</li>
 *     <li>flow_firstVerticalStyle = "spread|spread_inside|packed"</li>
 *     <li>flow_firstHorizontalBias = "<i>float</i>"</li>
 *     <li>flow_firstVerticalBias = "<i>float</i>"</li>
 * </ul>
 *
 * One last important attribute is <i>flow_maxElementsWrap</i>, which specify the number
 * of elements before wrapping, regardless if they
 * fit or not in the available space.
 *
 * <h4 id="wrap_aligned">flow_wrapMode = "aligned"</h4>
 *
 * Same XML attributes as for WRAP_CHAIN, with the difference that the elements are going to be
 * laid out in a set of rows and columns instead of chains.
 * The attribute specifying chains style and bias are thus not going to be applied.
 */
public class Flow extends VirtualLayout {
    private static final String TAG = "Flow";

    private androidx.constraintlayout.core.widgets.Flow mFlow;

    public static final int HORIZONTAL = androidx.constraintlayout.core.widgets.Flow.HORIZONTAL;
    public static final int VERTICAL = androidx.constraintlayout.core.widgets.Flow.VERTICAL;
    public static final int WRAP_NONE = androidx.constraintlayout.core.widgets.Flow.WRAP_NONE;
    public static final int WRAP_CHAIN = androidx.constraintlayout.core.widgets.Flow.WRAP_CHAIN;
    public static final int WRAP_ALIGNED = androidx.constraintlayout.core.widgets.Flow.WRAP_ALIGNED;

    public static final int CHAIN_SPREAD = ConstraintWidget.CHAIN_SPREAD;
    public static final int CHAIN_SPREAD_INSIDE = ConstraintWidget.CHAIN_SPREAD_INSIDE;
    public static final int CHAIN_PACKED = ConstraintWidget.CHAIN_PACKED;

    public static final int HORIZONTAL_ALIGN_START =
            androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_START;
    public static final int HORIZONTAL_ALIGN_END =
            androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_END;
    public static final int HORIZONTAL_ALIGN_CENTER =
            androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_CENTER;

    public static final int VERTICAL_ALIGN_TOP =
            androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_TOP;
    public static final int VERTICAL_ALIGN_BOTTOM =
            androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_BOTTOM;
    public static final int VERTICAL_ALIGN_CENTER =
            androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_CENTER;
    public static final int VERTICAL_ALIGN_BASELINE =
            androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_BASELINE;

    public Flow(Context context) {
        super(context);
    }

    public Flow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Flow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     *
     *
     * @param widget
     * @param isRtl
     */
    @Override
    public void resolveRtl(ConstraintWidget widget, boolean isRtl) {
        mFlow.applyRtl(isRtl);
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasure(mFlow, widthMeasureSpec, heightMeasureSpec);
    }

    /**
     *
     *
     * @param layout
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    public void onMeasure(androidx.constraintlayout.core.widgets.VirtualLayout layout,
                          int widthMeasureSpec,
                          int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (layout != null) {
            layout.measure(widthMode, widthSize, heightMode, heightSize);
            setMeasuredDimension(layout.getMeasuredWidth(), layout.getMeasuredHeight());
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    /**
     *
     *
     * @param constraint
     * @param child
     * @param layoutParams
     * @param mapIdToWidget
     */
    @Override
    public void loadParameters(ConstraintSet.Constraint constraint, HelperWidget child,
                               ConstraintLayout.LayoutParams layoutParams,
                               SparseArray<ConstraintWidget> mapIdToWidget) {
        super.loadParameters(constraint, child, layoutParams, mapIdToWidget);
        if (child instanceof androidx.constraintlayout.core.widgets.Flow) {
            androidx.constraintlayout.core.widgets.Flow flow =
                    (androidx.constraintlayout.core.widgets.Flow) child;
            if (layoutParams.orientation != -1) {
                flow.setOrientation(layoutParams.orientation);
            }
        }
    }

    /**
     *
     *
     * @param attrs
     */
    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mFlow = new androidx.constraintlayout.core.widgets.Flow();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_Layout);
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_android_orientation) {
                    mFlow.setOrientation(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_padding) {
                    mFlow.setPadding(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_paddingStart) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mFlow.setPaddingStart(a.getDimensionPixelSize(attr, 0));
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_paddingEnd) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mFlow.setPaddingEnd(a.getDimensionPixelSize(attr, 0));
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_paddingLeft) {
                    mFlow.setPaddingLeft(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_paddingTop) {
                    mFlow.setPaddingTop(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_paddingRight) {
                    mFlow.setPaddingRight(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_paddingBottom) {
                    mFlow.setPaddingBottom(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_wrapMode) {
                    mFlow.setWrapMode(a.getInt(attr,
                            androidx.constraintlayout.core.widgets.Flow.WRAP_NONE));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_horizontalStyle) {
                    mFlow.setHorizontalStyle(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_verticalStyle) {
                    mFlow.setVerticalStyle(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_firstHorizontalStyle) {
                    mFlow.setFirstHorizontalStyle(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_lastHorizontalStyle) {
                    mFlow.setLastHorizontalStyle(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_firstVerticalStyle) {
                    mFlow.setFirstVerticalStyle(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_lastVerticalStyle) {
                    mFlow.setLastVerticalStyle(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_horizontalBias) {
                    mFlow.setHorizontalBias(a.getFloat(attr, 0.5f));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_firstHorizontalBias) {
                    mFlow.setFirstHorizontalBias(a.getFloat(attr, 0.5f));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_lastHorizontalBias) {
                    mFlow.setLastHorizontalBias(a.getFloat(attr, 0.5f));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_firstVerticalBias) {
                    mFlow.setFirstVerticalBias(a.getFloat(attr, 0.5f));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_lastVerticalBias) {
                    mFlow.setLastVerticalBias(a.getFloat(attr, 0.5f));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_verticalBias) {
                    mFlow.setVerticalBias(a.getFloat(attr, 0.5f));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_horizontalAlign) {
                    mFlow.setHorizontalAlign(a.getInt(attr,
                            androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_CENTER));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_verticalAlign) {
                    mFlow.setVerticalAlign(a.getInt(attr,
                            androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_CENTER));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_horizontalGap) {
                    mFlow.setHorizontalGap(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_verticalGap) {
                    mFlow.setVerticalGap(a.getDimensionPixelSize(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_flow_maxElementsWrap) {
                    mFlow.setMaxElementsWrap(a.getInt(attr, -1));
                }
            }
            a.recycle();
        }

        mHelperWidget = mFlow;
        validateParams();
    }

    /**
     * Set the orientation of the layout
     *
     * @param orientation either Flow.HORIZONTAL or FLow.VERTICAL
     */
    public void setOrientation(int orientation) {
        mFlow.setOrientation(orientation);
        requestLayout();
    }

    /**
     * Set padding around the content
     *
     * @param padding
     */
    public void setPadding(int padding) {
        mFlow.setPadding(padding);
        requestLayout();
    }

    /**
     * Set padding left around the content
     *
     * @param paddingLeft
     */
    public void setPaddingLeft(int paddingLeft) {
        mFlow.setPaddingLeft(paddingLeft);
        requestLayout();
    }

    /**
     * Set padding top around the content
     *
     * @param paddingTop
     */
    public void setPaddingTop(int paddingTop) {
        mFlow.setPaddingTop(paddingTop);
        requestLayout();
    }

    /**
     * Set padding right around the content
     *
     * @param paddingRight
     */
    public void setPaddingRight(int paddingRight) {
        mFlow.setPaddingRight(paddingRight);
        requestLayout();
    }

    /**
     * Set padding bottom around the content
     *
     * @param paddingBottom
     */
    public void setPaddingBottom(int paddingBottom) {
        mFlow.setPaddingBottom(paddingBottom);
        requestLayout();
    }

    /**
     * Set the style of the last Horizontal column.
     * @param style Flow.CHAIN_SPREAD, Flow.CHAIN_SPREAD_INSIDE, or Flow.CHAIN_PACKED
     */
    public void setLastHorizontalStyle(int style) {
        mFlow.setLastHorizontalStyle(style);
        requestLayout();
    }

    /**
     * Set the style of the last vertical row.
     * @param style  Flow.CHAIN_SPREAD, Flow.CHAIN_SPREAD_INSIDE, or Flow.CHAIN_PACKED
     */
    public void setLastVerticalStyle(int style) {
        mFlow.setLastVerticalStyle(style);
        requestLayout();
    }

    /**
     * Set the bias of the last Horizontal column.
     * @param bias
     */
    public void setLastHorizontalBias(float bias) {
        mFlow.setLastHorizontalBias(bias);
        requestLayout();
    }

    /**
     * Set the bias of the last vertical row.
     * @param bias
     */
    public void setLastVerticalBias(float bias) {
        mFlow.setLastVerticalBias(bias);
        requestLayout();
    }

    /**
     * Set wrap mode for the layout. Can be:
     *
     * Flow.WRAP_NONE (default) -- no wrap behavior, create a single chain
     * Flow.WRAP_CHAIN -- if not enough space to fit the referenced elements,
     * will create additional chains after the first one
     * Flow.WRAP_ALIGNED -- if not enough space to fit the referenced elements,
     * will wrap the elements, keeping them aligned (like a table)
     *
     * @param mode
     */
    public void setWrapMode(int mode) {
        mFlow.setWrapMode(mode);
        requestLayout();
    }

    /**
     * Set horizontal chain style. Can be:
     *
     * Flow.CHAIN_SPREAD
     * Flow.CHAIN_SPREAD_INSIDE
     * Flow.CHAIN_PACKED
     *
     * @param style
     */
    public void setHorizontalStyle(int style) {
        mFlow.setHorizontalStyle(style);
        requestLayout();
    }

    /**
     * Set vertical chain style. Can be:
     *
     * Flow.CHAIN_SPREAD
     * Flow.CHAIN_SPREAD_INSIDE
     * Flow.CHAIN_PACKED
     *
     * @param style
     */
    public void setVerticalStyle(int style) {
        mFlow.setVerticalStyle(style);
        requestLayout();
    }

    /**
     * Set the horizontal bias applied to the chain
     *
     * @param bias from 0 to 1
     */
    public void setHorizontalBias(float bias) {
        mFlow.setHorizontalBias(bias);
        requestLayout();
    }

    /**
     * Set the vertical bias applied to the chain
     *
     * @param bias from 0 to 1
     */
    public void setVerticalBias(float bias) {
        mFlow.setVerticalBias(bias);
        requestLayout();
    }

    /**
     * Similar to setHorizontalStyle(), but only applies to the first chain.
     *
     * @param style
     */
    public void setFirstHorizontalStyle(int style) {
        mFlow.setFirstHorizontalStyle(style);
        requestLayout();
    }

    /**
     * Similar to setVerticalStyle(), but only applies to the first chain.
     *
     * @param style
     */
    public void setFirstVerticalStyle(int style) {
        mFlow.setFirstVerticalStyle(style);
        requestLayout();
    }

    /**
     * Similar to setHorizontalBias(), but only applied to the first chain.
     *
     * @param bias
     */
    public void setFirstHorizontalBias(float bias) {
        mFlow.setFirstHorizontalBias(bias);
        requestLayout();
    }

    /**
     * Similar to setVerticalBias(), but only applied to the first chain.
     *
     * @param bias
     */
    public void setFirstVerticalBias(float bias) {
        mFlow.setFirstVerticalBias(bias);
        requestLayout();
    }

    /**
     * Set up the horizontal alignment of the elements in the layout,
     * if the layout orientation is set to Flow.VERTICAL
     *
     * Can be either:
     * Flow.HORIZONTAL_ALIGN_START
     * Flow.HORIZONTAL_ALIGN_END
     * Flow.HORIZONTAL_ALIGN_CENTER
     *
     * @param align
     */
    public void setHorizontalAlign(int align) {
        mFlow.setHorizontalAlign(align);
        requestLayout();
    }

    /**
     * Set up the vertical alignment of the elements in the layout,
     * if the layout orientation is set to Flow.HORIZONTAL
     *
     * Can be either:
     * Flow.VERTICAL_ALIGN_TOP
     * Flow.VERTICAL_ALIGN_BOTTOM
     * Flow.VERTICAL_ALIGN_CENTER
     * Flow.VERTICAL_ALIGN_BASELINE
     *
     * @param align
     */
    public void setVerticalAlign(int align) {
        mFlow.setVerticalAlign(align);
        requestLayout();
    }

    /**
     * Set up the horizontal gap between elements
     *
     * @param gap
     */
    public void setHorizontalGap(int gap) {
        mFlow.setHorizontalGap(gap);
        requestLayout();
    }

    /**
     * Set up the vertical gap between elements
     *
     * @param gap
     */
    public void setVerticalGap(int gap) {
        mFlow.setVerticalGap(gap);
        requestLayout();
    }

    /**
     * Set up the maximum number of elements before wrapping.
     *
     * @param max
     */
    public void setMaxElementsWrap(int max) {
        mFlow.setMaxElementsWrap(max);
        requestLayout();
    }
}

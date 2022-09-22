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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.core.widgets.ConstraintWidget;

/**
 * <b>Added in 1.1</b>
 * <p>
 * A {@code Placeholder} provides a virtual object which can position an existing object.
 * <p>
 * When the id of another view is set on a placeholder (using {@code setContent()}),
 * the placeholder effectively becomes the content view. If the content view exist on the
 * screen it is treated as gone from its original location.
 * <p>
 * The content view is positioned using the layout of the parameters of the
 * {@code Placeholder}  (the {@code Placeholder}
 * is simply constrained in the layout like any other view).
 * </p>
 */
public class Placeholder extends View {

    private int mContentId = -1;
    private View mContent = null;
    private int mEmptyVisibility = View.INVISIBLE;

    public Placeholder(Context context) {
        super(context);
        init(null);
    }

    public Placeholder(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Placeholder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public Placeholder(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        super.setVisibility(mEmptyVisibility);
        mContentId = -1;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_placeholder);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_placeholder_content) {
                    mContentId = a.getResourceId(attr, mContentId);
                } else {
                    if (attr == R.styleable
                            .ConstraintLayout_placeholder_placeholder_emptyVisibility) {
                        mEmptyVisibility = a.getInt(attr, mEmptyVisibility);
                    }
                }
            }
            a.recycle();
        }
    }

    /**
     * Returns the behaviour of a placeholder when it contains no view.
     *
     * @return Either View.VISIBLE, View.INVISIBLE, View.GONE. Default is INVISIBLE
     */
    public int getEmptyVisibility() {
        return mEmptyVisibility;
    }

    /**
     * Sets the visibility of placeholder when not containing objects typically gone or invisible.
     * This can be important as it affects behaviour of surrounding components.
     *
     * @param visibility Either View.VISIBLE, View.INVISIBLE, View.GONE
     */
    public void setEmptyVisibility(int visibility) {
        mEmptyVisibility = visibility;
    }

    /**
     * Returns the content view
     *
     * @return {@code null} if no content is set, otherwise the content view
     */
    public View getContent() {
        return mContent;
    }

    /**
     * Placeholder does not draw anything itself - therefore Paint and Rect allocations
     * are fine to suppress and ignore.
     *
     * @param canvas
     *
     */
    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            canvas.drawRGB(223, 223, 223);

            @SuppressLint("DrawAllocation")
            Paint paint = new Paint();
            paint.setARGB(255, 210, 210, 210);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            @SuppressLint("DrawAllocation")
            Rect r = new Rect();
            canvas.getClipBounds(r);
            paint.setTextSize(r.height());
            int cHeight = r.height();
            int cWidth = r.width();
            paint.setTextAlign(Paint.Align.LEFT);
            String text = "?";
            paint.getTextBounds(text, 0, text.length(), r);
            float x = cWidth / 2f - r.width() / 2f - r.left;
            float y = cHeight / 2f + r.height() / 2f - r.bottom;
            canvas.drawText(text, x, y, paint);
        }
    }

    /**
     * @param container
     *
     */
    public void updatePreLayout(ConstraintLayout container) {
        if (mContentId == -1) {
            if (!isInEditMode()) {
                setVisibility(mEmptyVisibility);
            }
        }

        mContent = container.findViewById(mContentId);
        if (mContent != null) {
            ConstraintLayout.LayoutParams layoutParamsContent =
                    (ConstraintLayout.LayoutParams) mContent.getLayoutParams();
            layoutParamsContent.mIsInPlaceholder = true;
            mContent.setVisibility(View.VISIBLE);
            setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the content view id
     *
     * @param id the id of the content view we want to place in the Placeholder
     */
    public void setContentId(int id) {
        if (mContentId == id) {
            return;
        }
        if (mContent != null) {
            mContent.setVisibility(VISIBLE); // ???
            ConstraintLayout.LayoutParams layoutParamsContent =
                    (ConstraintLayout.LayoutParams) mContent.getLayoutParams();
            layoutParamsContent.mIsInPlaceholder = false;
            mContent = null;
        }

        mContentId = id;
        if (id != ConstraintLayout.LayoutParams.UNSET) {
            View v = ((View) getParent()).findViewById(id);
            if (v != null) {
                v.setVisibility(GONE);
            }
        }
    }

    /**
     * @param container
     *
     */
    public void updatePostMeasure(ConstraintLayout container) {
        if (mContent == null) {
            return;
        }
        ConstraintLayout.LayoutParams layoutParams =
                (ConstraintLayout.LayoutParams) getLayoutParams();
        ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) mContent
                .getLayoutParams();
        layoutParamsContent.mWidget.setVisibility(View.VISIBLE);
        if (layoutParams.mWidget.getHorizontalDimensionBehaviour()
                != ConstraintWidget.DimensionBehaviour.FIXED) {
            layoutParams.mWidget.setWidth(layoutParamsContent.mWidget.getWidth());
        }
        if (layoutParams.mWidget.getVerticalDimensionBehaviour()
                != ConstraintWidget.DimensionBehaviour.FIXED) {
            layoutParams.mWidget.setHeight(layoutParamsContent.mWidget.getHeight());
        }
        layoutParamsContent.mWidget.setVisibility(View.GONE);
    }
}

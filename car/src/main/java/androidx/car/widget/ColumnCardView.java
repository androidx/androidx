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

package androidx.car.widget;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;

import androidx.car.R;
import androidx.car.utils.ColumnCalculator;

/**
 * A {@link CardView} whose width can be specified by the number of columns that it will span.
 *
 * <p>The {@code ColumnCardView} works similarly to a regular {@link CardView}, except that
 * its {@code layout_width} attribute is always ignored. Instead, its width is automatically
 * calculated based on a specified {@code columnSpan} attribute. Alternatively, a user can call
 * {@link #setColumnSpan(int)}. If no column span is given, the {@code ColumnCardView} will have
 * a default span value that it uses.
 *
 * <pre>
 * &lt;androidx.car.widget.ColumnCardView
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     app:columnSpan="4" /&gt;
 * </pre>
 *
 * @see ColumnCalculator
 */
public final class ColumnCardView extends CardView {
    private static final String TAG = "ColumnCardView";

    private ColumnCalculator mColumnCalculator;
    private int mColumnSpan;

    public ColumnCardView(Context context) {
        super(context);
        init(context, null, 0 /* defStyleAttrs */);
    }

    public ColumnCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0 /* defStyleAttrs */);
    }

    public ColumnCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttrs) {
        mColumnCalculator = ColumnCalculator.getInstance(context);

        int defaultColumnSpan = getResources().getInteger(
                R.integer.column_card_default_column_span);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ColumnCardView,
                defStyleAttrs, 0 /* defStyleRes */);
        mColumnSpan = ta.getInteger(R.styleable.ColumnCardView_columnSpan, defaultColumnSpan);
        ta.recycle();

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Column span: " + mColumnSpan);
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Override any specified width so that the width is one that is calculated based on
        // column and gutter span.
        int width = mColumnCalculator.getSizeForColumnSpan(mColumnSpan);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                heightMeasureSpec);
    }

    /**
     * Sets the number of columns that this {@code ColumnCardView} will span. The given span is
     * ignored if it is less than 0 or greater than the number of columns that fit on screen.
     *
     * @param columnSpan The number of columns this {@code ColumnCardView} will span across.
     */
    public void setColumnSpan(int columnSpan) {
        if (columnSpan <= 0 || columnSpan > mColumnCalculator.getNumOfColumns()) {
            return;
        }

        mColumnSpan = columnSpan;
        requestLayout();
    }

    /**
     * Returns the currently number of columns that this {@code ColumnCardView} spans.
     *
     * @return The number of columns this {@code ColumnCardView} spans across.
     */
    public int getColumnSpan() {
        return mColumnSpan;
    }
}

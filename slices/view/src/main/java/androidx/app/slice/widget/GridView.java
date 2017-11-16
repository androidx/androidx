/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.app.slice.widget;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_COLOR;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GridView extends LinearLayout implements LargeSliceAdapter.SliceListView {

    private static final String TAG = "GridView";

    private static final int MAX_IMAGES = 3;
    private static final int MAX_ALL = 5;
    private boolean mIsAllImages;

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsAllImages) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = width / getChildCount();
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            getLayoutParams().height = height;
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).getLayoutParams().height = height;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setSliceItem(SliceItem slice) {
        mIsAllImages = true;
        removeAllViews();
        int total = 1;
        if (FORMAT_SLICE.equals(slice.getFormat())) {
            List<SliceItem> items = slice.getSlice().getItems();
            total = items.size();
            for (int i = 0; i < total; i++) {
                SliceItem item = items.get(i);
                if (isFull()) {
                    continue;
                }
                if (!addItem(item)) {
                    mIsAllImages = false;
                }
            }
        } else {
            if (!isFull()) {
                if (!addItem(slice)) {
                    mIsAllImages = false;
                }
            }
        }
        if (total > getChildCount() && mIsAllImages) {
            addExtraCount(total - getChildCount());
        }
    }

    private void addExtraCount(int numExtra) {
        View last = getChildAt(getChildCount() - 1);
        FrameLayout frame = new FrameLayout(getContext());
        frame.setLayoutParams(last.getLayoutParams());

        removeView(last);
        frame.addView(last, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        TextView v = new TextView(getContext());
        v.setTextColor(Color.WHITE);
        v.setBackgroundColor(0x4d000000);
        v.setText(getResources().getString(R.string.abc_slice_more_content, numExtra));
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        v.setGravity(Gravity.CENTER);
        frame.addView(v, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        addView(frame);
    }

    private boolean isFull() {
        return getChildCount() >= (mIsAllImages ? MAX_IMAGES : MAX_ALL);
    }

    /**
     * Returns true if this item is just an image.
     */
    private boolean addItem(SliceItem item) {
        if (FORMAT_IMAGE.equals(item.getFormat())) {
            ImageView v = new ImageView(getContext());
            v.setImageIcon(item.getIcon());
            v.setScaleType(ScaleType.CENTER_CROP);
            addView(v, new LayoutParams(0, MATCH_PARENT, 1));
            return true;
        } else {
            LinearLayout v = new LinearLayout(getContext());
            int s = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    12, getContext().getResources().getDisplayMetrics());
            v.setPadding(0, s, 0, 0);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setGravity(Gravity.CENTER_HORIZONTAL);
            // TODO: Unify sporadic inflates that happen throughout the code.
            ArrayList<SliceItem> items = new ArrayList<>();
            if (FORMAT_SLICE.equals(item.getFormat())) {
                items.addAll(item.getSlice().getItems());
            }
            items.forEach(i -> {
                Context context = getContext();
                switch (i.getFormat()) {
                    case FORMAT_TEXT:
                        boolean title = false;
                        if ((SliceQuery.hasAnyHints(item, new String[] {
                                HINT_LARGE, HINT_TITLE
                        }))) {
                            title = true;
                        }
                        TextView tv = (TextView) LayoutInflater.from(context).inflate(title
                                ? R.layout.abc_slice_title : R.layout.abc_slice_secondary_text,
                                null);
                        tv.setText(i.getText());
                        v.addView(tv);
                        break;
                    case FORMAT_IMAGE:
                        ImageView iv = new ImageView(context);
                        iv.setImageIcon(i.getIcon());
                        if (item.hasHint(HINT_LARGE)) {
                            iv.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                        } else {
                            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    48, context.getResources().getDisplayMetrics());
                            iv.setLayoutParams(new LayoutParams(size, size));
                        }
                        v.addView(iv);
                        break;
                    case FORMAT_COLOR:
                        // TODO: Support color to tint stuff here.
                        break;
                }
            });
            addView(v, new LayoutParams(0, WRAP_CONTENT, 1));
            return false;
        }
    }
}

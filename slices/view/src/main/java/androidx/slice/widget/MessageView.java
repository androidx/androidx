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

package androidx.slice.widget;

import static android.app.slice.Slice.SUBTYPE_SOURCE;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;

import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MessageView extends SliceChildView {

    private TextView mDetails;
    private ImageView mIcon;

    private int mRowIndex;

    public MessageView(Context context) {
        super(context);
    }

    @Override
    public int getMode() {
        return SliceView.MODE_LARGE;
    }

    @Override
    public void resetView() {
        // TODO
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDetails = findViewById(android.R.id.summary);
        mIcon = findViewById(android.R.id.icon);
    }

    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader, int index,
            int rowCount, SliceView.OnSliceActionListener observer) {
        setSliceActionListener(observer);
        mRowIndex = index;
        SliceItem source = SliceQuery.findSubtype(slice, FORMAT_IMAGE, SUBTYPE_SOURCE);
        if (source != null) {
            final int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24, getContext().getResources().getDisplayMetrics());
            // TODO: try and turn this into a drawable
            Bitmap iconBm = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBm);
            Drawable d = source.getIcon().loadDrawable(getContext());
            d.setBounds(0, 0, iconSize, iconSize);
            d.draw(iconCanvas);
            mIcon.setImageBitmap(SliceViewUtil.getCircularBitmap(iconBm));
        }
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        List<SliceItem> items = SliceQuery.findAll(slice, FORMAT_TEXT);
        for (SliceItem text : items) {
            if (builder.length() != 0) {
                builder.append('\n');
            }
            builder.append(text.getText());
        }
        mDetails.setText(builder.toString());
    }
}

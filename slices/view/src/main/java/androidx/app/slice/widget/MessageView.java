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

import static android.app.slice.Slice.SUBTYPE_SOURCE;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceQuery;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(24)
public class MessageView extends LinearLayout implements LargeSliceAdapter.SliceListView {

    private TextView mDetails;
    private ImageView mIcon;

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDetails = findViewById(android.R.id.summary);
        mIcon = findViewById(android.R.id.icon);
    }

    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader) {
        SliceItem source = SliceQuery.findSubtype(slice, FORMAT_IMAGE, SUBTYPE_SOURCE);
        if (source != null) {
            final int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24, getContext().getResources().getDisplayMetrics());
            // TODO try and turn this into a drawable
            Bitmap iconBm = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBm);
            Drawable d = source.getIcon().loadDrawable(getContext());
            d.setBounds(0, 0, iconSize, iconSize);
            d.draw(iconCanvas);
            mIcon.setImageBitmap(SliceViewUtil.getCircularBitmap(iconBm));
        }
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        SliceQuery.findAll(slice, FORMAT_TEXT).forEach(new Consumer<SliceItem>() {
            @Override
            public void accept(SliceItem text) {
                if (builder.length() != 0) {
                    builder.append('\n');
                }
                builder.append(text.getText());
            }
        });
        mDetails.setText(builder.toString());
    }

    @Override
    public void setColor(SliceItem color) {

    }

}

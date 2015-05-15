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

package android.support.v7.widget;

import android.content.Context;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A {@link TextView} which supports compatible features on older version of the platform,
 * including:
 * <ul>
 *     <li>Supports {@link R.attr#textAllCaps} style attribute which works back to
 *     {@link android.os.Build.VERSION_CODES#ECLAIR_MR1 Eclair MR1}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link TextView} in your layouts.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatTextView extends TextView {

    private AppCompatTextHelper mTextHelper;

    public AppCompatTextView(Context context) {
        this(context, null);
    }

    public AppCompatTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AppCompatTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }
}

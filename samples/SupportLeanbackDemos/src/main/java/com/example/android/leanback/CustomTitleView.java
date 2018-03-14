/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.TitleViewAdapter;

public class CustomTitleView extends LinearLayout implements TitleViewAdapter.Provider {

    private final TitleViewAdapter mTitleViewAdapter = new TitleViewAdapter() {
        @Override
        public View getSearchAffordanceView() {
            return null;
        }

        @Override
        public void setOnSearchClickedListener(View.OnClickListener listener) {
        }

        @Override
        public void setAnimationEnabled(boolean enable) {
        }

        @Override
        public Drawable getBadgeDrawable() {
            return null;
        }

        @Override
        public SearchOrbView.Colors getSearchAffordanceColors() {
            return null;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public void setBadgeDrawable(Drawable drawable) {
        }

        @Override
        public void setSearchAffordanceColors(SearchOrbView.Colors colors) {
        }

        @Override
        public void setTitle(CharSequence titleText) {
        }

        @Override
        public void updateComponentsVisibility(int flags) {
        }
    };

    public CustomTitleView(Context context) {
        this(context, null);
    }

    public CustomTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public TitleViewAdapter getTitleViewAdapter() {
        return mTitleViewAdapter;
    }
}

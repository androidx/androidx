/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v17.leanback.supportleanbackshowcase.app.page;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.TitleViewAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Custom title view to be used in {@link android.support.v17.leanback.app.BrowseFragment}.
 */
public class CustomTitleView extends RelativeLayout implements TitleViewAdapter.Provider {
    private final TextView mTitleView;
    private final ImageView mBadgeView;

    private final TitleViewAdapter mTitleViewAdapter = new TitleViewAdapter() {
        @Override
        public View getSearchAffordanceView() {
            return null;
        }

        @Override
        public void setTitle(CharSequence titleText) {
            CustomTitleView.this.setTitle(titleText);
        }

        @Override
        public void setBadgeDrawable(Drawable drawable) {
            CustomTitleView.this.setBadgeDrawable(drawable);
        }
    };

    public CustomTitleView(Context context) {
        this(context, null);
    }

    public CustomTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View root  = LayoutInflater.from(context).inflate(R.layout.custom_titleview, this);
        mTitleView = (TextView) root.findViewById(R.id.title_tv);
        mBadgeView = (ImageView)root.findViewById(R.id.title_badge_iv);
    }

    public void setTitle(CharSequence title) {
        if (title != null) {
            mTitleView.setText(title);
            mTitleView.setVisibility(View.VISIBLE);
            mBadgeView.setVisibility(View.GONE);
        }
    }


    public void setBadgeDrawable(Drawable drawable) {
        if (drawable != null) {
            mTitleView.setVisibility(View.GONE);
            mBadgeView.setImageDrawable(drawable);
            mBadgeView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public TitleViewAdapter getTitleViewAdapter() {
        return mTitleViewAdapter;
    }
}

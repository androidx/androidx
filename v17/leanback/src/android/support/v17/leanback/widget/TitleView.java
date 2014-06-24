/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Title view for a leanback fragment.
 * @hide
 */
public class TitleView extends FrameLayout {

    private ImageView mBadgeView;
    private TextView mTextView;
    private SearchOrbView mSearchOrbView;

    public TitleView(Context context) {
        this(context, null);
    }

    public TitleView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.browseTitleViewStyle);
    }

    public TitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(context);
        View rootView = inflater.inflate(R.layout.lb_title_view, this);

        mBadgeView = (ImageView) rootView.findViewById(R.id.browse_badge);
        mTextView = (TextView) rootView.findViewById(R.id.browse_title);
        mSearchOrbView = (SearchOrbView) rootView.findViewById(R.id.browse_orb);

        setClipToPadding(false);
        setClipChildren(false);
    }

    /**
     * Sets the title text.
     */
    public void setTitle(String titleText) {
        mTextView.setText(titleText);
    }

    /**
     * Returns the title text.
     */
    public CharSequence getTitle() {
        return mTextView.getText();
    }

    /**
     * Sets the badge drawable.
     * If non-null, the drawable is displayed instead of the title text.
     */
    public void setBadgeDrawable(Drawable drawable) {
        mBadgeView.setImageDrawable(drawable);
        if (drawable != null) {
            mBadgeView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        } else {
            mBadgeView.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Returns the badge drawable.
     */
    public Drawable getBadgeDrawable() {
        return mBadgeView.getDrawable();
    }

    /**
     * Sets the listener to be called when the search affordance is clicked.
     */
    public void setOnSearchClickedListener(View.OnClickListener listener) {
        mSearchOrbView.setOnOrbClickedListener(listener);
    }

    /**
     *  Returns the view for the search affordance.
     */
    public View getSearchAffordanceView() {
        return mSearchOrbView;
    }

    /**
     * Sets the {@link SearchOrbView.Colors} used to draw the search affordance.
     */
    public void setSearchAffordanceColors(SearchOrbView.Colors colors) {
        mSearchOrbView.setOrbColors(colors);
    }

    /**
     * Returns the {@link SearchOrbView.Colors} used to draw the search affordance.
     */
    public SearchOrbView.Colors getSearchAffordanceColors() {
        return mSearchOrbView.getOrbColors();
    }
}

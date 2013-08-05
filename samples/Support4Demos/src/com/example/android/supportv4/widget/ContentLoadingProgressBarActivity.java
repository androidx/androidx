/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv4.widget;

import android.app.Activity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.example.android.supportv4.R;

/**
 * Demonstrates how to use the ContentLoadingProgressBar. By default, the
 * developer should start the ContentLoadingProgressBar with visibility of
 * "gone" or "invisible". The ContentLoadingProgressBar will be shown after the
 * default delay for at least a minimum time regardless of when the "hide"
 * button is pressed.
 */
public class ContentLoadingProgressBarActivity extends Activity implements
        OnClickListener, ViewTreeObserver.OnGlobalLayoutListener {

    private Button mShowButton;
    private Button mHideButton;
    private ContentLoadingProgressBar mBar;
    private long mShowTime;
    private long mHideTime;
    private TextView mShowText;
    private TextView mShowTextDone;
    private TextView mHideText;
    private TextView mHideTextDone;
    private int mLastVisibility;
    private long mVisibilityChangedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_loading_progressbar);

        mBar = (ContentLoadingProgressBar)findViewById(R.id.progressbar);
        mShowButton = (Button)findViewById(R.id.show);
        mShowButton.setOnClickListener(this);
        mHideButton = (Button)findViewById(R.id.hide);
        mHideButton.setOnClickListener(this);

        mShowText = (TextView)findViewById(R.id.show_text);
        mShowTextDone = (TextView)findViewById(R.id.show_text_done);
        mHideText = (TextView)findViewById(R.id.hide_text);
        mHideTextDone = (TextView)findViewById(R.id.hide_text_done);

        mLastVisibility = mBar.getVisibility();

        mBar.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show:
                mBar.show();
                mShowTime = System.currentTimeMillis();
                mShowText.setText("Show clicked at " + mShowTime);
                break;
            case R.id.hide:
                mBar.hide();
                mHideTime = System.currentTimeMillis();
                mHideText.setText("Hide clicked at " + mHideTime);
                break;
        }
    }

    @Override
    public void onGlobalLayout() {
        final int visibility = mBar.getVisibility();

        if (mLastVisibility != visibility) {
            if (visibility == View.VISIBLE) {
                mVisibilityChangedTime = System.currentTimeMillis();
                mShowTextDone.setText("Shown at "
                    + (mVisibilityChangedTime - mShowTime));
            } else {
                mHideTextDone.setText("Hidden after "
                    + (System.currentTimeMillis() - mVisibilityChangedTime));
            }
            mLastVisibility = mBar.getVisibility();
        }
    }
}

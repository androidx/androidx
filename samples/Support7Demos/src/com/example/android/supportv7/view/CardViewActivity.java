/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.example.android.supportv7.view;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.android.supportv7.R;

public class CardViewActivity extends Activity {

    CardView mCardView;

    TextView mInfoText;

    SeekBar mCornerRadiusSeekBar;

    SeekBar mWidthSeekBar;

    SeekBar mHeightSeekBar;

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangedListener
            = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            update();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void update() {
        mCardView.setRadius(mCornerRadiusSeekBar.getProgress());
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mCardView.getLayoutParams();
        lp.width = mWidthSeekBar.getProgress();
        lp.height = mHeightSeekBar.getProgress();
        mCardView.setLayoutParams(lp);
        mInfoText.setText("radius : " + mCornerRadiusSeekBar.getProgress()
                + "\n w:" + lp.width + "\nh:" + lp.height);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_view);
        mInfoText = (TextView) findViewById(R.id.info_text);
        mCardView = (CardView) findViewById(R.id.card_view);
        mCornerRadiusSeekBar = (SeekBar) findViewById(R.id.corner_radius_seek_bar);
        mCornerRadiusSeekBar.setProgress((int) mCardView.getRadius());
        mCornerRadiusSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mWidthSeekBar = (SeekBar) findViewById(R.id.width_seek_bar);
        mWidthSeekBar.setProgress(mCardView.getLayoutParams().width);

        mWidthSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mHeightSeekBar = (SeekBar) findViewById(R.id.height_seek_bar);
        mHeightSeekBar.setProgress(mCardView.getLayoutParams().height);
        mHeightSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        update();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                View content = findViewById(android.R.id.content);
                mWidthSeekBar.setMax(content.getWidth());
                mHeightSeekBar.setMax(content.getHeight());
            }
        }, 100);
    }

}

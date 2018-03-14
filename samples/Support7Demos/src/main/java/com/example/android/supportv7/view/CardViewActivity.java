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

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.android.supportv7.R;

public class CardViewActivity extends AppCompatActivity {

    CardView mCardView;

    TextView mInfoText;

    SeekBar mCornerRadiusSeekBar;

    SeekBar mWidthSeekBar;

    SeekBar mHeightSeekBar;

    SeekBar mElevationSeekBar;

    SeekBar mMaxElevationSeekBar;

    SeekBar mAlphaSeekBar;

    boolean mResizeCardView = true;

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
        mElevationSeekBar.setMax(mMaxElevationSeekBar.getProgress());
        if (mCornerRadiusSeekBar.getProgress() != mCardView.getRadius()) {
            mCardView.setRadius(mCornerRadiusSeekBar.getProgress());
        }
        if (mElevationSeekBar.getProgress() != mCardView.getCardElevation()) {
            mCardView.setCardElevation(mElevationSeekBar.getProgress());
        }
        if (mMaxElevationSeekBar.getProgress() != mCardView.getMaxCardElevation()) {
            mCardView.setMaxCardElevation(mMaxElevationSeekBar.getProgress());
        }
        mCardView.setAlpha(mAlphaSeekBar.getProgress() / 255f);
        ViewGroup.LayoutParams lp;
        if (mResizeCardView) {
            lp = setViewBounds(mCardView);
        } else {
            lp = setViewBounds(mInfoText);
        }
        mInfoText.setText("radius: " + mCornerRadiusSeekBar.getProgress()
                + ", alpha: " + mAlphaSeekBar.getProgress()
                + "\n w: " + lp.width + "\nh: " + lp.height
                + "\nelevation: " + mCardView.getCardElevation() + " of "
                + mCardView.getMaxCardElevation());
    }

    private ViewGroup.LayoutParams setViewBounds(View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        boolean changed = lp.width != mWidthSeekBar.getProgress()
                || lp.height != mHeightSeekBar.getProgress();
        if (!changed) {
            return lp;
        }
        lp.width = mWidthSeekBar.getProgress();
        lp.height = mHeightSeekBar.getProgress();
        view.setLayoutParams(lp);
        return lp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_view);
        mInfoText = findViewById(R.id.info_text);
        mCardView = findViewById(R.id.card_view);
        mCornerRadiusSeekBar = findViewById(R.id.corner_radius_seek_bar);
        mCornerRadiusSeekBar.setProgress((int) mCardView.getRadius());
        mCornerRadiusSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mWidthSeekBar = findViewById(R.id.width_seek_bar);
        mWidthSeekBar.setProgress(mCardView.getLayoutParams().width);

        mWidthSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mHeightSeekBar = findViewById(R.id.height_seek_bar);
        mHeightSeekBar.setProgress(mCardView.getLayoutParams().height);
        mHeightSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mElevationSeekBar = findViewById(R.id.elevation_seek_bar);
        mElevationSeekBar.setProgress((int) mCardView.getCardElevation());
        mElevationSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mMaxElevationSeekBar = findViewById(R.id.max_elevation_seek_bar);
        mMaxElevationSeekBar.setProgress((int) mCardView.getMaxCardElevation());
        mMaxElevationSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        mAlphaSeekBar = findViewById(R.id.alpha_seek_bar);
        mAlphaSeekBar.setProgress((int) (mCardView.getAlpha() * 255));
        mAlphaSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangedListener);

        RadioGroup rb = findViewById(R.id.select_target_radio);
        rb.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mResizeCardView = checkedId == R.id.resize_card_view;
                update();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                View content = findViewById(android.R.id.content);
                mWidthSeekBar.setProgress(mCardView.getWidth());
                mHeightSeekBar.setProgress(mCardView.getHeight());
                mWidthSeekBar.setMax(content.getWidth());
                mHeightSeekBar.setMax(content.getHeight());
                update();
            }
        }, 100);

        ((RadioGroup) findViewById(R.id.select_bg_color_radio))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        mCardView.setCardBackgroundColor(
                                ContextCompat.getColorStateList(CardViewActivity.this,
                                        getColorId(checkedId)));
                    }
                });
    }

    private int getColorId(int id) {
        switch (id) {
            case R.id.yellow:
                return R.color.card_yellow;
            case R.id.aquatic:
                return R.color.card_aquatic;
            case R.id.classic:
                return R.color.card_classic;
            case R.id.sunbrite:
                return R.color.card_sunbrite;
            case R.id.tropical:
                return R.color.card_tropical;
            case R.id.selector:
                return R.color.card_selector;
            default:
                return R.color.cardview_light_background;
        }
    }
}

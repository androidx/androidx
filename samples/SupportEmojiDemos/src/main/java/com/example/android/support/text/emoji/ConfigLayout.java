/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.support.text.emoji;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.RequiresApi;

/**
 * Layout that includes configuration parameters.
 */
public class ConfigLayout extends LinearLayout {
    private Switch mEnableEmojiCompat;
    private Switch mReplaceAll;
    private Switch mDownloadable;
    private Switch mIndicator;

    public ConfigLayout(Context context) {
        super(context);
        init(context);
    }

    public ConfigLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public ConfigLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConfigLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.layout_config, this, true);

        mEnableEmojiCompat = findViewById(R.id.enable);
        mEnableEmojiCompat.setChecked(Config.get().isCompatEnabled());
        mEnableEmojiCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        fireListener();
                    }
                });
            }
        });

        mReplaceAll = findViewById(R.id.replaceAll);
        mReplaceAll.setChecked(Config.get().isReplaceAll());
        mReplaceAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        fireListener();
                    }
                });
            }
        });

        mDownloadable = findViewById(R.id.useDownloadable);
        mDownloadable.setChecked(Config.get().isDownloadable());
        mDownloadable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        fireListener();
                    }
                });
            }
        });

        mIndicator = findViewById(R.id.indicator);
        mIndicator.setChecked(Config.get().isIndicator());
        mIndicator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        fireListener();
                    }
                });
            }
        });

    }

    void fireListener() {
        Config.get().update(mEnableEmojiCompat.isChecked(), mReplaceAll.isChecked(),
                mDownloadable.isChecked(), mIndicator.isChecked());
    }

}

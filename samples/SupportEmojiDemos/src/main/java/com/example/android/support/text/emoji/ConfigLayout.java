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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.RequiresApi;

/**
 * Layout that includes configuration parameters.
 */
public class ConfigLayout extends LinearLayout {
    private Switch mReplaceAll;
    private Switch mIndicator;
    private Spinner mFontSource;

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

        mFontSource = findViewById(R.id.fontSource);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.sourcesArray, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFontSource.setAdapter(adapter);
        mFontSource.setSelection(Config.get().getSource().getPosition());

        mFontSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fireListener();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                fireListener();
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
        int itemPosition = mFontSource.getSelectedItemPosition();

        Config.Source source = Config.Source.DEFAULT;
        if (itemPosition == 1) {
            source = Config.Source.BUNDLED;
        } else if (itemPosition == 2) {
            source = Config.Source.DOWNLOADABLE;
        } else if (itemPosition == 3) {
            source = Config.Source.DISABLED;
        }

        Config.get().update(source, mReplaceAll.isChecked(), mIndicator.isChecked());
    }

}

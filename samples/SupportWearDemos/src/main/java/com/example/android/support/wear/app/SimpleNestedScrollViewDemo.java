/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.android.support.wear.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.android.support.wear.R;

/**
 * Main activity for the NestedScrollView demo.
 */
public class SimpleNestedScrollViewDemo extends Activity {
    private static final int ITEM_COUNT = 100;

    private static final int ITEM_HEIGHT_DP = 50;
    private static final int ITEM_TEXT_SIZE = 14;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nested_sv_demo);

        LinearLayout linearLayout = findViewById(R.id.linear_layout);
        for (int i = 0; i < ITEM_COUNT; i++) {
            TextView textView = new TextView(/* context= */ this);
            textView.setHeight(ITEM_HEIGHT_DP);
            textView.setTextSize(ITEM_TEXT_SIZE);
            textView.setText("Item " + i);
            linearLayout.addView(textView);
        }
    }
}

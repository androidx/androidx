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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.support.design.R;
import com.example.android.support.design.Shakespeare;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

/**
 * This demonstrates basic usage of {@link BottomSheetBehavior}.
 */
abstract class BottomSheetUsageBase extends AppCompatActivity {

    protected BottomSheetBehavior<LinearLayout> mBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        ((TextView) findViewById(R.id.dialogue)).setText(TextUtils.concat(Shakespeare.DIALOGUE));
        mBehavior = BottomSheetBehavior.from((LinearLayout) findViewById(R.id.bottom_sheet));
    }

    @Override
    public void onBackPressed() {
        if (mBehavior != null) {
            int state = mBehavior.getState();
            if (state != BottomSheetBehavior.STATE_COLLAPSED &&
                    state != BottomSheetBehavior.STATE_HIDDEN) {
                mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                return;
            }
        }
        super.onBackPressed();
    }

    protected abstract int getLayoutId();

}

/*
 * Copyright 2017 The Android Open Source Project
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
package com.example.android.supportv7.widget.selection.fancy;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.android.supportv7.R;

final class DemoHeaderHolder extends DemoHolder {

    private static final String HEADER_TAG = "I'm a header";
    final TextView mLabel;

    DemoHeaderHolder(LinearLayout layout) {
        super(layout);
        layout.setTag(HEADER_TAG);
        mLabel = layout.findViewById(R.id.label);
    }

    void update(String label) {
        mLabel.setText(label.toUpperCase() + label + label + "...");
    }

    @Override
    public String toString() {
        return "Header{name:" + mLabel.getText() + "}";
    }

    static boolean isHeader(@Nullable View view) {
        return view == null ? false : HEADER_TAG.equals(view.getTag());
    }
}

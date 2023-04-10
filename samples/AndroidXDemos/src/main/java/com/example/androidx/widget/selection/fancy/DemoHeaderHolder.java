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
package com.example.androidx.widget.selection.fancy;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.androidx.R;

final class DemoHeaderHolder extends DemoHolder {

    final TextView mLabel;

    DemoHeaderHolder(@NonNull Context context, @NonNull ViewGroup parent) {
        this(inflateLayout(context, parent, R.layout.selection_demo_list_header));
    }

    private DemoHeaderHolder(LinearLayout layout) {
        super(layout);
        mLabel = layout.findViewById(R.id.label);
    }

    @Override
    void update(@NonNull Uri uri) {
        String label = Uris.getGroup(uri);
        mLabel.setText(label.toUpperCase() + label + label + "...");
    }

    @Override
    public String toString() {
        return "Header{name:" + mLabel.getText() + "}";
    }
}

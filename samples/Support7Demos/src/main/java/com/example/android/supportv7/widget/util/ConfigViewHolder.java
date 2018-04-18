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

package com.example.android.supportv7.widget.util;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.recyclerview.widget.RecyclerView;

public class ConfigViewHolder extends RecyclerView.ViewHolder
        implements CompoundButton.OnCheckedChangeListener {

    private CheckBox mCheckBox;

    private ConfigToggle mConfigToggle;

    public ConfigViewHolder(View itemView) {
        super(itemView);
        mCheckBox = (CheckBox) itemView;
        mCheckBox.setOnCheckedChangeListener(this);
    }

    public void bind(ConfigToggle toggle) {
        mConfigToggle = toggle;
        mCheckBox.setText(toggle.getText());
        mCheckBox.setChecked(toggle.isChecked());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mConfigToggle != null) {
            mConfigToggle.onChange(isChecked);
        }
    }
}

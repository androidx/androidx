/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.supportv4.graphics;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.android.supportv4.R;

/**
 * Demonstrates use of a {@link DrawableCompat}'s ability to become circular.
 */
public class DrawableCompatActivity extends Activity {

    private static final int IMAGE_RES = R.drawable.ic_favorite;

    private ImageView mImageView;
    private Drawable mDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawable_compat);

        mImageView = findViewById(R.id.image);

        Drawable d = ContextCompat.getDrawable(this, IMAGE_RES);
        mDrawable = DrawableCompat.wrap(d.mutate());

        mImageView.setImageDrawable(mDrawable);

        RadioGroup rg = findViewById(R.id.drawable_compat_options);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.drawable_compat_no_tint:
                        clearTint();
                        break;
                    case R.id.drawable_compat_color:
                        setColorTint();
                        break;
                    case R.id.drawable_compat_state_list:
                        setColorStateListTint();
                        break;
                }
            }
        });
    }

    private void clearTint() {
        DrawableCompat.setTintList(mDrawable, null);
    }

    private void setColorTint() {
        DrawableCompat.setTint(mDrawable, Color.MAGENTA);
    }

    private void setColorStateListTint() {
        DrawableCompat.setTintList(mDrawable,
                ContextCompat.getColorStateList(this, R.color.tint_state_list));
    }

}

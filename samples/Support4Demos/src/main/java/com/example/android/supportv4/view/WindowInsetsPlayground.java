/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.android.supportv4.view;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.IdRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.android.supportv4.R;
import com.example.android.supportv4.graphics.DrawableCompatActivity;

import org.jspecify.annotations.Nullable;

@SuppressWarnings("deprecation")
public class WindowInsetsPlayground extends Activity {
    private boolean mRootWindowInsetsEnabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insets_playground);

        ToggleButton softInputType = findViewById(R.id.btn_soft_input_mode);
        softInputType.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_PAN);
            } else {
                getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE);
            }
        });
        softInputType.setChecked(true);

        ToggleButton insetsType = findViewById(R.id.btn_insets_type);
        insetsType.setOnCheckedChangeListener((button, checked) -> {
            setViewInsetsListenerEnabled(checked);
            setRootWindowInsetsEnabled(!checked);
        });
        insetsType.setChecked(true);

        updateTextForInsets(null);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(0x80000000);
        getWindow().setNavigationBarColor(0x80000000);

        Button newAct = findViewById(R.id.newAct);
        newAct.setOnClickListener(
                v -> startActivity(new Intent(this, DrawableCompatActivity.class)));
    }

    private void setRootWindowInsetsEnabled(boolean enabled) {
        mRootWindowInsetsEnabled = enabled;

        if (enabled) {
            final View root = findViewById(R.id.insets_root);
            // For root window insets, we just poll the insets every 500ms
            root.post(new Runnable() {
                @Override
                public void run() {
                    if (mRootWindowInsetsEnabled) {
                        updateTextForInsets(ViewCompat.getRootWindowInsets(root));
                        root.postDelayed(this, 500);
                    }
                }
            });
        }
    }

    private void setViewInsetsListenerEnabled(boolean enabled) {
        View root = findViewById(R.id.insets_root);
        if (enabled) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                Insets systemBars = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

                updateTextForInsets(windowInsets);
                return WindowInsetsCompat.CONSUMED;
            });
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(root, null);
        }
    }

    private void updateTextForInsets(@Nullable WindowInsetsCompat insets) {
        printInsetType(R.id.type_ime, insets, WindowInsetsCompat.Type.ime());
        printInsetType(R.id.type_system_bars, insets, WindowInsetsCompat.Type.systemBars());
        printInsetType(R.id.type_nav_bars, insets, WindowInsetsCompat.Type.navigationBars());
        printInsetType(R.id.type_status_bar, insets, WindowInsetsCompat.Type.statusBars());
        printInsetType(R.id.type_caption_bar, insets, WindowInsetsCompat.Type.captionBar());
        printInsetType(R.id.type_system_gestures, insets, WindowInsetsCompat.Type.systemGestures());
        printInsetType(R.id.type_mand_system_gestures, insets,
                WindowInsetsCompat.Type.mandatorySystemGestures());
        printInsetType(R.id.type_tappable, insets, WindowInsetsCompat.Type.tappableElement());
        printInsetType(R.id.type_cutout, insets, WindowInsetsCompat.Type.displayCutout());

        printLegacyInset(R.id.insets_system_window,
                insets != null ? insets.getSystemWindowInsets() : Insets.NONE);
        printLegacyInset(R.id.insets_stable,
                insets != null ? insets.getStableInsets() : Insets.NONE);
    }

    private void printInsetType(@IdRes int textViewResId,
            @Nullable WindowInsetsCompat insets, int type) {
        final TextView textView = findViewById(textViewResId);

        if (insets != null) {
            StringBuilder sb = new StringBuilder()
                    .append("<b>Normal:</b> ")
                    .append(insets.getInsets(type).toString())
                    .append("<br>");
            if ((type & WindowInsetsCompat.Type.ime()) == 0) {
                sb.append("<b>Ignoring Visibility:</b> ")
                        .append(insets.getInsetsIgnoringVisibility(type).toString())
                        .append("<br>");
            }
            sb.append("<b>Visible:</b> ")
                    .append(insets.isVisible(type));
            textView.setText(Html.fromHtml(sb.toString()));
        } else {
            textView.setText(R.string.insets_none);
        }
    }

    private void printLegacyInset(@IdRes int textViewResId, @Nullable Insets insets) {
        final TextView textView = findViewById(textViewResId);
        if (insets != null) {
            textView.setText(Html.fromHtml("<b>Value:</b> " + insets.toString()));
        } else {
            textView.setText(R.string.insets_none);
        }
    }
}

/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.compose.remote.player.core.platform.AndroidComponentSupport;

import org.jspecify.annotations.NonNull;

/**
 * Component support delegate for Android's ProgressBar.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SupportProgressBar implements AndroidComponentSupport {

    public static final short PROP_PROGRESS = 5;
    public static final short PROP_MAX_PROGRESS = 6;
    public static final short PROP_INDETERMINATE = 7;
    public static final short PROP_PROGRESS_COLOR = 8;
    public static final short RET_PROGRESS = 9;


    private final AndroidCustomSupport mSupport;

    public SupportProgressBar(@NonNull AndroidCustomSupport support) {
        mSupport = support;
    }

    @Override
    public @NonNull View createView(@NonNull Context context) {
        SeekBar progressBar = new SeekBar(context, null, android.R.attr.seekBarStyle);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(0, 0));

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int id = (Integer) seekBar.getTag();
                if (fromUser && id != -1 && mSupport.getRemoteContext() != null) {
                    mSupport.getRemoteContext().loadFloat(id, (float) progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        return progressBar;
    }

    @Override
    public void configure(@NonNull View view, int type, @NonNull String value) {
        // No string properties supported by ProgressBar currently
    }

    @Override
    public void configure(@NonNull View view, int type, int value) {
        if (view instanceof SeekBar) {
            SeekBar progressBar = (SeekBar) view;
            if (type == PROP_PROGRESS) {
                progressBar.setProgress(value);
            } else if (type == PROP_MAX_PROGRESS) {
                progressBar.setMax(value);
            } else if (type == PROP_INDETERMINATE) {
                progressBar.setIndeterminate(value != 0);
            } else if (type == PROP_PROGRESS_COLOR) {
                progressBar.setProgressTintList(ColorStateList.valueOf(value));
            } else if (type == RET_PROGRESS) {
                System.out.println("Return ..... RET_PROGRESS");

                view.setTag(value);
            }
        }
    }

    @Override
    public void configure(@NonNull View view, int type, float value) {
        if (view instanceof SeekBar) {
            SeekBar progressBar = (SeekBar) view;
            if (type == PROP_PROGRESS) {
                progressBar.setProgress((int) value);
            } else if (type == PROP_MAX_PROGRESS) {
                progressBar.setMax((int) value);
            }
        }
    }
}

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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.compose.remote.player.core.platform.AndroidComponentSupport;

import org.jspecify.annotations.NonNull;

/**
 * Component support delegate for Android's TextView.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SupportTextView implements AndroidComponentSupport {

    public static final short PROP_TEXT = 1;
    public static final short PROP_TEXT_COLOR = 2;
    public static final short PROP_TEXT_SIZE = 3;
    public static final short PROP_BACKGROUND_COLOR = 4;

    @Override
    public @NonNull View createView(@NonNull Context context) {
        TextView textView = new TextView(context);
        textView.setSingleLine(false);
        textView.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
        return textView;
    }

    @Override
    public void configure(@NonNull View view, int type, @NonNull String value) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (type == PROP_TEXT) {
                textView.setText(value);
            }
        }
    }

    @Override
    public void configure(@NonNull View view, int type, int value) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (type == PROP_TEXT_COLOR) {
                textView.setTextColor(value);
            } else if (type == PROP_TEXT_SIZE) {
                textView.setTextSize(value);
            } else if (type == PROP_BACKGROUND_COLOR) {
                textView.setBackgroundColor(value);
            }
        }
    }

    @Override
    public void configure(@NonNull View view, int type, float value) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (type == PROP_TEXT_SIZE) {
                textView.setTextSize(value);
            }
        }
    }
}

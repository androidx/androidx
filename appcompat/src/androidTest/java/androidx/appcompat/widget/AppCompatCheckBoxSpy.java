/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.VisibleForTesting;

/**
 * CompoundDrawable.getButtonDrawable() method was only added in API23. This class exposes the
 * mButton drawable for testing.
 */
public class AppCompatCheckBoxSpy extends AppCompatCheckBox {

    @VisibleForTesting
    Drawable mButton;

    public AppCompatCheckBoxSpy(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setButtonDrawable(Drawable buttonDrawable) {
        super.setButtonDrawable(buttonDrawable);
        mButton = buttonDrawable;
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.graphics;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.graphics.drawable.TintAwareDrawable;

public class TestTintAwareDrawable extends BitmapDrawable implements TintAwareDrawable {

    public TestTintAwareDrawable() {
        super();
    }

    @Override
    public void setTint(int tintColor) {
        // no-op so that the method isn't abstract on API 20 and below
    }

    @Override
    public void setTintList(ColorStateList tint) {
        // no-op so that the method isn't abstract on API 20 and below
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        // no-op so that the method isn't abstract on API 20 and below
    }
}
